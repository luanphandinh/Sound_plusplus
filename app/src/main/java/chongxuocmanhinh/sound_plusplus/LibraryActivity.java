package chongxuocmanhinh.sound_plusplus;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;

public class LibraryActivity extends Activity
{


    public ViewPager mViewPager;
    /**
     * The pager adapter that manages each media ListView.
     */
    public LibraryPagerAdapter mPagerAdapter;

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

        mPagerAdapter = new LibraryPagerAdapter(this,null);
        mViewPager.setAdapter(mPagerAdapter);
        mPagerAdapter.notifyDataSetChanged();
    }


    public void onItemClicked(Intent rowData){
        int action = mDefaultAction;
    }
}
