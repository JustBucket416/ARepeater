package justbucket.arepeater

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import java.io.File

/**
 * @author JustBucket on 29.05.2025
 */
class MediaPlayerService : Service() {

    private val binder = MyBinder()
    private val mp = MediaPlayer()
    private val songList = mutableListOf<File>()
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var ntf: Notification
    private lateinit var wl: PowerManager.WakeLock

    private var startTime = 0
    private var current = 0
    private var audioIndex = 0
    private var repeat = 0

    private val repeatTask = object : Runnable {
        override fun run() {
            mp.seekTo(startTime)
            handler.postDelayed(this, repeat.toLong() * 1000)
        }
    }

    private val notifTask = object : Runnable {
        override fun run() {
            updateNotification()
            handler.postDelayed(this, repeat.toLong() * 1000)
        }
    }

    override fun onCreate() {
        super.onCreate()

        wl = getSystemService<PowerManager>()!!.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Timer:WakeLock")
        wl.acquire()
        wl.setReferenceCounted(false)

        mp.setOnCompletionListener {
            if (current < repeat) {
                playSong(songList[audioIndex])
                current++
            } else {
                current = 1
                audioIndex = if (audioIndex + 1 < songList.size) audioIndex + 1 else 0
                /*if (++audioIndex <= songList.size - 1) {
                    playSong(songList[++audioIndex])
                } else {
                    audioIndex = 0
                    playSong(songList[audioIndex])
                }*/
                playSong(songList[audioIndex])
            }
        }

        notificationManager = NotificationManagerCompat.from(this)
        val channel = NotificationChannel("TimerChannel", "TimerChannel", NotificationManager.IMPORTANCE_DEFAULT)
        notificationManager.createNotificationChannel(channel)

        ntf = NotificationCompat.Builder(this, channel.id)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setContentTitle("ARepeater")
            .setContentText("00:00")
            .build()
        startForeground(NOTIF_ID, ntf)
    }

    override fun onBind(intent: Intent?) = binder

    override fun onUnbind(intent: Intent?): Boolean {
        mp.release()
        handler.removeCallbacks(repeatTask)
        handler.removeCallbacks(notifTask)
        return super.onUnbind(intent)
    }

    fun playOrPause(): Boolean {
        return if (mp.isPlaying) {
            mp.pause()
            wl.release()
            false
        } else {
            mp.start()
            wl.acquire()
            true
        }
    }

    fun playSong(songs: List<File>, index: Int) {
        songList.clear()
        songList.addAll(songs)
        audioIndex = index
        current = 1

        playSong(songList[index])
        wl.acquire()
    }

    private fun playSong(song: File) {
        mp.reset()
        mp.setDataSource(song.absolutePath)
        mp.prepare()
        mp.start()

        handler.post(notifTask)
    }

    fun getUiInfo(): UiInfo {
        val totalDuration = mp.duration.toLong()
        val currentDuration = mp.currentPosition.toLong()
        return UiInfo(
            String.format("%s", milliSecondsToTimer(totalDuration)),
            String.format("%s", milliSecondsToTimer(currentDuration)),
            getProgressPercentage(currentDuration, totalDuration),
            "${songList[audioIndex].nameWithoutExtension} - $current"
        )
    }

    @SuppressLint("MissingPermission")
    private fun updateNotification() {
        notificationManager.notify(
            NOTIF_ID,
            NotificationCompat.Builder(this, ntf).setContentText(milliSecondsToTimer(mp.currentPosition.toLong())).build()
        )
    }

    fun next() {
        current = 1
        if (audioIndex < songList.size - 1) {
            ++audioIndex
            playSong(songList[audioIndex])
        } else {
            audioIndex = 0
            playSong(songList[audioIndex])
        }
    }

    fun prev() {
        if (audioIndex != 0) {
            current = 1
            --audioIndex
            playSong(songList[audioIndex])
        }
    }

    fun updateRepeat(count: Int) {
        repeat = count
    }

    fun onStopSeek(position: Int, isInRepeatMode: Boolean) {
        if (mp.isPlaying || mp.currentPosition > 1) {
            val totalDuration = mp.duration
            val currentDuration = progressToTimer(position, totalDuration)

            // forward or backward to certain seconds
            mp.seekTo(currentDuration)

            //if in repeat mode, set the new time as the start time
            if (isInRepeatMode) startTime = currentDuration

            // update timer progress again
        }
    }

    fun startRepeat() {
        startTime = mp.currentPosition
        handler.postDelayed(repeatTask, repeat.toLong() * 1000)
    }

    fun stopRepeat() {
        handler.removeCallbacks(repeatTask)
    }

    inner class MyBinder : Binder() {
        val service = this@MediaPlayerService
    }

    data class UiInfo(
        val totalDuration: String,
        val currentDuration: String,
        val percentage: Int,
        val trackName: String
    )

    companion object {
        const val NOTIF_ID = 123

        fun newIntent(context: Context) = Intent(context, MediaPlayerService::class.java)
    }
}