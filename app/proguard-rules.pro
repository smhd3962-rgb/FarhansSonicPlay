# Add project specific ProGuard rules here.
# Keep Media3 classes and session
-keep class androidx.media3.** { *; }

# Keep Room entities and DAOs
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# Keep Kotlin Serialization / Data models
-keepclassmembers class com.farhan.sonicplay.data.model.** { *; }
-keepclassmembers class com.farhan.sonicplay.data.local.entities.** { *; }
