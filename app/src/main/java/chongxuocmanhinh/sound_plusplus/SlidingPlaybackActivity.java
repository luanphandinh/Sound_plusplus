package chongxuocmanhinh.sound_plusplus;

import android.os.Message;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Class này dùng để quản lý phần sliding view của app
 * có các seekbar và cái button ....
 * Created by L on 14/12/2016.
 */
public class SlidingPlaybackActivity extends PlaybackActiviy
            implements SlidingView.Callback,
                        SeekBar.OnSeekBarChangeListener
{

    /**
     * menu chừng infalte vào thì cái này đung để tham chieeud dến
     */
    private Menu mMenu;
    /**
     * thanh seekbar
     */
    private SeekBar mSeekBar;

    /**
     * Textivew nằm bên trái thanh seekbar,chỉ thời gian đã trôi qua của bài hát
     */
    private TextView mElapsedView;
    /**
     * Textivew nằm bên phải thanh seekbar,chỉ thời gian của bài hát.
     */
    private TextView mDurationView;
    /**
     * Thời gian hiện tại của bài hát tính theo miliseconds.
     */
    private long mDuration;
    /**
     * Trả về true nếu ta kéo thanh seekbar,ko thì để false
     */
    private boolean mSeekBarTracking;
    /**
     *Trả về true nếu seekBar không nên được update theo thời gian
     */
    private boolean mPaused;
    /**
     * Cached StringBuilder để format bài hát hiện tại.
     */
    private final StringBuilder mTimeBuilder = new StringBuilder();

    /**
     * sliding view
     */
    protected SlidingView mSlidingView;

    @Override
    protected void bindControlButtons() {
        super.bindControlButtons();

        mSlidingView = (SlidingView)findViewById(R.id.sliding_view);
        mSlidingView.setCallback(this);
        mElapsedView = (TextView)findViewById(R.id.elapsed);
        mDurationView = (TextView)findViewById(R.id.duration);
        mSeekBar = (SeekBar)findViewById(R.id.seek_bar);
        mSeekBar.setMax(1000);
        mSeekBar.setOnSeekBarChangeListener(this);
        //setDuration(0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPaused = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPaused = true;
    }

    @Override
    protected void onSongChange(Song song) {
        Log.d("TestPlay","OnSongChange");
        setDuration(song == null ? 0 : song.duration);
        updateElapsedTime();
        super.onSongChange(song);
    }

    /**
     *Cập nhật duration của bài hát
     * @param duration
     */
    private void setDuration(long duration) {
        mDuration = duration;
        mDurationView.setText(DateUtils.formatElapsedTime(mTimeBuilder, duration / 1000));
    }

    /**
     * Cập nhật thanh seek bar theo giây
     */
    private void updateElapsedTime() {
        if(PlaybackService.hasInstance())
            Log.d("TestPlay","has Instance");
        long position = PlaybackService.hasInstance() ? PlaybackService.get(this).getPosition() : 0;
            Log.d("TestPlay","" + PlaybackService.get(this).getPosition());
        if (!mSeekBarTracking) {
            long duration = mDuration;
            mSeekBar.setProgress(duration == 0 ? 0 : (int)(1000 * position / duration));
        }

        mElapsedView.setText(DateUtils.formatElapsedTime(mTimeBuilder, position / 1000));
        Log.d("TestPlay","SendMesage Update Progress");
        if (!mPaused) {
            // Try to update right after the duration increases by one second
            long next = 1050 - position % 1000;
            mUiHandler.removeMessages(MSG_UPDATE_PROGRESS);
            Log.d("TestPlay","SendMesage Update Progress");
            mUiHandler.sendEmptyMessageDelayed(MSG_UPDATE_PROGRESS, next);
        }
    }
    //===============================Handler.CallBack=========================//

    /**
     *Cập nhật thanh seekbar với vị trí hiện tại của bài hát hiện tại.
     * Phải được gọi trên UI Handler
     */
    private static final int MSG_UPDATE_PROGRESS = 20;
    /**
     * Calls {@link PlaybackService#seekToProgress(int)}.
     */
    private static final int MSG_SEEK_TO_PROGRESS = 21;

    @Override
    public boolean handleMessage(Message msg) {
        Log.d("TestPlay","Enter handleMEssage");
        switch (msg.what){
            case MSG_UPDATE_PROGRESS:
                updateElapsedTime();
                break;
            case MSG_SEEK_TO_PROGRESS:
                Log.d("TestPlay","Handle Message SeekTo");
                PlaybackService.get(this).seekToProgress(msg.arg1);
                updateElapsedTime();
                break;
            default:
                return super.handleMessage(msg);
        }
        return true;
    }
    //=====================================View.OnClickListener=========================//

    //======================================SlidingView.Callback===========================//
    @Override
    public void onSlideFullyExpanded(boolean expanded) {
        if(mMenu == null)
            return;

        final int[] slide_visisble = {MENU_HIDE_QUEUE,MENU_CLEAR_QUEUE,MENU_EMPTY_QUEUE};
        final int[] slide_hidden = {MENU_SHOW_QUEUE};

        for(int id : slide_visisble){
            MenuItem item = mMenu.findItem(id);
            if(item != null){
                item.setVisible(expanded);
            }
        }

        for(int id : slide_hidden){
            MenuItem item = mMenu.findItem(id);
            if(item != null){
                item.setVisible(!expanded);
            }
        }
    }

    //======================================SeekBar.OnSeekBarChangeListener===================//
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            Log.d("TestPlay","FromUser");
            mElapsedView.setText(DateUtils.formatElapsedTime(mTimeBuilder, progress * mDuration / 1000000));
            mUiHandler.removeMessages(MSG_UPDATE_PROGRESS);
            mUiHandler.removeMessages(MSG_SEEK_TO_PROGRESS);
            mUiHandler.sendMessageDelayed(mUiHandler.obtainMessage(MSG_SEEK_TO_PROGRESS, progress, 0), 150);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mSeekBarTracking = true;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        mSeekBarTracking = false;
    }

    static final int CTX_MENU_ADD_TO_PLAYLIST = 300;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        if(mMenu == null)
            mMenu = menu;

        menu.add(0, MENU_SHOW_QUEUE, 20, R.string.show_queue);
        menu.add(0, MENU_HIDE_QUEUE, 20, R.string.hide_queue);
        menu.add(0, MENU_CLEAR_QUEUE, 20, R.string.dequeue_rest);
        menu.add(0, MENU_EMPTY_QUEUE, 20, R.string.empty_the_queue);

        onSlideFullyExpanded(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d("TestOptionMenu","SldingView onOptionsItemSelected");
        switch (item.getItemId()){
            case MENU_SHOW_QUEUE:
                mSlidingView.expandSlide();
                break;
            case MENU_HIDE_QUEUE:
                mSlidingView.hideSlide();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }


}
