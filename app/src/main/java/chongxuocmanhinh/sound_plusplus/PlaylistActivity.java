package chongxuocmanhinh.sound_plusplus;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.Looper;
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
     * The id of the playlist this activity is currently viewing.
     */
    private long mPlaylistId;
    /**
     * If true, then playlists songs can be dragged to reorder.
     */
    /**
     * The name of the playlist this activity is currently viewing.
     */
    private String mPlaylistName;
    /**
     * If true, then playlists songs can be dragged to reorder.
     */
    private boolean mEditing;

    /**
     * The item click action specified in the preferences.
     */
    private int mDefaultAction;
    /**
     * The last action used from the context menu, used to implement
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


    @Override
    public void onClick(DialogInterface dialogInterface, int i) {

    }

    @Override
    public void onClick(View view) {

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


    @Override
    public void drop(int from, int to) {

    }

    @Override
    public void remove(int which) {

    }
}
