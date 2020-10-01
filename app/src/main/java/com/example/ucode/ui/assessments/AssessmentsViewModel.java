package com.example.ucode.ui.assessments;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class AssessmentsViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public AssessmentsViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is slideshow fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}