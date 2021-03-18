package com.example.ucode;

import java.io.Serializable;
import java.util.ArrayList;

public class StatisticsData implements Serializable {
    private ArrayList<Object[]> arrayList;
    private boolean nextPage;

    public void setStatisticsData(ArrayList<Object[]> arrayList, boolean nextPage) {
        this.arrayList = arrayList;
        this.nextPage = nextPage;
    }

    public ArrayList<Object[]> getArrayList() {
        return this.arrayList;
    }
    public boolean getNextPage() {
        return this.nextPage;
    }
}
