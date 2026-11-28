package com.urbanblade.mobile.t053

/**
 * T053 (HT-19): cambio con un ERROR DE LINT A PROPÓSITO para comprobar que el pipeline queda en rojo y no deja
 * integrar el cambio (RNF-19). `java.util.HexFormat` existe desde Android 14 (API 34) y la app soporta desde la
 * API 26 sin revisar la versión: Android Lint lo marca como error «NewApi». NO FUSIONAR: esta rama solo existe para la
 * prueba y se borra al terminar.
 */
object T053ErrorLint {
    fun hex(bytes: ByteArray): String = java.util.HexFormat.of().formatHex(bytes)
}
