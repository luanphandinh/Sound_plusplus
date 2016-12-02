package chongxuocmanhinh.sound_plusplus;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.SearchView;
import android.widget.TextView;

/**
 * Created by L on 02/12/2016.
 */
public class BottomBarControls extends LinearLayout
                            implements View.OnClickListener,
                                    PopupMenu.OnMenuItemClickListener
{
    /**
     * The application context
     */
    private final Context mContext;
    /**
     * Tên bài hát đang được play
     */
    private TextView mTitle;
    /**
     * Tên ca sĩ của bài hát đang được play
     */
    private TextView mArtist;
    /**
     * A layout hosting the song information
     */
    private LinearLayout mControlsContent;
    /**
     * Standard android search view
     */
    private SearchView mSearchView;

    public BottomBarControls(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    @Override
    protected void onFinishInflate() {
        mTitle = (TextView)findViewById(R.id.title);
        mArtist = (TextView)findViewById(R.id.artist);
        //mCover = (ImageView)findViewById(R.id.cover);
        mSearchView = (SearchView)findViewById(R.id.search_view);
        mControlsContent = (LinearLayout)findViewById(R.id.content_controls);

        super.onFinishInflate();
    }

    //==========================View.OnClickListener==================================//
    @Override
    public void onClick(View v) {

    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return false;
    }
}
