package com.togetherly.navigation.destination

import kotlinx.serialization.Serializable

/**
 * The four tabs [com.togetherly.navigation.shell.MainShell]'s bottom navigation switches between.
 * Each is a flat, single-screen destination for now — no nested sub-graphs — since detail screens
 * (a quest, a journey entry) are explicitly out of scope for this step; adding one later means
 * turning that tab into its own nested graph the same way [RootDestination.Onboarding] already is,
 * not changing how the bottom bar itself works.
 *
 * These are intentionally a different type from
 * [com.togetherly.designsystem.component.navigation.TogetherlyDestination] (the design system's
 * navigation-agnostic bottom-nav model) — [MainShell][com.togetherly.navigation.shell.MainShell]
 * is exactly the seam that maps between the two.
 */
sealed interface MainDestination {

    @Serializable
    data object Today : MainDestination

    @Serializable
    data object Explore : MainDestination

    @Serializable
    data object Journey : MainDestination

    @Serializable
    data object Family : MainDestination
}
