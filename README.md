# HITH (Hops in the Hangar) ✈️🍻

**HITH** (Hops in the Hangar) is the official companion app for the premier Craft Beer & Airshow event at the Middletown Regional Airport (MWO). Designed to enhance the attendee experience, the app provides interactive navigation, real-time schedules, and detailed information about participating breweries, food vendors, and sponsors in a vibrant **Neo-Brutalist** aesthetic.

## 🌟 Features

### 🗺️ Interactive Event Maps
*   **Dual View**: Separate interactive maps for **Inside the Hangar** and **Outside Hangar/Apron** areas.
*   **Custom SVG Engine**: High-performance, custom-built SVG map renderer supporting up to **25x zoom**.
*   **Dynamic Labels & Icons**: Smart icons for breweries, food trucks, restrooms, and amenities that scale and appear as you zoom.
*   **Deep Linking**: Tap any booth on the map to view vendor details, website, and contact info.

### 🍺 Vendor & Sponsor Discovery
*   **Brewery List**: Complete list of participating breweries with descriptions and website links.
*   **Food Truck Finder**: Explore local food vendors and their specialties.
*   **Sponsor Directory**: View and visit the websites of the partners making the event possible.
*   **Favorites**: "Heart" your favorite vendors to highlight them on the map and quickly find them in the list.

### 📅 Event Logistics
*   **Schedule**: Stay up to date with opening jumps, airshow performances, and ground entertainment.
*   **Venue Info**: Quick access to parking directions, event rules, and nearby hotels.
*   **FAQ**: Answers to common questions regarding ticket types, ID requirements, and prohibited items.

### 🎨 Neo-Brutalist UI/UX
*   **Design System**: High-contrast, bold **Neo-Brutalism** styling featuring stark black borders, vibrant accent colors (Neo Yellow, Hot Pink, Mint Green, Electric Blue), and tactile press effects.
*   **Modern Material 3 Integration**: Clean, custom components combining Material 3 foundations with brutalist visual flair.
*   **Edge-to-Edge**: Fully immersive experience with transparent system bars.

## 🛠️ Technologies Used

*   **Language**: [Kotlin](https://kotlinlang.org/)
*   **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose)
*   **Theming**: Custom Material 3 [Neo-Brutalist Theme](app/src/main/java/com/rahul/hopsinthehangar/ui/theme/)
*   **Architecture**: MVVM with Repository pattern
*   **Navigation**: [Jetpack Navigation Compose](https://developer.android.com/jetpack/compose/navigation)
*   **Image Loading**: [Coil](https://coil-kt.github.io/coil/)
*   **Data Persistence**: [Jetpack DataStore](https://developer.android.com/topic/libraries/architecture/datastore) (Preferences)
*   **Maps**: 
    *   [OSMDroid](https://github.com/osmdroid/osmdroid) (for external directions/GPS)
    *   Custom SVG Parser using `XmlPullParser` and Compose `Canvas`
*   **Media**: [Media3 / ExoPlayer](https://developer.android.com/guide/topics/media/media3) (for background video)
*   **Analytics**: [Firebase Analytics](https://firebase.google.com/docs/analytics)
*   **Serialization**: [Kotlin Serialization](https://kotlinlang.org/docs/serialization.html)

## 📂 Project Structure

```text
app/
├── src/main/
│   ├── assets/             # SVG Maps & Event JSON data
│   ├── java/.../
│   │   ├── MainActivity.kt # Main entry point and Navigation
│   │   └── ui/theme/       # Neo-Brutalist Material 3 Theme definitions
│   └── res/                # Drawables (Sponsor/Vendor Logos)
└── build.gradle.kts        # Dependency management
```

## 🚀 Getting Started

1.  **Clone the repository**:
    ```bash
    git clone https://github.com/yourusername/hops-in-the-hangar-android.git
    ```
2.  **Open in Android Studio**:
    Open the project using the latest version of Android Studio (Koala or newer recommended).
3.  **Build & Run**:
    Select the `:app` module and run on a physical device or emulator (API 24+).

---
*Created with ❤️ by the Middletown Aviation Foundation Team.*
