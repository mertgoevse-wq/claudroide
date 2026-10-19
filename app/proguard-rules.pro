# ClauDroide — R8/ProGuard rules for the release build.
#
# Goal: keep the release APK small without breaking Compose, Kotlin
# reflection-free serialization, or the security layer. Add a rule only with a
# concrete reason; unexplained -keep entries defeat shrinking.

# --- Crash reports that map back to source -----------------------------------
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- Kotlin metadata needed by reflection-light libraries --------------------
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# --- Jetpack Compose ---------------------------------------------------------
# Compose ships its own consumer rules; these cover composable lambdas that R8
# cannot trace when they are only referenced from generated code.
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# --- Coroutines --------------------------------------------------------------
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# --- androidx.security.crypto (Tink backend) ---------------------------------
# Tink resolves key managers by name at runtime; stripping them breaks the
# hardware-backed key vault (see SecureKeyStore.kt).
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# --- Keep our own public API surface stable for tests ------------------------
-keep class org.claudroide.app.core.security.** { *; }
