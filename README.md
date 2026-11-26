# Pudhir

**Pudhir** (புதிர், "puzzle") is an offline collection of 20 puzzle games for Android, built to sharpen your mind.
Every game has its own levels, a new daily puzzle, streaks and stars. It needs no internet and shows no pop-up ads.

## Games

| Brain trainers | Quick plays |
| --- | --- |
| Spice Box: sudoku | Brick Kiln: place blocks, fire full lines |
| Rangoli Threads: join colours, fill the floor | Temple Tiles: match free tile pairs |
| Handloom: weave the hidden pattern (nonogram) | Bazaar Trio: collect three of a kind |
| Bloom: balance two flowers (binary puzzle) | Chai Sort: pour until each glass is one colour |
| Tea Estate: one bush per row, column and plot | Bolt Bazaar: unscrew bolts into colour boxes |
| Auto Ride: one route through every square | Mithai Match: swap sweets, line up three |
| Clockwork: set every geared clock to twelve | Gold Merge: slide and merge coins |
| Hidden Wells: logic sweeper, no guessing | Postcards: swap tiles, restore the picture |
| Godown: push every crate home | Word Hunt: find the hidden words |
| Auto Jam: free the golden auto | Royal Patience: classic card solitaire |

## Project layout

- `core/`: pure Kotlin game logic, level generators and solvers. Every generated level is checked to be solvable
  (logic puzzles have exactly one answer). Run the tests with `PUDHIR_CORE_ONLY=1 ./gradlew :core:test`.
- `app/`: the Android app (Jetpack Compose).
- `.github/workflows/android.yml`: builds and tests in the cloud on every push, then publishes the test APK as a GitHub pre-release.

## Stars, hints and skips

Players start with 3 bonus stars and earn 1 to 3 stars per solved level. A hint costs 1 star and skipping a level
costs 3. Only when a player has no stars and taps Hint or Skip are they offered an optional rewarded video.
Ads are not wired up yet; test builds grant the reward directly.
