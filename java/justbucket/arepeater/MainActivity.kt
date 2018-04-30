package justbucket.arepeater

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var list: ArrayList<File>
    private lateinit var songList: ArrayList<File>
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var root: File
    private var startTime = 0
    private var current = 0
    private var audioIndex = 0
    private var repeat = 0
    private val utils = Utilities()
    private val mp = MediaPlayer()
    private val mHandler = Handler()

    private val mUpdateTimeTask = object : Runnable {
        override fun run() {
            val totalDuration = mp.duration.toLong()
            val currentDuration = mp.currentPosition.toLong()

            // Displaying Total Duration time
            textViewTotal.text = String.format("%s", utils.milliSecondsToTimer(totalDuration))
            // Displaying time completed playing
            textViewCurr.text = String.format("%s", utils.milliSecondsToTimer(currentDuration))

            // Updating progress bar
            val progress = utils.getProgressPercentage(currentDuration, totalDuration)
            seekBar.progress = progress

            // Running this thread after 100 milliseconds
            mHandler.postDelayed(this, 100)
        }
    }

    private val repeatTask = object : Runnable {
        override fun run() {
            mp.seekTo(startTime)
            mHandler.postDelayed(this, numberPicker.value.toLong() * 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        root = File(sharedPreferences.getString("root", Environment.getExternalStorageDirectory().toString()))

        imageButtonPP.setOnClickListener {
            if (mp.isPlaying) {
                mp.pause()
                imageButtonPP.setImageResource(R.drawable.play)
            } else {
                // Resume song
                mp.start()
                // Changing button image to pause button
                imageButtonPP.setImageResource(R.drawable.pause)
            }
        }

        imageButtonNext.setOnClickListener {
            current = 1
            if (audioIndex < songList.size - 1) {
                ++audioIndex
                playSong(songList[audioIndex])
            } else {
                audioIndex = 0
                playSong(songList[audioIndex])
            }
        }

        imageButtonPrev.setOnClickListener {
            current = 1
            if (audioIndex != 0) {
                --audioIndex
                playSong(songList[audioIndex])
            }
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {

            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                // remove message Handler from updating progress bar
                mHandler.removeCallbacks(mUpdateTimeTask)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                if (mp.isPlaying || mp.currentPosition > 1) {
                    mHandler.removeCallbacks(mUpdateTimeTask)
                    val totalDuration = mp.duration
                    val currentPosition = utils.progressToTimer(seekBar.progress, totalDuration)

                    // forward or backward to certain seconds
                    mp.seekTo(currentPosition)

                    // update timer progress again
                    updateProgressBar()
                }
            }
        })

        mp.setOnCompletionListener {
            repeat = numberPicker.value

            if (current < repeat) {
                playSong(songList[audioIndex])
                current++
            } else {
                current = 1
                if (++audioIndex <= songList.size - 1) {
                    playSong(songList[audioIndex])
                } else {
                    audioIndex = 0
                    playSong(songList[audioIndex])
                }
            }
        }

        checkBox.setOnCheckedChangeListener { compoundButton, checked ->
            if (checked) {
                startTime = mp.currentPosition
                mHandler.postDelayed(repeatTask, numberPicker.value.toLong() * 1000)
            } else mHandler.removeCallbacks(repeatTask)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        1)
            } else {
                scanFiles(root)
            }
        } else {
            scanFiles(root)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            scanFiles(root)
        } else if (requestCode == 1 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
            val toast = Toast.makeText(applicationContext,
                    "You need to grant storage access in order to view files", Toast.LENGTH_LONG)
            toast.show()
        }
    }

    private fun scanFiles(root: File) {

        this.root = root
        textPath.text = root.absolutePath
        list = ArrayList(root.listFiles().filter { (it.name[0] != '.') && (it.isDirectory || it.extension == "mp3") }.sorted().toList())
        //songList = ArrayList(list.filter { it.extension == "mp3" })
        fileMan.adapter = FileListAdapter(this, ::onItemClick, list)
    }

    private fun onItemClick(file: File) {
        if (file.isDirectory) {
            scanFiles(file)
        } else {
            playerLay.visibility = View.VISIBLE
            playSong(file)
            songList = ArrayList(list.filter { it.isFile })
            sharedPreferences.edit().putString("root", file.parentFile.absolutePath).apply()
        }
    }

    private fun playSong(file: File) {
        mp.reset()
        mp.setDataSource(file.path)
        mp.prepare()
        mp.start()

        current = 1
        //val str = list[audioIndex].absolutePath
        textViewTrack.text = file.nameWithoutExtension

        /*seekBar.progress = 0
        seekBar.max = 100*/

        updateProgressBar()
    }

    fun updateProgressBar() {
        mHandler.postDelayed(mUpdateTimeTask, 100)
    }

    override fun onBackPressed() {
        if (textPath.text.toString() != Environment.getExternalStorageDirectory().toString()) {
            scanFiles(root.parentFile)
        } else {
            mHandler.removeCallbacks(mUpdateTimeTask)
            mHandler.removeCallbacks(repeatTask)
            mp.release()
            super.onBackPressed()
        }
    }


}
