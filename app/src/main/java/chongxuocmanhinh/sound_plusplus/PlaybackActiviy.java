package chongxuocmanhinh.sound_plusplus;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

/**
 * Created by L on 07/11/2016.
 */
public abstract class PlaybackActiviy extends Activity
    implements  Handler.Callback,
                View.OnClickListener,
                TimelineCallback
{

    /**
     * A handler running on the UI Thread,in contrast with mHandler which runs
     * on a worker thread
     */
    protected final Handler mUiHandler = new Handler(this);
    /**
     * A Handler running on a worker thread
     */
    protected Handler mHandler;
    /**
     * The looper for the worker thread.
     */
    protected Looper mLooper;

    protected ImageButton mPlayPauseButton;
    protected ImageButton mShuffleButton;
    protected ImageButton mEndButton;

    protected int mState;
    private  long mLastStateEvent;
    private  long mLastSongEvent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PlaybackService.addTimelineCallback(this);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        HandlerThread thread = new HandlerThread(getClass().getName(), Process.THREAD_PRIORITY_LOWEST);
        thread.start();

        mLooper = thread.getLooper();
        mHandler = new Handler(mLooper,this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        PlaybackService.removeTimelineCallback(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if(PlaybackService.hasInstance()){
            onServiceReady();
        }
        else{
            Log.d("TestPlay","StartService");
            startService(new Intent(this,PlaybackService.class));
        }
    }

    /**
     * Cấu hình các thành phần của activity khi mà PlaybackService được khởi tạo và có thể sử dụng
     * để tương tác.
     */
    protected void onServiceReady(){
        PlaybackService service = PlaybackService.get(this);
        Log.d("TestPlay","ServiceReady");
        setSong(service.getSong(0));
    }
    /**
     * Called when the current song changes.
     *
     * @param song The new song
     */
    protected void onSongChange(Song song)
    {

    }

    protected void setSong(final Song song){
        mLastSongEvent =  System.nanoTime();
        runOnUiThread(new Runnable() {
            @Override
            public void run()
            {
                onSongChange(song);
            }
        });
    }

    public void setSong(long uptime, Song song){
        if (uptime > mLastSongEvent) {
            setSong(song);
            mLastSongEvent = uptime;
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        return false;
    }

    protected void bindControlButtons(){
        View previousButton = findViewById(R.id.previous);
        previousButton.setOnClickListener(this);
        mPlayPauseButton = (ImageButton)findViewById(R.id.play_pause);
        mPlayPauseButton.setOnClickListener(this);
        View nextButton = findViewById(R.id.next);
        nextButton.setOnClickListener(this);

        mShuffleButton = (ImageButton)findViewById(R.id.shuffle);
        mShuffleButton.setOnClickListener(this);
        registerForContextMenu(mShuffleButton);
        mEndButton = (ImageButton)findViewById(R.id.end_action);
        mEndButton.setOnClickListener(this);
        registerForContextMenu(mEndButton);
    }
}
