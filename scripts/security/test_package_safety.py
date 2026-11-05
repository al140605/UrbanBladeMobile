import tempfile
from pathlib import Path
import unittest
import zipfile
from check_android_package import PackageSafetyError, verify_archive, verify_manifest, verify_build_config


class PackageSafetyTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.root = Path(self.directory.name)
        self.addCleanup(self.directory.cleanup)

    def manifest(self, attributes='', extra=''):
        path = self.root / 'AndroidManifest.xml'
        path.write_text('<manifest xmlns:android="http://schemas.android.com/apk/res/android">'
                        + '<application android:usesCleartextTraffic="false" ' + attributes + '/>'
                        + extra + '</manifest>', encoding='utf-8')
        return path

    def apk(self, name='classes.dex', content=b'ordinary content'):
        path = self.root / 'app.apk'
        with zipfile.ZipFile(path, 'w') as archive:
            archive.writestr('AndroidManifest.xml', b'compiled manifest placeholder')
            archive.writestr(name, content)
        return path

    def test_accepts_non_debug_manifest_and_ordinary_archive(self):
        verify_manifest(self.manifest('android:debuggable="false"'))
        verify_archive(self.apk())

    def test_rejects_debuggable_and_test_only(self):
        for flag in ('debuggable', 'testOnly'):
            with self.subTest(flag=flag), self.assertRaises(PackageSafetyError):
                verify_manifest(self.manifest('android:' + flag + '="true"'))

    def test_rejects_cleartext(self):
        path = self.manifest()
        path.write_text(path.read_text().replace('Traffic="false"', 'Traffic="true"'))
        with self.assertRaises(PackageSafetyError):
            verify_manifest(path)

    def test_rejects_log_reading_permission(self):
        with self.assertRaises(PackageSafetyError):
            verify_manifest(self.manifest(extra='<uses-permission android:name="android.permission.READ_LOGS"/>'))

    def test_rejects_private_files(self):
        for name in ('assets/local.properties', 'assets/.env', 'assets/upload.jks', 'assets/debug.log'):
            with self.subTest(name=name), self.assertRaises(PackageSafetyError):
                verify_archive(self.apk(name))

    def test_sensitive_pattern_across_chunks_is_rejected_without_printing_value(self):
        secret = b'sk_' + b'test_' + b'A' * 32
        path = self.apk(content=b'x' * (1024 * 1024 - 5) + secret)
        with self.assertRaises(PackageSafetyError) as caught:
            verify_archive(path)
        self.assertNotIn(secret.decode(), str(caught.exception))

    def test_non_debug_endpoint_requires_configured_https(self):
        path = self.root / 'BuildConfig.java'
        for url in ('http://127.0.0.1:8001/api/v1/', 'https://PENDIENTE_CONFIGURAR.example/api/v1/'):
            path.write_text('boolean DEBUG = false; String API_BASE_URL = "' + url + '";', encoding='utf-8')
            with self.subTest(url=url), self.assertRaises(PackageSafetyError):
                verify_build_config(path)
        path.write_text('boolean DEBUG = false; String API_BASE_URL = "https://api.example.test/api/v1/";', encoding='utf-8')
        verify_build_config(path)


if __name__ == '__main__':
    unittest.main()
