package chongxuocmanhinh.sound_plusplus;

import android.content.Context;
import android.util.AttributeSet;
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

    }

    @Override
    public void setImageResource(int resId) {
        super.setImageResource(resId);
    }

    //    public SoundPlusPlusImageButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
//        super(context, attrs, defStyleAttr, defStyleRes);
//    }
}
