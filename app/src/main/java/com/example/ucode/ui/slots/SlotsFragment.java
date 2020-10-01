package com.example.ucode.ui.slots;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.example.ucode.R;

public class SlotsFragment extends Fragment {

    private SlotsViewModel slotsViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        slotsViewModel =
                ViewModelProviders.of(this).get(SlotsViewModel.class);
        View root = inflater.inflate(R.layout.fragment_activity, container, false);
        final TextView textView = root.findViewById(R.id.text_gallery);
        slotsViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                textView.setText(s);
            }
        });
        return root;
    }
}