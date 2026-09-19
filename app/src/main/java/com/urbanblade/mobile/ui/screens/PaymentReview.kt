package com.urbanblade.mobile.ui.screens

import kotlin.math.abs

/** Resultado de comparar lo transferido contra el precio de lista del servicio. */
enum class TransferCheck { Match, Differs, Unknown }

/** Diferencia máxima (en pesos) que se considera "mismo monto"; cubre redondeos y centavos. */
private const val TOLERANCE = 0.5

/**
 * Compara el monto del comprobante con el precio de lista. Una diferencia no es un error por sí sola:
 * el cobro final puede llevar descuento de nivel, puntos o membresía. Solo se le muestra al personal
 * para que decida con más contexto.
 */
fun compareTransfer(monto: Double, precioLista: Double?): TransferCheck = when {
    precioLista == null || precioLista <= 0.0 -> TransferCheck.Unknown
    abs(monto - precioLista) <= TOLERANCE -> TransferCheck.Match
    else -> TransferCheck.Differs
}

/** Si el monto que leyó el OCR no coincide con el registrado, conviene que el personal lo revise. */
fun ocrDisagrees(monto: Double, ocrMonto: Double?): Boolean = ocrMonto != null && abs(monto - ocrMonto) > TOLERANCE
