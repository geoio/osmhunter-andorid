package com.geoio.osmhunter.app.Workarounds;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import org.osmdroid.views.MapView;

// fix the MapView (Bug #2)
public final class MyMapView extends MapView {
    private static final int IGNORE_MOVE_COUNT = 2;
    private int moveCount = 0;
    public boolean locked = false;

    public MyMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if(locked) {
            return true;
        }

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_MOVE:

                if (moveCount > 0) {
                    moveCount--;
                    return true;
                }

                break;

            case MotionEvent.ACTION_POINTER_UP:
                moveCount = IGNORE_MOVE_COUNT;
                break;
        }
        return super.onTouchEvent(ev);
    }
}