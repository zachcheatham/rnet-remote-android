package me.zachcheatham.rnetremote.ui;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import com.nostra13.universalimageloader.core.imageaware.ViewAware;

public class BackgroundImageViewAware extends ViewAware
{
    public BackgroundImageViewAware(View view)
    {
        super(view, true);
    }

    @Override
    protected void setImageDrawableInto(Drawable drawable, View view) {}

    @Override
    protected void setImageBitmapInto(Bitmap bitmap, View view)
    {
        Drawable drawable = new CenterCropDrawable(new BitmapDrawable(view.getContext().getResources(), bitmap));
        drawable.setAlpha(51);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            view.setBackground(drawable);
        else
            view.setBackgroundDrawable(drawable);
    }
}
