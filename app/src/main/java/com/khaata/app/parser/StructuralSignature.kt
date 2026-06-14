package com.khaata.app.parser

/**
 * A coarse fingerprint of a message's *shape* — the sequence of character classes
 * (Word / Digit / Space / Punct), with consecutive runs collapsed. Two notifications of
 * the same format share a signature, so we never ask the AI to parse the same shape twice.
 */
object StructuralSignature {
    fun of(text: String): String {
        val sb = StringBuilder()
        var last = ' '
        for (c in text) {
            val cls = when {
                c.isLetter() -> 'W'
                c.isDigit() -> 'D'
                c.isWhitespace() -> 'S'
                else -> 'P'
            }
            if (cls != last) {
                sb.append(cls)
                last = cls
            }
        }
        return sb.toString()
    }
}
