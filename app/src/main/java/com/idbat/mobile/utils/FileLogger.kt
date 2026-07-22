package com.idbat.mobile.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

/**
 * Journalisation dans un fichier. On capture le logcat du process courant (`logcat --pid`), ce qui
 * enregistre tous les `Log.*` de l'app sans modifier les appels existants.
 *
 * Purge au démarrage : on ne conserve que les lignes du **jour en cours** (format logcat `-v time`
 * = `MM-DD …`). Partage via un intent e-mail (pièce jointe FileProvider).
 */
object FileLogger {

    private const val LOG_DIR = "logs"
    private const val LOG_FILE = "idbat_logs.txt"
    private const val TAG = "FILE_LOGGER"

    @Volatile private var logFile: File? = null
    @Volatile private var started = false
    private val lock = Any()

    /** À appeler tôt dans `Application.onCreate`. Purge les anciens jours puis démarre la capture. */
    fun init(context: Context) {
        val dir = File(context.filesDir, LOG_DIR).apply { mkdirs() }
        val file = File(dir, LOG_FILE)
        logFile = file

        purgeToToday(file)
        startCapture(file)
    }

    /** Ne conserve que les lignes du jour courant (préfixe `MM-DD` du format logcat `-v time`). */
    private fun purgeToToday(file: File) {
        if (!file.exists()) return
        try {
            val today = SimpleDateFormat("MM-dd", Locale.US).format(Date())
            val kept = file.readLines().filter { it.startsWith(today) }
            synchronized(lock) {
                file.writeText(if (kept.isEmpty()) "" else kept.joinToString("\n") + "\n")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Purge du fichier de log impossible", e)
        }
    }

    /** Capture le logcat du process (à partir de maintenant) et l'append au fichier. */
    private fun startCapture(file: File) {
        if (started) return
        started = true
        thread(isDaemon = true, name = "file-logger") {
            try {
                val pid = android.os.Process.myPid()
                val since = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
                val process = Runtime.getRuntime().exec(
                    arrayOf("logcat", "-v", "time", "-T", since, "--pid=$pid")
                )
                process.inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        synchronized(lock) { file.appendText(line + "\n") }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Capture du logcat impossible", e)
            }
        }
    }

    /**
     * Propose d'envoyer le fichier de log par e-mail (chooser du téléphone), en pièce jointe nommée
     * avec la date du jour. Retourne false si le fichier est indisponible.
     */
    fun shareLogsByEmail(context: Context): Boolean {
        val file = logFile ?: return false
        if (!file.exists()) return false

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val dateJour = SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE).format(Date())

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Logs idbat — $dateJour")
            putExtra(Intent.EXTRA_TEXT, "Journal applicatif idbat du $dateJour en pièce jointe.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Envoyer les logs").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Envoi des logs impossible", e)
            false
        }
    }
}
