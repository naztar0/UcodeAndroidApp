package com.example.ucode;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.appcompat.app.AlertDialog;

import org.jetbrains.annotations.NotNull;

public class ClusterSelectDialog extends AppCompatDialogFragment {
    @NotNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        if (getArguments() == null)
            return builder.create();
        String[] workplace = getArguments().getStringArray("workplace");
        if (workplace == null)
            return builder.create();
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.cluster_select_dialog, null);
        builder.setView(view);
        builder.setTitle(workplace[0]);
        builder.setNegativeButton("cancel", (dialogInterface, i) -> {});
        TextView profileTextView = view.findViewById(R.id.cluster_select_dialog_profile);
        TextView reportTextView = view.findViewById(R.id.cluster_select_dialog_report);
        if (workplace[1] != null) {
            String profile = "Open " + workplace[1] + " profile";
            profileTextView.setText(profile);
        }
        else {
            View line = view.findViewById(R.id.cluster_select_dialog_line);
            line.setVisibility(View.GONE);
            profileTextView.setVisibility(View.GONE);
        }
        reportTextView.setText(R.string.report_workplace);

        profileTextView.setOnClickListener(v -> {
            Toast.makeText(getActivity(), workplace[1], Toast.LENGTH_SHORT).show();
            String userUrl = "https://lms.ucode.world/api/v0/frontend/users/" + workplace[1] + "/";

            Intent intent = new Intent(getActivity(), UserPageActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString("userUrl", userUrl);
            intent.putExtras(bundle);
            getActivity().startActivity(intent);
        });
        reportTextView.setOnClickListener(v -> {

        });
        return builder.create();
    }
}
