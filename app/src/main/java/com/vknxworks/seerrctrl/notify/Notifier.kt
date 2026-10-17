package com.vknxworks.seerrctrl.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.vknxworks.seerrctrl.App
import com.vknxworks.seerrctrl.MainActivity
import com.vknxworks.seerrctrl.R
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

/** Schedules the background check and posts notifications. */
object Notifier {
    const val CH_MINE = "my_requests"
    const val CH_PENDING = "pending_requests"
    const val CH_ISSUES = "issues"
    private const val WORK = "seerr-check"

    /** Bumped after each check so open screens (tab badges) refresh. */
    var version by mutableIntStateOf(0)
        private set

    fun bump() = Handler(Looper.getMainLooper()).post { version++ }

    fun channels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannels(
            listOf(
                NotificationChannel(CH_MINE, "Your requests", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "When your requests are approved, declined, fail or become available"
                },
                NotificationChannel(CH_PENDING, "Requests to approve", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "New requests waiting for approval, with Approve and Decline buttons"
                },
                NotificationChannel(CH_ISSUES, "Issues", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "New issues, and replies or fixes on issues you reported"
                },
            )
        )
    }

    fun schedule(ctx: Context) {
        val s = App.settings
        if (!s.notifyEnabled || !App.prefs.isConfigured) { cancel(ctx); return }
        channels(ctx)
        val req = PeriodicWorkRequestBuilder<CheckWorker>(s.notifyEveryMinutes.coerceAtLeast(15).toLong(), TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.UPDATE, req)
    }

    fun checkNow(ctx: Context) {
        channels(ctx)
        WorkManager.getInstance(ctx).enqueueUniqueWork(
            "$WORK-now", ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<CheckWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build(),
        )
    }

    fun cancel(ctx: Context) {
        WorkManager.getInstance(ctx).cancelUniqueWork(WORK)
    }

    /**
     * Signed out (or the session ended): stop all checks, take down posted notifications so their Approve/Decline
     * buttons can't act on a later sign-in, and forget which requests and issues were already seen.
     */
    fun signOut(ctx: Context) {
        val wm = WorkManager.getInstance(ctx)
        wm.cancelUniqueWork(WORK)
        wm.cancelUniqueWork("$WORK-now")
        NotificationManagerCompat.from(ctx).cancelAll()
        ctx.getSharedPreferences("notify_state", Context.MODE_PRIVATE).edit().clear().apply()
    }

    fun canPost(ctx: Context): Boolean =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Random per-install secret that notification taps carry, so other apps can't drive navigation. */
    private fun routeToken(ctx: Context): String {
        val sp = ctx.getSharedPreferences("notify_state_token", Context.MODE_PRIVATE)
        return sp.getString("token", null) ?: ByteArray(32).also { SecureRandom().nextBytes(it) }
            .joinToString("") { "%02x".format(it) }
            .also { sp.edit().putString("token", it).apply() }
    }

    fun checkRouteToken(ctx: Context, token: String?): Boolean =
        token != null && MessageDigest.isEqual(token.toByteArray(), routeToken(ctx).toByteArray())

    fun openIntent(ctx: Context, route: String, code: Int): PendingIntent {
        val i = Intent(ctx, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_ROUTE, route)
            .putExtra(MainActivity.EXTRA_TOKEN, routeToken(ctx))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(ctx, code, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    @Suppress("MissingPermission")
    fun post(
        ctx: Context,
        id: Int,
        channel: String,
        title: String,
        text: String,
        route: String,
        poster: Bitmap? = null,
        actions: List<NotificationCompat.Action> = emptyList(),
        publicTitle: String = publicTitleFor(channel),
    ) {
        if (!canPost(ctx)) return
        val b = NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_stat_seerr)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openIntent(ctx, route, id))
            .setAutoCancel(true)
            .setGroup(channel)
            // Titles, usernames and comments stay off the lock screen; it only says what kind of news this is.
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion(ctx, channel, publicTitle))
        poster?.let { b.setLargeIcon(it) }
        actions.forEach { b.addAction(it) }
        NotificationManagerCompat.from(ctx).notify(id, b.build())
    }

    @Suppress("MissingPermission")
    fun replace(ctx: Context, id: Int, channel: String, title: String, text: String) {
        if (!canPost(ctx)) return
        val b = NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_stat_seerr)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setTimeoutAfter(8000)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion(ctx, channel, publicTitleFor(channel)))
        NotificationManagerCompat.from(ctx).notify(id, b.build())
    }

    fun publicTitleFor(channel: String): String = when (channel) {
        CH_PENDING -> "New request"
        CH_ISSUES -> "Issue update"
        else -> "Request update"
    }

    private fun publicVersion(ctx: Context, channel: String, title: String) =
        NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_stat_seerr)
            .setContentTitle(title)
            .setContentText("Unlock to see details")
            .build()
}
