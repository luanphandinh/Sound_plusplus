package chongxuocmanhinh.sound_plusplus;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.Checkable;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.w3c.dom.Text;

/**
 * Created by L on 10/11/2016.
 */
public class DraggableRow extends LinearLayout implements Checkable{

    /**
     * Trả về true nếu checkbox được check
     */
    private boolean mChecked;

    /**
     * True nếu setup layout đã được gọi
     * xem thêm 4 cái layoutType
     */
    private boolean mLayoutSet;

    public DraggableRow(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private TextView    mTextView;
    private CheckedTextView mCheckBox;
    private View        mPmark;
    private ImageView   mDragger;
    private LazyCoverView mCoverView;
    /**
     * Layout types
     *  Mỗi loại layout sẽ có cách hiển thị row khác nhau
     *  Đã set lay out rồi thì mLayoutSet thành true
     */
    public static final int LAYOUT_TEXTONLY = 0;
    public static final int LAYOUT_CHECKBOXES = 1;
    public static final int LAYOUT_DRAGGABLE  = 2;
    public static final int LAYOUT_LISTVIEW   = 3;

    @Override
    protected void onFinishInflate() {
        mCheckBox = (CheckedTextView) this.findViewById(R.id.checkbox);
        mTextView = (TextView) this.findViewById(R.id.text);
        mPmark = this.findViewById(R.id.pmark);
        mDragger = (ImageView) this.findViewById(R.id.dragger);
        mCoverView = (LazyCoverView)this.findViewById(R.id.cover);
        super.onFinishInflate();
    }

    /**
     * Dùng để setup dạng view chung của một layout
     * Chỉ được gọi 1 lần,sau khi gọi layoutSet sẽ về true.
     *
     * @param type
     */
    public void setupLayout(int type){
        if(!mLayoutSet){
            switch (type){
                case LAYOUT_CHECKBOXES:
                    mCheckBox.setVisibility(View.VISIBLE);
                    showDragger(true);
                    break;
                case LAYOUT_DRAGGABLE:
                    highlightRow(false);
                    mCoverView.setVisibility(View.VISIBLE);
                    showDragger(true);
                    break;
                case LAYOUT_LISTVIEW:
                    highlightRow(false);
                    mCoverView.setVisibility(View.VISIBLE);
                    mDragger.setImageResource(R.drawable.arrow);
                    break;
                case LAYOUT_TEXTONLY:
                default:
                    break; // do not care
            }
        }
    }


    public void highlightRow(boolean state){
        mPmark.setVisibility(state ? View.VISIBLE : View.INVISIBLE);
    }
    @Override
    public boolean isChecked() {
        return mChecked;
    }


    @Override
    public void setChecked(boolean checked) {
        mChecked = checked;
        mCheckBox.setChecked(mChecked);
    }

    @Override
    public Object getTag() {
        return mTextView.getTag();
    }

    @Override
    public void setTag(Object tag) {
        mTextView.setTag(tag);
    }


    @Override
    public void toggle() {
        setChecked(!mChecked);
    }
    /**
     * Thay đổi khả năng hiển thị của dragger
     * @param state
     */
    public void showDragger(boolean state){
        mDragger.setVisibility(state ? View.VISIBLE : View.INVISIBLE);
    }

    public void setDraggerOnClickListener(View.OnClickListener listener){
        TypedValue v = new TypedValue();
        getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground,v,true);

        mDragger.setBackgroundResource(v.resourceId);
        mDragger.setOnClickListener(listener);
    }

    public TextView getTextView(){
        return mTextView;
    }

    /**
     * Trả về coverview
     */
    public LazyCoverView getCoverView() {
        return mCoverView;
    }

}
