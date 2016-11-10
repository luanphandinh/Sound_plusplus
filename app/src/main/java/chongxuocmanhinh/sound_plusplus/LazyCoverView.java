package chongxuocmanhinh.sound_plusplus;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Created by L on 10/11/2016.
 */
public class LazyCoverView extends ImageView implements Handler.Callback {
    public LazyCoverView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean handleMessage(Message msg) {
        return false;
    }
}
