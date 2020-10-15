package com.example.ucode;

import java.io.Serializable;
import java.util.ArrayList;

public class ActivityData implements Serializable {
    private ArrayList<Object[]> arrayList;

    public void setActivityData(ArrayList<Object[]> arrayList) {
        this.arrayList = arrayList;
    }

    public ArrayList<Object[]> getArrayList() {
        return this.arrayList;
    }
}
