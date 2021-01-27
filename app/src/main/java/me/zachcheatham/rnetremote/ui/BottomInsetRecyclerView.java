package me.zachcheatham.rnetremote.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.WindowInsets;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

public class BottomInsetRecyclerView extends RecyclerView {

    private final int initialPaddingStart;
    private final int initialPaddingEnd;
    private final int initialPaddingTop;
    private final int initialPaddingBottom;

    public BottomInsetRecyclerView(@NonNull Context context) {
        super(context);

        initialPaddingStart = getPaddingStart();
        initialPaddingEnd = getPaddingEnd();
        initialPaddingTop = getPaddingTop();
        initialPaddingBottom = getPaddingBottom();
    }

    public BottomInsetRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        initialPaddingStart = getPaddingStart();
        initialPaddingEnd = getPaddingEnd();
        initialPaddingTop = getPaddingTop();
        initialPaddingBottom = getPaddingBottom();
    }

    public BottomInsetRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        initialPaddingStart = getPaddingStart();
        initialPaddingEnd = getPaddingEnd();
        initialPaddingTop = getPaddingTop();
        initialPaddingBottom = getPaddingBottom();
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        insets = insets.replaceSystemWindowInsets(
            insets.getSystemWindowInsetLeft() + initialPaddingStart,
            initialPaddingTop,
            insets.getSystemWindowInsetRight() + initialPaddingEnd,
            insets.getSystemWindowInsetBottom() + initialPaddingBottom);

        return super.onApplyWindowInsets(insets);
    }


}
