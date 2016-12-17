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
import android.widget.Toast;

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

    /**
     * Trạng thái của thằng service chạy phí dưới
     */
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

    @Override
    public void onTimelineChanged() {

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

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.next:
                shiftCurrentSong(SongTimeLine.SHIFT_NEXT_SONG);
                break;
            case R.id.play_pause:
                Log.d("TestShowQueue","----");
                Log.d("TestPlayPause","Clicked playpause button");
                playPause();
                break;
            case R.id.previous:
                rewindCurrentSong();
                break;
            case R.id.end_action:
               // cycleFinishAction();
                break;
            case R.id.shuffle:
                //cycleShuffle();
                break;
        }
    }

    public void shiftCurrentSong(int delta)
    {
        setSong(PlaybackService.get(this).shiftCurrentSong(delta));
    }

    public void playPause(){
        PlaybackService service = PlaybackService.get(this);
        int state = service.playPause();
        setState(state);
    }


    private void rewindCurrentSong()
    {
        setSong(PlaybackService.get(this).rewindCurrentSong());
    }


    /**
     * Được gọi khi trạng thái của playbackService bị thay đổi
     * @param state
     * @param toggled
     */
    protected void onStateChange(int state,int toggled){
        if ((toggled & PlaybackService.FLAG_PLAYING) != 0 && mPlayPauseButton != null) {
            mPlayPauseButton.setImageResource((state & PlaybackService.FLAG_PLAYING) == 0 ? R.drawable.play : R.drawable.pause);
        }
    }
    protected void setState(final int state){
        mLastStateEvent = System.nanoTime();

        if(mState != state){
            final int toggled = mState ^ state;
            mState = state;
            runOnUiThread(new Runnable() {
                @Override
                public void run()
                {
                    onStateChange(state, toggled);
                }
            });
        }
    }


}
