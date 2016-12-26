package chongxuocmanhinh.sound_plusplus;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.ImageButton;

/**
 * Created by L on 05/12/2016.
 */
public class SoundPlusPlusImageButton extends ImageButton {

    private Context mContext;
    private static int mNormalTint;
    private static int mActiveTint;

    public SoundPlusPlusImageButton(Context context) {
        super(context);
    }

    public SoundPlusPlusImageButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SoundPlusPlusImageButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        mNormalTint = fetchAttrColor(R.attr.controls_normal);
        mActiveTint = fetchAttrColor(R.attr.controls_active);
        updateImageTint(-1);
    }

    @Override
    public void setImageResource(int resId) {
        super.setImageResource(resId);
        this.updateImageTint(resId);
    }

    private void updateImageTint(int resHint) {
        int filterColor = mNormalTint;

        switch (resHint) {
            case R.drawable.repeat_active:
            case R.drawable.repeat_current_active:
            case R.drawable.stop_current_active:
            case R.drawable.shuffle_active:
            case R.drawable.shuffle_album_active:
            case R.drawable.random_active:
                filterColor = mActiveTint;
        }

        this.setColorFilter(filterColor);
    }

    private int fetchAttrColor(int attr) {
        TypedValue typedValue = new TypedValue();
        TypedArray a = mContext.obtainStyledAttributes(typedValue.data, new int[] { attr });
        int color = a.getColor(0, 0);
        a.recycle();
        return color;
    }
}
