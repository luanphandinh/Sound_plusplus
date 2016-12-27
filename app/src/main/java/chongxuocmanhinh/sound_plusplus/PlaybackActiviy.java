package chongxuocmanhinh.sound_plusplus;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

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

        SharedPreferences prefs = PlaybackService.getSettings(this);
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

    /************created by lordhung*****************/
    /**
     * Giống MSG_ADD_TO_PLAYLIST nhưng tạo playlist mới ngay lập tức (hoặc overwrite list đã có)
    */
    protected static final int MSG_CREATE_PLAYLIST = 0;
    /**
     * Gọi renamePlaylist với result từ NewPlaylistDialog lưu trong obj
     */
    protected static final int MSG_RENAME_PLAYLIST = 1;
    /**
     * Gọi addToPlaylist với data từ playlisttask obj
     */
    protected static final int MSG_ADD_TO_PLAYLIST = 2;
    /**
     * Gọi removeFromPlaylist với data từ playlisttask object
     */
    protected static final int MSG_REMOVE_FROM_PLAYLIST = 3;
    /**
     * Remove 1 mdeia object
     */
    protected static final int MSG_DELETE = 4;
    /**
     * Lưu queue hiện tại như 1 playlist
     */
    protected static final int MSG_ADD_QUEUE_TO_PLAYLIST = 5;
    /**
     * Thông báo đã thay đổi member nào đó trong playlist
     */
    protected static final int MSG_NOTIFY_PLAYLIST_CHANGED = 6;
    @Override
    public boolean handleMessage(Message message) {
        switch (message.what) {
            case MSG_CREATE_PLAYLIST: {
                PlaylistTask playlistTask = (PlaylistTask)message.obj;
                int nextAction = message.arg1;
                long playlistId = Playlist.createPlaylist(getContentResolver(), playlistTask.name);
                playlistTask.playlistId = playlistId;
                mHandler.sendMessage(mHandler.obtainMessage(nextAction, playlistTask));
                break;
            }
            case MSG_ADD_TO_PLAYLIST: {
                PlaylistTask playlistTask = (PlaylistTask)message.obj;
                addToPlaylist(playlistTask);
                break;
            }
            case MSG_ADD_QUEUE_TO_PLAYLIST: {
                PlaylistTask playlistTask = (PlaylistTask)message.obj;
                playlistTask.audioIds = new ArrayList<Long>();
                Song song;
                PlaybackService service = PlaybackService.get(this);
                for (int i=0; ; i++) {
                    song = service.getSongByQueuePosition(i);
                    if (song == null)
                        break;
                    playlistTask.audioIds.add(song.id);
                }
                addToPlaylist(playlistTask);
                break;
            }
            case MSG_REMOVE_FROM_PLAYLIST: {
                PlaylistTask playlistTask = (PlaylistTask)message.obj;
                removeFromPlaylist(playlistTask);
                break;
            }
            case MSG_RENAME_PLAYLIST: {
                PlaylistTask playlistTask = (PlaylistTask)message.obj;
                Playlist.renamePlaylist(getContentResolver(), playlistTask.playlistId, playlistTask.name);
                break;
            }
            case MSG_DELETE: {
                break;
            }
            case MSG_NOTIFY_PLAYLIST_CHANGED: {
                // super class sẽ hiện thực  nó
                break;
            }
            default:
                return false;
        }
        return true;
    }
    /**
     * Thêm 1 tập hợp các bài hát thể hiện trong playlistTask vào 1 playlist. Hiển thị
     * một Toast thông báo nếu thành công
     *
     * @param playlistTask PlaylistTask chờ để thực thi
     */
    protected void addToPlaylist(PlaylistTask playlistTask) {
        int count = 0;

        if (playlistTask.query != null) {
            count += Playlist.addToPlaylist(getContentResolver(), playlistTask.playlistId, playlistTask.query);
        }

        if (playlistTask.audioIds != null) {
            count += Playlist.addToPlaylist(getContentResolver(), playlistTask.playlistId, playlistTask.audioIds);
        }

        String message = getResources().getQuantityString(R.plurals.added_to_playlist, count, count, playlistTask.name);
        showToast(message, Toast.LENGTH_SHORT);
        mHandler.sendEmptyMessage(MSG_NOTIFY_PLAYLIST_CHANGED);
    }

    /**
     * Xóa 1 tập hợp các bài hát thể hiện trong playlistTask vào 1 playlist. Hiển thị
     * một Toast thông báo nếu thành công
     *
     * @param playlistTask PlaylistTask chờ để thực thi
     */
    private void removeFromPlaylist(PlaylistTask playlistTask) {
        int count = 0;

        if (playlistTask.query != null) {
            throw new IllegalArgumentException("Delete by query is not implemented yet");
        }

        if (playlistTask.audioIds != null) {
            count += Playlist.removeFromPlaylist(getContentResolver(), playlistTask.playlistId, playlistTask.audioIds);
        }

        String message = getResources().getQuantityString(R.plurals.removed_from_playlist, count, count, playlistTask.name);
        showToast(message, Toast.LENGTH_SHORT);
        mHandler.sendEmptyMessage(MSG_NOTIFY_PLAYLIST_CHANGED);
    }

    /**
     * Tạo và hiển thị message toast
     */
    private void showToast(final String message, final int duration) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), message, duration).show();
            }
        });
    }

    /********************************************************/


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
                cycleFinishAction();
                break;
            case R.id.shuffle:
                cycleShuffle();
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
     * Thay đổi hành động sau khi hết bài nhạc
     */
    public void cycleFinishAction(){
        setState(PlaybackService.get(this).cycleFinishAction());
    }

    /**
     * Thay đổi chức năng xáo bài hát của app
     */
    private void cycleShuffle() {
        setState(PlaybackService.get(this).cycleShuffle());
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

        if ((toggled & PlaybackService.MASK_FINISH) != 0 && mEndButton != null) {
            mEndButton.setImageResource(SongTimeLine.FINISH_ICONS[PlaybackService.finishAction(state)]);
        }

        if((toggled & PlaybackService.MASK_SHUFFLE) != 0 && mShuffleButton != null){
            mShuffleButton.setImageResource(SongTimeLine.SHUFFLE_ICONS[PlaybackService.shuffleMode(state)]);
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

    /**
     * Được gọi bởi playbackserivce để thay đổi state
     * @param uptime
     * @param state
     */

    public void setState(long uptime, int state){
        if (uptime > mLastStateEvent) {
            setState(state);
            mLastStateEvent = uptime;
        }
    }


    static final int MENU_SORT = 1;
    static final int MENU_PLAYBACK = 5;
    static final int MENU_SEARCH = 7;
    static final int MENU_CLEAR_QUEUE = 11;
    static final int MENU_SHOW_QUEUE = 13;
    static final int MENU_HIDE_QUEUE = 14;
    static final int MENU_EMPTY_QUEUE = 16;


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //menu.add(0)
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d("TestOptionMenu","Playback ACtivity onOptionsItemSelected");
        switch (item.getItemId()){
            case MENU_CLEAR_QUEUE:
                PlaybackService.get(this).clearQueue();
                break;
            case MENU_EMPTY_QUEUE:
                PlaybackService.get(this).emptyQueue();
                break;
            default:
                return false;

        }
        return true;
    }
}
