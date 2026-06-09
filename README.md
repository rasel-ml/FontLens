![AppBanner](.github/raw/banner.jpeg)

# FontLens

A fully offline Android font viewer and inspector.

## Features
- Browse & preview local font files (.ttf, .otf, .woff, .woff2)
- Live editable preview with font size, bold, italic controls
- Glyph keyboard showing all characters in the font
- Metadata viewer + local non-destructive metadata editor
- Font info panel (bold/italic/condensed support, weight class, etc.)
- Favorites system
- Language-based sample texts with configurable priority
- Settings for glyph display (font-supported only vs full Unicode)

## Building

### In Android Studio
1. Open the `FontLens/` folder
2. Wait for Gradle sync
3. Run ▶ on a device or emulator (min SDK 26)

### Via GitHub Actions
Push to `main` or `master` — the workflow builds a debug APK automatically.
Download it from the **Actions** tab → latest run → **Artifacts**.

### First-time Gradle wrapper setup
If `gradle/wrapper/gradle-wrapper.jar` is missing, run once locally:
```bash
gradle wrapper --gradle-version 8.4
```
This generates the `gradlew` binary and `.jar` needed by CI.

## Project Structure
```
app/src/main/
├── java/com/fontlens/
│   ├── MainActivity.kt
│   ├── data/          FontData.kt, FontRepository.kt
│   ├── utils/         FontParser.kt, FontLoader.kt
│   └── ui/
│       ├── list/      FontListFragment, FavoritesFragment, FontListAdapter
│       ├── preview/   PreviewFragment
│       ├── glyph/     GlyphFragment, GlyphAdapter
│       ├── meta/      MetaFragment, MetaEditFragment, MetaAdapter
│       ├── info/      FontInfoFragment
│       └── settings/  SettingsFragment
└── res/
    ├── layout/        13 XML layouts
    ├── drawable/      Vector icons + shape backgrounds
    ├── navigation/    nav_graph.xml
    ├── menu/          bottom_nav_menu.xml
    └── values/        strings, colors, themes, dimens
```