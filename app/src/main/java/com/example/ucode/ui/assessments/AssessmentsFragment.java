package com.example.ucode.ui.assessments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.ucode.Authorization;
import com.example.ucode.ChallengePageActivity;
import com.example.ucode.MyChallengesData;
import com.example.ucode.MyUtility;
import com.example.ucode.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AssessmentsFragment extends Fragment {

    SwipeRefreshLayout swipeRefreshLayout;
    static boolean cached = false;
    static boolean refresh = false;
    static boolean newToken = false;

    private static Authorization authorization;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.fragment_assessment, container, false);
        MyUtility myUtility = new MyUtility(getResources(), getContext(), getActivity());

        try { authorization = myUtility.authorize(); }
        catch (Exception ignored) {}
        if (authorization == null)
            return root;

        final String request_url = "https://lms.ucode.world/api/v0/frontend/user/self/";

        final GetJson getJson = new GetJson(getActivity(), getContext(), root);
        getJson.execute(request_url, "authorization", authorization.getToken());

        // refresh
        swipeRefreshLayout = root.findViewById(R.id.my_challenges_refresh);
        swipeRefreshLayout.setOnRefreshListener(() ->
                refreshData(getActivity(), getContext(), root, request_url, "authorization", authorization.getToken()));

        return root;
    }

    private void refreshData(Activity activity, Context context, View view, String... params) {
        refresh = true;
        GetJson getJson = new GetJson(activity, context, view);
        getJson.execute(params[0], params[1], params[2]);
    }

    static class GetJson extends AsyncTask<String, String, String> {
        private final WeakReference<Activity> mActivity;
        private final WeakReference<Context> mContext;
        private final WeakReference<View> root;

        public GetJson(Activity activity, Context context, View view) {
            mActivity = new WeakReference<>(activity);
            mContext = new WeakReference<>(context);
            root = new WeakReference<>(view);
        }


        ProgressDialog pd;
        MyChallengesData myChallengesData = new MyChallengesData();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            Activity activity = mActivity.get();
            if (activity == null || activity.isFinishing()) {
                return;
            }

            if (!refresh) {
                myChallengesData = (MyChallengesData) MyUtility.getData(R.string.my_challenges_cache_path);
                if (myChallengesData != null) {
                    cached = true;
                    return;
                }
                pd = new ProgressDialog(mActivity.get());
                pd.setMessage("Please wait");
                pd.setCancelable(false);
                pd.show();
            }
            myChallengesData = new MyChallengesData();
        }

        @Override
        protected String doInBackground(String... params) {
            if (cached)
                return "";

            String result = MyUtility.fetchData(params);
            if (result == null) {
                try {authorization.generateAuthToken();}
                catch (Exception ignore) {}
                result = MyUtility.fetchData(params[0], params[1], authorization.getToken());
                if (result != null)
                    newToken = true;
            }
            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            Activity activity = mActivity.get();
            if (activity == null || activity.isFinishing()) {
                return;
            }
            SwipeRefreshLayout swipeRefreshLayout1 = mActivity.get().findViewById(R.id.my_challenges_refresh);

            if (!cached) {
                if (!refresh) {
                    if (pd.isShowing())
                        pd.dismiss();
                } else {
                    if (swipeRefreshLayout1.isRefreshing())
                        swipeRefreshLayout1.setRefreshing(false);
                }

                if (result == null) {
                    Toast.makeText(mActivity.get(), "Can't get challenges data...", Toast.LENGTH_LONG).show();
                    return;
                }

                JSONObject jsonData = null;
                try {
                    jsonData = new JSONObject(result);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (jsonData == null) {
                    Toast.makeText(mActivity.get(), "An error occurred, please try later...", Toast.LENGTH_LONG).show();
                    return;
                }

                ArrayList<Object[]> challenges_arr = null;
                try {
                    JSONArray jsonArray = jsonData.getJSONArray("challenge_users");

                    challenges_arr = new ArrayList<>();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject res = jsonArray.getJSONObject(i);
                        JSONObject challenge = res.getJSONObject("challenge");
                        int id = res.getInt("id");
                        int mark = 0;
                        if (!res.isNull("mark"))
                            mark = res.getInt("mark");
                        String status = res.getString("status");
                        String title = challenge.getString("title");


                        Object[] arr = new Object[4];
                        arr[0] = id;
                        arr[1] = title;
                        arr[2] = mark;
                        arr[3] = status;
                        challenges_arr.add(arr);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                myChallengesData.setMyChallengesData(challenges_arr);

                MyUtility.saveData(myChallengesData, R.string.my_challenges_cache_path);
                if (newToken)
                    MyUtility.saveToken(authorization);
            }

            // displaying list
            ArrayList<Integer> ids = new ArrayList<>();
            ArrayList<String> titles = new ArrayList<>();
            ArrayList<String> statuses = new ArrayList<>();

            ArrayList<Object[]> data_arr = myChallengesData.getArrayList();
            for (int i = 0; i < data_arr.size(); i++) {
                ids.add((int)data_arr.get(i)[0]);
                titles.add((String)data_arr.get(i)[1]);
                if (data_arr.get(i)[3].equals("finished"))
                    statuses.add(String.valueOf((int)data_arr.get(i)[2]));
                else
                    statuses.add(((String)data_arr.get(i)[3]).replace("_", " "));
            }

            List<HashMap<String,String>> hashMaps = new ArrayList<>();
            for (int i = 0; i < data_arr.size(); i++) {
                HashMap<String, String> hm = new HashMap<>();
                hm.put("title", titles.get(i));
                hm.put("status", statuses.get(i));
                hm.put("id", String.valueOf(ids.get(i)));
                hashMaps.add(hm);
            }

            String[] from = {"title","status","id"};
            int[] to = { R.id.my_challenges_title,R.id.my_challenges_status,R.id.my_challenges_id};

            ListView listView = root.get().findViewById(R.id.my_challenges_list_view);
            SimpleAdapter simpleAdapter = new SimpleAdapter(mContext.get(), hashMaps, R.layout.my_challenge_item_layout, from, to);
            listView.setAdapter(simpleAdapter);

            listView.setTextFilterEnabled(true);

            listView.setOnItemClickListener((parent, view, position, id) -> {
                LinearLayout linearLayout = (LinearLayout) view;
                TextView textView = linearLayout.findViewById(R.id.my_challenges_id);
                String challengeUrl = "https://lms.ucode.world/api/v0/frontend/challenge_users/" + textView.getText() + "/";

                Intent intent = new Intent(mContext.get(), ChallengePageActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("challengeUrl", challengeUrl);
                intent.putExtras(bundle);
                mContext.get().startActivity(intent);
            });


            cached = false;
            refresh = false;
        }
    }

}