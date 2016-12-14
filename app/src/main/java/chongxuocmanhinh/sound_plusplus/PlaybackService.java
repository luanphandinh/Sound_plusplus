package chongxuocmanhinh.sound_plusplus;

import android.app.Service;
import android.content.Context;
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
import android.util.Log;
import android.widget.Toast;

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
                SongTimeLine.Callback,
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

    private String mErrorMessage;

    /**
     * Object được dùng  PlaybackService startup waiting.
     */
    private static final Object[] sWait = new Object[0];
    /**
     * Dùng để sử dụng ở mọi nơi,Single-ton.
     */
    public static PlaybackService sInstance;

    private boolean mMediaPlayerInitialized;
    @Override
    public void onCreate() {
        HandlerThread thread = new HandlerThread("PlaybackService", Process.THREAD_PRIORITY_DEFAULT);
        thread.start();
        mSongTimeLine = new SongTimeLine(this);
        mSongTimeLine.setCallback(this);
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

    /**
     *Trả về thằng  PlaybackService instance tạo mới nếu chưa có.
     */
    public static PlaybackService get(Context context)
    {
        if (sInstance == null) {
            context.startService(new Intent(context, PlaybackService.class));

            while (sInstance == null) {
                try {
                    synchronized (sWait) {
                        sWait.wait();
                    }
                } catch (InterruptedException ignored) {
                }
            }
        }
        Log.d("TestPlay","Return Service");
        return sInstance;
    }

    private SoundPlusPLusMediaPlayer getNewMediaPLayer(){
        SoundPlusPLusMediaPlayer mp = new SoundPlusPLusMediaPlayer(this);
        mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mp.setOnCompletionListener(this);
        mp.setOnErrorListener(this);
        return mp;
    }

    private void prepareMediaPlayer(SoundPlusPLusMediaPlayer mp,String path) throws IOException {
        mp.setDataSource(path);
        mp.prepare();
    }

    private void processSong(Song song){
        try {
            mMediaPlayerInitialized = false;
            mMediaPlayer.reset();
            Log.d("Test","Song path : " + song.path);
            if(mPreparedMediaPlayer.isPlaying()) {
                // The prepared media player is playing as the previous song
                // reched its end 'naturally' (-> gapless)
                // We can now swap mPreparedMediaPlayer and mMediaPlayer
                SoundPlusPLusMediaPlayer tmpPlayer = mMediaPlayer;
                mMediaPlayer = mPreparedMediaPlayer;
                mPreparedMediaPlayer = tmpPlayer; // this was mMediaPlayer and is in reset() state
            }
            else {
                prepareMediaPlayer(mMediaPlayer, song.path);
            }

        } catch (IOException e) {
            Log.d("Test","Error!");

        }

        mMediaPlayer.start();
    }

    public void addSongs(QueryTask query){
        Log.d("TestPlay","Service Add song");
        mHandler.sendMessage(mHandler.obtainMessage(MSG_QUERY, query));
    }
    //=========================Handler.Callback=====================================//
    /**
     * Chạy query và add các kết quả vào songtimeline.
     *
     * obj là  QueryTask. arg1 là chế độ add (add mode) (one of SongTimeLine.MODE_*)
     */
    private static final int MSG_QUERY = 2;

    private static final int MSG_PROCESS_SONG = 13;
    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what){
            case MSG_QUERY:
                runQuery((QueryTask)msg.obj);
                break;
            case MSG_PROCESS_SONG:
                processSong((Song)msg.obj);
                break;
            default:
                return false;
        }
        return true;
    }

    /**
     *
     * @param query
     */
    public void runQuery(QueryTask query){
        int count = mSongTimeLine.addSongs(this,query);
        int text;
        switch (query.mode) {
            case SongTimeLine.MODE_PLAY:
                text = R.plurals.playing;
        }
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

    //=======================SongTimeLine.CallBack===============================//
    @Override
    public void activeSongReplaced(int delta, Song song) {
        if (delta == 0)
            setCurrentSong(0);
    }

    public Song setCurrentSong(int delta){
        if (mMediaPlayer == null)
            return null;

        if (mMediaPlayer.isPlaying())
            mMediaPlayer.stop();

        Song song = mSongTimeLine.getSong(0);
        mCurrentSong = song;
        mHandler.removeMessages(MSG_PROCESS_SONG);

        mMediaPlayerInitialized = false;
        mHandler.sendMessage(mHandler.obtainMessage(MSG_PROCESS_SONG, song));
        return song;
    }

    public Song getSong(int delta){
        if(mSongTimeLine == null)
            return null;
        if (delta == 0)
            return mCurrentSong;
        return mSongTimeLine.getSong(delta);
    }

    @Override
    public void timelineChanged() {

    }

    @Override
    public void positionInfoChanged() {

    }


    public static boolean hasInstance(){
        return sInstance != null;
    }
}
