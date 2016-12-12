package chongxuocmanhinh.sound_plusplus;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.annotation.Nullable;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by L on 07/11/2016.
 *PlaybackSerivce cho app nghe nhạc
 * Tham khảo thêm MediapLayer android tại(các trạng thái ,các methods call ...)
 * https://developer.android.com/reference/android/media/MediaPlayer.html
 */
public class PlaybackService extends Service
        implements Handler.Callback,
                MediaPlayer.OnCompletionListener,/** Hai cái này cần phải implement*/
                MediaPlayer.OnErrorListener
{

    private Looper mLooper;
    private Handler mHandler;
    SoundPlusPLusMediaPlayer mMediaPlayer;
    SoundPlusPLusMediaPlayer mPreparedMediaPlayer;

    AudioManager mAudioManager;

    private Song mCurrentSong;
    SongTimeLine mSongTimeLine;

    /**
     * Object được dùng  PlaybackService startup waiting.
     */
    private static final Object[] sWait = new Object[0];
    /**
     * Dùng để sử dụng ở mọi nơi,Single-ton.
     */
    PlaybackService sInstance;

    @Override
    public void onCreate() {
        HandlerThread thread = new HandlerThread("PlaybackService", Process.THREAD_PRIORITY_DEFAULT);
        thread.start();

        mMediaPlayer = getNewMediaPLayer();
        mPreparedMediaPlayer = getNewMediaPLayer();
        //Ta chỉ sử dụng duy nhất một audioseissionId
        mPreparedMediaPlayer.setAudioSessionId(mMediaPlayer.getAudioSessionId());

        mAudioManager = (AudioManager)getSystemService(AUDIO_SERVICE);

        mLooper = thread.getLooper();
        mHandler = new Handler(mLooper, this);

        sInstance = this;
        synchronized (sWait) {
            sWait.notifyAll();
        }
    }

    private SoundPlusPLusMediaPlayer getNewMediaPLayer(){
        SoundPlusPLusMediaPlayer mp = new SoundPlusPLusMediaPlayer(this);
        mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mp.setOnCompletionListener(this);
        mp.setOnErrorListener(this);
        return mp;
    }

    private void preparedMediaPlayer(MediaPlayer mp,String path) throws IOException {
        mp.setDataSource(path);
        mp.prepare();
    }
    //=========================Handler.Callback=====================================//
    @Override
    public boolean handleMessage(Message msg) {
        return false;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    //=======================MediaPlayer.OnCompletionListener=========================//
    @Override
    public void onCompletion(MediaPlayer mp) {

    }
    //=======================MediaPlayer.OnErrorListener=========================//
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }

}
