package me.zachcheatham.rnetremote.ui;

import android.annotation.SuppressLint;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

// Source: https://gist.github.com/rudchenkos/e33dc0d6669a61dde9d6548f6c3e0e7e

/**
 * Drawable decorator which draws the target drawable similarly to an ImageView with scaleType=centerCrop
 * <p>
 * Example usage:
 * final Drawable bg = getResources().getDrawable(R.drawable.screen);
 * getWindow().setBackgroundDrawable(new CenterCropDrawable(bg));
 */
public class CenterCropDrawable extends Drawable
{

    @NonNull
    private final Drawable target;

    public CenterCropDrawable(@NonNull Drawable target)
    {
        this.target = target;
    }

    @Override
    public void setBounds(@NonNull Rect bounds)
    {
        super.setBounds(bounds.left, bounds.top, bounds.right, bounds.bottom);
    }

    @Override
    public void setBounds(int left, int top, int right, int bottom)
    {
        final RectF sourceRect = new RectF(0, 0, target.getIntrinsicWidth(), target.getIntrinsicHeight());
        final RectF screenRect = new RectF(left, top, right, bottom);

        final Matrix matrix = new Matrix();
        matrix.setRectToRect(screenRect, sourceRect, Matrix.ScaleToFit.CENTER);

        final Matrix inverse = new Matrix();
        matrix.invert(inverse);
        inverse.mapRect(sourceRect);

        target.setBounds(Math.round(sourceRect.left), Math.round(sourceRect.top),
                         Math.round(sourceRect.right), Math.round(sourceRect.bottom));

        super.setBounds(left, top, right, bottom);
    }

    @SuppressLint("WrongConstant")
    @Override
    public void draw(@NonNull Canvas canvas)
    {
        canvas.save();
        canvas.clipRect(getBounds());
        target.draw(canvas);
        canvas.restore();
    }

    @Override
    public void setAlpha(@IntRange(from = 0, to = 255) int alpha)
    {
        target.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter)
    {
        target.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity()
    {
        return target.getOpacity();
    }
}
