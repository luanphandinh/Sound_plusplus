package chongxuocmanhinh.sound_plusplus;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
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
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import Support.isoched.tabs.SoundPlusPlusTabLayout;

public class LibraryActivity extends SlidingPlaybackActivity
                implements Handler.Callback,
                            View.OnClickListener
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
     * The SongTimeLine add song modes tương ứng với mỗi action ở trên.
     */
    private static final int[] modeForAction =
            { SongTimeLine.MODE_PLAY, SongTimeLine.MODE_ENQUEUE, -1,-1, -1, -1, -1};

//    private HorizontalScrollView mLimiterScrollView;
//    private ViewGroup mLimiterViews;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("Test", "onCreate: ");
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
        mDefaultAction = ACTION_EXPAND;
        mLastActedId = LibraryAdapter.INVALID_ID;
        updateHeaders();
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
        Log.d("Testtt","LibraryActivity : OnItemClicked");
        if(action == ACTION_EXPAND && rowData.getBooleanExtra(LibraryAdapter.DATA_EXPANDABLE, false)){
            onItemExpanded(rowData);
        }else if (action != ACTION_DO_NOTHING){
            if(action == ACTION_EXPAND){
                //Mặc định thì mình để nó play cái gì mà ko expand được
                action = ACTION_PLAY;
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
        if(action == ACTION_PLAY_ALL || action == ACTION_ENQUEUE_ALL){
            int type = mCurrentAdapter.getMediaTypes();
            boolean notPlayAllAdapter = type >  MediaUtils.TYPE_SONG || id == LibraryAdapter.HEADER_ID;
            if(mode == ACTION_ENQUEUE_ALL && notPlayAllAdapter){
                mode = ACTION_ENQUEUE;
            }else if(mode == ACTION_PLAY_ALL && notPlayAllAdapter) {
                mode = ACTION_PLAY;
            }else{
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

    public void onItemExpanded(Intent rowData){
        int type = rowData.getIntExtra(LibraryAdapter.DATA_TYPE,MediaUtils.TYPE_INVALID);
        if(type == MediaUtils.TYPE_PLAYLIST)
            return;
        expand(rowData);
    }

    public void expand(Intent intent){
        int type = intent.getIntExtra(LibraryAdapter.DATA_TYPE,MediaUtils.TYPE_INVALID);
        long id = intent.getLongExtra(LibraryAdapter.DATA_ID,MediaUtils.TYPE_INVALID);
        int tab = mPagerAdapter.setLimiter(mPagerAdapter.mAdapters[type].buildLimiter(id));
        if (tab == -1 || tab == mViewPager.getCurrentItem())
            updateLimiterViews();
        else
            mViewPager.setCurrentItem(tab);
    }

    /**
     * Gán lại cái limiter của mLimiterType được truyền vào từ cái row đầu
     * tiên của MediaStore.Audio.Media với câu lệnh selection
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
    @Override
    public boolean handleMessage(Message msg) {
        return super.handleMessage(msg);
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
            if(i == 1 && type == MediaUtils.TYPE_ALBUM){
                setLimiter(MediaUtils.TYPE_ARTIST, limiter.data.toString());
            }else{
                mPagerAdapter.clearLimiter(type);
            }

            updateLimiterViews();
        }else{
            super.onClick(view);
        }
    }
}
