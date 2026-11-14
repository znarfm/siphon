# Keep Room Database implementations
-keep class * extends androidx.room.RoomDatabase {
    void <init>();
}

# Keep WebView @JavascriptInterface methods
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep Service, JobService & TileService entry points
-keep class * extends android.app.Service {
    public void <init>();
}
-keep class * extends android.app.job.JobService {
    public void <init>();
}
-keep class * extends android.service.quicksettings.TileService {
    public void <init>();
}

# OkHttp & Kotlin Coroutines R8 full mode compatibility
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# libtorrent4j JNI/SWIG bindings and native callbacks
-keep class org.libtorrent4j.swig.** { *; }
-keep interface org.libtorrent4j.AlertListener { *; }
-keepclasseswithmembers class * {
    native <methods>;
}
-dontwarn org.libtorrent4j.**

# NativeAdblockJni JNI bindings
-keep class com.znarfm.siphon.core.adblock.NativeAdblockJni { *; }
