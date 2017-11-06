package justbucket.arepeater;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.shawnlin.numberpicker.NumberPicker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private TextView textPath, textCurrTrack, textCurrentDuration, textTotalDuration;
    private LinearLayout fileMan;
    private LayoutInflater inflater;
    private Utilities utils;
    private ImageButton play;
    private NumberPicker numberPicker;
    private SeekBar seekBar;
    private MediaPlayer mp;
    private Handler mHandler = new Handler();
    private File[] list;
    private int audioIndex, repeat, current = 0;
    private String path = "";
    private Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            long totalDuration = mp.getDuration();
            long currentDuration = mp.getCurrentPosition();

            // Displaying Total Duration time
            textTotalDuration.setText(String.format("%s", utils.milliSecondsToTimer(totalDuration)));
            // Displaying time completed playing
            textCurrentDuration.setText(String.format("%s", utils.milliSecondsToTimer(currentDuration)));

            // Updating progress bar
            int progress = utils.getProgressPercentage(currentDuration, totalDuration);
            seekBar.setProgress(progress);

            // Running this thread after 100 milliseconds
            mHandler.postDelayed(this, 100);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        textPath = findViewById(R.id.textPath);
        textCurrTrack = findViewById(R.id.textViewTrack);
        textCurrentDuration = findViewById(R.id.textViewCurr);
        textTotalDuration = findViewById(R.id.textViewTotal);
        fileMan = findViewById(R.id.layMan);
        inflater = getLayoutInflater();
        play = findViewById(R.id.imageButtonPlay);
        ImageButton next = findViewById(R.id.imageButtonNext);
        ImageButton previous = findViewById(R.id.imageButtonPrev);
        numberPicker = findViewById(R.id.number_picker);
        seekBar = findViewById(R.id.seekBar);
        mp = new MediaPlayer();
        utils = new Utilities();

        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mp.isPlaying()) {
                    if (mp != null) {
                        mp.pause();
                        play.setImageResource(R.drawable.play);
                    }
                } else {
                    // Resume song
                    if (mp != null) {
                        mp.start();
                        // Changing button image to pause button
                        play.setImageResource(R.drawable.pause);
                    }
                }
            }
        });

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                current = 0;
                if (audioIndex < list.length - 1) {
                    ++audioIndex;
                    playSong();
                } else {
                    audioIndex = 0;
                    playSong();
                }
            }
        });

        previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                current = 0;
                if (audioIndex != 0) {
                    --audioIndex;
                    playSong();
                }
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // remove message Handler from updating progress bar
                mHandler.removeCallbacks(mUpdateTimeTask);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mp.isPlaying() || mp.getCurrentPosition() > 1) {
                    mHandler.removeCallbacks(mUpdateTimeTask);
                    int totalDuration = mp.getDuration();
                    int currentPosition = utils.progressToTimer(seekBar.getProgress(), totalDuration);

                    // forward or backward to certain seconds
                    mp.seekTo(currentPosition);

                    // update timer progress again
                    updateProgressBar();
                }
            }
        });

        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {

                repeat = numberPicker.getValue();

                if (current < repeat) {
                    playSong();
                } else {
                    current = 0;
                    if (++audioIndex <= list.length - 1) {
                        playSong();
                    } else {
                        audioIndex = 0;
                        playSong();
                    }
                }
            }
        });

        try {
            FileInputStream fis = new FileInputStream(getFilesDir() + "/last_path.txt");
            BufferedReader r = new BufferedReader(new InputStreamReader(fis));
            path = r.readLine();
            r.close();
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) !=
                    PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        1);
            } else {
                if (path.isEmpty()) scanFiles(Environment.getExternalStorageDirectory());
                else scanFiles(new File(path));
            }
        } else {
            if (path.isEmpty()) scanFiles(Environment.getExternalStorageDirectory());
            else scanFiles(new File(path));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if ((requestCode == 1 && (grantResults[0] == PackageManager.PERMISSION_GRANTED))) {
            if (path.isEmpty()) scanFiles(Environment.getExternalStorageDirectory());
            else scanFiles(new File(path));
        } else if ((requestCode == 1 && (grantResults[0] == PackageManager.PERMISSION_DENIED))) {
            Toast toast = Toast.makeText(getApplicationContext(),
                    "You need to grant storage access in order to view files", Toast.LENGTH_LONG);
            toast.show();
        }
    }

    public void scanFiles(File root) {
        textPath.setText(root.toString());
        fileMan.removeAllViews();
        list = root.listFiles();
        Arrays.sort(list);
        for (final File f : list) {
            if (f.isDirectory()) {
                View folder = inflater.inflate(R.layout.layout_folder, null);
                folder.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        scanFiles(new File(f.getAbsolutePath()));
                    }
                });

                TextView text = folder.findViewById(R.id.textView);
                text.setText(f.getAbsolutePath().substring(f.getAbsolutePath().lastIndexOf('/') + 1));

                fileMan.addView(folder);
            } else if ((f.getAbsolutePath().contains(".mp3"))) {
                View file = inflater.inflate(R.layout.layout_file, null);
                file.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String path = f.getAbsolutePath().substring(0, f.getAbsolutePath().lastIndexOf('/'));
                        list = new File(path).listFiles();
                        Arrays.sort(list);
                        for (int i = 0; i < list.length; i++) {
                            if (f.equals(list[i])) {
                                current = 0;
                                play.setImageResource(R.drawable.pause);
                                seekBar.setClickable(true);
                                audioIndex = i;
                                playSong();
                                break;
                            }
                        }
                        try {
                            File last = new File(getFilesDir(), "last_path.txt");
                            if (!last.createNewFile()) return;
                            FileOutputStream fos = openFileOutput("last_path.txt", Context.MODE_PRIVATE);
                            fos.write(path.getBytes());
                            fos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
                TextView text = file.findViewById(R.id.textView);
                String str = f.getAbsolutePath();
                text.setText(str.substring(str.lastIndexOf('/') + 1));
                fileMan.addView(file);
            }
        }
        if (list.length == 0) {
            TextView textView = new TextView(this);
            textView.setText("There are no audios here!");
            textView.setGravity(Gravity.CENTER);
            fileMan.addView(textView);
        }
    }

    public void playSong() {
        try {
            mp.reset();
            mp.setDataSource(list[audioIndex].getPath());
            mp.prepare();
            mp.start();

            String str = list[audioIndex].getAbsolutePath();
            textCurrTrack.setText(str.substring(str.lastIndexOf('/') + 1) + '(' + ++current + ')');

            seekBar.setProgress(0);
            seekBar.setMax(100);

            updateProgressBar();
        } catch (IllegalArgumentException | IOException | IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public void updateProgressBar() {
        mHandler.postDelayed(mUpdateTimeTask, 100);
    }

    @Override
    public void onBackPressed() {
        if (!textPath.getText().toString().equals(Environment.getExternalStorageDirectory().toString())) {
            String str = textPath.getText().toString();
            scanFiles(new File(str.substring(0, str.lastIndexOf('/'))));
        } else {
            mHandler.removeCallbacks(mUpdateTimeTask);
            mp.release();
            super.onBackPressed();
        }
    }
}
