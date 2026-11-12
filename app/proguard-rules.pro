# Keep Room entities, DAOs, and Database implementations
-keep class com.znarfm.siphon.data.db.** { *; }
-keep class * extends androidx.room.RoomDatabase {
    void <init>();
}

# Keep WebView @JavascriptInterface methods
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep Service & TileService entry points
-keep class com.znarfm.siphon.core.service.** { *; }

# OkHttp & Kotlin Coroutines R8 full mode compatibility
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# libtorrent4j JNI and wrapper classes
-keep class org.libtorrent4j.** { *; }
-dontwarn org.libtorrent4j.**
