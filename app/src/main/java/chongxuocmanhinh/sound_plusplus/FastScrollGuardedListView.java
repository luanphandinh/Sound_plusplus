package chongxuocmanhinh.sound_plusplus;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ListView;

/**
 * Created by L on 28/11/2016.
 */
public class FastScrollGuardedListView extends ListView{

    /**
     * Start edgeProtection at width-start
     */
    private static final int PROTECT_START_DP = 50; // AOSP has this set to 48dip in 5.x
    /**
     * End protection at width-end
     */
    private static final int PROTECT_END_DP = 12;
    /**
     * The calculated start position in pixel
     */
    private float mEdgeProtectStartPx = 0;
    /**
     * The calculated end position in pixel
     */
    private float mEdgeProtectEndPx = 0;


    public FastScrollGuardedListView(Context context) {
        super(context);
    }

    public FastScrollGuardedListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FastScrollGuardedListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public FastScrollGuardedListView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    /**
     * Intercepted touch event from ListView
     * We will use this callback to send fake X-coord events if
     * the actual event happened in the protected area (eg: the hardcoded fastscroll area)
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mEdgeProtectStartPx == 0)
            mEdgeProtectStartPx = getWidth() - PROTECT_START_DP * Resources.getSystem().getDisplayMetrics().density;
        if (mEdgeProtectEndPx == 0)
            mEdgeProtectEndPx = getWidth() - PROTECT_END_DP * Resources.getSystem().getDisplayMetrics().density;

        if (ev.getX() > mEdgeProtectStartPx && ev.getX() < mEdgeProtectEndPx) {
            // Cursor is in protected area: simulate an event with a faked x coordinate
            ev = MotionEvent.obtain(ev.getDownTime(), ev.getEventTime(), ev.getAction(), mEdgeProtectStartPx, ev.getY(), ev.getMetaState());
        }
        return super.onInterceptTouchEvent(ev);
    }
}
