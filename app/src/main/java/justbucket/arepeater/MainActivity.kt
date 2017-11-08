package justbucket.arepeater

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import com.shawnlin.numberpicker.NumberPicker
import java.io.*
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var textPath: TextView
    private lateinit var textCurrTrack: TextView
    private lateinit var textCurrentDuration: TextView
    private lateinit var textTotalDuration: TextView
    private lateinit var fileMan: LinearLayout
    private lateinit var inflater: LayoutInflater
    private lateinit var play: ImageButton
    private lateinit var numberPicker: NumberPicker
    private lateinit var seekBar: SeekBar
    private lateinit var list: Array<File>
    private var audioIndex = 0
    private var repeat = 0
    private var current = 0
    private val utils = Utilities()
    private val mp = MediaPlayer()
    private val mHandler = Handler()

    private val mUpdateTimeTask = object : Runnable {
        override fun run() {
            val totalDuration = mp.duration.toLong()
            val currentDuration = mp.currentPosition.toLong()

            // Displaying Total Duration time
            textTotalDuration.text = String.format("%s", utils.milliSecondsToTimer(totalDuration))
            // Displaying time completed playing
            textCurrentDuration.text = String.format("%s", utils.milliSecondsToTimer(currentDuration))

            // Updating progress bar
            val progress = utils.getProgressPercentage(currentDuration, totalDuration)
            seekBar.progress = progress

            // Running this thread after 100 milliseconds
            mHandler.postDelayed(this, 100)
        }
    }

    private fun path(): String {
        val text: String
        try {
            val fis = FileInputStream(filesDir.toString() + "/last_path.txt")
            val r = BufferedReader(InputStreamReader(fis))
            text = r.readLine()
            r.close()
            fis.close()
            return text
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        textPath = findViewById(R.id.textPath)
        textCurrTrack = findViewById(R.id.textViewTrack)
        textCurrentDuration = findViewById(R.id.textViewCurr)
        textTotalDuration = findViewById(R.id.textViewTotal)
        fileMan = findViewById(R.id.layMan)
        inflater = layoutInflater
        play = findViewById(R.id.imageButtonPlay)
        val next = findViewById<ImageButton>(R.id.imageButtonNext)
        val previous = findViewById<ImageButton>(R.id.imageButtonPrev)
        numberPicker = findViewById(R.id.number_picker)
        seekBar = findViewById(R.id.seekBar)

        play.setOnClickListener {
            if (mp.isPlaying) {
                mp.pause()
                play.setImageResource(R.drawable.play)
            } else {
                // Resume song
                mp.start()
                // Changing button image to pause button
                play.setImageResource(R.drawable.pause)
            }
        }

        next.setOnClickListener {
            current = 0
            if (audioIndex < list.size - 1) {
                ++audioIndex
                playSong()
            } else {
                audioIndex = 0
                playSong()
            }
        }

        previous.setOnClickListener {
            current = 0
            if (audioIndex != 0) {
                --audioIndex
                playSong()
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
                playSong()
            } else {
                current = 0
                if (++audioIndex <= list.size - 1) {
                    playSong()
                } else {
                    audioIndex = 0
                    playSong()
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        1)
            } else {
                scanFiles(path())
            }
        } else {
            scanFiles(path())
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            scanFiles(path())
        } else if (requestCode == 1 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
            val toast = Toast.makeText(applicationContext,
                    "You need to grant storage access in order to view files", Toast.LENGTH_LONG)
            toast.show()
        }
    }

    private fun scanFiles(rootPath: String) {
        val file: File
        if (rootPath.isEmpty()) file = Environment.getExternalStorageDirectory() else file = File(rootPath)
        textPath.text = file.absolutePath
        fileMan.removeAllViews()
        list = file.listFiles()
        Arrays.sort(list)
        for (f in list) {
            if (f.isDirectory) {
                val folderView = inflater.inflate(R.layout.layout_folder, null)
                folderView.setOnClickListener { scanFiles(f.absolutePath) }

                val text = folderView.findViewById<TextView>(R.id.textView)
                text.text = f.absolutePath.substring(f.absolutePath.lastIndexOf('/') + 1)

                fileMan.addView(folderView)
            } else if (f.absolutePath.contains(".mp3")) {
                val fileView = inflater.inflate(R.layout.layout_file, null)
                fileView.setOnClickListener(View.OnClickListener {
                    val path = f.absolutePath.substring(0, f.absolutePath.lastIndexOf('/'))
                    list = File(path).listFiles()
                    Arrays.sort(list)
                    for (i in list.indices) {
                        if (f == list[i]) {
                            current = 0
                            play.setImageResource(R.drawable.pause)
                            seekBar.isClickable = true
                            audioIndex = i
                            playSong()
                            break
                        }
                    }
                    try {
                        val last = File(filesDir, "last_path.txt")
                        if (!last.createNewFile()) return@OnClickListener
                        val fos = openFileOutput("last_path.txt", Context.MODE_PRIVATE)
                        fos.write(path.toByteArray())
                        fos.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                })
                val text = fileView.findViewById<TextView>(R.id.textView)
                val str = f.absolutePath
                text.text = str.substring(str.lastIndexOf('/') + 1)
                fileMan.addView(fileView)
            }
        }
        if (list.isEmpty()) {
            val textView = TextView(this)
            textView.text = "There are no audios here!"
            textView.gravity = Gravity.CENTER
            fileMan.addView(textView)
        }
    }

    private fun playSong() {
        try {
            mp.reset()
            mp.setDataSource(list[audioIndex].path)
            mp.prepare()
            mp.start()

            val str = list[audioIndex].absolutePath
            textCurrTrack.text = str.substring(str.lastIndexOf('/') + 1) + '(' + ++current + ')'

            seekBar.progress = 0
            seekBar.max = 100

            updateProgressBar()
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }

    }

    fun updateProgressBar() {
        mHandler.postDelayed(mUpdateTimeTask, 100)
    }

    override fun onBackPressed() {
        if (textPath.text.toString() != Environment.getExternalStorageDirectory().toString()) {
            val str = textPath.text.toString()
            scanFiles(str.substring(0, str.lastIndexOf('/')))
        } else {
            mHandler.removeCallbacks(mUpdateTimeTask)
            mp.release()
            super.onBackPressed()
        }
    }
}
