package chongxuocmanhinh.sound_plusplus;

import android.database.ContentObserver;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
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
    private final Handler mUiHandler;

    /**
     * Cái Handler chạy trên WorkerThread.
     */
    private final Handler mWorkerHandler;
    /**
     * Cái dòng text dùng để hiển thị dòng đầu tiên trên cùng cho
     * các tab artist, album, với song.
     */
    private String headerText;
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
        mActivity = activity;
        mUiHandler = new Handler(this);
        mWorkerHandler = new Handler(workerLooper,this);
        mCurrentPage = -1;
        activity.getContentResolver().registerContentObserver(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, true, mPlaylistObserver);
    }

    public boolean loadTabOrder(){
        mTabOrder = DEFAULT_ORDER;
        mTabCount = MAX_ADAPTER_COUNT;
        return true;
    }


    @Override
    public Object instantiateItem(ViewGroup container, int position) {

        int type = mTabOrder[position];
        ListView view = mLists[type];

        return super.instantiateItem(container, position);
    }

    /**
     *
     * @return
     */
    @Override
    public int getCount() {
        return 0;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return false;
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
}
