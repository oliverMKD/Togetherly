package com.togetherly.app.application

/**
 * Legal/support links shown from the Family tab's Legal and About destinations. All URLs are
 * **placeholders** — see `docs/legal-configuration.md` for exactly what must be replaced with the
 * real, legally-reviewed documents before release. Never invent a production legal URL here.
 *
 * [supportContactUrl] stays `null` (hiding its row entirely) rather than a fake-looking address
 * until a real one is configured — same "don't fabricate production-looking values" principle
 * used for every other placeholder in this app.
 */
data class LegalConfiguration(
    val privacyPolicyUrl: String,
    val termsOfUseUrl: String,
    val subscriptionTermsUrl: String,
    val supportContactUrl: String?,
) {
    companion object {
        fun placeholder(): LegalConfiguration = LegalConfiguration(
            privacyPolicyUrl = "https://example.com/togetherly/privacy",
            termsOfUseUrl = "https://example.com/togetherly/terms",
            subscriptionTermsUrl = "https://example.com/togetherly/subscription-terms",
            supportContactUrl = null,
        )
    }
}
