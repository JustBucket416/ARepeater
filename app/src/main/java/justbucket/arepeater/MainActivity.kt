package justbucket.arepeater

import android.Manifest
import android.content.ComponentName
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.recyclerview.widget.LinearLayoutManager
import java.io.File
import justbucket.arepeater.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val mHandler = Handler(Looper.getMainLooper())
    private val adapter = FileListAdapter(::onItemClick)

    private lateinit var list: List<File>
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var root: File
    private lateinit var binding: ActivityMainBinding

    private var songs: List<File> = emptyList()
    private var index = 0

    private val updateTimeTask = object : Runnable {
        override fun run() {
            val (total, current, percentage, trackName) = service?.getUiInfo() ?: return

            binding.textViewTotal.text = total
            binding.textViewCurr.text = current
            binding.textViewTrack.text = trackName

            binding.seekBar.setProgress(percentage, true)

            mHandler.postDelayed(this, 500)
        }
    }

    private var service: MediaPlayerService? = null
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = (binder as MediaPlayerService.MyBinder).service.also { it.playSong(songs, index) }
            updateProgressBar()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            //do nothing
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        binding.fileMan.layoutManager = LinearLayoutManager(this)
        binding.fileMan.adapter = adapter
        binding.group.visibility = View.GONE

        sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)

        binding.imageButtonPP.setOnClickListener {
            val drw = if (service?.playOrPause() == true) R.drawable.pause else R.drawable.play
            binding.imageButtonPP.setImageResource(drw)
        }

        binding.imageButtonNext.setOnClickListener {
            binding.checkBox.isChecked = false
            service?.next()
        }

        binding.imageButtonPrev.setOnClickListener {
            binding.checkBox.isChecked = false
            service?.prev()
        }

        binding.numberPicker.setOnValueChangedListener { _, _, new ->
            service?.updateRepeat(new)
        }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {

            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                mHandler.removeCallbacks(updateTimeTask)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                mHandler.removeCallbacks(updateTimeTask)
                service?.onStopSeek(binding.seekBar.progress, binding.checkBox.isChecked)
                updateProgressBar()
            }
        })

        binding.checkBox.setOnCheckedChangeListener { _, checked ->
            if (checked) service?.startRepeat() else service?.stopRepeat()
        }

        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
        } else {
            root = File(sharedPreferences.getString("root", Environment.getExternalStorageDirectory().toString()))
            scanFiles(root)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            root = File(sharedPreferences.getString("root", Environment.getExternalStorageDirectory().toString()))
            scanFiles(root)
        } else if (requestCode == 1 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
            val toast = Toast.makeText(
                applicationContext,
                "You need to grant storage access in order to view files", Toast.LENGTH_LONG
            )
            toast.show()
        }
    }

    private fun scanFiles(root: File) {
        this.root = root
        binding.textPath.text = root.absolutePath
        //list = root.listFiles().filter { (it.name[0] != '.') && (it.isDirectory || it.extension == "mp3") }.sorted()
        adapter.submitList(list)
    }

    private fun onItemClick(file: File) {
        if (file.isDirectory) {
            scanFiles(file)
        } else {
            binding.group.visibility = View.VISIBLE
            binding.textViewTrack.text = file.nameWithoutExtension
            binding.imageButtonPP.setImageResource(R.drawable.pause)

            songs = ArrayList(list.filter { it.isFile })
            index = songs.indexOf(file)
            if (service == null) {
                bindService(MediaPlayerService.newIntent(this), serviceConnection, BIND_AUTO_CREATE or BIND_ABOVE_CLIENT)
            } else {
                service?.playSong(songs, index)
            }
            sharedPreferences.edit { putString("root", file.parentFile.absolutePath)}
        }
    }

    fun updateProgressBar() {
        mHandler.post(updateTimeTask)
    }

    override fun onBackPressed() {
        if (binding.textPath.text.toString() != Environment.getExternalStorageDirectory().toString()) {
            scanFiles(root.parentFile)
        } else {
            mHandler.removeCallbacks(updateTimeTask)
            unbindService(serviceConnection)
            super.onBackPressed()
        }
    }

}
