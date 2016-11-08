package chongxuocmanhinh.sound_plusplus;

import android.app.Activity;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.view.View;
import android.widget.ImageButton;

/**
 * Created by L on 07/11/2016.
 */
public abstract class PlaybackActiviy extends Activity
    implements  Handler.Callback,
                View.OnClickListener
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

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        HandlerThread thread = new HandlerThread(getClass().getName(), Process.THREAD_PRIORITY_LOWEST);
        thread.start();

        mLooper = thread.getLooper();
        mHandler = new Handler(mLooper,this);
    }
}
