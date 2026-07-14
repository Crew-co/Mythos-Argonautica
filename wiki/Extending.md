# Argonautica — Extending

## What it reaches into

**Orpheus** (ChthonicRealm) was on the Argo, so he gains `row`.

## The hole it opens in itself

**`argo:landfalls`** → contribute a `Landfall`.

A place the Argo stops. Five ship with the addon; the whole point of the poem is that it's a *list*, and lists can be added to. Talos on the beach. The Sirens. The Harpies.

```kotlin
compileOnly("net.crewco:argonautica:0.1.0")   // for the type
// addon.yml:  depends: [ Argonautica ]

mythos.extensions.contribute(Landfalls.POINT, ...)
```

**Load order does not matter.** `consume` replays every contribution already posted and receives
every one posted afterwards — so your jar may load before or after this one, and neither cares.
