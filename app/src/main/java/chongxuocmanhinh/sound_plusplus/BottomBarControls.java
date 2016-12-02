package chongxuocmanhinh.sound_plusplus;

import android.content.Context;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
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
    //private final Context mContext;
    /**
     * Tên bài hát đang được play
     */
    private TextView mTitle;
    /**
     * Tên ca sĩ của bài hát đang được play
     */
    private TextView mArtist;
    public BottomBarControls(Context context) {
        super(context);
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
