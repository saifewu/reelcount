# ReelCount

Counts the Facebook Reels you watch and shows the live number in a small pill at your phone's camera notch.

## Get the APK (no Android Studio needed)

1. Create a **private** repository on GitHub and push this folder to its `main` branch.
2. GitHub Actions builds it in a few minutes. Open the repo's **Releases** page on your phone and download `ReelCount.apk`.
3. Every later push makes a new release. It installs over the old app, so your counts are kept.

The first build commits a signing key to `signing/`. That's why the repository should stay private.

## Set up on the phone

1. Install the APK and allow your browser to install unknown apps.
2. Open ReelCount and tap **Open Accessibility**, then switch on ReelCount under *Downloaded apps*.
3. If the switch is greyed out (Android 13+), go to **App info → ⋮ → Allow restricted settings** and try again.
4. Turn off battery optimisation for ReelCount so Android doesn't stop it.

## How counting works

Facebook's view ids are scrambled, so the service looks for the shape of the Reels viewer: a scrolling container that fills the screen, with one item that fills the container. When a swipe settles on a reel it hasn't counted yet during this visit to Facebook, the count goes up. Your first swipe also counts the reel you started on.

If counts look wrong, long-press the big number in the app to see what the detector last decided, then adjust `MIN_CONTAINER_PCT` / `MIN_ITEM_PCT` in `ReelCounterService.kt`.

## Design

Anthropic palette (ivory `#FAF9F5`, near-black `#141413`, clay `#D97757`). Poppins and Lora are open-licence stand-ins for Anthropic's Styrene and Tiempos fonts, downloaded at build time by `scripts/fetch-fonts.sh`.
