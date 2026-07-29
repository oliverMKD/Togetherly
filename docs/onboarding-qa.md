# Onboarding & Bootstrap — Manual QA Checklist

Manual verification for the bootstrap → onboarding → Main flow (Steps 7.1–7.6). Automated tests
cover the same ground programmatically (see [architecture.md](architecture.md)'s testing
conventions and each feature's own test files) — this checklist exists for what only a real
device/simulator, a real screen reader, and a real human can confirm: how it actually *feels*, and
the platform-integration behaviors (rotation, backgrounding, gestures) automated tests in this
project can't exercise without a connected device.

Run through the whole list on **both** a small phone and a large phone/tablet-sized window, in
**both** light and dark mode, at least once each. Re-run the "Fresh install" section any time
onboarding's screens or validation rules change.

## Fresh install

- [ ] First launch shows a branded loading state, then either Welcome (no profile) or Today
      (existing profile from a previous debug install) — never a flash of the wrong one.
- [ ] Welcome → Family name → Age bands → Interests → Preferences → Reminder → Review → Main
      completes end to end with a real tap-through, not just automated clicks.
- [ ] After creating a family, killing and reopening the app lands directly on Main — never back
      on Welcome.

## Light / dark

- [ ] Every onboarding step, Bootstrap's loading/error state, and Main's shell look intentional
      (not just "technically the right colors") in both themes.
- [ ] Selected chips/cards are visually distinguishable from unselected ones in both themes without
      relying on being told which is which.

## Small / large device

- [ ] On the smallest phone available, no onboarding step clips or requires horizontal scrolling.
- [ ] On a tablet or a resized/large window, content stays a readable width and doesn't stretch
      edge-to-edge into an uncomfortable line length.

## Large font

- [ ] Set the system font size to its largest supported step (or largest + "display size" on
      Android). Confirm on Welcome, Family name, Preferences (the most content-dense step) and
      Review specifically:
      - [ ] The primary action button is still reachable without being pushed off-screen.
      - [ ] No text is truncated or overlapping another element.

## Screen reader (TalkBack / VoiceOver)

- [ ] Turn on TalkBack (Android) or VoiceOver (iOS, when available). Swipe through an entire
      onboarding step and confirm:
      - [ ] Every interactive control announces a meaningful label (not "button" alone).
      - [ ] Selected chips/cards announce their selected state.
      - [ ] The step-progress indicator announces "Step X of Y".
      - [ ] A validation error is announced when it appears (not only shown visually).
      - [ ] Decorative content (Welcome's abstract visual, chip color dots) is silent — it's never
            read aloud or focusable.
- [ ] Confirm swipe order roughly matches visual top-to-bottom order on each step.

## Keyboard

- [ ] On a device/emulator with a hardware or Bluetooth keyboard, Tab through the Family name step
      and confirm the text field, Continue, and Skip are all reachable and operable via keyboard
      alone.

## Back gestures

- [ ] System back gesture (or hardware/nav-bar back) on Welcome exits/dismisses onboarding — it
      never silently does nothing.
- [ ] System back on any later onboarding step returns to the previous step, preserving whatever
      was already entered.
- [ ] System back on Main's Today tab follows platform-default behavior (does not return to
      onboarding under any circumstance, including immediately after finishing it).

## Rotation (where supported)

- [ ] Rotate the device mid-onboarding (e.g. on Age bands, after selecting one). Confirm the
      selection and current step both survive the rotation.

## App background/foreground

- [ ] Background the app mid-onboarding (home button, not kill), reopen it, and confirm the draft
      is exactly as left.

## Kill and reopen

- [ ] Force-stop the app mid-onboarding, reopen it. (Process-death persistence of an *incomplete*
      draft is explicitly optional for this MVP — confirm it restarts cleanly at Welcome rather
      than crashing or showing a broken partial state; losing the draft itself is acceptable.)

## Persistence

- [ ] After finishing onboarding, the family's choices show up correctly summarized on the Review
      step exactly as selected before tapping "Start our first adventure" (nothing silently reset).

## Failure / retry

- [ ] With the device offline or storage otherwise interfered with (if there's a way to simulate
      a write failure in a debug build), confirm Review shows an inline error with a Retry action,
      every selection is still there, and Retry succeeds once the interference is removed.

## Data deletion

- [ ] Using whatever debug/dev entry point exists for "delete all family data," delete the family,
      then relaunch. Confirm the app returns to Welcome, not Main and not a broken state.
