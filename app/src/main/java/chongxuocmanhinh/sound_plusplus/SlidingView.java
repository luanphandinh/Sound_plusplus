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


    public SlidingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

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
        return false;
    }
}
