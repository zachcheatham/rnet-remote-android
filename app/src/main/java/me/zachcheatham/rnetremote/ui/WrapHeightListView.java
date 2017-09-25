package me.zachcheatham.rnetremote.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListView;

public class WrapHeightListView extends ListView
{
    public WrapHeightListView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        int expandSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 2, MeasureSpec.AT_MOST);
        super.onMeasure(widthMeasureSpec, expandSpec);
    }
}
