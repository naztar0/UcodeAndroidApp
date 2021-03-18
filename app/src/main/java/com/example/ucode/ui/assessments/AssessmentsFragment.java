package com.example.ucode.ui.assessments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.icu.text.SimpleDateFormat;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.ucode.AssessmentsData;
import com.example.ucode.Authorization;
import com.example.ucode.ChallengePageActivity;
import com.example.ucode.MyChallengesData;
import com.example.ucode.MyUtility;
import com.example.ucode.R;
import com.example.ucode.ReflectionActivity;
import com.example.ucode.UserPageActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

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

        final String request_url = "https://lms.ucode.world/api/v0/frontend/slots/scheduled/";

        final GetJson getJson = new GetJson(getActivity(), getContext(), root);
        getJson.execute(request_url, "authorization", authorization.getToken());

        // refresh
        swipeRefreshLayout = root.findViewById(R.id.assessments_refresh);
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
        AssessmentsData assessmentsData = new AssessmentsData();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            Activity activity = mActivity.get();
            if (activity == null || activity.isFinishing()) {
                return;
            }

            if (!refresh) {
                // TODO: I decided to remove caching... But not completely yet, if I'll like that idea in future than all implementation of caching will be removed

                /*assessmentsData = (AssessmentsData) MyUtility.getData(R.string.assessments_cache_path);
                if (assessmentsData != null) {
                    cached = true;
                    return;
                }*/
                pd = new ProgressDialog(activity);
                pd.setMessage("Please wait");
                pd.setCancelable(false);
                pd.show();
            }
            assessmentsData = new AssessmentsData();
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
            SwipeRefreshLayout swipeRefreshLayout1 = activity.findViewById(R.id.assessments_refresh);
            MyUtility myUtility = new MyUtility(activity.getResources(), activity, activity);

            if (!cached) {
                if (!refresh) {
                    if (pd.isShowing())
                        pd.dismiss();
                } else {
                    if (swipeRefreshLayout1.isRefreshing())
                        swipeRefreshLayout1.setRefreshing(false);
                    // reload activity after refresh
                    new RecreateActivity(activity, 100);
                }

                if (result == null) {
                    Toast.makeText(activity, "Can't get assessments data...", Toast.LENGTH_LONG).show();
                    return;
                }

                JSONObject jsonData = null;
                try {
                    jsonData = new JSONObject(result/*"{\"as_assessor\":[{\"id\":526650,\"begin_at\":\"2020-10-20T12:30:00+03:00\",\"end_at\":\"2020-10-20T12:45:00+03:00\",\"user\":null,\"assessment_team\":null,\"is_subscribable\":false,\"grouped_slots\":526650,\"can_cancel\":true,\"is_allowed\":false,\"challenge\":{\"id\":138,\"title\":\"Connect uls\",\"description\":\"The challenge to refresh all the knowledge you have gained at  Marathon C.\",\"slug\":\"connect-refresh-marathon-c\",\"experience\":500,\"type\":\"challenge\",\"estimated_time\":72,\"game\":5}},{\"id\":536650,\"begin_at\":\"2020-10-20T12:30:00+03:00\",\"end_at\":\"2020-10-20T12:45:00+03:00\",\"user\":null,\"assessment_team\":null,\"is_subscribable\":false,\"grouped_slots\":536650,\"can_cancel\":true,\"is_allowed\":false,\"challenge\":{\"id\":138,\"title\":\"Connect uls and bla bla bla la la la Lorem ipsum dolor si amet\",\"description\":\"The challenge to refresh all the knowledge you have gained at  Marathon C.\",\"slug\":\"connect-refresh-marathon-c\",\"experience\":500,\"type\":\"challenge\",\"estimated_time\":72,\"game\":5}}],\"as_assessed\":[{\"id\":524260,\"begin_at\":\"2020-10-20T11:30:00+03:00\",\"end_at\":\"2020-10-20T11:45:00+03:00\",\"user\":{\"id\":1187,\"username\":\"vkharchenk\",\"photo\":\"media/profile_photo/vkharchenk.png\",\"email\":\"vkharchenk@stud.khpi.ucode-connect.study\"},\"assessment_team\":57337,\"is_subscribable\":false,\"grouped_slots\":524260,\"can_cancel\":false,\"is_allowed\":true,\"challenge\":{\"id\":138,\"title\":\"Connect Refresh Marathon C\",\"description\":\"The challenge to refresh all the knowledge you have gained at  Marathon C.\",\"slug\":\"connect-refresh-marathon-c\",\"experience\":500,\"type\":\"challenge\",\"estimated_time\":72,\"game\":5}}]}"*/);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (jsonData == null) {
                    Toast.makeText(activity, "An error occurred, please try later...", Toast.LENGTH_LONG).show();
                    return;
                }

                ArrayList<Object[]> assessments_arr = null;
                try {
                    JSONArray jsonArray1 = jsonData.getJSONArray("as_assessor");
                    JSONArray jsonArray2 = jsonData.getJSONArray("as_assessed");

                    if (jsonArray1.length() != 0 || jsonArray2.length() != 0) {
                        assessments_arr = new ArrayList<>();
                        JSONArray curr_arr = jsonArray1;
                        for (int i = 0; i < curr_arr.length(); i++) {
                            JSONObject assessment = curr_arr.getJSONObject(i);
                            int id = assessment.getInt("id");
                            String timeBegin = assessment.getString("begin_at");
                            String[] user = null;
                            if (!assessment.isNull("user")) {
                                JSONObject userJson = assessment.getJSONObject("user");
                                String username = userJson.getString("username");
                                String photoUrl = "https://lms.ucode.world/api/" + userJson.getString("photo");
                                user = new String[]{username, photoUrl};
                            }
                            boolean can_cancel = assessment.getBoolean("can_cancel");
                            boolean is_allowed = assessment.getBoolean("is_allowed");
                            JSONObject challenge = assessment.getJSONObject("challenge");
                            String title = challenge.getString("title");
                            int assessment_team = 0;
                            if (!assessment.isNull("assessment_team"))
                                assessment_team = assessment.getInt("assessment_team");

                            String[] dateTime = MyUtility.parseDateTime(timeBegin);
                            timeBegin = dateTime[1] + " " + dateTime[0];

                            Object[] arr = new Object[8];
                            arr[0] = curr_arr == jsonArray1;  // is_assessor?
                            arr[1] = id;
                            arr[2] = user;
                            arr[3] = title;
                            arr[4] = timeBegin;
                            arr[5] = can_cancel;
                            arr[6] = is_allowed;
                            arr[7] = assessment_team;
                            assessments_arr.add(arr);

                            if (i == curr_arr.length() - 1 && curr_arr == jsonArray1) {
                                curr_arr = jsonArray2;
                                i = -1;
                            }
                        }
                    }
                } catch (JSONException | ParseException e) {
                    e.printStackTrace();
                }
                assessmentsData.setAssessmentsData(assessments_arr);
                // MyUtility.saveData(assessmentsData, R.string.assessments_cache_path);
                if (newToken)
                    MyUtility.saveToken(authorization);
            }

            ArrayList<Object[]> data_arr = assessmentsData.getArrayList();

            if (data_arr == null) {
                LinearLayout empty_layout = new LinearLayout(activity);
                TextView textView = new TextView(activity);
                ImageView imageView = new ImageView(activity);

                empty_layout.setLayoutParams(new ConstraintLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT));
                empty_layout.setOrientation(LinearLayout.VERTICAL);
                empty_layout.setGravity(Gravity.CENTER);

                LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                textParams.bottomMargin = myUtility.dpToPx(100);
                textView.setLayoutParams(textParams);
                textView.setTextSize(24);
                textView.setTextColor(ContextCompat.getColor(activity, R.color.profile_black_text_80));
                textView.setText(activity.getString(R.string.no_assessments));

                imageView.setLayoutParams(new LinearLayout.LayoutParams(myUtility.dpToPx(200), myUtility.dpToPx(200)));
                imageView.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_baseline_event_busy_24));

                empty_layout.addView(imageView);
                empty_layout.addView(textView);
                swipeRefreshLayout1.addView(empty_layout);
            }
            else {
                ScrollView scrollView = new ScrollView(activity);
                LinearLayout assessments_layout = new LinearLayout(activity);
                LinearLayout rowLayout = null;

                scrollView.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

                assessments_layout.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                assessments_layout.setOrientation(LinearLayout.VERTICAL);

                scrollView.addView(assessments_layout);
                swipeRefreshLayout1.addView(scrollView);
                for (int i = 0; i < data_arr.size(); i++) {
                    if (i % 2 == 0) {
                        rowLayout = new LinearLayout(activity);
                        rowLayout.setLayoutParams(new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                        rowLayout.setOrientation(LinearLayout.HORIZONTAL);
                        rowLayout.setBaselineAligned(false);
                    }
                    LinearLayout barLayout = new LinearLayout(activity);
                    CardView cardView = new CardView(activity);
                    ImageView imageView = new ImageView(activity);
                    TextView usernameTextView = new TextView(activity);
                    CardView cancelCard = new CardView(activity);
                    ImageView cancelImage = new ImageView(activity);
                    TextView titleTextView = new TextView(activity);
                    TextView timeTextView = new TextView(activity);
                    Button button = new Button(activity);
                    View emptyView = null;

                    LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT);
                    barParams.leftMargin = myUtility.dpToPx(i % 2 == 0 ? 6 : 3);
                    barParams.rightMargin = myUtility.dpToPx(i % 2 == 0 ? 3 : 6);
                    barParams.topMargin = myUtility.dpToPx(i < 2 ? 6 : 3);
                    barParams.bottomMargin = myUtility.dpToPx(3);
                    barParams.weight = 1;
                    barLayout.setLayoutParams(barParams);
                    barLayout.setPadding(myUtility.dpToPx(10), myUtility.dpToPx(10), myUtility.dpToPx(10), myUtility.dpToPx(10));
                    barLayout.setOrientation(LinearLayout.VERTICAL);
                    barLayout.setClickable(true);
                    barLayout.setFocusable(true);
                    int layout_bg = (boolean) data_arr.get(i)[0] ? R.drawable.ripple_assessment_blue : R.drawable.ripple_assessment_green;
                    barLayout.setBackground(ContextCompat.getDrawable(activity, layout_bg));

                    CardView.LayoutParams cardParams = new CardView.LayoutParams(
                            CardView.LayoutParams.MATCH_PARENT, myUtility.dpToPx(170));
                    cardView.setLayoutParams(cardParams);
                    cardView.setRadius(myUtility.dpToPx(10));
                    cardView.setClickable(true);
                    cardView.setFocusable(true);

                    CardView.LayoutParams imageParams = new CardView.LayoutParams(
                            CardView.LayoutParams.MATCH_PARENT, CardView.LayoutParams.MATCH_PARENT);
                    imageView.setLayoutParams(imageParams);
                    imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    if (data_arr.get(i)[2] != null) {
                        int imageId = View.generateViewId();
                        imageView.setId(imageId);
                        GetImageBitmap getImageBitmap = new GetImageBitmap(activity);
                        getImageBitmap.execute(((String[]) data_arr.get(i)[2])[1], Integer.toString(imageId));
                        int finalI = i;
                        imageView.setOnClickListener(v -> {
                            String userUrl = "https://lms.ucode.world/api/v0/frontend/users/" + ((String[]) data_arr.get(finalI)[2])[0] + "/";
                            Intent intent = new Intent(activity, UserPageActivity.class);
                            Bundle bundle = new Bundle();
                            bundle.putString("userUrl", userUrl);
                            intent.putExtras(bundle);
                            activity.startActivity(intent);
                        });
                    } else
                        imageView.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.default_avatar));

                    CardView.LayoutParams usernameParams = new CardView.LayoutParams(
                            CardView.LayoutParams.MATCH_PARENT, CardView.LayoutParams.WRAP_CONTENT);
                    usernameParams.topMargin = myUtility.dpToPx(140);
                    usernameTextView.setLayoutParams(usernameParams);
                    usernameTextView.setPadding(myUtility.dpToPx(10), 0, myUtility.dpToPx(10), 0);
                    usernameTextView.setTextSize(20);
                    usernameTextView.setTextColor(ContextCompat.getColor(activity, R.color.profile_white_text));
                    usernameTextView.setTypeface(Typeface.DEFAULT_BOLD);
                    usernameTextView.setShadowLayer(20, 0, 0, R.color.black);
                    if (data_arr.get(i)[2] != null)
                        usernameTextView.setText(((String[]) data_arr.get(i)[2])[0]);
                    else
                        usernameTextView.setText(activity.getString(R.string.someone));

                    CardView.LayoutParams cancelParams = new CardView.LayoutParams(
                            myUtility.dpToPx(30), myUtility.dpToPx(30));
                    cancelParams.gravity = Gravity.END;
                    cancelParams.rightMargin = myUtility.dpToPx(5);
                    cancelParams.topMargin = myUtility.dpToPx(5);
                    cancelCard.setLayoutParams(cancelParams);
                    cancelCard.setRadius(myUtility.dpToPx(15));
                    cancelCard.setClickable(true);
                    cancelCard.setFocusable(true);
                    if (!(boolean) data_arr.get(i)[5])
                        cancelCard.setVisibility(View.GONE);
                    else {
                        int finalI = i;
                        cancelCard.setOnClickListener(v -> {
                            String cancelUrl = "https://lms.ucode.world/api/v0/frontend/slots/" + data_arr.get(finalI)[1] + "/cancel-assessment/";
                            JSONObject jsonObject = new JSONObject();  // without payload, just for test, it may be doesn't work
                            Object[] objects = new Object[2];
                            objects[0] = jsonObject;
                            objects[1] = new String[]{cancelUrl, "authorization", authorization.getToken()};
                            CancelAssessment cancelAssessment = new CancelAssessment(activity);
                            cancelAssessment.execute(objects);
                            new RecreateActivity(activity, 100);
                        });
                    }

                    cancelImage.setLayoutParams(new CardView.LayoutParams(
                            CardView.LayoutParams.MATCH_PARENT, CardView.LayoutParams.MATCH_PARENT));
                    cancelImage.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ripple_close));

                    titleTextView.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    titleTextView.setPadding(0, myUtility.dpToPx(5), 0, 0);
                    titleTextView.setTextSize(17);
                    titleTextView.setTextColor(ContextCompat.getColor(activity, R.color.profile_black_text_80));
                    titleTextView.setText(myUtility.trimEllipsis((String) data_arr.get(i)[3], 17));
                    titleTextView.setTag("short");

                    timeTextView.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    timeTextView.setTextSize(13);
                    timeTextView.setText((String) data_arr.get(i)[4]);

                    LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, myUtility.dpToPx(30));
                    buttonParams.topMargin = myUtility.dpToPx(5);
                    button.setLayoutParams(buttonParams);
                    button.setPadding(0, 0, 0, 0);
                    button.setBackground(ContextCompat.getDrawable(activity, R.drawable.ripple_dark_bg));
                    button.setTextColor(ContextCompat.getColor(activity, R.color.profile_white_text));
                    button.setTextSize(14);
                    boolean isAssessor = (boolean) data_arr.get(i)[0];
                    button.setText(isAssessor ? "Begin" : "Allow");
                    if ((!(boolean) data_arr.get(i)[6] && isAssessor) || ((boolean) data_arr.get(i)[6] && !isAssessor))
                        button.setEnabled(false);
                    int finalI = i;
                    button.setOnClickListener(v -> {
                        if (isAssessor) {
                            String assessmentTeamUrl = "https://lms.ucode.world/api/v0/frontend/assessment_teams/" + data_arr.get(finalI)[7] + "/";
                            Intent intent = new Intent(activity, ReflectionActivity.class);
                            Bundle bundle = new Bundle();
                            bundle.putString("reflectionUrl", assessmentTeamUrl);
                            intent.putExtras(bundle);
                            activity.startActivity(intent);
                        } else {
                            String allowAssessmentUrl = "https://lms.ucode.world/api/v0/frontend/assessment_teams/" + data_arr.get(finalI)[7] + "/allow/";
                            AllowAssessment allowAssessment = new AllowAssessment(activity);
                            allowAssessment.execute(allowAssessmentUrl, "authorization", authorization.getToken());
                            activity.startActivity(activity.getIntent());
                        }
                    });

                    if (i % 2 == 0 && i == data_arr.size() - 1) {
                        emptyView = new View(activity);
                        LinearLayout.LayoutParams viewParams = new LinearLayout.LayoutParams(0, 0);
                        viewParams.weight = 1;
                        viewParams.rightMargin = myUtility.dpToPx(9);
                        emptyView.setLayoutParams(viewParams);
                    }

                    barLayout.setOnClickListener(v -> {
                        if (titleTextView.getTag() == "short") {
                            titleTextView.setTag("long");
                            titleTextView.setText((String) data_arr.get(finalI)[3]);
                        }
                        else {
                            titleTextView.setTag("short");
                            titleTextView.setText(myUtility.trimEllipsis((String) data_arr.get(finalI)[3], 17));
                        }
                    });


                    cardView.addView(imageView);
                    cardView.addView(usernameTextView);
                    cancelCard.addView(cancelImage);
                    cardView.addView(cancelCard);
                    barLayout.addView(cardView);
                    barLayout.addView(titleTextView);
                    barLayout.addView(timeTextView);
                    barLayout.addView(button);
                    rowLayout.addView(barLayout);
                    if (emptyView != null)
                        rowLayout.addView(emptyView);
                    if (i % 2 == 0)
                        assessments_layout.addView(rowLayout);
                    Log.d("HEIGHT", String.valueOf(barLayout.getHeight()));
                }
            }

            cached = false;
            refresh = false;
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
                }
                catch (java.lang.NullPointerException ignore) {}
            }
        }
    }

    static class CancelAssessment extends AsyncTask<Object, String, String> {
        WeakReference<Activity> mActivity;
        ProgressDialog pd;

        CancelAssessment (Activity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        protected void onPreExecute() {
            pd = new ProgressDialog(mActivity.get());
            pd.setMessage("Please wait");
            pd.setCancelable(false);
            pd.show();
        }

        @Override
        protected String doInBackground(Object... objects) {
            return MyUtility.fetchPostData("PUT", true, (JSONObject)objects[0], (String[])objects[1]);
        }

        @Override
        protected void onPostExecute(String result) {
            if (pd.isShowing())
                pd.dismiss();
            Log.d("RESULT", String.valueOf(result));
        }
    }

    static class AllowAssessment extends AsyncTask<String, String, String> {
        WeakReference<Activity> mActivity;
        ProgressDialog pd;

        AllowAssessment (Activity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        protected void onPreExecute() {
            pd = new ProgressDialog(mActivity.get());
            pd.setMessage("Please wait");
            pd.setCancelable(false);
            pd.show();
        }

        @Override
        protected String doInBackground(String... strings) {
            return MyUtility.fetchPostData("PATCH", true, null, strings);
        }

        @Override
        protected void onPostExecute(String result) {
            if (pd.isShowing())
                pd.dismiss();
            Toast.makeText(mActivity.get(), String.valueOf(result), Toast.LENGTH_SHORT).show();
            Log.d("RESULT", String.valueOf(result));
        }
    }

    static class RecreateActivity extends AsyncTask<Integer, Void, Void> {
        WeakReference<Activity> mActivity;

        RecreateActivity (Activity activity, int sleep) {
            mActivity = new WeakReference<>(activity);
            this.execute(sleep);
        }

        @Override
        protected Void doInBackground(Integer... ints) {
            SystemClock.sleep(ints[0]);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            mActivity.get().recreate();
        }
    }
}