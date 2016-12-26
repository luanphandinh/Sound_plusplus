package chongxuocmanhinh.sound_plusplus;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

import java.util.ArrayList;

/**
 * Created by L on 02/12/2016.
 */
public class SlidingView extends FrameLayout
    implements View.OnTouchListener
{

    /**
     * Bỏ qua phần kéo thả cho đến khi đi quá 30px
     */
    private final float MAX_PROGRESS = 30;

    /**
     * Thòi gian của hàm animate() tính theo ms
     */
    private final int ANIMATION_DURATION = 250;
    /**S
     * Giá trị offset lớn nhất của view
     */
    private float mMaxOffsetY = 0;
    /**
     * Tọa độ Y lúc trước,được sửu dụng để tính sự khác biệt lúc kéo thả
     */
    private float mPreviousY = 0;
    /**
     * Tổng số px hiện đang được kéo thả
     */
    private float mProgressPx = 0;
    /**
     * Tính hướng và tốc độ kéo thả
     */
    private float mFlingVelocity = 0;
    /**
     * TRUE nếu ta bắt đầu kéo thả view(hàng trong listview)
     */
    private boolean mDidScroll = false;
    /**
     * TRUE nếu ta phải ẩn slide khi visiblity thay đổi
     */
    private boolean mDelayedHide = false;
    /**
     * Reference to the gesture detector
     */
    private GestureDetector mDetector;
    /**
     * An external View we are managing during layout changes.
     */
    private View mSlaveView;
    /**
     * The resource id to listen for touch events
     */
    private int mSliderHandleId = 0;
    /**
     * The current expansion stage
     */
    int mCurrentStage = 0;
    /**
     * List with all possible stages and their offsets
     */
    ArrayList<Integer> mStages = new ArrayList<Integer>();
    /**
     * Our callback interface
     */
    private Callback mCallback;
    public interface Callback {
        public abstract void onSlideFullyExpanded(boolean expanded);
    }

    public SlidingView(Context context) {
        this(context, null);
    }

    public SlidingView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    public SlidingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setBackgroundColor(ThemeHelper.getDefaultCoverColors(context)[0]);
        mDetector = new GestureDetector(new GestureListener());
        TypedArray  a = context.obtainStyledAttributes(attrs,R.styleable.SlidingViewPreferences);
        mSliderHandleId = a.getResourceId(R.styleable.SlidingViewPreferences_slider_handle_id,0);
        int slaveId = a.getResourceId(R.styleable.SlidingViewPreferences_slider_slave_id,0);
        a.recycle();

        // This is probably a parent view: so we need the context but can search
        // it before we got inflated:
        mSlaveView = ((Activity)context).findViewById(slaveId);
    }

    /**
     * Gán cái callbakc instance cho thằng này
     *
     * @param callback tham chiếu tới activity có implenet cái callbakc interface của lớp này
     */
    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    /**
     * Expands toàn bộ slide
     */
    public void expandSlide() {
        setExpansionStage(mStages.size()-1);
    }

    /**
     * Hides the slide
     */
    public void hideSlide() {
        setSlaveViewStage(0); // ensure that parent is visible before the animation starts
        setExpansionStage(0);
    }
    /**
     * Same as hideSlide(), but will fire after we are not
     * visible anymore
     */
    public void hideSlideDelayed() {
        mDelayedHide = true;
    }

    /**
     * Returns true if the slide is fully hidden
     */
    public boolean isHidden() {
        return mCurrentStage == 0;
    }

    /**
     * Returns true nếu slide được expand hoàn toàn lên màn hình
     */
    public boolean isExpanded() {
        return mCurrentStage == (mStages.size()-1);
    }

    /**
     * Transforms to the new expansion state
     *
     * @param stage the stage to transform to
     */
    private void setExpansionStage(int stage) {
        if (mStages.size() < 1)
            return;

        mCurrentStage = stage;
        mDelayedHide = false;

        int pxOff = mStages.get(stage);
        this
                .animate()
                .translationY(pxOff)
                .setDuration(ANIMATION_DURATION)
                .setListener(new AnimationListener())
                .setInterpolator(new DecelerateInterpolator());
    }


    /**
     * Changes the parent view to fit given stage
     *
     * @param stage the stage to transform to
     */
    private void setSlaveViewStage(int stage) {
        if (mSlaveView == null)
            return;

        int totalOffset = 0;
        for (int i = 0; i <= stage; i++) {
            totalOffset += getChildAt(i).getHeight();
        }
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams)mSlaveView.getLayoutParams();
        params.bottomMargin = totalOffset;
        mSlaveView.setLayoutParams(params);
    }
    /**
     * Được gọi sau khi view được inflated, binds  onTouchListener tới các elements con của child view
     */
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        View handle = findViewById(mSliderHandleId);
        if(handle != null){
            if(handle instanceof ViewGroup){
                ViewGroup group = (ViewGroup)handle;
                for(int i = 0;i <group.getChildCount();i++){
                    group.getChildAt(i).setOnTouchListener(this);
                }
            }else{
                handle.setOnTouchListener(this);
            }
        }
    }


    /**
     *stack tất cả các views horizontally
     */
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        int viewHeight = getMeasuredHeight();
        int childCount = getChildCount();
        int topOffset = 0;
        View lastChild = null;

        mStages.clear();

        for (int i = 0; i < childCount ; i++) {
            lastChild = getChildAt(i);
            int childWidth = lastChild.getMeasuredWidth();
            int childHeight = lastChild.getMeasuredHeight();
            int childBottom = childHeight + topOffset;

            // No child should consume space outside of our view
            if (topOffset > viewHeight)
                topOffset = viewHeight;
            if (childBottom > viewHeight)
                childBottom = viewHeight;

            lastChild.layout(0, topOffset, childWidth, childBottom);
            mStages.add(viewHeight - childBottom);
            topOffset += childHeight;
        }

        if (lastChild != null && mMaxOffsetY == 0) {
            // Sizes are now fixed: Overwrite any (possible) FILL_PARENT or WRAP_CONTENT
            // value with the measured size
            // This should only happen on the first run (mMaxOffsetY == 0)
            for (int i = 0; i < childCount ; i++) {
                View child = getChildAt(i);
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams)child.getLayoutParams();
                params.height = child.getHeight();
                params.width = child.getWidth();
                child.setLayoutParams(params);
            }
        }

        if (changed) {
            mMaxOffsetY = mStages.get(0);
            setTranslationY(mMaxOffsetY);
            setExpansionStage(0);
        }
    }


    class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent event1, MotionEvent event2,  float velocityX, float velocityY) {
            mFlingVelocity = velocityY;
            return true;
        }
    }

    class AnimationListener extends AnimatorListenerAdapter {
        @Override
        public void onAnimationEnd(Animator animation) {
            setSlaveViewStage(mCurrentStage);
            if (mCallback != null) {
                mCallback.onSlideFullyExpanded( mCurrentStage == mStages.size()-1 );
            }
        }
        @Override
        public void onAnimationCancel(Animator animation) {
            onAnimationEnd(animation);
        }
    }

    //========================implement for View.OnTouchListener================================//
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        // Fix up the event offset as we are moving the view itself.
        // This is required to get flings correctly detected
        event.setLocation(event.getRawX(), event.getRawY());

        mDetector.onTouchEvent(event);
        float y = event.getRawY();
        float dy = y - mPreviousY;    // diff Y
        float vy = getTranslationY(); // view Y

        switch(event.getActionMasked()) {
            case MotionEvent.ACTION_UP : {
                if (mDidScroll == false) { // Dispatch event if we never scrolled
                    v.onTouchEvent(event);
                } else {
                    int nstages = mStages.size();
                    int tstage = 0;
                    // add the amounts of pixels we would progress in HALF of the time of the animation as a virtual progress
                    int tbonus = (int)(mFlingVelocity * 0.001 * ANIMATION_DURATION * 0.5);
                    for (int i = 0; i < nstages; i++) {
                        int csnap = getChildAt(i).getHeight() / 2; // try to 'snap in' at half of this childs height
                        if (vy+tbonus-csnap <= mStages.get(i))
                            tstage = i;
                    }
                    setExpansionStage(tstage);
                }
                break;
            }
            case MotionEvent.ACTION_DOWN : {
                v.onTouchEvent(event);

                mProgressPx = 0;
                mFlingVelocity = 0;
                mDidScroll = false;
                break;
            }
            case MotionEvent.ACTION_MOVE : {
                mProgressPx += Math.abs(dy);
                float usedY = vy + dy;

                if (usedY < 0)
                    usedY = 0;
                if (usedY > mMaxOffsetY)
                    usedY = mMaxOffsetY;

                if (mProgressPx < MAX_PROGRESS) {
                    // we did not reach a minimum of progress: do not scroll yet
                    usedY = vy;
                } else if (mDidScroll == false) {
                    mDidScroll = true;
                    event.setAction(MotionEvent.ACTION_CANCEL);
                    v.onTouchEvent(event);
                    setSlaveViewStage(0); // parent can use full view, will be reset on ACTION_UP handlers
                }

                setTranslationY(usedY);
                break;
            }
        }
        mPreviousY = y;
        return true;
    }
}
