# Neo-Brutalism Theme Revamp Implementation Plan

Revamp the "Hops in the Hangar" app from its current Aesthetic Navy theme to a vibrant, high-contrast **Neo-Brutalism** design system.

## User Review Required

- **Visual Style**: Neo-Brutalism features stark black borders (3.dp solid black), sharp/minimal corner radii (4.dp - 8.dp), vibrant high-contrast accent colors (Neo Yellow, Hot Pink, Mint Green, Electric Blue, Cream background), and bold typography.
- **Components**: Cards, buttons, top bar, bottom navigation, search bars, and bottom sheets will be refactored to incorporate thick black outlines and bold brutalist aesthetics.

## Proposed Changes

### Theme & Design System (`ui/theme/`)

#### [MODIFY] [Color.kt](file:///Users/rahulmenon/Programming/hops-in-the-hangar-android/app/src/main/java/com/rahul/hopsinthehangar/ui/theme/Color.kt)
- Define Neo-Brutalism color palette:
  - `NeoCream = Color(0xFFFFFDF5)` (background)
  - `NeoBlack = Color(0xFF000000)` (borders, text)
  - `NeoYellow = Color(0xFFFFDE59)` (primary accent)
  - `NeoPink = Color(0xFFFF69B4)` (secondary accent)
  - `NeoGreen = Color(0xFF7ED957)` (highlight)
  - `NeoBlue = Color(0xFF38BDF8)` (info accent)
  - `NeoPurple = Color(0xFFD8B4FE)` (premier sponsor highlight)
  - `NeoWhite = Color(0xFFFFFFFF)` (card surface)

#### [MODIFY] [Theme.kt](file:///Users/rahulmenon/Programming/hops-in-the-hangar-android/app/src/main/java/com/rahul/hopsinthehangar/ui/theme/Theme.kt)
- Update `LightColorScheme` and `DarkColorScheme` to map to Neo-Brutalism colors (`NeoCream`, `NeoBlack`, `NeoYellow`, `NeoPink`, `NeoWhite`, etc.).

#### [MODIFY] [Type.kt](file:///Users/rahulmenon/Programming/hops-in-the-hangar-android/app/src/main/java/com/rahul/hopsinthehangar/ui/theme/Type.kt)
- Emphasize bold typography (`FontWeight.Black`, `FontWeight.ExtraBold`, uppercase headings).

---

### App UI & Screens (`MainActivity.kt`)

#### [MODIFY] [MainActivity.kt](file:///Users/rahulmenon/Programming/hops-in-the-hangar-android/app/src/main/java/com/rahul/hopsinthehangar/MainActivity.kt)
- Create helper components (`NeoCard`, `NeoButton`, `NeoTextField`) with 3.dp solid black borders and 4.dp/8.dp corner radii.
- Revamp `HomeScreen`, `SponsorsScreen`, `VendorsScreen`, `EntertainmentScreen`, `DetailScreen`, and `FaqSection` to use Neo-Brutalism cards, vibrant color blocks, and bold typography.
- Update TopAppBar, BottomBar, and FloatingActionButton with stark black borders and neo-brutalist styling.

## Verification Plan

### Automated Tests
- Run Gradle build (`app:assembleDebug`) to ensure clean compilation and zero syntax/resource errors.

### Manual Verification
- Deploy app to emulator/device or inspect Compose Previews to verify the Neo-Brutalism aesthetic (thick borders, high-contrast colors, bold typography).
