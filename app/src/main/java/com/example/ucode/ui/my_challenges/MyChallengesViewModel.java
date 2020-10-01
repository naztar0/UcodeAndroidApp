package com.example.ucode.ui.my_challenges;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MyChallengesViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public MyChallengesViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is gallery fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}