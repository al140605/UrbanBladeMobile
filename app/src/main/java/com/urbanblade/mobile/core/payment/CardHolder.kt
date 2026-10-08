package com.urbanblade.mobile.core.payment

/** Largo máximo del nombre impreso en una tarjeta. */
const val HOLDER_MAX_LENGTH = 26

/**
 * Nombre del titular como se imprime en una tarjeta: mayúsculas, solo letras (con acentos), espacios,
 * apóstrofo, guion y punto, sin espacios repetidos ni al inicio, y con el largo máximo de una tarjeta.
 * Se aplica mientras la persona escribe.
 */
private val APOSTROPHE = "'"[0]

fun normalizeHolderName(raw: String): String {
    val out = StringBuilder()
    for (c in raw.uppercase()) {
        when {
            c.isLetter() || c == APOSTROPHE || c == '-' || c == '.' -> out.append(c)
            // Un solo espacio entre palabras (también al final, para poder seguir escribiendo el apellido).
            c == ' ' && out.isNotEmpty() && out.last() != ' ' -> out.append(' ')
        }
    }
    return out.toString().take(HOLDER_MAX_LENGTH)
}

/** Nombre y apellido: al menos 3 letras en total y una separación entre palabras. */
fun isValidHolderName(name: String): Boolean {
    val trimmed = name.trim()
    return trimmed.count { it.isLetter() } >= 3 && trimmed.contains(' ') && trimmed.split(' ').all { it.any(Char::isLetter) }
}

/** Mensaje cuando falta o es inválido el nombre del titular; null si está bien. */
fun holderNameProblem(name: String): String? =
    if (isValidHolderName(name)) null else "Escribe el nombre y apellido como aparecen en la tarjeta."

/** Solo los dígitos de lo que escribió la persona, hasta el máximo de una tarjeta (19). */
fun cardDigits(raw: String): String = raw.filter(Char::isDigit).take(19)

/** American Express agrupa 4-6-5 y tiene 15 dígitos; el resto, 4-4-4-4 con 16. */
private fun groups(brand: String): List<Int> = if (brand.equals("amex", ignoreCase = true)) listOf(4, 6, 5) else listOf(4, 4, 4, 4)

/**
 * Número para la tarjeta en pantalla: lo escrito agrupado, completado con puntos hasta el largo de la
 * marca («4242 42•• •••• ••••»). Solo se muestra en la propia pantalla del titular mientras escribe.
 */
fun cardNumberPreview(digits: String, brand: String): String {
    val layout = groups(brand)
    val clean = cardDigits(digits)
    var index = 0
    return layout.joinToString(" ") { size ->
        buildString {
            repeat(size) {
                append(clean.getOrNull(index) ?: '•')
                index++
            }
        }
    }
}

/** MM/AA a partir de los dígitos escritos del vencimiento; incompleto se muestra parcial («1», «12/3»). */
fun expiryPreview(raw: String): String {
    val digits = raw.filter(Char::isDigit).take(4)
    return if (digits.length <= 2) digits else digits.take(2) + "/" + digits.drop(2)
}

/**
 * Marca de la tarjeta según los primeros dígitos, con los mismos códigos que usa Stripe (visa, mastercard,
 * amex, discover, jcb, diners, unionpay) y «unknown» mientras no alcanzan para saberlo. Es propia porque
 * `CardBrand.fromCardNumber` de Stripe es una API restringida (el lint la rechaza).
 */
fun detectCardBrand(rawDigits: String): String {
    val digits = cardDigits(rawDigits)
    fun prefix(size: Int): Int? = digits.take(size).takeIf { it.length == size }?.toIntOrNull()
    val p1 = prefix(1)
    val p2 = prefix(2)
    val p3 = prefix(3)
    val p4 = prefix(4)
    val p6 = prefix(6)
    return when {
        p2 == 34 || p2 == 37 -> "amex"
        p1 == 4 -> "visa"
        p2 != null && p2 in 51..55 -> "mastercard"
        p4 != null && p4 in 2221..2720 -> "mastercard"
        p4 == 6011 || p2 == 65 || (p3 != null && p3 in 644..649) || (p6 != null && p6 in 622126..622925) -> "discover"
        p4 != null && p4 in 3528..3589 -> "jcb"
        p2 == 36 || p2 == 38 || p2 == 39 || (p3 != null && p3 in 300..305) -> "diners"
        p2 == 62 -> "unionpay"
        else -> "unknown"
    }
}
