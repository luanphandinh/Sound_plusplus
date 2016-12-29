package chongxuocmanhinh.sound_plusplus;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.PaintDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.provider.MediaStore;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import Support.isoched.tabs.SoundPlusPlusTabLayout;

public class LibraryActivity extends SlidingPlaybackActivity
                implements android.widget.SearchView.OnQueryTextListener,
                            DialogInterface.OnClickListener,
                            DialogInterface.OnDismissListener
{

    private Looper mLooper;
    private Handler mHandler;

    public ViewPager mViewPager;
    /**
     * The id of the media that was last pressed in the current adapter. Used to
     * open the playback activity when an item is pressed twice.
     */
    private long mLastActedId;
    /**
     * The pager adapter that manages each media ListView.
     */
    public LibraryPagerAdapter mPagerAdapter;
    /**
     * Adapter cho list đang được hiển thị.
     * được set mỗi lần pagechange ở viewpager thông qua {@link LibraryPagerAdapter#setPrimaryItem(ViewGroup, int, Object)}
     * sau đó gọi vào hàm {@link LibraryActivity#onPageChanged(int, LibraryAdapter)}
     */
    private LibraryAdapter mCurrentAdapter;

    private BottomBarControls mBottomBarControls;
    private SoundPlusPlusTabLayout mSoundPlusPlusTabLayout;
    /**
     * Hai cái dưới này dùng để update khi mà có limiter nào đó được set
     */
    private HorizontalScrollView mLimiterScroller;
    private ViewGroup mLimiterViews;

    /**
     * Hành động được thực thi khi một dòng được nhấp
     */
    private int mDefaultAction;
    /**
     * Hành động được sử dụng gần nhất từ menu. được sử dụng với ACTION_LAST_USED.
     */
    private int mLastAction = ACTION_PLAY;

    /**
     * Khi click vào cái row nào thì expand
     */
    public static final int ACTION_EXPAND = 6;

    /**
     * Khi click vào row,chơi nhạc tại row đó
     */
    public static final int ACTION_PLAY = 0;

    /**
     * Khi click vào row: đưa row vào hàng đợi.
     */
    public static final int ACTION_ENQUEUE = 1;
    /**
     *Khi click vào row: thực hiện action được sử dụng gần nhất.
     */
    public static final int ACTION_LAST_USED = 2;
    /**
     * Khi click vào row: chạy tất cả các bài hát trong adapter hiện tại
     * bắt đầu từ row được click
     */
    public static final int ACTION_PLAY_ALL = 3;
    /**
     * Khi click vào row: xếp vào hàng đợi tất cả bài hát trong adapter hiện tại
     * bắt đầu từ row
     */
    public static final int ACTION_ENQUEUE_ALL = 4;

    /**
     * Khi click vào row:ko làm gì hết.
     */
    public static final int ACTION_DO_NOTHING = 5;
    /**
     * Khi click vào row: play nếu đã paused hoặc enqueue nếu đang playing.
     */
    public static final int ACTION_PLAY_OR_ENQUEUE = 7;

    /**
     * Khi click vào row: đưa row được chọn vào vị trí tiếp theo
     */
    public static final int ACTION_ENQUEUE_AS_NEXT = 8;
    /**
     * The SongTimeLine add song modes tương ứng với mỗi action ở trên.
     */
    private static final int[] modeForAction =
            { SongTimeLine.MODE_PLAY, SongTimeLine.MODE_ENQUEUE, -1,
                    SongTimeLine.MODE_PLAY_ID_FIRST, SongTimeLine.MODE_ENQUEUE_ID_FIRST,
                    -1, -1, -1, SongTimeLine.MODE_ENQUEUE_AS_NEXT };

//    private HorizontalScrollView mLimiterScrollView;
//    private ViewGroup mLimiterViews;


    /**
     * Same as MSG_ADD_TO_PLAYLIST but creates the new playlist on-the-fly (or overwrites an existing list)
     */
    protected static final int MSG_CREATE_PLAYLIST = 0;
    /**
     * Call renamePlaylist with the results from a NewPlaylistDialog stored in
     * obj.
     */
    protected static final int MSG_RENAME_PLAYLIST = 1;
    /**
     * Call addToPlaylist with data from the playlisttask object.
     */
    protected static final int MSG_ADD_TO_PLAYLIST = 2;
    /**
     * Call removeFromPlaylist with data from the playlisttask object.
     */
    protected static final int MSG_REMOVE_FROM_PLAYLIST = 3;
    /**
     * Removes a media object
     */
    protected static final int MSG_DELETE = 4;
    /**
     * Saves the current queue as a playlist
     */
    protected static final int MSG_ADD_QUEUE_TO_PLAYLIST = 5;
    /**
     * Notification that we changed some playlist members
     */
    protected static final int MSG_NOTIFY_PLAYLIST_CHANGED = 6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("Test", "onCreate: ");
        ThemeHelper.setTheme(this,R.style.Library);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.library_content);

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mLimiterScroller = (HorizontalScrollView) findViewById(R.id.limiter_scroller);
        mLimiterViews = (ViewGroup) findViewById(R.id.limiter_layout);

        HandlerThread thread = new HandlerThread(getClass().getName(), Process.THREAD_PRIORITY_LOWEST);
        thread.start();

        mLooper = thread.getLooper();
        mHandler = new Handler(mLooper, this);

        mPagerAdapter = new LibraryPagerAdapter(this,mLooper);
        mViewPager.setAdapter(mPagerAdapter);
        mPagerAdapter.notifyDataSetChanged();

        mBottomBarControls = (BottomBarControls)findViewById(R.id.bottombar_controls);
        mBottomBarControls.setOnClickListener(this);
        mBottomBarControls.enableOptionsMenu(this);
        mBottomBarControls.setOnQueryTextListener(this);

        if(PermissionRequestActivity.havePermissions(this) == false) {
            PermissionRequestActivity.showWarning(this, getIntent());
        }


        mSoundPlusPlusTabLayout = (SoundPlusPlusTabLayout)findViewById(R.id.sliding_tabs);
        mSoundPlusPlusTabLayout.setOnPageChangeListener(mPagerAdapter);

        loadTabOrder();
        bindControlButtons();
    }

    private void loadTabOrder(){
        if(mPagerAdapter.loadTabOrder()){
            mSoundPlusPlusTabLayout.setViewPager(mViewPager);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        SharedPreferences settings = PlaybackService.getSettings(this);
        mDefaultAction = Integer.parseInt(settings.getString(PrefKeys.DEFAULT_ACTION_INT, PrefDefaults.DEFAULT_ACTION_INT));
        mLastActedId = LibraryAdapter.INVALID_ID;
        updateHeaders();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.d("TestNotificationClicked","onNewItent main");
        return;
    }


    //=====================Khi pagerAdapter bắt được clickListener thì gọi tới đống này===================//

    /**
     * Được gọi khi người dùng clcik vào bất kì row nào trên listview thông quan
     * {@link LibraryPagerAdapter#onItemClick}
     *
     * @param rowData
     */
    public void onItemClicked(Intent rowData){
        int action = mDefaultAction;
        if (action == ACTION_LAST_USED)
            action = mLastAction;
        Log.d("Testtt","LibraryActivity : OnItemClicked");
        if(action == ACTION_EXPAND && rowData.getBooleanExtra(LibraryAdapter.DATA_EXPANDABLE, false)){
            onItemExpanded(rowData);
        }else if (action != ACTION_DO_NOTHING){
            if(action == ACTION_EXPAND){
                //Mặc định thì mình để nó play cái gì mà ko expand được
                action = ACTION_PLAY;
            }else if (action == ACTION_PLAY_OR_ENQUEUE) {
                action = (mState & PlaybackService.FLAG_PLAYING) == 0 ? ACTION_PLAY : ACTION_ENQUEUE;
            }
            pickSongs(rowData,action);
        }

    }

    /**
     * Sử dụng dữ liệu từ dòng được click trên
     * {@link LibraryActivity#onItemClicked(Intent)}
     * thông qua dữ liệu từ intent,ta sẽ add một hay nhiều bài hát vào trong service để play
     * @param intent
     * @param action
     */
    private void pickSongs(Intent intent,int action){
        Log.d("Testtt","Pick Song");
        long id = intent.getLongExtra("id", LibraryAdapter.INVALID_ID);

        /**
         * query tất cả bài hát khi người dùng muốn play tất cả
         */
        boolean all = false;
        int mode = action;
        //Nếu chọn 1 trong 2 options này
        if(action == ACTION_PLAY_ALL || action == ACTION_ENQUEUE_ALL){
            int type = mCurrentAdapter.getMediaTypes();
            //Nếu nó là artist hoặc album thì notPlayAllAdapter sẽ bằng true
            //Chỉ queery các bài hát liên quan đến row được chọn
            boolean notPlayAllAdapter = type >  MediaUtils.TYPE_SONG || id == LibraryAdapter.HEADER_ID;
            if(mode == ACTION_ENQUEUE_ALL && notPlayAllAdapter){
                mode = ACTION_ENQUEUE;
            }else if(mode == ACTION_PLAY_ALL && notPlayAllAdapter) {
                mode = ACTION_PLAY;
            }else{//
                all = true;
            }
        }

        if(id == LibraryAdapter.HEADER_ID)
            all = true;
        /**
         * Để tiết kiệm thời gian,ở trên {@link MediaAdapter#buildQuery(String[], boolean)}
         * Ta chỉ query chủ yếu phần id và text
         * Ở đây sau khi row được click,dựa vào dữ liệu được truyền.ta sẽ queery hết tất
         * cả dữ liệu của các bài hát dự trên {@link Song#FILLED_PROJECTION}
         *
         * Nếu muốn chơi hết tất cả bài hát (ở đây là tất cả bài hát trong adapter luôn)
         * thì all = true,adapter sẽ queery tất cả bài hát.
         * Nếu chỉ muốn play các bài hát liên qua tới row được pick thì truyền null vào
         */
        final QueryTask queryTask = buildQueryFromIntent(intent,false,( all ? (MediaAdapter)(mCurrentAdapter) : null));
        queryTask.mode = modeForAction[mode];
        PlaybackService.get(this).addSongs(queryTask);
        mLastActedId = id;

        if(mDefaultAction == ACTION_LAST_USED && mLastAction != action){
            mLastAction = action;
            updateHeaders();
        }
    }

    private void updateHeaders(){
        int action = mDefaultAction;
        if (action == ACTION_LAST_USED)
            action = mLastAction;
        boolean isEnqueue = action == ACTION_ENQUEUE || action == ACTION_ENQUEUE_ALL;
        String text = getString(isEnqueue ? R.string.enqueue_all : R.string.play_all);
        mPagerAdapter.setHeaderText(text);
    }

    /**
     * Build mediaqeury dựa trên dữ liệu được đưa vào thông qua intent
     *
     * @param intent Itnet dùng để tạo queryTask
     * {@link LibraryAdapter#createData(View)}.
     * @param empty nếu empty để true,thì ta sử dụng Song.EMPTY_PROJECTION query duy nhất thằng id ra.
     * @param allSource sử dụng mediaAdapter để query tất cả item được giữ
     */
    protected QueryTask buildQueryFromIntent(Intent intent, boolean empty, MediaAdapter allSource)
    {
        Log.d("Testtt","Build Query by MediaUtils");
        int type = intent.getIntExtra("type",MediaUtils.TYPE_INVALID);

        String[] projection;
        if (type == MediaUtils.TYPE_PLAYLIST)
            projection = empty ? Song.EMPTY_PLAYLIST_PROJECTION : Song.FILLED_PLAYLIST_PROJECTION;
        else
            projection = empty ? Song.EMPTY_PROJECTION : Song.FILLED_PROJECTION;

        long id = intent.getLongExtra("id", LibraryAdapter.INVALID_ID);
        QueryTask queryTask;

        if(allSource != null) {
            queryTask = allSource.buildSongQuery(projection);
            queryTask.data = id;
        }
        else
            queryTask = MediaUtils.buildQuery(type, id, projection, null);

        return queryTask;
    }
    /**
     * Mở playlist editor
     */
    private void editPlaylist(Intent rowData)
    {
        Intent launch = new Intent(this, PlaylistActivity.class);
        launch.putExtra("playlist", rowData.getLongExtra(LibraryAdapter.DATA_ID, LibraryAdapter.INVALID_ID));
        launch.putExtra("title", rowData.getStringExtra(LibraryAdapter.DATA_TITLE));
        startActivity(launch);
    }

    public void onItemExpanded(Intent rowData){
        int type = rowData.getIntExtra(LibraryAdapter.DATA_TYPE,MediaUtils.TYPE_INVALID);
        if(type == MediaUtils.TYPE_PLAYLIST)
            // playlist is coming :v
            editPlaylist(rowData);

        else
            expand(rowData);
    }

    public void expand(Intent intent){
        mBottomBarControls.showSearch(false);
        int type = intent.getIntExtra(LibraryAdapter.DATA_TYPE,MediaUtils.TYPE_INVALID);
        long id = intent.getLongExtra(LibraryAdapter.DATA_ID,MediaUtils.TYPE_INVALID);
        int tab = mPagerAdapter.setLimiter(mPagerAdapter.mAdapters[type].buildLimiter(id));
        if (tab == -1 || tab == mViewPager.getCurrentItem())
            updateLimiterViews();
        else
            mViewPager.setCurrentItem(tab);
    }

    /**
     * {@link LibraryActivity#onClick(View)}
     * Gán lại cái limiter của mLimiterType
     * Hàm này được sử dụng để nhận câu selection có sẵn,là limiter.data
     * sau đó sử dụng limiterTYpe được truyền vào để gán lại limiter cho mPagerAdaper.
     *
     * @param limiterType loại mediaUtils.Type cần nhận
     * @param selection câu truy vấn selection
     */
    public void setLimiter(int limiterType,String selection){
        ContentResolver resolver = getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = new String[] { MediaStore.Audio.Media.ARTIST_ID, MediaStore.Audio.Media.ALBUM_ID, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM };
        Cursor cursor = MediaUtils.queryResolver(resolver, uri, projection, selection, null, null);
        if (cursor != null) {
            if (cursor.moveToNext()) {
                String[] fields;
                String data;
                switch (limiterType) {
                    case MediaUtils.TYPE_ARTIST:
                        fields = new String[] { cursor.getString(2) };
                        data = String.format("artist_id=%d", cursor.getLong(0));
                        break;
                    case MediaUtils.TYPE_ALBUM:
                        fields = new String[] { cursor.getString(2), cursor.getString(3) };
                        data = String.format("album_id=%d", cursor.getLong(1));
                        break;
                    default:
                        throw new IllegalArgumentException("setLimiter() does not support limiter type " + limiterType);
                }
                mPagerAdapter.setLimiter(new Limiter(limiterType, fields, data));
            }
            cursor.close();
        }
    }

    /**
     * Được gọi khi chuyển tab hoặc row có khả năng expand được click
     * Update lại limiterView dựa trên adapter được chuyển tới
     */
    public void updateLimiterViews(){
        mLimiterViews.removeAllViews();

        Limiter limiterData = mPagerAdapter.getCurrentLimiter();
        if (limiterData != null) {
            String[] limiter = limiterData.names;

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.leftMargin = 5;
            for (int i = 0; i != limiter.length; ++i) {
                PaintDrawable background = new PaintDrawable(Color.GRAY);
                background.setCornerRadius(5);

                TextView view = new TextView(this);
                view.setSingleLine();
                view.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                view.setText(limiter[i] + " | X");
                view.setTextColor(Color.WHITE);
                view.setBackgroundDrawable(background);
                view.setLayoutParams(params);
                view.setPadding(5, 2, 5, 2);
                view.setTag(i);
                view.setOnClickListener(this);
                mLimiterViews.addView(view);
            }

            mLimiterScroller.setVisibility(View.VISIBLE);
        } else {
            mLimiterScroller.setVisibility(View.GONE);
        }
    }
    //=======================================Handler.Callback==============================//
    /**
     * Updates mCover sử dụng background thread
     */
    private static final int MSG_UPDATE_COVER = 41;
    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_UPDATE_COVER: {
                Bitmap cover = null;
                Song song = (Song)msg.obj;
                if (song != null) {
                    cover = song.getSmallCover(this);
                }
                // Dispatch view update to UI thread
                updateCover(cover);
                break;
            }
            default:
                return super.handleMessage(msg);
        }

        return true;
    }

    /**
     * Updates mCover với bitmap mới, chạy trên UI thread
     *
     * @param cover the cover to set, will use a fallback drawable if null
     */
    private void updateCover(final Bitmap cover) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mBottomBarControls.setCover(cover);
            }
        });
    }

    public void onPageChanged(int position, LibraryAdapter adapter)
    {
        mCurrentAdapter = adapter;
        mLastActedId = LibraryAdapter.INVALID_ID;
        updateLimiterViews();
//        if (adapter != null && (adapter.getLimiter() == null || adapter.getMediaType() == MediaUtils.TYPE_FILE)) {
//            // Save current page so it is opened on next startup. Don't save if
//            // the page was expanded to, as the expanded page isn't the starting
//            // point. This limitation does not affect the files tab as the limiter
//            // (the files almost always have a limiter)
//            Handler handler = mHandler;
//            handler.sendMessage(mHandler.obtainMessage(MSG_SAVE_PAGE, position, 0));
//        }
    }
    //=====================================View.OnClickListener==============================//
    @Override
    public void onClick(View view) {
        if (view == mBottomBarControls) {
            return;
        }
        else if(view.getTag() != null){
            int i = (int) view.getTag();

            Limiter limiter = mPagerAdapter.getCurrentLimiter();
            int type = limiter.type;
            //Trường hợp i = 1,có nghĩa là cái ô màu xám thứ 2 được click,chỗ ô của album
            //Hoặc limiter.type trả về là album
            //thì sử dụng hàm riêng setLimiter của Activity
            //bình thường thì ta sử dụng thằng pager để set trực tiếp
            if(i == 1 && type == MediaUtils.TYPE_ALBUM){
                //truyền vào loại artist để set lại limiter
                setLimiter(MediaUtils.TYPE_ARTIST, limiter.data.toString());
            }else{//Nếu chọn trúng thằng nghệ sĩ,thì ta clear đi
                mPagerAdapter.clearLimiter(type);
            }

            updateLimiterViews();
        }else{
            super.onClick(view);
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode){
            case KeyEvent.KEYCODE_BACK:
                Limiter limiter = mPagerAdapter.getCurrentLimiter();
                if(mSlidingView.isHidden() == false){
                    mSlidingView.hideSlide();
                    break;
                }

                if (mBottomBarControls.showSearch(false)) {
                    break;
                }

                if(limiter != null){
                    int pos = -1;
                    switch (limiter.type){
                        case MediaUtils.TYPE_ALBUM:
                            setLimiter(MediaUtils.TYPE_ARTIST,limiter.data.toString());
                            pos = mPagerAdapter.mAlbumsPosition;
                            break;
                        case MediaUtils.TYPE_ARTIST:
                            mPagerAdapter.clearLimiter(MediaUtils.TYPE_ARTIST);
                            pos = mPagerAdapter.mArtistsPosition;
                            break;
                        case MediaUtils.TYPE_GENRE:
                            mPagerAdapter.clearLimiter(MediaUtils.TYPE_GENRE);
                            pos = mPagerAdapter.mGenresPosition;
                            break;
                    }

                    if (pos == -1) {
                        updateLimiterViews();
                    } else {
                        mViewPager.setCurrentItem(pos);
                    }
                }else{
                    finish();
                }
                break;
            default:
                return false;
        }

        return true;
    }

    //=====================================ContextMenu========================================//
    private static final int CTX_MENU_PLAY = 0;
    private static final int CTX_MENU_ENQUEUE = 1;
    private static final int CTX_MENU_EXPAND = 2;
    private static final int CTX_MENU_ENQUEUE_AS_NEXT = 3;
    private static final int CTX_MENU_DELETE = 4;
    private static final int CTX_MENU_RENAME_PLAYLIST = 5;
    private static final int CTX_MENU_PLAY_ALL = 6;
    private static final int CTX_MENU_ENQUEUE_ALL = 7;
    private static final int CTX_MENU_MORE_FROM_ALBUM = 8;
    private static final int CTX_MENU_MORE_FROM_ARTIST = 9;
    private static final int CTX_MENU_OPEN_EXTERNAL = 10;

    public void onCreateContextMenu(ContextMenu menu,Intent rowData){
        if(rowData.getLongExtra(LibraryAdapter.DATA_ID,LibraryAdapter.INVALID_ID) == LibraryAdapter.HEADER_ID){
            menu.setHeaderTitle(getString(R.string.all_songs));
            menu.add(0, CTX_MENU_PLAY_ALL, 0, R.string.play_all).setIntent(rowData);
            menu.add(0, CTX_MENU_ENQUEUE_ALL, 0, R.string.enqueue_all).setIntent(rowData);
            menu.addSubMenu(0, CTX_MENU_ADD_TO_PLAYLIST, 0, R.string.add_to_playlist).getItem().setIntent(rowData);
        }else{
            int type = rowData.getIntExtra(LibraryAdapter.DATA_TYPE,MediaUtils.TYPE_INVALID);

            menu.setHeaderTitle(rowData.getStringExtra(LibraryAdapter.DATA_TITLE));

            menu.add(0, CTX_MENU_PLAY, 0, R.string.play).setIntent(rowData);
            if (type <= MediaUtils.TYPE_SONG) {
                menu.add(0, CTX_MENU_PLAY_ALL, 0, R.string.play_all).setIntent(rowData);
            }
            menu.add(0, CTX_MENU_ENQUEUE_AS_NEXT, 0, R.string.enqueue_as_next).setIntent(rowData);
            menu.add(0, CTX_MENU_ENQUEUE, 0, R.string.enqueue).setIntent(rowData);
            if (type == MediaUtils.TYPE_PLAYLIST) {
                menu.add(0, CTX_MENU_RENAME_PLAYLIST, 0, R.string.rename).setIntent(rowData);
            } else if (rowData.getBooleanExtra(LibraryAdapter.DATA_EXPANDABLE, false)) {
                menu.add(0, CTX_MENU_EXPAND, 0, R.string.expand).setIntent(rowData);
            }
            if (type == MediaUtils.TYPE_ALBUM || type == MediaUtils.TYPE_SONG)
                menu.add(0, CTX_MENU_MORE_FROM_ARTIST, 0, R.string.more_from_artist).setIntent(rowData);
            if (type == MediaUtils.TYPE_SONG)
                menu.add(0, CTX_MENU_MORE_FROM_ALBUM, 0, R.string.more_from_album).setIntent(rowData);
            menu.addSubMenu(0, CTX_MENU_ADD_TO_PLAYLIST, 0, R.string.add_to_playlist).getItem().setIntent(rowData);
            menu.add(0, CTX_MENU_DELETE, 0, R.string.delete).setIntent(rowData);
        }

    }


    @Override
    public boolean onContextItemSelected(MenuItem item) {

        final Intent intent = item.getIntent();
        switch (item.getItemId()){
            case CTX_MENU_EXPAND:
                expand(intent);
                if (mDefaultAction == ACTION_LAST_USED && mLastAction != ACTION_EXPAND) {
                    mLastAction = ACTION_EXPAND;
                    updateHeaders();
                }
                break;
            case CTX_MENU_ENQUEUE:
                pickSongs(intent,ACTION_ENQUEUE);
                break;
            case CTX_MENU_PLAY:
                pickSongs(intent,ACTION_PLAY);
                break;
            case CTX_MENU_PLAY_ALL:
                pickSongs(intent,ACTION_PLAY_ALL);
            case CTX_MENU_ENQUEUE_ALL:
                pickSongs(intent, ACTION_ENQUEUE_ALL);
                break;
            case CTX_MENU_ENQUEUE_AS_NEXT:
                pickSongs(intent, ACTION_ENQUEUE_AS_NEXT);
                break;
            case CTX_MENU_MORE_FROM_ARTIST:{
                String selection;
                if(intent.getIntExtra(LibraryAdapter.DATA_TYPE,-1) == MediaUtils.TYPE_ALBUM){
                    selection = "album_id=";
                }else{
                    selection = "_id=";
                }
                selection += intent.getLongExtra(LibraryAdapter.DATA_ID, LibraryAdapter.INVALID_ID);
                setLimiter(MediaUtils.TYPE_ARTIST, selection);
                updateLimiterViews();
                break;
            }
            case CTX_MENU_MORE_FROM_ALBUM:
                setLimiter(MediaUtils.TYPE_ALBUM, "_id=" + intent.getLongExtra(LibraryAdapter.DATA_ID, LibraryAdapter.INVALID_ID));
                updateLimiterViews();
                break;
            case CTX_MENU_RENAME_PLAYLIST: {
                final String playlistName = intent.getStringExtra("title");
                final long playlistId = intent.getLongExtra("id", -1);
                PlaylistInputDialog dialog = new PlaylistInputDialog(new PlaylistInputDialog.Callback() {
                    @Override
                    public void onSuccess(String input) {
                        PlaylistTask playlistTask = new PlaylistTask(playlistId, input);
                        mHandler.sendMessage(mHandler.obtainMessage(MSG_RENAME_PLAYLIST, playlistTask));
                    }
                }, playlistName, R.string.rename);
                dialog.show(getFragmentManager(), "RenamePlaylistInputDialog");
                break;

            }
            case CTX_MENU_ADD_TO_PLAYLIST:
                long id = intent.getLongExtra("id", LibraryAdapter.INVALID_ID);
                PlaylistDialog plDialog = new PlaylistDialog( this,intent,(id==LibraryAdapter.HEADER_ID?(MediaAdapter)mCurrentAdapter:null));
                plDialog.show(getFragmentManager(), "PlaylistDialog");
                break;
            /**********************created by lordhung*****************************/
            // tạm thời chỉ để xóa playlist
            case CTX_MENU_DELETE:
                String delete_message = getString(R.string.delete_item, intent.getStringExtra("title"));
                AlertDialog.Builder dialog = new AlertDialog.Builder(this);
                dialog.setTitle(R.string.delete);
                dialog
                        .setMessage(delete_message)
                        .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                mHandler.sendMessage(mHandler.obtainMessage(MSG_DELETE, intent));
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        });
                dialog.create().show();
                break;
            default:
                return super.onContextItemSelected(item);
        }

        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0,MENU_PLAYBACK,0,R.string.playback_view);
        menu.add(0, MENU_SEARCH, 0, R.string.search).setIcon(R.drawable.ic_menu_search).setVisible(false);
        menu.add(0, MENU_SORT, 30, R.string.sort_by);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d("TestOptionMenu","Librabry ACtivity onOptionsItemSelected");
        switch (item.getItemId()){
            case MENU_SEARCH:
                mBottomBarControls.showSearch(true);
                return true;
            case MENU_SORT:
                MediaAdapter adapter = (MediaAdapter) mCurrentAdapter;
                LinearLayout header = (LinearLayout) getLayoutInflater().inflate(R.layout.sort_dialog,null);
                CheckBox reverseSort = (CheckBox) header.findViewById(R.id.reverse_sort);

                int[] itemIds = adapter.getSortEntries();
                String[] items = new String[itemIds.length];
                Resources res = getResources();
                for(int i = itemIds.length;--i != -1;){
                    items[i] = res.getString(itemIds[i]);
                }

                int mode = adapter.getSortMode();
                if(mode < 0){
                    mode =~ mode;
                    reverseSort.setChecked(true);
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.sort_by);
                //phần mode + 1 là để hiển thị dấu check tại vị trí thích hợp
                builder.setSingleChoiceItems(items, mode + 1, this); // cộng 1 cho header
                builder.setNeutralButton(R.string.done, null);

                AlertDialog dialog = builder.create();
                dialog.getListView().addHeaderView(header);
                dialog.setOnDismissListener(this);
                dialog.show();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }

    }



    @Override
    protected void onStateChange(int state, int toggled) {
        super.onStateChange(state, toggled);

        if ((state & PlaybackService.FLAG_EMPTY_QUEUE) != 0)
            mBottomBarControls.setSong(null);
    }

    @Override
    protected void onSongChange(Song song) {
        super.onSongChange(song);
        mBottomBarControls.setSong(song);
        if (song != null) {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_COVER, song));
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        mPagerAdapter.setFilter(query);
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
       mPagerAdapter.setFilter(newText);
        return true;
    }
    //============================== DialogInterface.OnClickListener=========================//
    @Override
    public void onClick(DialogInterface dialog, int which) {
        dialog.dismiss();
    }
    //============================== DialogInterface.OnDismissListener======================//
    @Override
    public void onDismiss(DialogInterface dialog) {
        ListView list = ((AlertDialog)dialog).getListView();
        //Trừ đi 1 cho header
        int which = list.getCheckedItemPosition() - 1;

        CheckBox reverseSort = (CheckBox)list.findViewById(R.id.reverse_sort);
        if (reverseSort.isChecked()) {
            which = ~which;
        }

        mPagerAdapter.setSortMode(which);
    }
}
