package chongxuocmanhinh.sound_plusplus;
import android.content.DialogInterface;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;

public class LibraryActivity extends AppCompatActivity
{


    public ViewPager mViewPager;

    private HorizontalScrollView mLimiterScrollView;
    private ViewGroup mLimiterViews;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.library_content);

        mViewPager = (ViewPager) findViewById(R.id.pager);

    }
}
