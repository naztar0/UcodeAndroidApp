package com.example.ucode;

import android.content.Context;
import android.graphics.Color;
import android.view.View;

public class GrayLine {
    public static View add(Context c){
        View view = new View(c);
        view.setMinimumHeight(3);
        view.setBackgroundColor(Color.argb(30,0,0,0));
        return view;
    }
}
