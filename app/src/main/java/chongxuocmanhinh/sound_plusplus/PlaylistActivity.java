package chongxuocmanhinh.sound_plusplus;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.Looper;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;

import com.mobeta.android.dslv.DragSortListView;

/**
 * Created by lordhung on 26/12/2016.
 * Playlist activity là nơi để xem, sắp xếp các bài hát trong playlist
 */

public class PlaylistActivity extends Activity
        implements View.OnClickListener,
        AbsListView.OnItemClickListener,
        DialogInterface.OnClickListener,
        DragSortListView.DropListener,
        DragSortListView.RemoveListener {
    /**
     * SongTimeline play mode tương ứng với mỗi LibraryActivity.ACTION_*
     */
    private static final int[] MODE_FOR_ACTION = {
            SongTimeLine.MODE_PLAY, SongTimeLine.MODE_ENQUEUE, -1,
            SongTimeLine.MODE_PLAY_ID_FIRST, SongTimeLine.MODE_ENQUEUE_ID_FIRST,
            -1, -1, -1, SongTimeLine.MODE_ENQUEUE_AS_NEXT
    };


    private Looper mLooper;
    private DragSortListView mListView;
    private PlaylistAdapter mAdapter;

    private Button mEditButton;
    private Button mDeleteButton;

    /**
     * id của playlist đang xem.
     */
    private long mPlaylistId;
    /**
     * tên của playlist đang xem .
     */
    private String mPlaylistName;
    /**
     * Nếu true, các bài hát trong playlists có thể dc drag hay sắp xếp lại .
     */
    private boolean mEditing;

    /**
     * Click action của item xác định trong preferences.
     */
    private int mDefaultAction;
    /**
     * lastaction sử dụng từ context menu , dùng để implement
     * LAST_USED_ACTION action.
     */
    private int mLastAction = LibraryActivity.ACTION_PLAY;

    @Override
    public void onCreate(Bundle state) {
        ThemeHelper.setTheme(this, R.style.BackActionBar);
        super.onCreate(state);

        HandlerThread thread = new HandlerThread(getClass().getName());
        thread.start();

        setContentView(R.layout.playlist_activity);

        DragSortListView view = (DragSortListView) findViewById(R.id.list);
        view.setOnItemClickListener(this);
        view.setOnCreateContextMenuListener(this);
        view.setDropListener(this);
        view.setRemoveListener(this);
        mListView = view;

        View header = LayoutInflater.from(this).inflate(R.layout.playlist_buttons, null);
        mEditButton = (Button) header.findViewById(R.id.edit);
        mEditButton.setOnClickListener(this);
        mDeleteButton = (Button) header.findViewById(R.id.delete);
        mDeleteButton.setOnClickListener(this);
        view.addHeaderView(header, null, false);
        mLooper = thread.getLooper();
        mAdapter = new PlaylistAdapter(this, mLooper);
        view.setAdapter(mAdapter);

        onNewIntent(getIntent());

    }

    // onStart chạy sau onCreate
    @Override
    public void onStart() {
        super.onStart();
        SharedPreferences settings = PlaybackService.getSettings(this);
        mDefaultAction = Integer.parseInt(settings.getString(PrefKeys.DEFAULT_PLAYLIST_ACTION, PrefDefaults.DEFAULT_PLAYLIST_ACTION));
    }

    @Override
    public void onDestroy() {
        mLooper.quit();
        super.onDestroy();
    }

    /**
     * Enable or disable edit mode, cho phép sắp xép, xóa các bài hát
     *
     * @param editing True để enable edit mode
     * */
    public void setEditing(boolean editing){
        mListView.setDragEnabled(editing);
        mAdapter.setEditable(editing);
    }

    /**
     * User click vào button edit: hiện edit mode, cho phép kéo thả sắp xếp song
     * or click button delete, show lên 1 cái alert delete or not
     * */
    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.edit:
                setEditing(!mEditing);
                break;
            case R.id.delete:
                AlertDialog.Builder builder=new AlertDialog.Builder(this);
                String message=getResources().getString(R.string.delete_playlist,mPlaylistName);
                builder.setMessage(message);
                builder.setPositiveButton(R.string.delete,this);
                builder.setNegativeButton(android.R.string.cancel,this);
                builder.show();
                break;
        }
    }

    /**
     * Khi user ấn nút delete trong dialog delete, xóa playlist dc chọn
     * */
    @Override
    public void onClick(DialogInterface dialog, int which) {
        if(which==DialogInterface.BUTTON_POSITIVE){
            Playlist.deletePlaylist(getContentResolver(),mPlaylistId);
            finish();
        }
        dialog.dismiss();
    }

    /**
     * Thực hiện một action xác định nào đó trên adapter row với id dc cho và position
     */
    private void performAction(int action, int position, long audioId) {
        if (action == LibraryActivity.ACTION_PLAY || action == LibraryActivity.ACTION_ENQUEUE)
            action = (PlaybackService.get(this).isPlaying() ? LibraryActivity.ACTION_ENQUEUE : LibraryActivity.ACTION_PLAY);

        if (action == LibraryActivity.ACTION_LAST_USED)
            action = mLastAction;

        switch (action) {
            case LibraryActivity.ACTION_PLAY:
            case LibraryActivity.ACTION_ENQUEUE:
            case LibraryActivity.ACTION_ENQUEUE_AS_NEXT: {
                QueryTask query = MediaUtils.buildQuery(MediaUtils.TYPE_SONG, audioId, Song.FILLED_PROJECTION, null);
                query.mode = MODE_FOR_ACTION[action];
                PlaybackService.get(this).addSongs(query);
                break;
            }
            case LibraryActivity.ACTION_PLAY_ALL:
            case LibraryActivity.ACTION_ENQUEUE_ALL: {
                QueryTask query = MediaUtils.buildPlaylistQuery(mPlaylistId, Song.FILLED_PLAYLIST_PROJECTION, null);
                query.mode = MODE_FOR_ACTION[action];
                query.data = position - mListView.getHeaderViewsCount();
                PlaybackService.get(this).addSongs(query);
                break;
            }
        }
        mLastAction=action;
    }

    // khi user click 1 dòng trong list
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int pos, long id) {

        // nếu ko fải đang edit và action ko fải donothing thì sẽ thực hiện mDefaultAction
        if(!mEditing&&mDefaultAction!=LibraryActivity.ACTION_DO_NOTHING){
            performAction(mDefaultAction,pos, (Long) view.findViewById(R.id.text).getTag());
        }
    }

    //
    @Override
    public void onNewIntent(Intent intent) {
        long id = intent.getLongExtra("playlist", 0);
        String title = intent.getStringExtra("title");
        mAdapter.setPlaylistId(id);
        setTitle(title);
        mPlaylistId = id;
        mPlaylistName = title;
    }

    /**
     * hàm này dc gọi từ adapter listview nếu user dịch chuyển 1 bài hát
     * @param from index của bài hát cần drag
     * @param to index mà bài hát dc drop
     * */
    @Override
    public void drop(int from, int to) {
        mAdapter.moveSong(from,to);
    }
    /**
     * hàm này dc gọi từ adapter listview nếu user fling bài hát về fía trái, sẽ xóa bài đó
     * @param position vị trí của bài hát cần xóa
     * */
    @Override
    public void remove(int position) {
        mAdapter.removeSong(position);
    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item){
//        if(item.getItemId()==android.R.id.home){
//            finish();
//            return true;
//        }
//        else
//            return super.onOptionsItemSelected(item);
//    }

    /*******************CONTEXTMENU*************************/
    /**
     * các menu action tương ứng với action trong LibraryActivity
     * */
    private static final int MENU_PLAY = LibraryActivity.ACTION_PLAY;
    private static final int MENU_PLAY_ALL = LibraryActivity.ACTION_PLAY_ALL;
    private static final int MENU_ENQUEUE = LibraryActivity.ACTION_ENQUEUE;
    private static final int MENU_ENQUEUE_ALL = LibraryActivity.ACTION_ENQUEUE_ALL;
    private static final int MENU_ENQUEUE_AS_NEXT = LibraryActivity.ACTION_ENQUEUE_AS_NEXT;
    private static final int MENU_REMOVE = -1;

    @Override
    public void onCreateContextMenu(ContextMenu menu,View listView,ContextMenu.ContextMenuInfo absInfo){
        AdapterView.AdapterContextMenuInfo info= (AdapterView.AdapterContextMenuInfo) absInfo;
        Intent intent=new Intent();
        intent.putExtra("id",info.id);
        intent.putExtra("position",info.position);
        intent.putExtra("audioId", (Long) info.targetView.findViewById(R.id.text).getTag());

        // add action vào menu
        menu.add(0, MENU_PLAY, 0, R.string.play).setIntent(intent);
        menu.add(0, MENU_PLAY_ALL, 0, R.string.play_all).setIntent(intent);
        menu.add(0, MENU_ENQUEUE_AS_NEXT, 0, R.string.enqueue_as_next).setIntent(intent);
        menu.add(0, MENU_ENQUEUE, 0, R.string.enqueue).setIntent(intent);
        menu.add(0, MENU_ENQUEUE_ALL, 0, R.string.enqueue_all).setIntent(intent);
        menu.add(0, MENU_REMOVE, 0, R.string.remove).setIntent(intent);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item){
        int itemId=item.getItemId();
        Intent intent=item.getIntent();
        int pos=intent.getIntExtra("position",-1);

        if(itemId==MENU_REMOVE)
            mAdapter.removeSong(pos-mListView.getHeaderViewsCount());
        else
            performAction(itemId,pos,intent.getLongExtra("audioId",-1));

        return true;
    }
    /****************************************************/

}
