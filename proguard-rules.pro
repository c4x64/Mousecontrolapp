# Keep Shizuku-related classes
-keep class rikka.shizuku.** { *; }

# Keep JNI methods
-keepclasseswithmembers class * {
    native <methods>;
}

# Keep MainActivity and MouseService
-keep public class com.mouselock.app.** { *; }