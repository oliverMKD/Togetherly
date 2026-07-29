# Design system

Togetherly's shared theme and component library live in `com.togetherly.designsystem`
(`shared/src/commonMain`) — one Compose Multiplatform implementation for Android and iOS, no
per-platform UI.

## Token structure

```
designsystem/
├── color/       TogetherlyColors — semantic roles (backgroundCanvas, actionPrimary, error,
│                categoryTalk..categoryMemories, ...), light/dark palettes, primitives
├── typography/  TogetherlyTypography — type roles (displayL..labelM), all sp-based so every
│                role scales with the system font setting
├── spacing/     TogetherlySpacing — a 4/8dp rhythm (xxs..xxl)
├── size/        TogetherlySize — cross-screen reusable sizes (minimumTouchTarget = 48dp,
│                buttonHeight, contentMaxWidth, ...) — never a single-use, screen-specific size
├── shape/       TogetherlyShapes — small/medium/large/card/pill/circular
├── motion/      TogetherlyMotion — duration tokens + the reduced-motion seam (LocalReduceMotion)
├── theme/       TogetherlyTheme — the one entry point wrapping all of the above, plus the
│                Material 3 colorScheme/typography/shapes mapping stock Material components need
└── component/   the component library — see below
```

Every token is a **role**, named for its purpose ("the largest hero moment on a screen"), never
for a screen or a raw value ("onboardingTitleSize"). Feature code always reads
`MaterialTheme.togetherly*` — `togetherlyColors`, `togetherlyTypography`, `togetherlySpacing`,
`togetherlySize`, `togetherlyShapes`, `togetherlyMotion` — never a primitive color, a hardcoded
`.dp`, or a raw `TextStyle`/`RoundedCornerShape`. The one accepted exception is a purely
decorative, single-use measurement local to one component (e.g. a placeholder illustration's own
circle sizes) — see `TogetherlySize`'s own KDoc for exactly that boundary.

## Component library (`designsystem.component`)

```
component/
├── button/      TogetherlyPrimaryButton, TogetherlySecondaryButton, TogetherlyTextButton,
│                TogetherlyIconButton
├── card/        TogetherlyCard, TogetherlySelectableCard
├── input/       TogetherlyTextField
├── selection/   TogetherlyChoiceChip, TogetherlyChipFlowRow
├── progress/    TogetherlyStepProgress
├── navigation/  TogetherlyTopBar, TogetherlyBottomNavigationBar, TogetherlyDestination
├── feedback/    TogetherlyLoadingIndicator, TogetherlyInlineError
└── layout/      TogetherlyScreen
```

## Component rules

Every component:

- Uses semantic tokens only (see above).
- Supports light/dark and dynamic font scaling (typography is `sp`-based throughout).
- Exposes a `Modifier` parameter.
- Owns no feature state, no `ViewModel`, no Koin lookup, no navigation — a component's inputs are
  plain data and callbacks, full stop.
- Never hardcodes feature/product copy — text is always a parameter, or (for a component's own
  small fixed vocabulary, like a loading announcement or "Retry") a Compose resource string owned
  by the design system, documented as such in `strings.xml`'s own header comment.
- Provides real accessibility semantics: correct `Role` (`RadioButton` for single-select,
  `Checkbox` for multi-select — `TogetherlySelectableCard`'s `multiSelect` parameter exists exactly
  for this), `selected` state, error/loading announcements, and a ≥48dp touch target even when the
  visual control is smaller (`Modifier.minimumInteractiveComponentSize()`).
- Never communicates selection through color alone — a border, a shape/fill change, or a check
  indicator always accompanies the color change.
- Doesn't duplicate a stock Material component without adding real value — see `TogetherlyChoiceChip`'s
  own KDoc for what "wrapping Material's `FilterChip` is worth it" looks like versus reinventing
  one from scratch.

`TogetherlyScreen` (`layout/`) is the shared per-screen scaffold: background, safe-area/inset
handling, an optional top bar, scrollable content, a stable bottom action area, and a maximum
readable content width on large windows — built from `Box`/`Column`, deliberately not
`material3.Scaffold`, so a screen that wants to be fully edge-to-edge is never fighting scaffold
assumptions it didn't ask for.

## Adding a new design-system component

1. Pick the right subpackage above (or, if the library is still small, avoid adding a new one
   unless the component genuinely doesn't fit an existing category).
2. Read semantic values only from `MaterialTheme.togetherly*` — never a primitive.
3. Take a `Modifier` parameter; apply the caller's `modifier` first, your own chrome after.
4. Add previews for every meaningful state (including light/dark and, for anything text-heavy or
   interactive, a `fontScale = 2f` variant).
5. Add Compose UI tests (`androidDeviceTest`, following this project's existing convention — see
   `docs/architecture.md`'s testing conventions for why instrumented rather than `commonTest`) for
   interaction, disabled/loading states as applicable, minimum touch target, and selection
   semantics if the component is selectable.
6. If the component wraps a stock Material composable, document in its KDoc what value the wrapper
   adds — token mapping, a consistent default, real accessibility semantics the raw component
   didn't have — not just "so we have our own name for it."
