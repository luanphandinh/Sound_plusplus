package chongxuocmanhinh.sound_plusplus;

import android.content.Intent;
import android.content.SharedPreferences;
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
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
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
                    ,AdapterView.OnItemClickListener
                    ,View.OnCreateContextMenuListener
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
            /**
             * thêm sửa xóa playlist sẽ requery lại playlists
             * */
            if(mPlaylistAdapter!=null)
                postRequestRequery(mPlaylistAdapter);
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
        mUiHandler = new Handler(this);
        mWorkerHandler = new Handler(workerLooper,this);
        mCurrentPage = -1;
        loadTabOrder();
        activity.getContentResolver().registerContentObserver(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, true, mPlaylistObserver);
    }

    /**
     * Hàm này dùng để loadTabOrder của pager
     * @return
     */
    public boolean loadTabOrder(){
        Log.d("Test :","LoadTabOrder");
        mTabOrder = DEFAULT_ORDER;
        mTabCount = MAX_ADAPTER_COUNT;
        computeExpansions();
        return true;
    }


    /**
     * Hàm này được dùng để set trạng thái expanable của
     * các row trong listview
     */
    public void computeExpansions()
    {
        int[] order = mTabOrder;
        int songsPosition = -1;
        int albumsPosition = -1;
        int artistsPosition = -1;
        int genresPosition = -1;

        for(int i = mTabCount;--i != -1;){
            switch (order[i]) {
                case MediaUtils.TYPE_ALBUM:
                    albumsPosition = i;
                    break;
                case MediaUtils.TYPE_SONG:
                    songsPosition = i;
                    break;
                case MediaUtils.TYPE_ARTIST:
                    artistsPosition = i;
                    break;
                case MediaUtils.TYPE_GENRE:
                    genresPosition = i;
                    break;
            }
        }

        if(mArtistAdapter != null)
            mArtistAdapter.setExpandable(songsPosition != -1 || albumsPosition != -1);
        if(mAlbumAdapter != null)
            mAlbumAdapter.setExpandable(songsPosition != -1);
        if(mGenreAdapter != null)
            mGenreAdapter.setExpandable(songsPosition != -1);

        mSongsPosition = songsPosition;
        mAlbumsPosition = albumsPosition;
        mArtistsPosition = artistsPosition;
        mGenresPosition = genresPosition;
        Log.d("Test :","Song Pos - " + songsPosition);
        Log.d("Test :","Album Pos - " + albumsPosition);
        Log.d("Test :","genres Pos - " + genresPosition);
    }


    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Log.d("Test", "instantiateItem: start");
        int type = mTabOrder[position];
        ListView view = mLists[type];
        if(view == null){
            Log.d("Test", "instantiateItem: create new view");
            LibraryActivity activity = mActivity;
            LayoutInflater inflater = activity.getLayoutInflater();
            LibraryAdapter adapter;
            DraggableRow header = null;

            switch (type){
                case MediaUtils.TYPE_ARTIST:
                    Log.d("Test", "instantiateItem: artist");
                    adapter = mArtistAdapter = new MediaAdapter(activity,MediaUtils.TYPE_ARTIST,mPendingArtistLimiter,activity);
                    mArtistAdapter.setExpandable(mSongsPosition != -1 || mAlbumsPosition != -1);
                    mArtistHeader = header = (DraggableRow) inflater.inflate(R.layout.draggable_row,null);
                    break;
                case MediaUtils.TYPE_ALBUM:
                    Log.d("Test", "instantiateItem: album");
                    adapter = mAlbumAdapter = new MediaAdapter(activity, MediaUtils.TYPE_ALBUM, mPendingAlbumLimiter, activity);
                    mPendingAlbumLimiter = null;
                    mAlbumAdapter.setExpandable(mSongsPosition != -1);
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
            view.setOnItemClickListener(this);
            view.setOnCreateContextMenuListener(this);
            view.setTag(type);
            if(header != null){
                header.getTextView().setText(mHeaderText);
                header.setTag(new ViewHolder());//Làm cái này để cho nó giống với mấy cái row bình thường
                view.addHeaderView(header);
            }
            view.setAdapter(adapter);
            loadSortOrder((MediaAdapter)adapter);
            //cần setFilter tại chỗ này
            //adapter.commitQuery(adapter.query());
            mAdapters[type] = adapter;
            mLists[type] = view;
            mRequeryNeeded[type] = true;
        }
        Log.d("Test", "instantiateItem: reuse created view");
        requeryIfNeeded(type);
        container.addView(view);
        return view;
    }

    public Limiter getCurrentLimiter(){
        LibraryAdapter current = mCurrentAdapter;
        if(current == null)
            return null;
        return current.getLimiter();
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
    /**
     * Run on query on the adapter passed in obj.
     *
     * Runs on worker thread.
     */
    private static final int MSG_RUN_QUERY = 0;
    /**
     * Save the sort mode for the adapter passed in obj.
     *
     * Runs on worker thread.
     */
    private static final int MSG_SAVE_SORT = 1;
    /**
     * passed in obj.
     *
     * Runs on worker thread.
     */
    private static final int MSG_REQUEST_REQUERY = 2;
    /**
     * Commit the cursor passed in obj to the adapter at the index passed in
     * arg1.
     *
     * Runs on UI thread.
     */
    private static final int MSG_COMMIT_QUERY = 3;
    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what){
            case MSG_RUN_QUERY: {
                LibraryAdapter adapter = (LibraryAdapter) msg.obj;
                int index = adapter.getMediaTypes();
                Handler handler = mUiHandler;
                handler.sendMessage(handler.obtainMessage(MSG_COMMIT_QUERY, index, 0, adapter.query()));
                break;
            }
            case MSG_COMMIT_QUERY: {
                int index = msg.arg1;
                mAdapters[index].commitQuery(msg.obj);
                Cursor cursor = (Cursor) msg.obj;
//                cursor.moveToFirst();
//                while(cursor.moveToNext()){
//                    Log.d("Test","New Data");
//                }
                break;
            }
            case MSG_REQUEST_REQUERY:
                requestRequery((LibraryAdapter)msg.obj);
                break;
            case MSG_SAVE_SORT: {
                MediaAdapter adapter = (MediaAdapter)msg.obj;
                SharedPreferences.Editor editor = PlaybackService.getSettings(mActivity).edit();
                editor.putInt(String.format("sort_%d_%d", adapter.getMediaTypes(), adapter.getLimiterType()), adapter.getSortMode());
                editor.apply();
                break;
            }
            default:
                return false;

        }
        return  true;
    }

    /**
     * Requery lại cái adapter được truyền vào ,nếu mà nó chính là cái mCurrentAdapter
     * thì requery luôn,nếu không thì đánh dấu cho nó cần queyr thành true
     * lần tiếp theo nếu người dùng kéo tới cái tab đó thì nó requery lại
     * @param adapter
     */
    public void requestRequery(LibraryAdapter adapter){
        if(adapter == mCurrentAdapter){
            postRunQuery(adapter);
        }
        else{
            mRequeryNeeded[adapter.getMediaTypes()] = true;
            Log.d("Test","Request requery");
            //adapter.clear();
            Log.d("Test","Clear adapter");
        }
    }

    /**
     * Gọi tới thằng {@link LibraryPagerAdapter#requestRequery(LibraryAdapter)} trên UI
     * thread.
     *
     * @param adapter để truyền vào requestRequery .
     */
    public void postRequestRequery(LibraryAdapter adapter)
    {
        Handler handler = mUiHandler;
        handler.sendMessage(handler.obtainMessage(MSG_REQUEST_REQUERY, adapter));
    }

    /**
     * Query cho cái adapter được truyền vào
     *
     * @param adapter cần được query
     */
    public void postRunQuery(LibraryAdapter adapter){
        mRequeryNeeded[adapter.getMediaTypes()] = false;
        Handler handler = mWorkerHandler;
        handler.removeMessages(MSG_RUN_QUERY,adapter);
        handler.sendMessage(handler.obtainMessage(MSG_RUN_QUERY,adapter));
    }



    public void requeryIfNeeded(int type){
        LibraryAdapter adapter = mAdapters[type];
        if(adapter != null && mRequeryNeeded[type]){
            Log.d("Test :","Requeired");
            postRunQuery(adapter);
        }
    }
    //=========================override cho thằng ViewPager.OnPageChangeListener=================//
    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        setPrimaryItem(null, position, null);
    }

    @Override
    public CharSequence getPageTitle(int position)
    {
        return mActivity.getResources().getText(TITLES[mTabOrder[position]]);
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        int type = mTabOrder[position];
        LibraryAdapter adapter = mAdapters[type];
        if(position != mCurrentPage || adapter != mCurrentAdapter){
            requeryIfNeeded(type);
            mCurrentAdapter = adapter;
            mCurrentPage = position;
            mActivity.onPageChanged(position, adapter);
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }
    //=========================override cho thằng ViewPager.OnPageChangeListener=================//

    @Override
    public void destroyItem(View container, int position, Object object) {
        ((ViewPager) container).removeView((View) object);
    }

    //=========================AdapterView.OnItemClickListenerr=================//

    /**
     * Khi mà cái item trong listview được click thì tạo dữ liệu từ row đó
     * đưa vào intent và truyền vào mActivity để xử lý
     * @param parent
     * @param view
     * @param position
     * @param id
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.d("Test :","LibraryPagerAdatper : onItemClick");
        Intent intent = (id == -1 ? createHeaderIntent(view) : mCurrentAdapter.createData(view));
        Log.d("Test :","LibraryPagerAdatper : Intent not null");
        mActivity.onItemClicked(intent);
    }

    //==========================Xử lý với các limiter của Adapters==============================//

    /**
     * Xóa hết các limiter hiện có
     * @param type
     */
    public void clearLimiter(int type){
        if(mArtistAdapter == null) {
            mPendingArtistLimiter = null;
        }else{
            mArtistAdapter.setLimiter(null);
            loadSortOrder(mArtistAdapter);
            requestRequery(mArtistAdapter);
        }

        if(mAlbumAdapter == null) {
            mPendingAlbumLimiter = null;
        }else{
            mAlbumAdapter.setLimiter(null);
            loadSortOrder(mAlbumAdapter);
            requestRequery(mAlbumAdapter);
        }

        if(mSongAdapter == null) {
            mPendingSongLimiter = null;
        }else{
            mSongAdapter.setLimiter(null);
            loadSortOrder(mSongAdapter);
            requestRequery(mSongAdapter);
        }
    }


    /**
     * Cập nhật lại các adatpers với cái limiter được đưa vào
     *
     * @param limiter    được đưa vào để thực hiện
     * @return ra một cái tab,là cái tab cần được chuyển đến khi expand cái row trong listview ra
     */
    public int setLimiter(Limiter limiter){
        int tab;

        switch (limiter.type){
            case MediaUtils.TYPE_ALBUM:
                if(mSongAdapter == null) {
                    mPendingSongLimiter = limiter;
                }else{
                    mSongAdapter.setLimiter(limiter);
                    loadSortOrder(mSongAdapter);
                    requestRequery(mSongAdapter);
                }
                tab = mSongsPosition;
                break;
            case MediaUtils.TYPE_ARTIST:
                if (mAlbumAdapter == null) {
                    mPendingAlbumLimiter = limiter;
                } else {
                    mAlbumAdapter.setLimiter(limiter);
                    loadSortOrder(mAlbumAdapter);
                    requestRequery(mAlbumAdapter);
                }
                if (mSongAdapter == null) {
                    mPendingSongLimiter = limiter;
                } else {
                    mSongAdapter.setLimiter(limiter);
                    loadSortOrder(mSongAdapter);
                    requestRequery(mSongAdapter);
                }
                tab = mAlbumsPosition;
                if (tab == -1)
                    tab = mSongsPosition;
                break;
            case MediaUtils.TYPE_GENRE:
                if(mArtistAdapter == null){
                    mPendingArtistLimiter = limiter;
                }else{
                    mArtistAdapter.setLimiter(limiter);
                    loadSortOrder(mArtistAdapter);
                    requestRequery(mArtistAdapter);
                }
                if (mAlbumAdapter == null) {
                    mPendingAlbumLimiter = limiter;
                } else {
                    mAlbumAdapter.setLimiter(limiter);
                    loadSortOrder(mAlbumAdapter);
                    requestRequery(mAlbumAdapter);
                }
                if (mSongAdapter == null) {
                    mPendingSongLimiter = limiter;
                } else {
                    mSongAdapter.setLimiter(limiter);
                    loadSortOrder(mSongAdapter);
                    requestRequery(mSongAdapter);
                }
                tab = mArtistsPosition;
                if (tab == -1)
                    tab = mAlbumsPosition;
                if (tab == -1)
                    tab = mSongsPosition;
                break;
            default:
                throw new IllegalArgumentException("Unsupported limiter type: " + limiter.type);
        }

        return tab;
    }

    /**
     * Gán filter mới cho tất cả adapters.
     */
    public void setFilter(String text){
        if(text.length() == 0)
            text = null;

        mFilter = text;
        for(LibraryAdapter adapter : mAdapters){
            if(adapter != null){
                adapter.setFilters(text);
                requestRequery(adapter);
            }
        }
    }

    /**
     * Gán sort mode cho adapter hiện tại,adapter hiện tại phải là MediaAdapter.
     * Lưu sort mode này vào preferences và updates danh sách view của adapter hiện tại
     * để hiển thị theo sort mode mới
     *
     * @param mode The sort mode. See {@link MediaAdapter#setSortMode(int)}
     */
    public void setSortMode(int mode)
    {
        MediaAdapter adapter = (MediaAdapter)mCurrentAdapter;
        if (mode == adapter.getSortMode())
            return;

        adapter.setSortMode(mode);
        requestRequery(adapter);

        Handler handler = mWorkerHandler;
        handler.sendMessage(handler.obtainMessage(MSG_SAVE_SORT, adapter));
    }

    /**
     * Gán lại sortmode đã được lưu cho adapter được truyền vào.adapter sẽ được re-queried sau
     * khi gọi hàm này
     *
     * @param adapter The adapter to load for.
     */
    public void loadSortOrder(MediaAdapter adapter)
    {
        String key = String.format("sort_%d_%d", adapter.getMediaTypes(), adapter.getLimiterType());
        int def = adapter.getDefaultSortMode();
        int sort = PlaybackService.getSettings(mActivity).getInt(key, def);
        adapter.setSortMode(sort);
    }
    //==========================Xử lý với các limiter của Adapters==============================//

    private static Intent createHeaderIntent(View header) {
        header = (View) header.getParent();//tag is set on parent view of header
        int type = (Integer) header.getTag();
        Intent intent = new Intent();
        intent.putExtra(LibraryAdapter.DATA_ID,LibraryAdapter.HEADER_ID);
        intent.putExtra(LibraryAdapter.DATA_TYPE, type);
        return intent;
    }


    /**
     * Set text cho dòng đầu tiên của các apdater song,album,artist
     * @param text
     */
    public void setHeaderText(String text){
        if (mArtistHeader != null)
            mArtistHeader.getTextView().setText(text);
        if (mAlbumHeader != null)
            mAlbumHeader.getTextView().setText(text);
        if (mSongHeader != null)
            mSongHeader.getTextView().setText(text);
        mHeaderText = text;
    }

    //=========================================View.OnCreateContextMenuListener====================//
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        View targetView = info.targetView;
        Intent intent = info.id == -1 ? createHeaderIntent(targetView) : mCurrentAdapter.createData(targetView);
        mActivity.onCreateContextMenu(menu, intent);
    }
}
