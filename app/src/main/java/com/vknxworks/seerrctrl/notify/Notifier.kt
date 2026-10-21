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
                NotificationChannel(CH_MINE, App.str(R.string.your_requests), NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = App.str(R.string.when_your_requests_are_approved)
                },
                NotificationChannel(CH_PENDING, App.str(R.string.requests_to_approve), NotificationManager.IMPORTANCE_HIGH).apply {
                    description = App.str(R.string.new_requests_waiting_for_approval)
                },
                NotificationChannel(CH_ISSUES, App.str(R.string.issues), NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = App.str(R.string.new_issues_and_replies_or_fixes_on)
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
        // Approve/Decline buttons handed out for this sign-in stop working, even if another app kept a copy.
        ctx.getSharedPreferences("notify_state_token", Context.MODE_PRIVATE).edit().remove(KEY_ACTION).commit()
    }

    private const val KEY_ACTION = "action"

    /**
     * Random secret for the current sign-in, carried by Approve/Decline buttons. It is replaced on sign-out,
     * so a button from an earlier sign-in can't act with whichever account is signed in later.
     */
    @Synchronized
    fun actionToken(ctx: Context): String {
        val sp = ctx.getSharedPreferences("notify_state_token", Context.MODE_PRIVATE)
        return sp.getString(KEY_ACTION, null) ?: ByteArray(32).also { SecureRandom().nextBytes(it) }
            .joinToString("") { "%02x".format(it) }
            .also { sp.edit().putString(KEY_ACTION, it).commit() }
    }

    /** True only for the current sign-in's token; never creates one. */
    fun checkActionToken(ctx: Context, token: String?): Boolean {
        val current = ctx.getSharedPreferences("notify_state_token", Context.MODE_PRIVATE).getString(KEY_ACTION, null)
        return token != null && current != null && MessageDigest.isEqual(token.toByteArray(), current.toByteArray())
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
        CH_PENDING -> App.str(R.string.new_request)
        CH_ISSUES -> App.str(R.string.issue_update)
        else -> App.str(R.string.request_update)
    }

    private fun publicVersion(ctx: Context, channel: String, title: String) =
        NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_stat_seerr)
            .setContentTitle(title)
            .setContentText(App.str(R.string.unlock_to_see_details))
            .build()
}
