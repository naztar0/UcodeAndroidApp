package com.example.ucode;

import java.io.Serializable;
import java.util.ArrayList;

public class AssessmentsData implements Serializable {
    private ArrayList<Object[]> arrayList;

    public void setAssessmentsData(ArrayList<Object[]> arrayList) {
        this.arrayList = arrayList;
    }

    public ArrayList<Object[]> getArrayList() {
        return this.arrayList;
    }
}
