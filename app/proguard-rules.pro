# SeerrCtrl R8 rules. Libraries (OkHttp, Coil, coroutines, WorkManager, Compose) bring their own.

# Keep file and line numbers so crash reports point at real code (decode names with mapping.txt from the CI run).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Background check and notification buttons are created by the system from their class names.
-keep class com.vknxworks.seerrctrl.notify.** { *; }

# Optional extension APIs that androidx.window looks for at runtime on some devices.
-dontwarn androidx.window.extensions.**
-dontwarn androidx.window.sidecar.**
