package com.subramanya.artha.data.entity.enums

/**
 * How an investment's current value is determined.
 *  - DERIVED: deposits (FD/RD/PPF/EPF/Bonds). value = contributions + posted interest.
 *  - MARKET:  market instruments. value = the manually-entered current price.
 */
enum class ValuationMode { DERIVED, MARKET }

/** Sensible default mode for a freshly-created investment of this type. */
fun InvestmentType.defaultValuationMode(): ValuationMode = when (this) {
    InvestmentType.FD, InvestmentType.RD, InvestmentType.PPF,
    InvestmentType.EPF, InvestmentType.BONDS,
    -> ValuationMode.DERIVED
    else -> ValuationMode.MARKET
}
