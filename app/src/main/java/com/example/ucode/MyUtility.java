package com.example.ucode;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.TypedValue;

import androidx.core.content.ContextCompat;

public class MyUtility {
    private Resources mResources;
    private Context mContext;

    public MyUtility(Resources resources, Context context) {
        this.mResources = resources;
        this.mContext = context;
    }

    public float dpToPx(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, mResources.getDisplayMetrics());
    }
    public int parseColor(int color) {
        return Color.parseColor("#" + Integer.toHexString (ContextCompat.getColor(mContext, color)));
    }
}
