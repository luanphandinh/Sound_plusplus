package chongxuocmanhinh.sound_plusplus;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by L on 07/11/2016.
 *PlaybackSerivce cho app nghe nhạc
 * Tham khảo thêm MediapLayer android tại(các trạng thái ,các methods call ...)
 * https://developer.android.com/reference/android/media/MediaPlayer.html
 */
public class PlaybackService extends Service
        implements Handler.Callback
                , SongTimeLine.Callback
                , MediaPlayer.OnCompletionListener/** Hai cái này cần phải implement*/
                , MediaPlayer.OnErrorListener
                /**
                 * Tham khảo thêm về cách kiểm soát AudioFucus
                 * https://developer.android.com/guide/topics/media-apps/volume-and-earphones.html
                 */
                , AudioManager.OnAudioFocusChangeListener
               /// ,SharedPreferences.OnSharedPreferenceChangeListener
{
    /**
     * Tên của file lưu các trạng thái của service
     */
    private static final String STATE_FILE = "state";
    /**
     * Header cho state file để chỉ rõ file có đúng định dạng hay không
     */
    private static final long STATE_FILE_MAGIC = 0x1533574DC74B6ECL;
    /**
     * version của state file,chỉ rõ data order.
     */
    private static int STATE_VERSION = 6;

    private Looper mLooper;
    private Handler mHandler;
    SoundPlusPLusMediaPlayer mMediaPlayer;
    SoundPlusPLusMediaPlayer mPreparedMediaPlayer;
    private NotificationManager mNotificationManager;
    AudioManager mAudioManager;

    private Song mCurrentSong;
    SongTimeLine mSongTimeLine;

    private int mPendingSeek;
    private boolean mHeadsetOnly;
    /**
     * tham chiếu tới Playcounts helper class
     */
    private PlayCountsHelper mPlayCounts;

    private static final int NOTIFICATION_ID = 2;
    /**
     * A remote control client implementation
     */
    private RemoteControl.Client mRemoteControlClient;
    /**
     * Id của bài cho mPendingSeek.bằng -1 thì bài hát ko đúng
     * thì mPendingSeek sẽ bằng 0
     */
    private long mPendingSeekSong;
    /**
     * Nếu set true,thì notification sẽ được hiển thị cho dù đang pausing
     */
    private boolean mForceNotificationVisible;

    /**
     * mảng static tham chiếu đến các playbackActivities, sử dụng cho các hàm callbacks
     */
    private static final ArrayList<TimelineCallback> sCallbacks = new ArrayList<TimelineCallback>(5);

    private String mErrorMessage;

    /**
     * Object được dùng  PlaybackService startup waiting.
     */
    private static final Object[] sWait = new Object[0];

    /**
     * Object dùng để lock các trạng thái khi đang synchronized
     */
    final Object[] mStateLock = new Object[0];
    /**
     * Nếu được set,thì sẽ play nhạc.
     * 1
     */
    public static final int FLAG_PLAYING = 0x1;
    /**
     * Được set khi không có media nào trong device.
     * 0x2 = 10
     */
    public static final int FLAG_NO_MEDIA = 0x2;
    /**
     * Set khi xảy ra lỗi,có thể là bài hát ko mở được.
     * 0x4 = 100
     */
    public static final int FLAG_ERROR = 0x4;
    /**
     * Được Set khi người dùng cần phải chọn bài hát.
     * 0x8 = 1000
     */
    public static final int FLAG_EMPTY_QUEUE = 0x8;
    public static final int SHIFT_FINISH = 4;
    /**
     * 3 bit này sẽ là một trong  SongTimeline.FINISH_*.
     * 0x7 = 111
     * 0x7 << 4 = 111 0000
     */
    public static final int MASK_FINISH = 0x7 << SHIFT_FINISH;
    public static final int SHIFT_SHUFFLE = 7;
    /**
     * 3 bit này sẽ là một trong SongTimeline.SHUFFLE_*.
     * 0x7 = 111
     * 0x7 << 7 = 11 1000 0000
     */
    public static final int MASK_SHUFFLE = 0x7 << SHIFT_SHUFFLE;
    public static final int SHIFT_DUCKING = 10;

    /**
     * The PlaybackService state, indicating if the service is playing,
     * repeating, etc.
     * <p/>
     * The format of this is 0b00000000_00000000_000000gff_feeedcba,
     * where each bit is:
     * a:   {@link PlaybackService#FLAG_PLAYING}
     * b:   {@link PlaybackService#FLAG_NO_MEDIA}
     * c:   {@link PlaybackService#FLAG_ERROR}
     * d:   {@link PlaybackService#FLAG_EMPTY_QUEUE}
     * eee: {@link PlaybackService#MASK_FINISH}
     * fff: {@link PlaybackService#MASK_SHUFFLE}
     */
    int mState;

    /**
     * Chạy lại bài hát từ đầu nếu bài hát được chạy nhiều hơn 2.5
     */
    private static final int REWIND_AFTER_PLAYED_MS = 2500;

    /**
     * Lưu lại danh sách nhạc đang được play hiên tại trên hàng đợi sau 5000 ms.
     */
    private static final int SAVE_STATE_DELAY = 5000;

    /**
     * Dùng để sử dụng ở mọi nơi,Single-ton.
     */
    public static PlaybackService sInstance;

    /**
     * SharedPreferences được sử dụng trên toàn app.
     */
    private static SharedPreferences sSettings;

    private boolean mMediaPlayerInitialized;

    /**
     * Hành động khi startService(hàm onStartCommand được gọi): toggle playback on/off.
     */
    public static final String ACTION_TOGGLE_PLAYBACK = "cchongxuocmanhinh.sound_plusplus.action.TOGGLE_PLAYBACK";
    /**
     * Hành động khi startService(hàm onStartCommand được gọi): start playback nếu paused.
     */
    public static final String ACTION_PLAY = "chongxuocmanhinh.sound_plusplus.action.PLAY";
    /**
     * Hành động khi startService(hàm onStartCommand được gọi): pause playback nếu playing.
     */
    public static final String ACTION_PAUSE = "chongxuocmanhinh.sound_plusplus.action.PAUSE";

    /**
     * Hành động khi startService(hàm onStartCommand được gọi): chuyển sang bài tiếp theo.
     */
    public static final String ACTION_NEXT_SONG = "chongxuocmanhinh.sound_plusplus.action.NEXT_SONG";
    /**
     * Hành động khi startService(hàm onStartCommand được gọi): advance to the next song.
     * <p/>
     * Giống ACTION_NEXT_SONG, nhưng tự động play bài hát khi chuyển nếu đang pause
     */
    public static final String ACTION_NEXT_SONG_AUTOPLAY = "ch.teamuit.android.soundplusplus.action.NEXT_SONG_AUTOPLAY";
    /**
     * Hành động khi startService(hàm onStartCommand được gọi): chuyển về bài hát trước đó.
     */
    public static final String ACTION_PREVIOUS_SONG = "ch.teamuit.android.soundplusplus.action.PREVIOUS_SONG";
    /**
     * Hành động khi startService(hàm onStartCommand được gọi): chuyển về bài hát trước đó.
     * <p/>
     * Giống ACTION_PREVIOUS_SONG, nhưng tự động play bài hát khi chuyển nếu đang pause.
     */
    public static final String ACTION_PREVIOUS_SONG_AUTOPLAY = "ch.teamuit.android.soundplusplus.action.PREVIOUS_SONG_AUTOPLAY";
    /**
     * Hành động khi startService(hàm onStartCommand được gọi): toggle playback on/off.
     * <p/>
     * hoạt động giống ACTION_TOGGLE_PLAYBACK,nhưng ko bắt buộc ẩn thông báo như ACTION_TOGGLE_PLAYBACK
     */
    public static final String ACTION_TOGGLE_PLAYBACK_NOTIFICATION = "chongxuocmanhinh.sound_plusplus.action.TOGGLE_PLAYBACK_NOTIFICATION";
    /**
     * Pause music and hide the notifcation.
     */
    public static final String ACTION_CLOSE_NOTIFICATION = "chongxuocmanhinh.sound_plusplus.action.CLOSE_NOTIFICATION";

    public static final int NEVER = 0;
    public static final int WHEN_PLAYING = 1;
    public static final int ALWAYS = 2;

    /**
     * Chế độ của notification,1 trong 3 chế độ ở trên
     */
    private int mNotificationMode;
    /**
     *
     */
    private boolean mIgnoreAudioFocusLoss;
    private boolean mTransientAudioLoss;

    @Override
    public void onCreate() {
        HandlerThread thread = new HandlerThread("PlaybackService", Process.THREAD_PRIORITY_DEFAULT);
        thread.start();
        Log.d("PlayTest", "Created Service");
        mSongTimeLine = new SongTimeLine(this);
        mSongTimeLine.setCallback(this);

        int state = loadState();
        mPlayCounts = new PlayCountsHelper(this);

        mMediaPlayer = getNewMediaPLayer();
        mPreparedMediaPlayer = getNewMediaPLayer();
        //Ta chỉ sử dụng duy nhất một audioseissionId
        mPreparedMediaPlayer.setAudioSessionId(mMediaPlayer.getAudioSessionId());
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        SharedPreferences settings = getSettings(this);
        //settings.registerOnSharedPreferenceChangeListener(this);
        mNotificationMode = Integer.parseInt(settings.getString(PrefKeys.NOTIFICATION_MODE, PrefDefaults.NOTIFICATION_MODE));
        mHeadsetOnly = settings.getBoolean(PrefKeys.HEADSET_ONLY, PrefDefaults.HEADSET_ONLY);

        mIgnoreAudioFocusLoss = settings.getBoolean(PrefKeys.IGNORE_AUDIOFOCUS_LOSS, PrefDefaults.IGNORE_AUDIOFOCUS_LOSS);

        ///================//
        mRemoteControlClient = new RemoteControl().getClient(this);
        mRemoteControlClient.initializeRemote();


        mLooper = thread.getLooper();
        mHandler = new Handler(mLooper, this);
        updateState(state);
        setCurrentSong(0);
//        mState !=
        sInstance = this;
        synchronized (sWait) {
            sWait.notifyAll();
        }
    }


    @Override
    public void onDestroy() {

        sInstance = null;

        mLooper.quit();

        // clear the notification
        stopForeground(true);

        enterSleepState();

        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }

        if (mPreparedMediaPlayer != null) {
            mPreparedMediaPlayer.release();
            mPreparedMediaPlayer = null;
        }

        if (mRemoteControlClient != null)
            mRemoteControlClient.unregisterRemote();

        super.onDestroy();
    }

    /**
     * Trả về thằng  PlaybackService instance tạo mới nếu chưa có.
     */
    public static PlaybackService get(Context context) {
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
        Log.d("TestPlay", "Return Service");
        return sInstance;
    }


    public static void addTimelineCallback(TimelineCallback consumer) {
        sCallbacks.add(consumer);
    }


    public static void removeTimelineCallback(TimelineCallback consumer) {
        sCallbacks.remove(consumer);
    }

    private SoundPlusPLusMediaPlayer getNewMediaPLayer() {
        SoundPlusPLusMediaPlayer mp = new SoundPlusPLusMediaPlayer(this);
        mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mp.setOnCompletionListener(this);
        mp.setOnErrorListener(this);
        return mp;
    }

    private void prepareMediaPlayer(SoundPlusPLusMediaPlayer mp, String path) throws IOException {
        mp.setDataSource(path);
        mp.prepare();
    }

    private void processSong(Song song) {

        try {

            mMediaPlayerInitialized = false;
            mMediaPlayer.reset();
            Log.d("Test", "Song path : " + song.path);
            if (mPreparedMediaPlayer.isPlaying()) {
                // The prepared media player is playing as the previous song
                // reched its end 'naturally' (-> gapless)
                // We can now swap mPreparedMediaPlayer and mMediaPlayer
                SoundPlusPLusMediaPlayer tmpPlayer = mMediaPlayer;
                mMediaPlayer = mPreparedMediaPlayer;
                mPreparedMediaPlayer = tmpPlayer; // this was mMediaPlayer and is in reset() state
            } else {
                prepareMediaPlayer(mMediaPlayer, song.path);
            }

            mMediaPlayerInitialized = true;

            if (mPendingSeek != 0 && mPendingSeekSong == song.id) {
                mMediaPlayer.seekTo(mPendingSeek);
                mPendingSeek = 0;
            }

            if ((mState & FLAG_PLAYING) != 0)
                mMediaPlayer.start();


        } catch (IOException e) {
            Log.d("Test", "Error!");

        }
        updateNotification();
    }

    public void addSongs(QueryTask query) {
        Log.d("Testtt", "Service Add song");
        mHandler.sendMessage(mHandler.obtainMessage(MSG_QUERY, query));
    }

    /**
     * If playing, pause. If paused, play.
     *
     * @return The new state after this is called.
     */
    public int playPause() {
        mForceNotificationVisible = false;
        Log.d("TestPlayPause", "Enter service playpause()");
        synchronized (mStateLock) {
            if ((mState & FLAG_PLAYING) != 0)
                return pause();
            else
                return play();
        }
    }

    /**
     * Bắt đầu chơi nhạc nếu đang pause
     *
     * @return
     */
    public int play() {
        Log.d("TestPlayPause", "Enter service play()");
        synchronized (mStateLock) {
            if ((mState & FLAG_EMPTY_QUEUE) != 0) {
                setCurrentSong(0);
            }

            int state = updateState(mState | FLAG_PLAYING);
            return state;
        }
    }

    public int pause() {
        Log.d("TestPlayPause", "Enter service pause()");
        synchronized (mStateLock) {
            int state = updateState(mState & ~FLAG_PLAYING);
            return state;
        }
    }

    /**
     * @return
     */
    public int cycleFinishAction() {
        synchronized (mStateLock) {
            int mode = finishAction(mState) + 1;
            if (mode > SongTimeLine.FINISH_RANDOM)
                mode = SongTimeLine.FINISH_STOP;
            return setFinishAction(mode);
        }
    }

    /**
     * Lặp vòng shuffleMode.
     *
     * @return The new state after this is called.
     */
    public int cycleShuffle() {
        synchronized (mStateLock) {
            int mode = shuffleMode(mState) + 1;
            if (mode > SongTimeLine.SHUFFLE_ALBUMS)
                mode = SongTimeLine.SHUFFLE_NONE; // end reached: switch to none
            return setShuffleMode(mode);
        }
    }


    /**
     * Thay đổi hành động sau khi kết thúc bài hát (lặp lại,random....)
     *
     * @param action
     * @return
     */
    public int setFinishAction(int action) {
        synchronized (mStateLock) {
            return updateState(mState & ~MASK_FINISH | action << SHIFT_FINISH);
        }
    }


    public int setShuffleMode(int mode) {
        synchronized (mStateLock) {
            return updateState(mState & ~MASK_SHUFFLE | mode << SHIFT_SHUFFLE);
        }
    }

    /**
     * Thay đổi state của service
     *
     * @param state
     * @return
     */
    private int updateState(int state) {
        if ((state & (FLAG_NO_MEDIA | FLAG_ERROR | FLAG_EMPTY_QUEUE)) != 0)
            state &= ~FLAG_PLAYING;

        int oldState = mState;
        mState = state;

        if (state != oldState) {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_PROCESS_STATE, oldState, state));
            mHandler.sendMessage(mHandler.obtainMessage(MSG_BROADCAST_CHANGE, state, 0, new TimestampedObject(null)));
        }

        return state;
    }

    /**
     * Defer entering deep sleep for this time (in ms).
     */
    private static final int SLEEP_STATE_DELAY = 60000;

    private void processNewState(int oldState, int state) {
        Log.d("TestPlayPause", "processNewState()");
        int toggled = oldState ^ state;
        if (((toggled & FLAG_PLAYING) != 0) && mCurrentSong != null) {
            if ((state & FLAG_PLAYING) != 0) {//Nếu đang pause và state mới là play
                Log.d("TestPlayPause", "mMediaPlayer.start()");
                if (mMediaPlayerInitialized)
                    mMediaPlayer.start();

                if (mNotificationMode != NEVER)
                    startForeground(NOTIFICATION_ID, createNotification(mCurrentSong, mState, mNotificationMode));

                //Dùng để getfocus,sẽ không thể hiển thị removecontrol trên lockscreen nếu ko request hoặc request failed
                final int result = mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
                if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    unsetFlag(FLAG_PLAYING);
                }

                mHandler.removeMessages(MSG_ENTER_SLEEP_STATE);
            } else {//Nếu state mới là pause
                Log.d("TestPlayPause", " mMediaPlayer.pause();");
                if (mMediaPlayerInitialized)
                    mMediaPlayer.pause();

                /**
                 * Khi chuyển trạng thái sang pause  nếu ko cần hiển thị thông báo
                 * thì có thể stopForeground
                 */
                boolean removeNotification = (mForceNotificationVisible == false && mNotificationMode != ALWAYS);
                stopForeground(removeNotification);
                updateNotification();

                mHandler.sendEmptyMessageDelayed(MSG_ENTER_SLEEP_STATE, SLEEP_STATE_DELAY);
            }
        }

        if ((toggled & FLAG_NO_MEDIA) != 0 && (state & FLAG_NO_MEDIA) != 0) {
            Song song = mCurrentSong;
            if (song != null && mMediaPlayerInitialized) {
                mPendingSeek = mMediaPlayer.getCurrentPosition();
                mPendingSeekSong = song.id;
            }
        }

        if ((toggled & MASK_SHUFFLE) != 0)
            mSongTimeLine.setShuffleMode(shuffleMode(state));
        if ((toggled & MASK_FINISH) != 0)
            mSongTimeLine.setFinishAction(finishAction(state));

    }
    //=========================Handler.Callback=====================================//
    /**
     * Chạy query và add các kết quả vào songtimeline.
     * <p/>
     * obj là  QueryTask. arg1 là chế độ add (add mode) (one of SongTimeLine.MODE_*)
     */

    /**
     * Releases mWakeLock and closes any open AudioFx sessions
     */
    private static final int MSG_ENTER_SLEEP_STATE = 1;
    private static final int MSG_QUERY = 2;
    private static final int MSG_BROADCAST_CHANGE = 10;
    private static final int MSG_SAVE_STATE = 12;
    private static final int MSG_PROCESS_SONG = 13;
    private static final int MSG_PROCESS_STATE = 14;
    private static final int MSG_UPDATE_PLAYCOUNTS = 17;

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_QUERY:
                runQuery((QueryTask) msg.obj);
                break;
            case MSG_PROCESS_SONG:
                processSong((Song) msg.obj);
                break;
            case MSG_BROADCAST_CHANGE:
                TimestampedObject tso = (TimestampedObject) msg.obj;
                broadcastChange(msg.arg1, (Song) tso.object, tso.uptime);
                break;
            case MSG_SAVE_STATE:
                saveState(0);
                break;
            case MSG_PROCESS_STATE:
                processNewState(msg.arg1, msg.arg2);
                break;
            case MSG_UPDATE_PLAYCOUNTS:
                Song song = (Song) msg.obj;
                boolean played = msg.arg1 == 1;
                mPlayCounts.countSong(song,played);
            case MSG_ENTER_SLEEP_STATE:
                enterSleepState();
                break;
            default:
                return false;
        }
        return true;
    }

    /**
     * @param query
     */
    public void runQuery(QueryTask query) {
        Log.d("Testtt", "Service run query");
        int count = mSongTimeLine.addSongs(this, query);
        int text;
        switch (query.mode) {
            case SongTimeLine.MODE_PLAY:
            case SongTimeLine.MODE_PLAY_ID_FIRST:
                if (count != 0 && (mState & FLAG_PLAYING) == 0)
                    setFlag(FLAG_PLAYING);
                break;
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

        //Đếm số lần được mở,played
        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_UPDATE_PLAYCOUNTS, 1, 0, mCurrentSong), 800);

        if (finishAction(mState) == SongTimeLine.FINISH_REPEAT_CURRENT) {
            setCurrentSong(0);
        } else if (finishAction(mState) == SongTimeLine.FINISH_STOP_CURRENT) {
            unsetFlag(FLAG_PLAYING);
            setCurrentSong(+1);
        } else if (mSongTimeLine.isEndQueue()) {
            unsetFlag(FLAG_PLAYING);
        } else {
            setCurrentSong(+1);
        }
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

    @Override
    public void timelineChanged() {

        mHandler.removeMessages(MSG_SAVE_STATE);
        mHandler.sendEmptyMessageDelayed(MSG_SAVE_STATE, SAVE_STATE_DELAY);

        ArrayList<TimelineCallback> list = sCallbacks;
        for (int i = list.size(); --i != -1; )
            list.get(i).onTimelineChanged();
    }

    @Override
    public void positionInfoChanged() {

    }


    /**
     * Khi PlaybackService  sleep / shutdown
     * Đóng tất cả audiosession
     */
    private void enterSleepState(){
        if(mMediaPlayer != null){
            saveState(mMediaPlayer.getCurrentPosition());
        }
    }

    /**
     * Di chuyển tơi bài hát tiếp theo hay trước đó trong hàng đợi
     *
     * @return
     */
    public Song shiftCurrentSong(int delta) {
        preparePlayCountsUpdate(delta);
        Song song = setCurrentSong(delta);
        return song;
    }

    /**
     * Di chuyển tới bài hát hay album kế tiếp trong timline
     *
     * @param delta Là một trong  SongTimeline.SHIFT_*. 0 cungx có thể được truyền
     *              để khởi tạo bài hát với media player ,notification.....
     * @return The new current song
     */
    public Song setCurrentSong(int delta) {
        if (mMediaPlayer == null)
            return null;

        if (mMediaPlayer.isPlaying())
            mMediaPlayer.stop();

        Song song = mSongTimeLine.shiftCurrentSong(delta);
        mCurrentSong = song;

        if (song == null) {
            return null;
        } else if ((mState & (FLAG_NO_MEDIA | FLAG_EMPTY_QUEUE)) != 0) {
            synchronized (mStateLock) {
                updateState(mState & ~(FLAG_EMPTY_QUEUE | FLAG_NO_MEDIA));
            }
        }


        mHandler.removeMessages(MSG_PROCESS_SONG);

        mMediaPlayerInitialized = false;
        mHandler.sendMessage(mHandler.obtainMessage(MSG_PROCESS_SONG, song));
        mHandler.sendMessage(mHandler.obtainMessage(MSG_BROADCAST_CHANGE, -1, 0, new TimestampedObject(song)));

        return song;
    }

    public Song getSong(int delta) {
        if (mSongTimeLine == null)
            return null;
//        if (delta == 0)
//            return mCurrentSong;
        return mSongTimeLine.getSong(delta);
    }

    /**
     * Nhảy tới bài hát trước đó hoặc chạy lại bài hát từ đầu
     *
     * @return
     */
    public Song rewindCurrentSong() {
        int delta = SongTimeLine.SHIFT_PREVIOUS_SONG;
        if (isPlaying() && getPosition() > REWIND_AFTER_PLAYED_MS && getDuration() > REWIND_AFTER_PLAYED_MS * 2) {
            delta = SongTimeLine.SHIFT_KEEP_SONG;
        }

        return shiftCurrentSong(delta);
    }

    /**
     * Trả về trạng thái playing của bài hát hiện tại
     */
    public boolean isPlaying() {
        Log.d("Testtt", ((mState & FLAG_PLAYING) != 0) ? "" : "isPlaying false");
        return (mState & FLAG_PLAYING) != 0;
    }

    /**
     * Trả về thời gian của bài hát dưới dạng miliseconds
     */
    public int getDuration() {
        if (!mMediaPlayerInitialized)
            return 0;
        return mMediaPlayer.getDuration();
    }

    /**
     * Returns the current position in current song in milliseconds.
     */
    public int getPosition() {
        if (!mMediaPlayerInitialized) {
            Log.d("TestPlay", "mMediaPlayerInitialized = false");
            return 0;
        }
        return mMediaPlayer.getCurrentPosition();
    }

    private void preparePlayCountsUpdate(int delta){
        if(isPlaying()){
            double pctPlayed = (double) getPosition()/getDuration();
            int action = 0;

            if (delta == SongTimeLine.SHIFT_KEEP_SONG && pctPlayed >= 0.8) {
                action = 1; // nếu bài hát được mở lại và đã được mở hơn 80% thì đếm
            } else if(delta == SongTimeLine.SHIFT_NEXT_SONG && pctPlayed <= 0.5) {
                action = -1;
            }

            if (action != 0) {
                mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_UPDATE_PLAYCOUNTS, action, 0, mCurrentSong), 800);
            }
        }
    }

    /**
     * Seek to a position in the current song.
     *
     * @param progress Proportion of song completed (where 1000 is the end of the song)
     */
    public void seekToProgress(int progress) {
        if (!mMediaPlayerInitialized)
            return;
        long position = (long) mMediaPlayer.getDuration() * progress / 1000;
        mMediaPlayer.seekTo((int) position);
    }


    public static boolean hasInstance() {
        return sInstance != null;
    }

    /**
     * Set a state flag.
     */
    public void setFlag(int flag) {
        synchronized (mStateLock) {
            updateState(mState | flag);
        }
    }

    /**
     * Unset a state flag.
     */
    public void unsetFlag(int flag) {
        synchronized (mStateLock) {
            updateState(mState & ~flag);
        }
    }

    /**
     * @param state
     * @param song
     * @param uptime
     */
    private void broadcastChange(int state, Song song, long uptime) {

        if (state != -1) {
            ArrayList<TimelineCallback> list = sCallbacks;
            for (int i = list.size(); --i != -1; )
                list.get(i).setState(uptime, state);
        }

        if (song != null) {
            ArrayList<TimelineCallback> list = sCallbacks;
            for (int i = list.size(); --i != -1; )
                list.get(i).setSong(uptime, song);
        }

        mRemoteControlClient.updateRemote(mCurrentSong, mState, mForceNotificationVisible);
    }

    /**
     * Trả về bài hát tại vị trí trong queue.
     *
     * @param id
     * @return
     */
    public Song getSongByQueuePosition(int id) {
        return mSongTimeLine.getSongByQueuePosition(id);
    }

    /**
     * Nhảy tới vị trí được đưa vào
     *
     * @param id
     */
    public void jumpToQueuePosition(int id) {
        mSongTimeLine.setCurrentQueuePosition(id);
        play();
    }

    public int getTimelinePosition() {
        return mSongTimeLine.getPosition();
    }

    public int getTimeLineLength() {
        return mSongTimeLine.getLength();
    }

    /**
     * Xóa hàng đợi các bài hát đằng sau bài hát hiện tại.
     */
    public void clearQueue() {
        mSongTimeLine.clearQueue();
    }

    /**
     * Xóa toàn bộ danh sách nhạc.
     */
    public void emptyQueue() {
        pause();
        setFlag(FLAG_EMPTY_QUEUE);
        mSongTimeLine.emptyQueue();
    }

    /**
     * Đưa tất cả các bài hát có cùng artist/album/genre giống với bài hát được truyền vào
     * vô hàng đợi play
     * @param song bìa hát được truyền vào
     * @param type media type, 1 trong MediaUtils.TYPE_ALBUM, TYPE_ARTIST,
     * or TYPE_GENRE
     */
    public void enqueueFromSong(Song song,int type){
        if (song == null)
            return;

        long id;
        switch (type){
            case MediaUtils.TYPE_ARTIST:
                id = song.artistId;
                break;
            case MediaUtils.TYPE_ALBUM:
                id = song.albumId;
                break;
            case MediaUtils.TYPE_GENRE:
                id = MediaUtils.queryGenreForSong(getContentResolver(), song.id);
                break;
            default:
                throw new IllegalArgumentException("Unsupported media type: " + type);
        }

        //Chỉ query các bài hát cùng artist/album/genre và trừ bản thân nó ra
        String selection = "_id!=" + song.id;
        QueryTask queryTask = MediaUtils.buildQuery(type,id,Song.FILLED_PROJECTION,selection);
        queryTask.mode = SongTimeLine.MODE_FLUSH_AND_PLAY_NEXT;
        addSongs(queryTask);
    }

    public void moveSongPosition(int from,int to){
        mSongTimeLine.moveSongPosition(from ,to);
    }

    /**
     * Trả về finish action khi truyền trạng thái vào
     * Đầu tiên ta đưa hết 4  bits phí sau về 0,sau đó dịch theo SHIFT_FINISH
     * ta sẽ có được finishaction của state được truyền vào
     *
     * @param state
     * @return Trạng thái sau khi kết thúc play 1 bài hát.1 trong SongTimeline.FINISH_*.
     */
    public static int finishAction(int state) {
        return (state & MASK_FINISH) >> SHIFT_FINISH;
    }

    /**
     * Trả về shuffle mode khi truyền trạng thái vào
     * Đầu tiên ta đưa hết 7  bits phí sau về 0,sau đó dịch theo SHIFT_SHUFFLE(7 bits)
     * ta sẽ có được shuffle mode của state được truyền vào
     *
     * @param state
     * @return Trạng thái sau khi kết thúc play 1 bài hát.1 trong SongTimeLine.SHUFFLE_*.
     */
    public static int shuffleMode(int state) {
        return (state & MASK_SHUFFLE) >> SHIFT_SHUFFLE;
    }

    public int loadState() {
        int state = 0;
        try {
            DataInputStream in = new DataInputStream(openFileInput(STATE_FILE));

            if (in.readLong() == STATE_FILE_MAGIC && in.readInt() == STATE_VERSION) {
                mPendingSeek = in.readInt();
                mPendingSeekSong = in.readLong();
                mSongTimeLine.readState(in);
                state |= mSongTimeLine.getFinishAction() << SHIFT_FINISH;
            }

            in.close();
        } catch (EOFException e) {
            Log.w("SoundPlusPlus", "Failed to load state", e);
        } catch (IOException e) {
            Log.w("SoundPlusPlus", "Failed to load state", e);
        }

        return state;
    }

    public void saveState(int pendingSeek) {
        try {
            DataOutputStream out = new DataOutputStream(openFileOutput(STATE_FILE, 0));
            Song song = mCurrentSong;
            out.writeLong(STATE_FILE_MAGIC);
            out.writeInt(STATE_VERSION);
            out.writeInt(pendingSeek);
            out.writeLong(song == null ? -1 : song.id);
            mSongTimeLine.writeState(out);
            out.close();
        } catch (IOException e) {
            Log.w("SoundPlusPlus", "Failed to save state", e);
        }
    }


    /**
     * Bỏ bài hát ra khỏi hàng đợi
     *
     * @param which index to remove
     */
    public void removeSongPosition(int which) {
        mSongTimeLine.removeSongPosition(which);
    }

    /**
     * Trả về đối tượng SharedPreferences chứa settings của playbackservice,
     * tạo mới nếu cần thiết.
     *
     * @param context
     * @return
     */
    public static SharedPreferences getSettings(Context context) {
        if (sSettings == null) {
            sSettings = PreferenceManager.getDefaultSharedPreferences(context);
        }

        return sSettings;
    }

    private void updateNotification() {
        Log.d("NotificationTest", "UpdateNotification");
        //Nếu mode đang playing và state đang playing và có bài hát
        if ((mForceNotificationVisible || mNotificationMode == ALWAYS || mNotificationMode == WHEN_PLAYING && (mState & FLAG_PLAYING) != 0) && mCurrentSong != null) {
            Log.d("NotificationTest", "notify");
            mNotificationManager.notify(NOTIFICATION_ID, createNotification(mCurrentSong, mState, mNotificationMode));
        } else mNotificationManager.cancel(NOTIFICATION_ID);

    }

    public Notification createNotification(Song song, int state, int mode) {
        Log.d("NotificationTest", "createNotification");
        boolean playing = (state & FLAG_PLAYING) != 0;

        //views cho notification bình thường
        RemoteViews views = new RemoteViews(getPackageName(), R.layout.notification);
        //expand views với nhiều chức năng hơn cho các thiết bị API lv cao hơn 16
        RemoteViews expanded = new RemoteViews(getPackageName(), R.layout.notification_expanded);
        Log.d("NotificationTest", "Finish infate layout");
        /**
         * Cần implement COVER sau và chỗ này
         */

        int playButton = ThemeHelper.getPlayButtonResource(playing);
        views.setImageViewResource(R.id.play_pause, playButton);
        expanded.setImageViewResource(R.id.play_pause,playButton);
        Log.d("NotificationTest", "Finish get playbutton");

        //==============================================================================//
        ComponentName service = new ComponentName(this, PlaybackService.class);

        //bấm nút previous,có thêm trên expanded views
        Intent previous = new Intent(PlaybackService.ACTION_PREVIOUS_SONG);
        previous.setComponent(service);
        expanded.setOnClickPendingIntent(R.id.previous, PendingIntent.getService(this, 0, previous, 0));
        //==============================================================================//
        //Bấm nút pause play
        Intent playPause = new Intent(PlaybackService.ACTION_TOGGLE_PLAYBACK_NOTIFICATION);
        playPause.setComponent(service);
        views.setOnClickPendingIntent(R.id.play_pause, PendingIntent.getService(this, 0, playPause, 0));
        expanded.setOnClickPendingIntent(R.id.play_pause, PendingIntent.getService(this, 0, playPause, 0));
        Log.d("NotificationTest", "Finish set playPause");
        //==============================================================================//
        //Bấm nút next
        Intent next = new Intent(PlaybackService.ACTION_NEXT_SONG);
        next.setComponent(service);
        views.setOnClickPendingIntent(R.id.next, PendingIntent.getService(this, 0, next, 0));
        expanded.setOnClickPendingIntent(R.id.next, PendingIntent.getService(this, 0, next, 0));
        Log.d("NotificationTest", "Finish set next");
        //==============================================================================//
        //cấu hình cho nút close
        int closeButtonVisibility = (mode == WHEN_PLAYING) ? View.VISIBLE : View.INVISIBLE;
        Intent close = new Intent(PlaybackService.ACTION_CLOSE_NOTIFICATION);
        close.setComponent(service);
        views.setOnClickPendingIntent(R.id.close, PendingIntent.getService(this, 0, close, 0));
        views.setViewVisibility(R.id.close, closeButtonVisibility);
        expanded.setOnClickPendingIntent(R.id.close, PendingIntent.getService(this, 0, close, 0));
        expanded.setViewVisibility(R.id.close, closeButtonVisibility);
        Log.d("NotificationTest", "Finish set close");


        //set 2 cái text view cho notification
        views.setTextViewText(R.id.title, song.title);
        views.setTextViewText(R.id.artist, song.artist);
        expanded.setTextViewText(R.id.title, song.title);
        expanded.setTextViewText(R.id.album, song.album);
        expanded.setTextViewText(R.id.artist, song.artist);
        Log.d("NotificationTest", "Finish set title,artist");
        Notification notification = new Notification();
        notification.contentView = views;
        notification.icon = R.drawable.status_icon;
        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        //notification.contentIntent = mNotificationAction;
        // notification.visibility = Notification.VISIBILITY_PUBLIC;
        Log.d("NotificationTest", "Return notification");
        //Sử dụng expaned view cho android 4.x trở lên
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            // expanded view is available since 4.1
            notification.bigContentView = expanded;
        }
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            notification.visibility = Notification.VISIBILITY_PUBLIC;
        }
        return notification;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("NotificationTest", "onStartCommand");
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_TOGGLE_PLAYBACK.equals(action)) {
                Log.d("NotificationTest", "ACTION_TOGGLE_PLAYBACK");
                playPause();
            } else if (ACTION_TOGGLE_PLAYBACK_NOTIFICATION.equals(action)) {
                Log.d("NotificationTest", "ACTION_TOGGLE_PLAYBACK_NOTIFICATION");
                mForceNotificationVisible = true;
                synchronized (mStateLock) {
                    if ((mState & FLAG_PLAYING) != 0)
                        pause();
                    else
                        play();
                }
            } else if (ACTION_NEXT_SONG.equals(action)) {
                Log.d("NotificationTest", "ACTION_NEXT_SONG");
                shiftCurrentSong(SongTimeLine.SHIFT_NEXT_SONG);
            } else if (ACTION_NEXT_SONG_AUTOPLAY.equals(action)) {
                shiftCurrentSong(SongTimeLine.SHIFT_NEXT_SONG);
                play();
            } else if (ACTION_PLAY.equals(action)) {
                Log.d("NotificationTest", "ACTION_PLAY");
                play();
            } else if (ACTION_PREVIOUS_SONG.equals(action)) {
                rewindCurrentSong();
            } else if (ACTION_PREVIOUS_SONG_AUTOPLAY.equals(action)) {
                rewindCurrentSong();
                play();
            } else if (ACTION_PAUSE.equals(action)) {
                Log.d("NotificationTest", "ACTION_PAUSE");
                pause();
            } else if (ACTION_CLOSE_NOTIFICATION.equals(action)) {
                Log.d("NotificationTest", "ACTION_CLOSE_NOTIFICATION");
                mForceNotificationVisible = false;
                pause();
                stopForeground(true); // sometimes required to clear notification
                updateNotification();
            }
        }
        return START_NOT_STICKY;
        //return super.onStartCommand(intent,flags,startId);
    }

    //===================AudioManager.OnAudioFocusChangeListener================================//
    @Override
    public void onAudioFocusChange(int focusChange) {
        Log.d("TestRemote", "audio focus change: " + focusChange);
        if (mIgnoreAudioFocusLoss) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                case AudioManager.AUDIOFOCUS_LOSS:
                    focusChange = AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK;
            }
        }

        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                synchronized (mStateLock) {
                    if ((mState & FLAG_PLAYING) != 0) {
                        mTransientAudioLoss = true;

                        if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
//                            setFlag(FLAG_DUCKING);
                        } else {
                            mForceNotificationVisible = true;
                            unsetFlag(FLAG_PLAYING);
                        }
                    }
                    break;
                }
            case AudioManager.AUDIOFOCUS_LOSS:
                mTransientAudioLoss = false;
                mForceNotificationVisible = true;
                unsetFlag(FLAG_PLAYING);
                break;
            case AudioManager.AUDIOFOCUS_GAIN:
                if (mTransientAudioLoss) {
                    mTransientAudioLoss = false;
                    // Restore all flags possibly changed by AUDIOFOCUS_LOSS_TRANSIENT_*
//                    unsetFlag(FLAG_DUCKING);
                    setFlag(FLAG_PLAYING);
                }
                break;
        }
    }


    //===============SharedPreferences.OnSharedPreferenceChangeListener===============//
//    @Override
//    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
//
//    }
}
