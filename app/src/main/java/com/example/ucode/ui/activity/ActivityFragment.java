/*package com.example.ucode.ui.activity;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.ucode.Authorization;
import com.example.ucode.CircularProgressBar;
import com.example.ucode.GrayLine;
import com.example.ucode.MyUtility;
import com.example.ucode.R;
import com.example.ucode.User;
import com.example.ucode.ui.home.HomeFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ActivityFragment extends Fragment {

    //private ActivityViewModel activityViewModel;
    private static Resources mResources;

    SwipeRefreshLayout swipeRefreshLayout;
    static boolean cached = false;
    static boolean refresh = false;
    static boolean newToken = false;

    private static Authorization authorization;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.fragment_activity, container, false);
        mResources = getResources();
        MyUtility myUtility = new MyUtility(getResources(), getContext(), getActivity());

        try { authorization = myUtility.authorize(); }
        catch (Exception ignored) {}
        if (authorization == null)
            return root;

        final String request_url = "https://lms.ucode.world/api/v0/frontend/activity_logs/";

        final ActivityFragment.GetJson getJson = new ActivityFragment.GetJson(getActivity(), getContext(), root);
        getJson.execute(request_url, "authorization", authorization.getToken());

        // refresh
        swipeRefreshLayout = root.findViewById(R.id.profile_refresh);
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
        private WeakReference<Activity> mActivity;
        private WeakReference<Context> mContext;
        private WeakReference<View> root;
        private MyUtility myUtility;

        public GetJson(Activity activity, Context context, View view) {
            mActivity = new WeakReference<>(activity);
            mContext = new WeakReference<>(context);
            root = new WeakReference<>(view);
            myUtility = new MyUtility(mResources, mContext.get(), mActivity.get());
        }


        ProgressDialog pd;
        User user = new User();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            Activity activity = mActivity.get();
            if (activity == null || activity.isFinishing()) {
                return;
            }

            if (!refresh) {
                user = (User) MyUtility.getData();
                if (user != null) {
                    cached = true;
                    return;
                }
                pd = new ProgressDialog(mActivity.get());
                pd.setMessage("Please wait");
                pd.setCancelable(false);
                pd.show();
            }
            user = new User();
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
            SwipeRefreshLayout swipeRefreshLayout1 = mActivity.get().findViewById(R.id.profile_refresh);

            if (!cached) {
                if (!refresh) {
                    if (pd.isShowing())
                        pd.dismiss();
                } else {
                    if (swipeRefreshLayout1.isRefreshing())
                        swipeRefreshLayout1.setRefreshing(false);
                }

                if (result == null) {
                    Toast.makeText(mActivity.get(), "Can't get profile data...", Toast.LENGTH_LONG).show();
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

                String username = null, first_name = null, last_name = null, email = null,
                        location = null, adventure = null, photoUrl = null, phone = null;
                int user_id = 0, tokens = 0, lives = 0, toxic = 0;
                double level = 0, assessor_mark = 0;
                boolean n_mail = false, n_push = false, n_slack = false;
                ArrayList<Object[]> skills_arr = null;
                try {
                    user_id = jsonData.getInt("id");
                    username = jsonData.getString("username");
                    first_name = jsonData.getString("first_name");
                    last_name = jsonData.getString("last_name");
                    email = jsonData.getString("email");

                    JSONArray location_temp = jsonData.getJSONArray("location_users");
                    location = ((JSONObject) location_temp.get(0)).getJSONObject("location").getString("name");

                    JSONArray adventure_temp = jsonData.getJSONArray("adventure_users");
                    adventure = ((JSONObject) adventure_temp.get(0)).getString("adventure_name");
                    level = ((JSONObject) adventure_temp.get(0)).getDouble("level");

                    photoUrl = jsonData.getString("photo_url");
                    phone = jsonData.getString("phone");
                    tokens = jsonData.getInt("tokens");
                    lives = jsonData.getInt("lives");
                    assessor_mark = jsonData.getDouble("assessor_mark");
                    toxic = jsonData.getInt("toxic_feedbacks");

                    JSONObject notifications = jsonData.getJSONObject("notifications_settings");
                    n_mail = notifications.getBoolean("mail");
                    n_push = notifications.getBoolean("push");
                    n_slack = notifications.getBoolean("slack");

                    JSONArray jsonArray = jsonData.getJSONArray("skill_users");
                    skills_arr = new ArrayList<>();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject skill = jsonArray.getJSONObject(i);
                        int progress = skill.getInt("experience");
                        JSONObject skill_values = skill.getJSONObject("skill");
                        int max_value = skill_values.getInt("max_value");
                        String skill_name = skill_values.getString("name");
                        Object[] arr = new Object[3];
                        arr[0] = skill_name;
                        arr[1] = progress;
                        arr[2] = max_value;
                        skills_arr.add(arr);
                    }
                    Comparator<Object[]> comparator = (Object[] a, Object[] b) -> ((Integer) b[1]).compareTo((int) a[1]);
                    Collections.sort(skills_arr, comparator);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                user.setProfileData(user_id, username, first_name, last_name, email,
                        location, adventure, level, photoUrl, phone,
                        tokens, lives, assessor_mark, toxic, n_mail, n_push, n_slack, skills_arr);

                MyUtility.saveData(user);
                if (newToken)
                    MyUtility.saveToken(authorization);
            }
        }
    }

}*/