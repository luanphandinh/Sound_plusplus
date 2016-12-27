package chongxuocmanhinh.sound_plusplus;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
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
     * Cover image
     */
    private ImageView mCover;
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
    /**
     * Activiy chứa options menu và nhận event click
     */
    private Activity mParentMenuConsumer;

    private PopupMenu mPopupMenu;

    private View.OnClickListener mParentClickConsumer;

    public BottomBarControls(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    @Override
    protected void onFinishInflate() {
        mTitle = (TextView)findViewById(R.id.title);
        mArtist = (TextView)findViewById(R.id.artist);
        mCover = (ImageView)findViewById(R.id.cover);
        mSearchView = (SearchView)findViewById(R.id.search_view);
        mControlsContent = (LinearLayout)findViewById(R.id.content_controls);

        styleSearchView(mSearchView, mContext.getResources().getColor(android.R.color.background_light));

        super.onFinishInflate();
    }

    /**
     * Cấu hình cho OnQueryTextListenern cho searchview
     */
    public void setOnQueryTextListener(SearchView.OnQueryTextListener owner) {
        mSearchView.setOnQueryTextListener(owner);
    }

    @SuppressLint("NewApi")//Popup menu với gravity API19,được kiểm tra bằng hàm menuMargin()
    public void enableOptionsMenu(Activity owner){
        mParentMenuConsumer = owner;

        ImageButton menuButton = getImageButton(getResources().getDrawable(R.drawable.ic_menu_moreoverflow));
        mPopupMenu = (menuMargin() ? new PopupMenu(mContext, menuButton, Gravity.RIGHT) : new PopupMenu(mContext, menuButton));
        mPopupMenu.setOnMenuItemClickListener(this);

        //Để cho thằng cha populate menu
        mParentMenuConsumer.onCreateOptionsMenu(mPopupMenu.getMenu());

        //Menu đã có,add tất cả menuItem bị ẩn mà có icon vào toolbar
        //vd : Thanh menuSearch ở thằng cha sẽ bị invisible,do đó ta add menuItem này vào thanh
        //toolbar
        Menu menu = mPopupMenu.getMenu();
        for (int i=0; i < menu.size(); i++) {
            MenuItem menuItem = menu.getItem(i);
            if (menuItem.isVisible() == false && menuItem.getIcon() != null) {
                ImageButton button = getImageButton(menuItem.getIcon());
                button.setTag(menuItem);
                button.setOnClickListener(this);
                mControlsContent.addView(button, -1);
            }
        }
        //Add menu button vào cuối
        menuButton.setTag(mPopupMenu);
        menuButton.setOnClickListener(this);
        int specialSnowflake = menuMargin() ? dpToPx(36) : LinearLayout.LayoutParams.WRAP_CONTENT;
        menuButton.setLayoutParams(new LinearLayout.LayoutParams(specialSnowflake, LinearLayout.LayoutParams.WRAP_CONTENT));
        mControlsContent.addView(menuButton, -1);


        View spacer = new View(mContext);
        spacer.setOnClickListener(this);
        spacer.setTag(mPopupMenu);
        int spacerDp = menuMargin() ? dpToPx(4) : 0;
        spacer.setLayoutParams(new LinearLayout.LayoutParams(spacerDp, LinearLayout.LayoutParams.MATCH_PARENT));
        mControlsContent.addView(spacer, -1);
    }

    //==========================View.OnClickListener==================================//
    @Override
    public void onClick(View v) {
        Object tag = v.getTag();
        if(tag instanceof PopupMenu){
            Log.d("TestOptionMenu","BottomBarControls PopupMenu");
            openMenu();
        }else if (tag instanceof MenuItem){
            Log.d("TestOptionMenu","BottomBarControls itemSelected");
            mParentMenuConsumer.onOptionsItemSelected((MenuItem)tag);
        } else if (v == mControlsContent && mParentMenuConsumer != null){
            mParentClickConsumer.onClick(this);
        }
    }

    /**
     * Opens the OptionsMenu of this view
     */
    public void openMenu() {
        if (mPopupMenu == null || mParentMenuConsumer == null)
            return;
        mParentMenuConsumer.onPrepareOptionsMenu(mPopupMenu.getMenu());
        mPopupMenu.show();
    }


    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return mParentMenuConsumer.onOptionsItemSelected(item);
    }

    public void setSong(Song song){
        if(song == null){
            mTitle.setText(null);
            mArtist.setText(null);
            mCover.setImageBitmap(null);
        }else{
            Resources res = mContext.getResources();
            String title = song.title == null ? res.getString(R.string.unknown) : song.title;
            String artist = song.artist == null ? res.getString(R.string.unknown) : song.artist;
            mTitle.setText(title);
            mArtist.setText(artist);
        }
    }
    public void setOnClickListener(View.OnClickListener listener){
        mParentClickConsumer = listener;
        mControlsContent.setOnClickListener(this);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        showSearch(false);
        return super.onSaveInstanceState();
    }

    public boolean showSearch(boolean visible){
        boolean wasVisible = mSearchView.getVisibility() == View.VISIBLE;
        if(wasVisible != visible){
            mSearchView.setVisibility(visible ? View.VISIBLE : View.GONE);
            mControlsContent.setVisibility(visible ? View.GONE : View.VISIBLE);
            if(visible)
                mSearchView.setIconified(false);// yêu cầu focus và hiên keyboard
            //cho dù view đã được chọn.
            else
                mSearchView.setQuery("",false);
        }
        return wasVisible;
    }

    /**
     * Thay đổi màu của text và image sang style
     * @param view
     * @param color
     */
    private void styleSearchView(View view, int color) {
        if (view != null) {
            if (view instanceof TextView) {
                ((TextView)view).setTextColor(color);
            } else if (view instanceof ImageView) {
                ((ImageView)view).setColorFilter(color);
            } else if (view instanceof ViewGroup) {
                ViewGroup group = (ViewGroup)view;
                for (int i=0; i< group.getChildCount(); i++) {
                    styleSearchView(group.getChildAt(i), color);
                }
            }
        }
    }

    /**
     *Trả về true nếu thiết bị sử dụng HOLO (android 4) theme
     */
    private boolean menuMargin(){
        return usesHoloTheme() == false;
    }

    final public static boolean usesHoloTheme() {
        return (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP);
    }

    private ImageButton getImageButton(Drawable drawable){
        ImageButton button = new ImageButton(mContext);
        button.setImageDrawable(drawable);
        button.setBackgroundResource(R.drawable.unbound_ripple_light);
        return button;
    }

    /**
     * Updates cover image của view
     *
     * @param cover the bitmap to display. Will use a placeholder image if cover is null
     */
    public void setCover(Bitmap cover) {
        if (cover == null)
            mCover.setImageResource(R.drawable.fallback_cover);
        else
            mCover.setImageBitmap(cover);
    }

    /**
     * chuyển dp sang pixels
     *
     * @param dp input as dp
     * @return output as px
     */
    private int dpToPx(int dp) {
        return (int)(getResources().getDisplayMetrics().density * dp);
    }
}
