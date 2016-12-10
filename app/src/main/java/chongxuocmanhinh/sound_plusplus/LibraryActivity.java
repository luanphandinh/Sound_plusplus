package chongxuocmanhinh.sound_plusplus;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;

public class LibraryActivity extends Activity
                implements Handler.Callback
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
     * The adapter for the currently visible list.
     */
    private LibraryAdapter mCurrentAdapter;

    /**
     * Hành động được thực thi khi một dòng được nhấp
     */
    private int mDefaultAction;

//    private HorizontalScrollView mLimiterScrollView;
//    private ViewGroup mLimiterViews;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("Test", "onCreate: ");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.library_content);

        mViewPager = (ViewPager) findViewById(R.id.pager);
//        mLimiterScrollView = (HorizontalScrollView) findViewById(R.id.limiter_scroller);
//        mLimiterViews = (ViewGroup) findViewById(R.id.limiter_layout);

        HandlerThread thread = new HandlerThread(getClass().getName(), Process.THREAD_PRIORITY_LOWEST);
        thread.start();

        mLooper = thread.getLooper();
        mHandler = new Handler(mLooper, this);

        mPagerAdapter = new LibraryPagerAdapter(this,mLooper);
        mViewPager.setAdapter(mPagerAdapter);
        mPagerAdapter.notifyDataSetChanged();

        if(PermissionRequestActivity.havePermissions(this) == false) {
            PermissionRequestActivity.showWarning(this, getIntent());
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
       // mDefaultAction =
    }

    //=====================Khi pagerAdapter bắt được clickListener thì gọi tới đống này===================//
    public void onItemClicked(Intent rowData){
        //int action = mDefaultAction;
        Log.d("Test :","LibraryActivity : OnItemClicked");
        if(rowData.getBooleanExtra(LibraryAdapter.DATA_EXPANDABLE, false)){
            onItemExpanded(rowData);
        }
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


    public void updateLimiterViews(){

    }
    //=======================================Handler.Callback==============================//
    @Override
    public boolean handleMessage(Message msg) {
        return false;
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
}
