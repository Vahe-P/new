package com.example.anew;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.OverScroller;
import android.widget.ScrollView;

public class InertialScrollView extends ScrollView {

    private OverScroller scroller;

    public InertialScrollView(Context context) {
        super(context);
        init(context);
    }

    public InertialScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public InertialScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        scroller = new OverScroller(context);
    }

    @Override
    public void fling(int velocityY) {
        super.fling(velocityY/10);
        // Customize the fling behavior by modifying velocityY or adding more inertia
        scroller.fling(
                getScrollX(), getScrollY(),
                0, velocityY, // X and Y velocity
                0, 0,         // X min and max bounds
                Integer.MIN_VALUE, Integer.MAX_VALUE // Y min and max bounds
        );
        invalidate(); // Trigger a redraw to keep the fling active
    }


    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollTo(scroller.getCurrX(), scroller.getCurrY());
            postInvalidate(); // Continue scrolling
        } else {
            super.computeScroll();
        }
    }
}