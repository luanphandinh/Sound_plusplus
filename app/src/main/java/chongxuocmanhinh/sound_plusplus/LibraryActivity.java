package chongxuocmanhinh.sound_plusplus;
import android.app.Activity;
import android.content.ContentResolver;
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

public class LibraryActivity extends Activity
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
     * The adapter for the currently visible list.
     */
    private LibraryAdapter mCurrentAdapter;

    private BottomBarControls mBottomBarControls;
    /**
     * Hai cái dưới này dùng để update khi mà có limiter nào đó được set
     */
    private HorizontalScrollView mLimiterScroller;
    private ViewGroup mLimiterViews;

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
    //=====================================View.OnClickListener==============================//
    @Override
    public void onClick(View view) {
        if (view == mBottomBarControls) {
            return;
        }
        if(view.getTag() != null){
            int i = (int) view.getTag();

            Limiter limiter = mPagerAdapter.getCurrentLimiter();
            int type = limiter.type;
            if(i == 1 && type == MediaUtils.TYPE_ALBUM){
                setLimiter(MediaUtils.TYPE_ARTIST, limiter.data.toString());
            }else{
                mPagerAdapter.clearLimiter(type);
            }

            updateLimiterViews();
        }
    }
}
