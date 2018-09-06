package me.zachcheatham.rnetremote.ui;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.widget.ImageView;
import com.nostra13.universalimageloader.core.imageaware.ViewAware;
import jp.wasabeef.blurry.Blurry;

public class BlurryImageViewAware extends ViewAware
{
    public BlurryImageViewAware(ImageView view)
    {
        super(view, true);
    }

    @Override
    protected void setImageDrawableInto(Drawable drawable, View view) {}

    @Override
    protected void setImageBitmapInto(Bitmap bitmap, View view)
    {
        Blurry.with(view.getContext()).from(bitmap).into((ImageView) view);
    }
}
