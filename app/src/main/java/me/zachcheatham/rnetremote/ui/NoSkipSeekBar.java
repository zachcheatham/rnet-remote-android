package me.zachcheatham.rnetremote.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatSeekBar;

import me.zachcheatham.rnetremote.R;

public class NoSkipSeekBar extends AppCompatSeekBar {

    private float startX = -1;
    private int startProgress = -1;

    private OnSeekBarChangeListener mListener;

    public NoSkipSeekBar(@NonNull Context context) {
        super(context);
    }

    public NoSkipSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);

        setClickable(true);
        setFocusable(true);
    }

    public NoSkipSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private void startDrag(MotionEvent event) {
        setPressed(true);

        setBackgroundColor(getResources().getColor(R.color.colorMute));

        if (getThumb() != null) {
            invalidate(getThumb().getBounds());
        }

        if (mListener != null) {
            mListener.onStartTrackingTouch(this);
        }

        startX = event.getX();
        startProgress = getProgress();
        claimDrag();
    }

    private void stopDrag(MotionEvent event) {
        setPressed(false);

        setBackgroundColor(0);

        if (getThumb() != null) {
            invalidate(getThumb().getBounds());
        }

        if (mListener != null) {
            mListener.onStopTrackingTouch(this);
        }

        startX = -1;
        startProgress = -1;
    }

    private void trackTouchEvent(MotionEvent event) {
        float x = event.getX();
        float width = getWidth() - getPaddingLeft() - getPaddingRight();
        int progressMax = getMax();

        float delta = x - startX;
        int progressDelta = Math.round(delta * progressMax / width);
        int newProgress = Math.min(Math.max(0, startProgress + progressDelta), progressMax);

        setProgress(newProgress);
        if (mListener != null) {
            mListener.onProgressChanged(this, newProgress, true);
        }

        setProgress(newProgress);
    }

    private void claimDrag() {
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (!isEnabled()) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startDrag(event);
                break;
            case MotionEvent.ACTION_MOVE:
                if (startX == -1) {
                    startDrag(event);
                }
                else {
                    trackTouchEvent(event);
                }

                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                stopDrag(event);
                break;
        }

        return true;
    }



    @Override
    public void setOnSeekBarChangeListener(OnSeekBarChangeListener l) {
        super.setOnSeekBarChangeListener(l);
        this.mListener = l;
    }
}
