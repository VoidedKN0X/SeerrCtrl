# SeerrCtrl R8 rules. Libraries (OkHttp, Coil, coroutines, WorkManager, Compose) bring their own.

# The background check is created by WorkManager from its class name with this constructor. The receivers and
# the service in the notify package are kept through the manifest; nothing else there needs to keep its name.
-keep class com.vknxworks.seerrctrl.notify.CheckWorker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Optional extension APIs that androidx.window looks for at runtime on some devices.
-dontwarn androidx.window.extensions.**
-dontwarn androidx.window.sidecar.**

# Release builds drop verbose, debug and info logging (SeerrCtrl's own code doesn't log; this also covers libraries).
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# WorkManager keeps its job list in a Room 2.6.1 database, and Room creates the generated
# WorkDatabase_Impl by reflection through its no-argument constructor. Room 2.6.1's own rule
# (-keep class * extends androidx.room.RoomDatabase) names no members, and the R8 in Android
# Gradle Plugin 9 no longer keeps a constructor for such a rule, so without this line the app
# would crash at start (NoSuchMethodException: WorkDatabase_Impl.<init>). Same rule Room 2.7 ships.
-keep class * extends androidx.room.RoomDatabase { void <init>(); }
