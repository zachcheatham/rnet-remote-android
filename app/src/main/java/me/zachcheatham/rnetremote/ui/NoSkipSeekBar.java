package me.zachcheatham.rnetremote.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatSeekBar;

public class NoSkipSeekBar extends AppCompatSeekBar {

    private float startX = -1;
    private int startProgress = -1;

    private OnSeekBarChangeListener mListener;

    public NoSkipSeekBar(@NonNull Context context) {
        super(context);
    }

    public NoSkipSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NoSkipSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (!isEnabled()) {
            return false;
        }

        float x = event.getX();
        float width = getWidth() - getPaddingLeft() - getPaddingRight();
        int progressMax = getMax();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = event.getX();
                startProgress = getProgress();

                getParent().requestDisallowInterceptTouchEvent(true);
                setPressed(true);

                return true;
            case MotionEvent.ACTION_MOVE:
                float delta = x - startX;
                int progressDelta = Math.round(delta * progressMax / width);
                int newProgress = Math.min(Math.max(0, startProgress + progressDelta), progressMax);

                setProgress(newProgress);
                if (mListener != null) {
                    mListener.onProgressChanged(this, newProgress, true);
                }

                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                startX = -1;
                startProgress = -1;
                getParent().requestDisallowInterceptTouchEvent(false);
                setPressed(false);
                return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void setOnSeekBarChangeListener(OnSeekBarChangeListener l) {
        super.setOnSeekBarChangeListener(l);
        this.mListener = l;
    }
}
