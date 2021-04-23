fun getCurrentTS(): Long {
    return System.currentTimeMillis()
}

val GOOD_CHARS = listOf('_', ' ', '"', '\'', '-', ',', '.', '!', '?')
val VERY_GOOD_CHARS = listOf('_')

fun isGoodStr(s: String, maybeEmpty: Boolean = false): Boolean {
    return (maybeEmpty || s.isNotEmpty()) && s.all {
        it.isLetterOrDigit() || it in GOOD_CHARS
    }
}

fun isVeryGoodStr(s: String, maybeEmpty: Boolean = false): Boolean {
    return (maybeEmpty || s.isNotEmpty()) && s.all {
        it.isLetterOrDigit() || it in VERY_GOOD_CHARS
    }
}
