package com.spendio.app.domain.model

enum class Direction { DEBIT, CREDIT }

enum class TxnSource { NOTIFICATION, MANUAL }

/** Which tier of the parser produced a result. See docs/04-lld.md §4. */
enum class ParseTier { TIER1_REGEX, TIER2_AI, TIER3_HEURISTIC, MANUAL }

enum class AccountType { BANK, CREDIT_CARD, WALLET, CASH }
