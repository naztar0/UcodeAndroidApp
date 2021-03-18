package com.example.ucode.ui.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.ucode.ActivityData;
import com.example.ucode.Authorization;
import com.example.ucode.MyUtility;
import com.example.ucode.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class ActivityFragment extends Fragment {

    SwipeRefreshLayout swipeRefreshLayout;
    static boolean cached = false;
    static boolean refresh = false;
    static boolean newToken = false;

    private static Authorization authorization;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.fragment_activity, container, false);
        //private ActivityViewModel activityViewModel;
        MyUtility myUtility = new MyUtility(getResources(), getContext(), getActivity());

        try { authorization = myUtility.authorize(); }
        catch (Exception ignored) {}
        if (authorization == null)
            return root;

        final String request_url = "https://lms.ucode.world/api/v0/frontend/activity_logs/";

        final ActivityFragment.GetJson getJson = new ActivityFragment.GetJson(getActivity(), getContext(), root);
        getJson.execute(request_url, "authorization", authorization.getToken());

        // refresh
        swipeRefreshLayout = root.findViewById(R.id.activity_refresh);
        swipeRefreshLayout.setOnRefreshListener(() ->
                refreshData(getActivity(), getContext(), root, request_url, "authorization", authorization.getToken()));

        return root;
    }

    private void refreshData(Activity activity, Context context, View view, String... params) {
        refresh = true;
        ActivityFragment.GetJson getJson = new ActivityFragment.GetJson(activity, context, view);
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
        ActivityData activityData = new ActivityData();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            Activity activity = mActivity.get();
            if (activity == null || activity.isFinishing()) {
                return;
            }

            if (!refresh) {
                activityData = (ActivityData) MyUtility.getData(R.string.activity_cache_path);
                if (activityData != null) {
                    cached = true;
                    return;
                }
                pd = new ProgressDialog(mActivity.get());
                pd.setMessage("Please wait");
                pd.setCancelable(false);
                pd.show();
            }
            activityData = new ActivityData();
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
            SwipeRefreshLayout swipeRefreshLayout1 = activity.findViewById(R.id.activity_refresh);

            if (!cached) {
                if (!refresh) {
                    if (pd.isShowing())
                        pd.dismiss();
                } else {
                    if (swipeRefreshLayout1.isRefreshing())
                        swipeRefreshLayout1.setRefreshing(false);
                }

                if (result == null) {
                    Toast.makeText(activity, "Can't get activity data...", Toast.LENGTH_LONG).show();
                    return;
                }

                JSONObject jsonData = null;
                try {
                    jsonData = new JSONObject(result);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (jsonData == null) {
                    Toast.makeText(activity, "An error occurred, please try later...", Toast.LENGTH_LONG).show();
                    return;
                }

                ArrayList<Object[]> activities_arr = null;
                try {
                    JSONArray jsonArray = jsonData.getJSONArray("results");

                    activities_arr = new ArrayList<>();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject res = jsonArray.getJSONObject(i);
                        String created = res.getString("created_at");
                        String message = res.getString("message");
                        String type = res.getString("type");

                        String[] dateTime = MyUtility.parseDateTime(created);
                        String created_date = dateTime[0];
                        String created_time = dateTime[1];
                        message = message.substring(message.indexOf(" ") + 1);

                        String[] arr = new String[4];
                        arr[0] = message;
                        arr[1] = type;
                        arr[2] = created_date;
                        arr[3] = created_time;
                        activities_arr.add(arr);
                    }
                } catch (JSONException | ParseException e) {
                    e.printStackTrace();
                }

                activityData.setActivityData(activities_arr);

                MyUtility.saveData(activityData, R.string.activity_cache_path);
                if (newToken)
                    MyUtility.saveToken(authorization);
            }

            // displaying list
            ArrayList<String> messages = new ArrayList<>();
            ArrayList<String> emoji_arr = new ArrayList<>();
            ArrayList<String> dates = new ArrayList<>();

            ArrayList<Object[]> data_arr = activityData.getArrayList();
            for (int i = 0; i < data_arr.size(); i++) {
                messages.add((String)data_arr.get(i)[0]);
                dates.add(data_arr.get(i)[3] + " " + data_arr.get(i)[2]);
                switch ((String) data_arr.get(i)[1]) {
                    case "validate_challenge":
                        emoji_arr.add("\uD83C\uDF89");
                        break;
                    case "level_up":
                        emoji_arr.add("\uD83D\uDD1D");
                        break;
                    case "defense_assessment":
                        emoji_arr.add("\uD83D\uDCE5");
                        break;
                    case "fail_challenge":
                        emoji_arr.add("❌");
                        break;
                    case "accept_challenge":
                        emoji_arr.add("\uD83D\uDCAA");
                        break;
                    case "made_assessment":
                        emoji_arr.add("\uD83D\uDCE4");
                        break;
                    case "give_up_challenge":
                        emoji_arr.add("\uD83D\uDE14");
                        break;
                    default:
                        emoji_arr.add(" ");
                        break;
                }
            }

            List<HashMap<String,String>> hashMaps = new ArrayList<>();
            for (int i = 0; i < 20; i++){
                HashMap<String, String> hm = new HashMap<>();
                hm.put("emoji", emoji_arr.get(i));
                hm.put("name", messages.get(i));
                hm.put("date", dates.get(i));
                hashMaps.add(hm);
            }

            String[] from = {"emoji","name","date"};
            int[] to = { R.id.activity_emoji,R.id.activity_message,R.id.activity_datetime};

            ListView listView = root.get().findViewById(R.id.activity_list_view);
            SimpleAdapter simpleAdapter = new SimpleAdapter(activity, hashMaps, R.layout.activity_item_layout, from, to);
            listView.setAdapter(simpleAdapter);


            cached = false;
            refresh = false;
        }
    }

}