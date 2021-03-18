package com.example.ucode.ui.cluster;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.ucode.Authorization;
import com.example.ucode.ChallengePageActivity;
import com.example.ucode.Cluster;
import com.example.ucode.ClusterSelectDialog;
import com.example.ucode.MyChallengesData;
import com.example.ucode.MyUtility;
import com.example.ucode.R;
import com.example.ucode.ui.my_challenges.MyChallengesFragment;
import com.example.ucode.ui.statistics.StatisticsFragment;
import com.otaliastudios.zoom.ZoomLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.zip.Inflater;

public class ClusterFragment extends Fragment {
    static boolean newToken = false;
    static int cluster;

    private static Authorization authorization;

    public View onCreateView(@NonNull LayoutInflater inflater,  ViewGroup container, Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.fragment_cluster, container, false);
        MyUtility myUtility = new MyUtility(getResources(), getContext(), getActivity());

        try { authorization = myUtility.authorize(); }
        catch (Exception ignored) {}
        if (authorization == null)
            return root;

        final String request_url1 = "https://lms.ucode.world/api/v0/frontend/logtime/active/";
        final String request_url2 = "https://lms.ucode.world/api/v0/frontend/workplaces/workload/";
        final String request_url3 = "https://lms.ucode.world/api/v0/frontend/workplace_reports/active/";

        cluster = Cluster.C1;
        String[] cluster_names = new String[] {"c1", "e1", "e2", "e3"};
        Spinner cluster_name = root.findViewById(R.id.cluster_cluster_name_spinner);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(root.getContext(), android.R.layout.simple_spinner_item, cluster_names);
        cluster_name.setAdapter(arrayAdapter);
        cluster_name.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = (String) parent.getItemAtPosition(position);
                switch (selected) {
                    case "c1":
                        cluster = Cluster.C1;
                        break;
                    case "e1":
                        cluster = Cluster.E1;
                        break;
                    case "e2":
                        cluster = Cluster.E2;
                        break;
                    case "e3":
                        cluster = Cluster.E3;
                        break;
                }
                ClusterFragment.GetJson getJson = new ClusterFragment.GetJson(getActivity(), getContext(), root);
                getJson.execute(request_url1, request_url2, request_url3, "authorization", authorization.getToken());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        ConstraintLayout constraintLayout = root.findViewById(R.id.cluster_main_layout);
        constraintLayout.setVisibility(View.GONE);

        ClusterFragment.GetJson getJson = new ClusterFragment.GetJson(getActivity(), getContext(), root);
        getJson.execute(request_url1, request_url2, request_url3, "authorization", authorization.getToken());
        return root;
    }

    static class GetJson extends AsyncTask<String, String, String[]> {
        private final WeakReference<Activity> mActivity;
        private final WeakReference<Context> mContext;
        private final WeakReference<View> root;

        public GetJson(FragmentActivity activity, Context context, View view) {
            mActivity = new WeakReference<>(activity);
            mContext = new WeakReference<>(context);
            root = new WeakReference<>(view);
        }


        ProgressDialog pd;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            Activity activity = mActivity.get();
            if (activity == null || activity.isFinishing()) {
                return;
            }
            pd = new ProgressDialog(mActivity.get());
            pd.setMessage("Please wait");
            pd.setCancelable(false);
            pd.show();
        }

        @Override
        protected String[] doInBackground(String... params) {
            String[] params1 = new String[] {params[0], params[3], params[4]};
            String[] params2 = new String[] {params[1], params[3], params[4]};
            String[] params3 = new String[] {params[2], params[3], params[4]};
            String result1 = MyUtility.fetchData(params1);
            if (result1 == null) {
                try {authorization.generateAuthToken();}
                catch (Exception ignore) {}
                result1 = MyUtility.fetchData(params[0], params[1], authorization.getToken());
                if (result1 != null)
                    newToken = true;
            }
            String result2 = MyUtility.fetchData(params2);
            String result3 = MyUtility.fetchData(params3);
            return new String[] {result1, result2, result3};
        }

        @Override
        protected void onPostExecute(String[] results) {
            FragmentActivity activity = (FragmentActivity) mActivity.get();
            if (activity == null || activity.isFinishing()) {
                return;
            }
            if (pd.isShowing())
                pd.dismiss();

            if (results[0] == null) {
                Toast.makeText(mActivity.get(), "Can't get cluster data...", Toast.LENGTH_LONG).show();
                return;
            }

            JSONArray jsonDataActive = null;
            JSONArray jsonDataWorkload = null;
            JSONArray jsonDataReports = null;
            try {
                jsonDataActive = new JSONArray(results[0]/*"[{\"workplace\":\"c1r9p1\",\"user\":\"rbovkun\",\"photo\":\"media/profile_photo/rbovkun_small.png\"},{\"workplace\":\"c1r12p10\",\"user\":\"osavich\",\"photo\":\"media/profile_photo/osavich_small.png\"},{\"workplace\":\"c1r12p11\",\"user\":\"vsvietkov\",\"photo\":\"media/profile_photo/vsvietkov_small.png\"}]"*/);
                jsonDataWorkload = new JSONArray(results[1]);
                jsonDataReports = new JSONArray(results[2]);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (jsonDataActive == null || jsonDataWorkload == null || jsonDataReports == null) {
                Toast.makeText(mActivity.get(), "An error occurred, please try later...", Toast.LENGTH_LONG).show();
                return;
            }
            ConstraintLayout constraintLayout = mActivity.get().findViewById(R.id.cluster_main_layout);
            constraintLayout.setVisibility(View.VISIBLE);

            // Workload
            int[] workload = null;
            try {
                for (int i = 0; i < jsonDataWorkload.length() && workload == null; i++) {
                    JSONObject jsonObject = jsonDataWorkload.getJSONObject(i);
                    if (jsonObject.getInt("location") == Cluster.getLocation(cluster)) {
                        JSONArray jsonArray = jsonObject.getJSONArray("floors");
                        for (int j = 0; j < jsonArray.length() && workload == null; j++) {
                            JSONObject jsonObject1 = jsonArray.getJSONObject(j);
                            if (jsonObject1.getInt("floor") == Cluster.getFloor(cluster)) {
                                workload = new int[] {jsonObject1.getInt("total"),
                                        jsonObject1.getInt("free"),
                                        jsonObject1.getInt("busy")};
                            }
                        }
                    }
                }
            }
            catch (JSONException ignored) {}
            if (workload != null) {
                TextView totalTextView = activity.findViewById(R.id.cluster_total);
                TextView freeTextView = activity.findViewById(R.id.cluster_free);
                TextView busyTextView = activity.findViewById(R.id.cluster_busy);
                String totalStr = Integer.toString(workload[0]);
                totalTextView.setText(totalStr);
                String freeStr = Integer.toString(workload[1]);
                freeTextView.setText(freeStr);
                String busyStr = Integer.toString(workload[2]);
                busyTextView.setText(busyStr);
            }

            // Active
            @NonNull
            String[] clusterMap = Cluster.getMap(cluster);
            ArrayList<String[]> active_data = new ArrayList<>();
            ArrayList<Integer[]> active_pos = new ArrayList<>();
            try {
                for (int i = 0; i < jsonDataActive.length(); i++) {
                    JSONObject jsonObject = jsonDataActive.getJSONObject(i);
                    String workplace = jsonObject.getString("workplace");
                    String user = jsonObject.getString("user");
                    String photo_url = "https://lms.ucode.world/api/" + jsonObject.getString("photo");

                    int row = Integer.parseInt(workplace.substring(workplace.indexOf('r') + 1, workplace.indexOf('p')));
                    int place = Integer.parseInt(workplace.substring(workplace.indexOf('p') + 1));
                    int pos = 0;
                    for (int j = 0, c = 0; j < Cluster.MAX_WIDTH; j++) {
                        if (clusterMap[clusterMap.length - row].charAt(j) == Cluster.MAC || clusterMap[clusterMap.length - row].charAt(j) == Cluster.NON_MAC)
                            c++;
                        if (c == place) {
                            pos = j;
                            break;
                        }
                    }
                    String[] objects = new String[3];
                    objects[0] = workplace;
                    objects[1] = user;
                    objects[2] = photo_url;
                    active_data.add(objects);
                    active_pos.add(new Integer[]{row, pos});
                    Log.d("active_pos", row + " " + pos);
                }
            }
            catch (JSONException ignored) {}
            MyUtility myUtility = new MyUtility(activity.getResources(), activity, activity);

            TableLayout tableLayout = activity.findViewById(R.id.cluster_table);
            tableLayout.removeAllViews();
            for (int i = 0; i < clusterMap.length; i++) {
                int curr_row = clusterMap.length - i;
                TableLayout.LayoutParams tableRowParams = new TableLayout.LayoutParams(
                        TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT);
                tableRowParams.topMargin = myUtility.dpToPx(4);
                tableRowParams.bottomMargin = myUtility.dpToPx(4);
                TableRow tableRow = new TableRow(mContext.get());
                tableRow.setLayoutParams(tableRowParams);
                int real_pos = 1;
                for (int j = 0; j < Cluster.MAX_WIDTH; j++) {
                    TableRow.LayoutParams linearParams = new TableRow.LayoutParams(
                            TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT
                    );
                    LinearLayout linearLayout = new LinearLayout(mContext.get());
                    linearLayout.setLayoutParams(linearParams);
                    linearLayout.setOrientation(LinearLayout.VERTICAL);
                    linearLayout.setGravity(Gravity.CENTER);

                    LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(myUtility.dpToPx(30), myUtility.dpToPx(30));
                    imageParams.leftMargin = myUtility.dpToPx(5);
                    imageParams.rightMargin = myUtility.dpToPx(5);
                    ImageView imageView = new ImageView(mContext.get());
                    imageView.setLayoutParams(imageParams);

                    LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                    textParams.topMargin = -4;
                    TextView textView = new TextView(mContext.get());
                    textView.setLayoutParams(textParams);
                    textView.setTextSize(8);

                    boolean isActive = false;
                    for (int p = 0; p < active_pos.size(); p++)
                        if (active_pos.get(p)[0] == curr_row && active_pos.get(p)[1] == j) {
                            isActive = true;
                            int viewId = View.generateViewId();
                            imageView.setId(viewId);
                            ClusterFragment.GetImageBitmap getImageBitmap = new ClusterFragment.GetImageBitmap(activity);
                            getImageBitmap.execute(active_data.get(p)[2], Integer.toString(viewId));
                            textView.setText(active_data.get(p)[1]);
                            String workplace = "c1" + "r" + curr_row + "p" + real_pos;
                            real_pos++;
                            linearLayout.setTag(new String[]{workplace, active_data.get(p)[1]});
                            break;
                        }
                    if (!isActive && clusterMap[i].charAt(j) != Cluster.SPACE) {
                        if (clusterMap[i].charAt(j) == Cluster.MAC)
                            imageView.setImageDrawable(ContextCompat.getDrawable(mContext.get(), R.drawable.mac));
                        else if (clusterMap[i].charAt(j) == Cluster.NON_MAC)
                            imageView.setImageDrawable(ContextCompat.getDrawable(mContext.get(), R.drawable.cube));
                        String workplace = "c1" + "r" + curr_row + "p" + real_pos;
                        real_pos++;
                        textView.setText(workplace);
                        linearLayout.setTag(new String[]{workplace, null});
                    }
                    if (clusterMap[i].charAt(j) != Cluster.SPACE) {
                        linearLayout.setOnClickListener(v -> {
                            ClusterSelectDialog clusterSelectDialog = new ClusterSelectDialog();
                            Bundle bundle = new Bundle();
                            bundle.putStringArray("workplace", (String[]) linearLayout.getTag());
                            clusterSelectDialog.setArguments(bundle);
                            clusterSelectDialog.show(activity.getSupportFragmentManager(), "dialog");
                        });
                    }
                    linearLayout.addView(imageView);
                    linearLayout.addView(textView);
                    tableRow.addView(linearLayout);
                }
                tableLayout.addView(tableRow);
            }
            // Empty layout to make bottom margin ¯\_(ツ)_/¯
            TableLayout.LayoutParams layoutParams = new TableLayout.LayoutParams(
                    TableLayout.LayoutParams.MATCH_PARENT, myUtility.dpToPx(50));
            FrameLayout frameLayout = new FrameLayout(activity);
            frameLayout.setLayoutParams(layoutParams);
            tableLayout.addView(frameLayout);
        }
    }

    static class GetImageBitmap extends AsyncTask<String, String, Object[]> {
        private final WeakReference<Activity> mActivity;

        public GetImageBitmap(Activity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        protected Object[] doInBackground(String... strings) {
            HttpURLConnection connection = null;
            Bitmap bitmap;

            try {
                URL url = new URL(strings[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                InputStream stream = connection.getInputStream();
                bitmap = BitmapFactory.decodeStream(stream);
                Object[] objects = new Object[2];
                objects[0] = Integer.parseInt(strings[1]);
                objects[1] = bitmap;
                return objects;
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Object[] objects) {
            if (objects != null) {
                try {
                    ImageView imageView = mActivity.get().findViewById((int) objects[0]);
                    imageView.setImageBitmap((Bitmap) objects[1]);
                } catch (java.lang.NullPointerException ignore) {}
            }
        }
    }
}