# AetherTV R8 / ProGuard Rules

# Preserve Media3 player views and custom attributes
-keep class androidx.media3.ui.** { *; }
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.datasource.** { *; }

# Keep data models
-keep class com.aethertv.v2.data.** { *; }

# OkHttp optional platform classes
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
-dontwarn org.bouncycastle.**

# Coil & Haze
-dontwarn coil.**
-dontwarn dev.chrisbanes.haze.**
