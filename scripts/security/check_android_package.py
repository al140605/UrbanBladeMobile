"""Comprueba manifiesto empaquetado y APK sin imprimir valores sensibles."""
import argparse
import json
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET
import zipfile
from urllib.parse import urlparse

ANDROID = "{http://schemas.android.com/apk/res/android}"
SECRET_PATTERNS = {
    "clave privada Stripe": rb"(?:sk|rk)_(?:test|live)_[A-Za-z0-9]{24,}",
    "secreto webhook": rb"whsec_[A-Za-z0-9]{16,}",
    "client secret de pago": rb"pi_[A-Za-z0-9]+_secret_[A-Za-z0-9]{16,}",
    "clave privada": rb"-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----[\r\n]+[A-Za-z0-9+/=\r\n]{64,}",
    "conexion Mongo con credenciales": rb"mongodb(?:\+srv)?://[^\s/@:]+:[^\s/@]+@",
}


class PackageSafetyError(ValueError):
    pass


def verify_manifest(path):
    root = ET.parse(path).getroot()
    application = root.find("application")
    if application is None:
        raise PackageSafetyError("El manifiesto no contiene application")
    for flag in ("debuggable", "testOnly", "usesCleartextTraffic"):
        if application.get(ANDROID + flag, "false") != "false":
            raise PackageSafetyError("Bandera no permitida: " + flag)
    if root.find("instrumentation") is not None:
        raise PackageSafetyError("El paquete contiene instrumentacion de pruebas")
    profile = application.find("profileable")
    if profile is not None and profile.get(ANDROID + "shell") == "true":
        raise PackageSafetyError("El paquete permite perfilado por shell")
    forbidden = {"android.permission.READ_LOGS", "android.permission.SET_DEBUG_APP", "android.permission.DUMP"}
    if any(node.get(ANDROID + "name") in forbidden for node in root.findall("uses-permission")):
        raise PackageSafetyError("El paquete solicita un permiso de diagnostico restringido")


def verify_archive(path):
    with zipfile.ZipFile(path) as archive:
        if "AndroidManifest.xml" not in archive.namelist():
            raise PackageSafetyError("No es un APK valido")
        for entry in archive.infolist():
            name = entry.filename.lower()
            basename = name.rsplit("/", 1)[-1]
            if (basename in {"local.properties", "google-services.json", ".env"}
                    or basename.startswith(".env.")
                    or basename.endswith((".jks", ".keystore", ".log", ".sql", ".dump"))):
                raise PackageSafetyError("El APK contiene un archivo privado o de diagnostico")
            if entry.is_dir():
                continue
            with archive.open(entry) as stream:
                tail = b""
                while chunk := stream.read(1024 * 1024):
                    data = tail + chunk
                    for label, pattern in SECRET_PATTERNS.items():
                        if re.search(pattern, data):
                            raise PackageSafetyError("Patron sensible en APK: " + label)
                    tail = data[-512:]


def verify_build_config(path):
    source = Path(path).read_text(encoding="utf-8")
    debug = re.search(r"boolean DEBUG\s*=\s*([^;]+);", source)
    api = re.search(r'String API_BASE_URL\s*=\s*("[^"\n]*");', source)
    if debug is None or debug.group(1).strip() != "false" or api is None:
        raise PackageSafetyError("BuildConfig no confirma una variante sin depuracion")
    parsed = urlparse(json.loads(api.group(1)))
    if (parsed.scheme != "https" or not parsed.hostname or parsed.username or parsed.password
            or parsed.hostname in {"localhost", "127.0.0.1", "10.0.2.2"}
            or "PENDIENTE" in parsed.hostname.upper()):
        raise PackageSafetyError("API_BASE_URL no es un endpoint HTTPS configurado")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--variant", required=True, choices=("staging", "release"))
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[2]
    variant = args.variant
    task = "process" + variant.title() + "ManifestForPackage"
    manifest = root / "app/build/intermediates/packaged_manifests" / variant / task / "AndroidManifest.xml"
    output = root / "app/build/outputs/apk" / variant
    metadata = json.loads((output / "output-metadata.json").read_text(encoding="utf-8"))
    verify_manifest(manifest)
    verify_build_config(root / "app/build/generated/source/buildConfig" / variant / "com/urbanblade/mobile/BuildConfig.java")
    elements = metadata.get("elements", [])
    if not elements:
        raise PackageSafetyError("No hay APKs generados")
    for element in elements:
        verify_archive(output / element["outputFile"])
    print(variant + ": manifiesto seguro; sin archivos privados ni patrones sensibles detectados en APK")


if __name__ == "__main__":
    try:
        main()
    except (PackageSafetyError, OSError, ValueError, ET.ParseError, zipfile.BadZipFile) as error:
        # No imprimir contenido de archivos, URLs, tokens ni valores capturados.
        print("Comprobacion de paquete fallida: " + (str(error) if isinstance(error, PackageSafetyError) else type(error).__name__), file=sys.stderr)
        sys.exit(1)
