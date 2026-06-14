package com.khaata.app.parser

import com.khaata.app.domain.model.ParseResult

/**
 * Turns one AI-parsed example into a reusable regex template (docs/04-lld.md §8). It locates
 * the dynamic field spans (amount, account, ref, merchant) in the raw text, replaces them
 * with named capture groups, and **generalizes the static remainder** — runs of digits
 * become `\d+` (dates, times, balances) and runs of whitespace become `\s+`, while the
 * literal words that define the format are frozen. The result matches future messages of
 * the same shape with different values.
 *
 * This is what makes the parser self-learning: each unseen format costs exactly one AI call.
 */
object TemplateInducer {

    private const val AMOUNT_GROUP = """(?<amount>[0-9][0-9,]*(?:\.[0-9]{1,2})?)"""
    private const val ACCT_GROUP = """(?<acct>\d{3,4})"""
    private const val REF_GROUP = """(?<ref>[A-Za-z0-9]{6,20})"""

    private data class Span(val start: Int, val end: Int, val fragment: String)

    fun induce(rawText: String, result: ParseResult): NotificationTemplate? {
        val direction = result.direction ?: return null
        val spans = mutableListOf<Span>()

        // Amount is mandatory — we locate the numeric token via the shared extractor regex.
        val amountRange = AmountExtractor.numericRange(rawText) ?: return null
        spans += Span(amountRange.first, amountRange.last + 1, AMOUNT_GROUP)

        // Account: the last-4 digits, if present outside the amount span.
        result.maskedAccount?.takeLast(4)?.let { last4 ->
            digitRunRange(rawText, last4, exclude = spans)?.let { spans += Span(it.first, it.last + 1, ACCT_GROUP) }
        }

        // Ref id, if present outside existing spans.
        result.refId?.let { ref ->
            literalRange(rawText, ref, exclude = spans)?.let { spans += Span(it.first, it.last + 1, REF_GROUP) }
        }

        // Merchant: variable free text. Greedy if it runs to the end, else lazy.
        result.rawMerchant?.let { merchant ->
            literalRange(rawText, merchant, exclude = spans)?.let { range ->
                val tailIsBlank = rawText.substring(range.last + 1).isBlank()
                val frag = if (tailIsBlank) "(?<merchant>.+)" else "(?<merchant>.+?)"
                spans += Span(range.first, range.last + 1, frag)
            }
        }

        spans.sortBy { it.start }

        val pattern = buildString {
            var pos = 0
            for (s in spans) {
                if (s.start < pos) continue // skip overlaps defensively
                append(staticToRegex(rawText.substring(pos, s.start)))
                append(s.fragment)
                pos = s.end
            }
            append(staticToRegex(rawText.substring(pos)))
        }

        return NotificationTemplate(
            id = "induced",
            direction = direction,
            pattern = runCatching { Regex(pattern, RegexOption.IGNORE_CASE) }.getOrNull() ?: return null,
            confidence = LEARNED_CONFIDENCE,
        )
    }

    /** Freezes literal words but wildcards digit runs (`\d+`) and whitespace runs (`\s+`). */
    private fun staticToRegex(s: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            when {
                c.isDigit() -> { sb.append("""\d+"""); while (i < s.length && s[i].isDigit()) i++ }
                c.isWhitespace() -> { sb.append("""\s+"""); while (i < s.length && s[i].isWhitespace()) i++ }
                else -> { if (c in "\\.[]{}()*+?^$|/") sb.append('\\'); sb.append(c); i++ }
            }
        }
        return sb.toString()
    }

    private fun literalRange(text: String, sub: String, exclude: List<Span>): IntRange? {
        if (sub.isEmpty()) return null
        var from = 0
        while (true) {
            val idx = text.indexOf(sub, from, ignoreCase = true)
            if (idx < 0) return null
            val range = idx until (idx + sub.length)
            if (exclude.none { overlaps(range, it) }) return range
            from = idx + 1
        }
    }

    private fun digitRunRange(text: String, digits: String, exclude: List<Span>): IntRange? {
        var from = 0
        while (true) {
            val idx = text.indexOf(digits, from)
            if (idx < 0) return null
            val before = idx - 1
            val after = idx + digits.length
            val standalone = (before < 0 || !text[before].isDigit()) && (after >= text.length || !text[after].isDigit())
            val range = idx until after
            if (standalone && exclude.none { overlaps(range, it) }) return range
            from = idx + 1
        }
    }

    private fun overlaps(range: IntRange, span: Span): Boolean =
        range.first < span.end && span.start <= range.last

    const val LEARNED_CONFIDENCE = 0.9f
}
