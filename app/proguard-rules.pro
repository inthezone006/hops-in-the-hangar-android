# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/rahulmenon/Library/Android/sdk/tools/proguard/proguard-android.txt

# Keep Kotlin Serialization classes and their properties
-keepattributes *Annotation*, EnclosingMethod, InnerClasses, Signature
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable *;
}

# Keep the data models from being obfuscated if they are used by name in JSON parsing
# (though Kotlin Serialization usually handles this, it's safer for your event_data.json)
-keep class com.rahul.hopsinthehangar.EventData { *; }
-keep class com.rahul.hopsinthehangar.SponsorItem { *; }
-keep class com.rahul.hopsinthehangar.VendorItem { *; }
-keep class com.rahul.hopsinthehangar.ScheduleItem { *; }
-keep class com.rahul.hopsinthehangar.GeneralInfo { *; }
-keep class com.rahul.hopsinthehangar.HotelItem { *; }
-keep class com.rahul.hopsinthehangar.FaqItemData { *; }
-keep class com.rahul.hopsinthehangar.SponsorLink { *; }

# OSMDroid requires some keeps for its custom views and configuration
-keep class org.osmdroid.** { *; }

# Media3 / ExoPlayer
-keep class androidx.media3.** { *; }

# If you use reflection for resource lookup by name, keep those strings from being stripped
# (Already handled by your getResourceName function as it uses strings directly)
