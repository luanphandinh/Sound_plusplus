package chongxuocmanhinh.sound_plusplus;

import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

/**
 * Created by L on 07/11/2016.
 */

/**
 * PagerAdapter that manages the library media listviews
 */
public class LibraryPagerAdapter
        extends PagerAdapter
        implements  Callback
                    ,ViewPager.OnPageChangeListener
{
    /**
     *The number of unique list types.The number of visible lists may be smaller
     */
    public static final int MAX_ADAPTER_COUNT = 5;

    /**
     *The human-readable title for each list.The positions correspond to
     *the MediaUtls ids,eo e.g.TITLES[MediaUtils.TYPE_SONG] = R.string.songs
     */
    public static final int[] TITLES = { R.string.artists, R.string.albums, R.string.songs,
            R.string.playlists, R.string.genres };

    /**
     * Default tab order.
     */
    public static final int[] DEFAULT_ORDER = { MediaUtils.TYPE_ARTIST, MediaUtils.TYPE_ALBUM, MediaUtils.TYPE_SONG,
            MediaUtils.TYPE_PLAYLIST, MediaUtils.TYPE_GENRE };
    /**
     * The user-choosen tab order
     */
    int[] mTabOrder;
    /**
     * The number of visible Tabs.
     */
    private int mTabCount;

    /**
     *The listView for each adapter.Each index corresponds to that list's
     * MediaUtils's id.
     */
    private final ListView[] mLists = new ListView[MAX_ADAPTER_COUNT];

    /**
     *Mấy cái adapters,mỗi cái index sẽ tương ứng với MediaUtils id.
     */
    public LibraryAdapter[] mAdapters = new LibraryAdapter[MAX_ADAPTER_COUNT];


    /**
     * Xem thử coi cái adapter tương ứng với index cần query lại ko,danh sách cũ
     */
    private final boolean[] mRequeryNeeded = new boolean[MAX_ADAPTER_COUNT];

    /**
     * Cái artist adapters instance ,cái này cũng được lưu trong mAdapters[MediaUtils.TYPE_ARTIST].
     */
    private MediaAdapter mArtistAdapter;
    /**
     * Cái album adapters instance ,cái này cũng được lưu trong mAdapters[MediaUtils.TYPE_ALBUM].
     */
    private MediaAdapter mAlbumAdapter;
    /**
     * Cái song adapters instance ,cái này cũng được lưu trong mAdapters[MediaUtils.TYPE_SONG].
     */
    private MediaAdapter mSongAdapter;
    /**
     * Cái genre adapters instance ,cái này cũng được lưu trong mAdapters[MediaUtils.TYPE_GENRE].
     */
    private MediaAdapter mGenreAdapter;
    /**
     * Cái playlist adapters instance ,cái này cũng được lưu trong mAdapters[MediaUtils.TYPE_PLAYLIST].
     */
    MediaAdapter mPlaylistAdapter;

    /**
     * Cái adapter của cái danh sách hiện đang được hiển thị
     */
    private LibraryAdapter mCurrentAdapter;

    /**
     * Index của trang hiện tại trong viewpager
     */
    private int mCurrentPage;

    /**
     * Cái limiter dành cho  artist adapter khi nó được tạo.
     */
    private Limiter mPendingArtistLimiter;
    /**
     * Cái limiter dành cho  album adapter khi nó được tạo.
     */
    private Limiter mPendingAlbumLimiter;
    /**
     * Cái limiter dành cho  song adapter khi nó được tạo.
     */
    private Limiter mPendingSongLimiter;

    /**
     * Cái LibraryActivity chứa thằng này,dùng để bắt mấy cái sự kiện
     * của cái trang hiện tại trên LibraryActivity.
     */
    private final LibraryActivity mActivity;
    /**
     * Cái Handler chạy trên UI thread.
     */
   // private final Handler mUiHandler;

    /**
     * Cái Handler chạy trên WorkerThread.
     */
    //private final Handler mWorkerHandler;
    /**
     * Cái dòng text dùng để hiển thị dòng đầu tiên trên cùng cho
     * các tab artist, album, với song.
     */
    private String mHeaderText;
    private DraggableRow mArtistHeader;
    private DraggableRow mAlbumHeader;
    private DraggableRow mSongHeader;

    /**
     * Cái text dùng để lọc nội dung,null nếu không có gì
     */
    private String mFilter;

    /**
     *  Cái position trên song page, = -1  nếu nó ẩn(đang mở tab khác).
     */
    public int mSongsPosition = -1;
    /**
     *  Cái position trên album page, = -1  nếu nó ẩn(đang mở tab khác).
     */
    public int mAlbumsPosition = -1;
    /**
     *  Cái position trên artist page, = -1  nếu nó ẩn(đang mở tab khác).
     */
    public int mArtistsPosition = -1;
    /**
     *  Cái position trên genre page, = -1  nếu nó ẩn(đang mở tab khác).
     */
    public int mGenresPosition = -1;

    /**
     * Thằng này dùng để bắt trạng thái data có thay đổi hay không ở dưới
     * conten provider
     * link tham khảo
     * http://www.grokkingandroid.com/use-contentobserver-to-listen-to-changes/
     */
    private final ContentObserver mPlaylistObserver = new ContentObserver(null) {
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
        }
    };

    /**
     *
     * @param activity cái LibraryActivity sở hữu thằng này,sẽ nhận các hàm callbacks từ
     *                 mấy thằng listviews
     * @param workerLooper cái Looper chạy trên worker thread
     */
    public LibraryPagerAdapter(LibraryActivity activity, Looper workerLooper) {
        Log.d("Test", "LibraryPagerAdapter");
        mActivity = activity;
//        mUiHandler = new Handler(this);
//        mWorkerHandler = new Handler(workerLooper,this);
        mCurrentPage = -1;
        loadTabOrder();
        activity.getContentResolver().registerContentObserver(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, true, mPlaylistObserver);
    }

    public boolean loadTabOrder(){
        mTabOrder = DEFAULT_ORDER;
        mTabCount = MAX_ADAPTER_COUNT;
        return true;
    }


    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Log.d("Test", "instantiateItem: start");
        int type = mTabOrder[position];
        ListView view = mLists[type];
        if(view == null){
            Log.d("Test", "instantiateItem: view not null");
            LibraryActivity activity = mActivity;
            LayoutInflater inflater = activity.getLayoutInflater();
            LibraryAdapter adapter;
            DraggableRow header = null;

            switch (type){
                case MediaUtils.TYPE_ARTIST:
                    Log.d("Test", "instantiateItem: artist");
                    adapter = mArtistAdapter = new MediaAdapter(activity,MediaUtils.TYPE_ARTIST,mPendingArtistLimiter,activity);
                   mArtistHeader = header = (DraggableRow) inflater.inflate(R.layout.draggable_row,null);
                    break;
                case MediaUtils.TYPE_ALBUM:
                    Log.d("Test", "instantiateItem: album");
                    adapter = mAlbumAdapter = new MediaAdapter(activity, MediaUtils.TYPE_ALBUM, mPendingAlbumLimiter, activity);
                    mPendingAlbumLimiter = null;
                    mAlbumHeader = header = (DraggableRow)inflater.inflate(R.layout.draggable_row, null);
                    break;
                case MediaUtils.TYPE_SONG:
                    Log.d("Test", "instantiateItem: song");
                    adapter = mSongAdapter = new MediaAdapter(activity, MediaUtils.TYPE_SONG, mPendingSongLimiter, activity);
                    mPendingSongLimiter = null;
                    mSongHeader = header = (DraggableRow)inflater.inflate(R.layout.draggable_row, null);
                    break;
                case MediaUtils.TYPE_PLAYLIST:
                    Log.d("Test", "instantiateItem: playlist");
                    adapter = mPlaylistAdapter = new MediaAdapter(activity, MediaUtils.TYPE_PLAYLIST, null, activity);
                    break;
                case MediaUtils.TYPE_GENRE:
                    Log.d("Test", "instantiateItem: genre");
                    adapter = mGenreAdapter = new MediaAdapter(activity, MediaUtils.TYPE_GENRE, null, activity);
                    mGenreAdapter.setExpandable(mSongsPosition != -1);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid media type: " + type);
            }

            view = (ListView) inflater.inflate(R.layout.listview,null);
            /**
             * Cần phải gán hàm tạo context menu và hàm listtenItem click tại chỗ này
             */

            view.setTag(type);
            if(header != null){
                header.getTextView().setText(mHeaderText);
                header.setTag(new ViewHolder());//Làm cái này để cho nó giống với mấy cái row bình thường
                view.addHeaderView(header);
            }
            view.setAdapter(adapter);
            //cần load sortOrder và setFilter tại chỗ này
            adapter.commitQuery(adapter.query());
            mAdapters[type] = adapter;
            mLists[type] = view;
            mRequeryNeeded[type] = true;
        }

        //requeryIfNeeded(type);
        container.addView(view);
        return view;
    }

    /**
     *
     * @return
     */
    @Override
    public int getCount() {
        return mTabCount;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    //=========================override cho thằng Handler.CallBack=================//
    @Override
    public boolean handleMessage(Message msg) {
        return false;
    }

    //=========================override cho thằng ViewPager.OnPageChangeListener=================//
    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {

    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }
    //=========================override cho thằng ViewPager.OnPageChangeListener=================//

    @Override
    public void destroyItem(View container, int position, Object object) {
        ((ViewPager) container).removeView((View) object);
    }
}
