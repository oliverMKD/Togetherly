package com.togetherly.domain.family

/**
 * The age compatibility required for quest recommendations — not a child identity.
 * Togetherly never stores a child's name, birth date, or any other personal data;
 * a household is represented only by which age bands are present in it.
 */
enum class AgeBand {
    AGE_6_TO_8,
    AGE_9_TO_11,
    AGE_12_TO_13,
}
