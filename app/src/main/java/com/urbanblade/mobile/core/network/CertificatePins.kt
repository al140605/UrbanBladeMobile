package com.urbanblade.mobile.core.network

import okhttp3.CertificatePinner

/**
 * Certificate pinning de la API (Actividad 09, déficit 2: la app aceptaba cualquier certificado
 * firmado por una CA que el teléfono tuviera instalada, incluida una CA falsa de un atacante
 * que intercepta el Wi-Fi).
 *
 * Se fijan las llaves públicas (SPKI sha256) de las cuatro CA raíz de Amazon y no la del
 * certificado de hoja ni la del intermedio: ACM los renueva cada año y las raíces duran hasta
 * 2038-2040, así que la app no deja de conectar cuando AWS rota el certificado. El dominio usa
 * un certificado de ACM (Amazon RSA 2048 M04 → Amazon Root CA 1).
 *
 * Hashes calculados de los .pem oficiales de https://www.amazontrust.com/repository/
 */
object CertificatePins {
    const val API_HOST = "api.urbanblade.com.mx"

    val AMAZON_ROOT_PINS: List<String> = listOf(
        "sha256/++MBgDH5WGvL9Bcn5Be30cRcL0f5O+NyoXuWtQdX1aI=", // Amazon Root CA 1
        "sha256/f0KW/FtqTjs108NpYj42SrGvOB2PpxIVM8nWxjPqJGE=", // Amazon Root CA 2
        "sha256/NqvDJlas/GRcYbcWE8S/IceH9cq77kg0jVhZeAPXq8k=", // Amazon Root CA 3
        "sha256/9+ze1cZgR9KO1kZrVDxA4HQ6voHRCSVNz4RdTCx4U8U=" // Amazon Root CA 4
    )

    fun build(host: String, pins: List<String>): CertificatePinner =
        CertificatePinner.Builder().add(host, *pins.toTypedArray()).build()

    /** Solo el dominio de producción/staging se fija; localhost y servidores de desarrollo no. */
    fun forApiHost(host: String): CertificatePinner? =
        if (host.equals(API_HOST, ignoreCase = true)) build(API_HOST, AMAZON_ROOT_PINS) else null
}
