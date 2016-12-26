package chongxuocmanhinh.sound_plusplus;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.mobeta.android.dslv.DragSortListView;

/**
 * Created by L on 05/12/2016.
 * Update 25/12/2016 Thêm DSLV vào fragment thay thế cho listview bình thường
 * xem thêm DSLV tại:
 *  https://github.com/bauerca/drag-sort-listview
 */
public class ShowQueueFragment extends Fragment
        implements  TimelineCallback,
                    AdapterView.OnItemClickListener,
                    MenuItem.OnMenuItemClickListener,
                    DragSortListView.DropListener,
                    DragSortListView.RemoveListener

{
    //Sau này phần này sẽ chuyển sang draggable row nếu có thể
    private DragSortListView mListView;
    private ShowQueueAdapter mListAdapter;
    private PlaybackService mService;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.showqueue_listview, container, false);
        Context context = getActivity();

        mListView    = (DragSortListView) view.findViewById(R.id.list);
        mListAdapter = new ShowQueueAdapter(context, R.layout.draggable_row);
        mListView.setAdapter(mListAdapter);
        mListView.setDropListener(this);
        mListView.setRemoveListener(this);
        mListView.setOnItemClickListener(this);
        mListView.setOnCreateContextMenuListener(this);
        PlaybackService.addTimelineCallback(this);
        return view;
    }

    @Override
    public void onDestroyView() {
        PlaybackService.removeTimelineCallback(this);
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();

        //Lấy thằng playbackservice nếu có thể
        if (mService == null && PlaybackService.hasInstance())
            mService = PlaybackService.get(getActivity());

        //làm mới lại cái trang với đối số scroll truyền là true để nhảy tới bài hát
        //đang được phát
        if (mService != null)
            refreshSongQueueList(true);
    }

    //===========================AdapterView.OnItemClickListener=====================//
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mService.jumpToQueuePosition(position);
    }

    public void remove(int which){
        mService.removeSongPosition(which);
    }

    /**
     * làm mới danh sách nhạc
     * @param scroll
     */
    public void refreshSongQueueList(final boolean scroll){
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d("Testtt","refresh");
                int i,stotal,spos;
                stotal = mService.getTimeLineLength();
                spos = mService.getTimelinePosition();

                mListAdapter.clear();
                mListAdapter.highlightRow(spos);

                for(i = 0;i <stotal; i++){
                    mListAdapter.add(mService.getSongByQueuePosition(i));
                }
                if(scroll)
                    scrollToCurrentSong(spos);
            }
        });
    }

    /**
     * Phần này được dùng để scroll tới bài hát đang được mở
     * Scrolls to the current song<br/>
     * We suppress the new api lint check as lint thinks
     * {@link android.widget.AbsListView#setSelectionFromTop(int, int)} was only added in
     * {@link Build.VERSION_CODES#JELLY_BEAN}, but it was actually added in API
     * level 1<br/>
     * <a href="https://developer.android.com/reference/android/widget/AbsListView.html#setSelectionFromTop%28int,%20int%29">
     *     Android reference: AbsListView.setSelectionFromTop()</a>
     * @param currentSongPosition The position in {@link #mListView} of the current song
     */
    @SuppressLint("NewApi")
    private void scrollToCurrentSong(int currentSongPosition){
        mListView.setSelectionFromTop(currentSongPosition, 0); /* scroll to currently playing song */
    }
    //=========================TimeLineCallBack==========================//
    @Override
    public void setSong(long uptime, Song song) {
        Log.d("Testtt","fragment setSong");
        if (mService == null) {
            mService = PlaybackService.get(getActivity());
            onTimelineChanged();
        }
    }

    @Override
    public void onTimelineChanged() {
        Log.d("Testtt","fragment onTimelineChanged");
        if (mService != null)
            refreshSongQueueList(false);
    }

    @Override
    public void setState(long uptime, int state) {

    }

    private final static int CTX_MENU_PLAY           = 100;
    private final static int CTX_MENU_ENQUEUE_ALBUM  = 101;
    private final static int CTX_MENU_ENQUEUE_ARTIST = 102;
    private final static int CTX_MENU_ENQUEUE_GENRE  = 103;
    private final static int CTX_MENU_REMOVE         = 104;

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        Song song = mService.getSongByQueuePosition(info.position);

        Intent intent = new Intent();
        intent.putExtra("id", song.id);
        intent.putExtra("type", MediaUtils.TYPE_SONG);
        intent.putExtra("position", info.position);
        menu.setHeaderTitle(song.title);
        menu.add(0, CTX_MENU_PLAY, 0, R.string.play).setIntent(intent).setOnMenuItemClickListener(this);
        menu.add(0, CTX_MENU_ENQUEUE_ALBUM, 0, R.string.enqueue_current_album).setIntent(intent).setOnMenuItemClickListener(this);
        menu.add(0, CTX_MENU_ENQUEUE_ARTIST, 0, R.string.enqueue_current_artist).setIntent(intent).setOnMenuItemClickListener(this);
        menu.add(0, CTX_MENU_ENQUEUE_GENRE, 0, R.string.enqueue_current_genre).setIntent(intent).setOnMenuItemClickListener(this);
        menu.add(0, CTX_MENU_REMOVE, 0, R.string.remove).setIntent(intent).setOnMenuItemClickListener(this);
    }

    //================================MenuItem.OnMenuItemClickListener=====================//
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        Intent intent = item.getIntent();
        int itemId = item.getItemId();
        int pos = intent.getIntExtra("position", -1);

        Song song = mService.getSongByQueuePosition(pos);
        switch (item.getItemId()){
            case CTX_MENU_PLAY:
                onItemClick(null, null, pos, -1);
                break;
            case CTX_MENU_REMOVE:
                remove(pos);
                break;
            case CTX_MENU_ENQUEUE_ARTIST:
                mService.enqueueFromSong(song,MediaUtils.TYPE_ARTIST);
                break;
            case CTX_MENU_ENQUEUE_ALBUM:
                mService.enqueueFromSong(song,MediaUtils.TYPE_ALBUM);
                break;
            case CTX_MENU_ENQUEUE_GENRE:
                mService.enqueueFromSong(song,MediaUtils.TYPE_GENRE);
                break;
            default:
                throw new IllegalArgumentException("Unhandled menu id received!");
        }
        return true;
    }

    //===================DragSortListView.DropListener======================//
    @Override
    public void drop(int from, int to) {
        if (from != to) {
            mService.moveSongPosition(from, to);
        }
    }
}
