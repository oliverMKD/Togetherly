package com.togetherly.data.telemetry

/**
 * The one narrow boundary any PostHog SDK type may cross — [PostHogProductAnalytics] depends on
 * this interface only, never on `com.posthog.kmp.PostHog` (a global `object`) directly, so tests
 * can substitute a fake instead of depending on real global SDK state. No PostHog type appears in
 * this interface's own signature either (every parameter is a primitive/`Map<String, Any>`), so
 * nothing above [PostHogProductAnalytics] ever needs to know PostHog exists.
 */
internal interface PostHogSdkAdapter {
    fun setup(projectKey: String, host: String?, debug: Boolean)
    fun capture(event: String, properties: Map<String, Any>)
    fun screen(screenName: String, properties: Map<String, Any>)
    fun optIn()
    fun optOut()
    fun reset()
    fun flush()

    /** PostHog's own local anonymous distinct id (see `PostHog.getAnonymousId()`) — never a `identify()`d id, since this app never calls `identify`. `null` before [setup] has ever run. */
    fun anonymousId(): String?
}
