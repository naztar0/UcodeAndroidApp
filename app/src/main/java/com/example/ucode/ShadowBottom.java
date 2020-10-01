package com.example.ucode;
import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.LinearLayout;

public class ShadowBottom {
    public static LinearLayout add(Context c, int intensity) {
        View view1 = new View(c);
        view1.setBackgroundColor(Color.parseColor("#E6555555"));
        View view2 = new View(c);
        view2.setBackgroundColor(Color.parseColor("#D9666666"));
        View view3 = new View(c);
        view3.setBackgroundColor(Color.parseColor("#B3777777"));
        View view4 = new View(c);
        view4.setBackgroundColor(Color.parseColor("#A6888888"));
        View view5 = new View(c);
        view5.setBackgroundColor(Color.parseColor("#99999999"));
        View view6 = new View(c);
        view6.setBackgroundColor(Color.parseColor("#8CAAAAAA"));
        View view7 = new View(c);
        view7.setBackgroundColor(Color.parseColor("#80BBBBBB"));
        View view8 = new View(c);
        view8.setBackgroundColor(Color.parseColor("#73CCCCCC"));
        View view9 = new View(c);
        view9.setBackgroundColor(Color.parseColor("#66DDDDDD"));
        View view10 = new View(c);
        view10.setBackgroundColor(Color.parseColor("#59EEEEEE"));

        view1.setMinimumHeight((int)(c.getResources().getDisplayMetrics().scaledDensity * 1));
        view2.setMinimumHeight((int)(c.getResources().getDisplayMetrics().scaledDensity * 1));
        view3.setMinimumHeight((int)(c.getResources().getDisplayMetrics().scaledDensity * 1));
        view4.setMinimumHeight((int)(c.getResources().getDisplayMetrics().scaledDensity * 1));
        view5.setMinimumHeight((int)(c.getResources().getDisplayMetrics().scaledDensity * 1));
        view6.setMinimumHeight((int)(c.getResources().getDisplayMetrics().scaledDensity * 1));
        view7.setMinimumHeight((int)(c.getResources().getDisplayMetrics().scaledDensity * 1));
        view8.setMinimumHeight((int)(c.getResources().getDisplayMetrics().scaledDensity * 1));
        view9.setMinimumHeight((int)(c.getResources().getDisplayMetrics().scaledDensity * 1));
        view10.setMinimumHeight((int)(c.getResources().getDisplayMetrics().scaledDensity * 1));

        View[] view_list = new View[]{view10, view9, view8, view7, view6, view5, view4, view3, view2, view1};

        LinearLayout linearLayout = new LinearLayout(c);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < intensity; i++)
            linearLayout.addView(view_list[i]);

        return linearLayout;

    }
}
