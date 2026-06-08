package com.idbat.mobile.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.idbat.mobile.data.AppDatabase
import com.idbat.mobile.singleton.AuthManager
import com.idbat.mobile.singleton.SyncManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Service en premier plan qui exécute la synchronisation.
 * Garantit que le transfert survit à l'écran éteint et au deep doze
 * (un foreground service de type dataSync est exempté des restrictions réseau de Doze).
 */
@AndroidEntryPoint
class SyncService : Service() {

    @Inject lateinit var syncManager: SyncManager
    @Inject lateinit var authManager: AuthManager
    @Inject lateinit var database: AppDatabase

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())

        val siteId = intent?.getLongExtra(EXTRA_SITE_ID, -1L) ?: -1L
        scope.launch {
            try {
                val site = database.siteDao().getSiteById(siteId)
                if (site != null) {
                    syncManager.executeTransfer(site)
                    authManager.refreshLoggedInContrat()
                }
            } finally {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf(startId)
            }
        }
        return START_NOT_STICKY
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        "Synchronisation",
                        NotificationManager.IMPORTANCE_LOW
                    )
                )
            }
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Synchronisation en cours")
            .setContentText("Transfert des données avec le serveur…")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_SITE_ID = "site_id"
        private const val NOTIF_ID = 4242
        private const val CHANNEL_ID = "sync_channel"

        fun start(context: Context, siteId: Long) {
            val intent = Intent(context, SyncService::class.java)
                .putExtra(EXTRA_SITE_ID, siteId)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
