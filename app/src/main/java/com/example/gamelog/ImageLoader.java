package com.example.gamelog;

import android.content.res.ColorStateList;
import android.widget.ImageView;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import com.bumptech.glide.Glide;

public final class ImageLoader {

    private ImageLoader() {
    }

    public static void loadCover(ImageView imageView, @Nullable String coverImageUrl, @DrawableRes int fallbackResId) {
        imageView.setImageTintList((ColorStateList) null);
        imageView.setBackground(null);
        imageView.setPadding(0, 0, 0, 0);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

        String normalizedUrl = coverImageUrl == null ? "" : coverImageUrl.trim();
        if (normalizedUrl.isEmpty()) {
            Glide.with(imageView).clear(imageView);
            imageView.setImageResource(fallbackResId);
            return;
        }

        Glide.with(imageView)
                .load(normalizedUrl)
                .placeholder(fallbackResId)
                .error(fallbackResId)
                .fallback(fallbackResId)
                .into(imageView);
    }
}
