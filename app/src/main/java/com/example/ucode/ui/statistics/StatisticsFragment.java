package com.example.ucode.ui.statistics;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.ucode.Authorization;
import com.example.ucode.GrayLine;
import com.example.ucode.MyUtility;
import com.example.ucode.R;
import com.example.ucode.StatisticsData;
import com.example.ucode.UserPageActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class StatisticsFragment extends Fragment {

    private static Resources mResources;
    SwipeRefreshLayout swipeRefreshLayout;
    static boolean cached = false;
    static boolean refresh = false;
    static boolean reload = false;
    static boolean load_up = false;
    static boolean newToken = false;

    private static StatisticRequestUrl statisticRequestUrl;
    private static int usersCount;
    private static boolean nextPage;

    private static Authorization authorization;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_statistics, container, false);
        MyUtility myUtility = new MyUtility(getResources(), getContext(), getActivity());
        mResources = getResources();

        try {
            authorization = myUtility.authorize();
        } catch (Exception ignored) {}
        if (authorization == null)
            return root;

        ConstraintLayout constraintLayout = root.findViewById(R.id.statistics_main_layout);
        constraintLayout.setVisibility(View.GONE);

        final String request_url = "https://lms.ucode.world/api/v0/frontend/statistics/users/";
        statisticRequestUrl = new StatisticRequestUrl(request_url);
        usersCount = 0;
        nextPage = false;

        final StatisticsFragment.GetJson getJson = new StatisticsFragment.GetJson(getActivity(), getContext(), root);
        getJson.execute(request_url, "authorization", authorization.getToken());

        // refresh
        swipeRefreshLayout = root.findViewById(R.id.statistics_refresh);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            usersCount = 0;
            StatisticRequestUrl.firstPage();
            refreshData(getActivity(), getContext(), root, statisticRequestUrl.getRequestUrl(), "authorization", authorization.getToken());
        });

        // loading next pages
        ScrollView scrollView = root.findViewById(R.id.statistics_scroll_view);
        scrollView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            double scrollViewHeight = scrollView.getChildAt(0).getBottom() - scrollView.getHeight();
            double scrollPosition = (scrollY / scrollViewHeight) * 100d;
            if (scrollPosition > 95 && !load_up && nextPage) {
                load_up = true;
                refreshData(getActivity(), getContext(), root, statisticRequestUrl.getRequestUrl(), "authorization", authorization.getToken());
            }
        });

        // sort by level
        LinearLayout sort_by_level = root.findViewById(R.id.statistics_sort_by_level);
        sort_by_level.setOnClickListener(v -> {
            statisticRequestUrl.setOrdering();
            ImageView imageView = root.findViewById(R.id.statistics_sort_by_level_arrow);
            imageView.setImageDrawable(ContextCompat.getDrawable(root.getContext(), statisticRequestUrl.getOrdering() ?
                    R.drawable.ic_baseline_arrow_drop_up_24 : R.drawable.ic_baseline_arrow_drop_down_24));
            reload = true;
            usersCount = 0;
            StatisticRequestUrl.firstPage();
            refreshData(getActivity(), getContext(), root, statisticRequestUrl.getRequestUrl(), "authorization", authorization.getToken());
        });
        return root;
    }

    private void refreshData(Activity activity, Context context, View view, String... params) {
        refresh = true;
        StatisticsFragment.GetJson getJson = new StatisticsFragment.GetJson(activity, context, view);
        getJson.execute(params[0], params[1], params[2]);
    }

    static class GetJson extends AsyncTask<String, String, String> {
        private final WeakReference<Activity> mActivity;
        private final WeakReference<Context> mContext;
        private final WeakReference<View> root;
        private final MyUtility myUtility;

        public GetJson(Activity activity, Context context, View view) {
            mActivity = new WeakReference<>(activity);
            mContext = new WeakReference<>(context);
            root = new WeakReference<>(view);
            myUtility = new MyUtility(mResources, mContext.get(), mActivity.get());
        }


        ProgressDialog pd;
        StatisticsData statisticsData = new StatisticsData();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            Activity activity = mActivity.get();
            if (activity == null || activity.isFinishing()) {
                return;
            }

            if (!refresh || reload) {
                if (!reload) {
                    statisticsData = (StatisticsData) MyUtility.getData(R.string.statistics_cache_path);
                    if (statisticsData != null) {
                        cached = true;
                        return;
                    }
                }
                pd = new ProgressDialog(mActivity.get());
                pd.setMessage("Please wait");
                pd.setCancelable(false);
                pd.show();
            }
            statisticsData = new StatisticsData();
        }

        @Override
        protected String doInBackground(String... params) {
            if (cached)
                return "";

            String result = MyUtility.fetchData(params);
            if (result == null) {
                try {
                    authorization.generateAuthToken();
                } catch (Exception ignore) {
                }
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
            ConstraintLayout constraintLayout = mActivity.get().findViewById(R.id.statistics_main_layout);
            constraintLayout.setVisibility(View.VISIBLE);
            SwipeRefreshLayout swipeRefreshLayout1 = mActivity.get().findViewById(R.id.statistics_refresh);

            if (!cached) {
                if (!refresh || reload) {
                    if (pd.isShowing())
                        pd.dismiss();
                } else {
                    if (swipeRefreshLayout1.isRefreshing())
                        swipeRefreshLayout1.setRefreshing(false);
                }

                if (result == null) {
                    Toast.makeText(mActivity.get(), "Can't get statistics data...", Toast.LENGTH_LONG).show();
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

                ArrayList<Object[]> users_arr = null;
                try {
                    JSONArray jsonArray = jsonData.getJSONArray("results");
                    nextPage = !jsonData.isNull("next");

                    users_arr = new ArrayList<>();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject res = jsonArray.getJSONObject(i);
                        int id = res.getInt("id");
                        String username = res.getString("username");
                        String photo_url = "https://lms.ucode.world/api/" + res.getString("photo_url");
                        double level = res.getDouble("level");
                        String spent_time = res.getString("spent_time");

                        Object[] arr = new Object[5];
                        arr[0] = id;
                        arr[1] = username;
                        arr[2] = photo_url;
                        arr[3] = level;
                        arr[4] = spent_time;
                        users_arr.add(arr);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                statisticsData.setStatisticsData(users_arr, nextPage);

                MyUtility.saveData(statisticsData, R.string.statistics_cache_path);
                if (newToken)
                    MyUtility.saveToken(authorization);
            }

            nextPage = statisticsData.getNextPage();
//            Log.d("123", load_up+" "+refresh+" "+reload);
            if (nextPage && (load_up || (!reload)))  // TODO: not working correctly
                StatisticRequestUrl.nextPage();
            ArrayList<Object[]> data_arr = statisticsData.getArrayList();

            LinearLayout results_linear_layout = activity.findViewById(R.id.statistics_list_view);
            if ((usersCount == 0 || refresh || reload) && !load_up)
                results_linear_layout.removeAllViews();

            for (int i = 0; i < data_arr.size(); i++) {
                usersCount++;
                LinearLayout linearLayout = new LinearLayout(activity);
//                TextView numTextView = new TextView(activity);
                CardView cardView = new CardView(activity);
                ImageView imageView = new ImageView(activity);
                LinearLayout textLayout = new LinearLayout(activity);
                TextView usernameTextView = new TextView(activity);
                TextView levelTextView = new TextView(activity);
                TextView timeTextView = new TextView(activity);
                View gray_line = GrayLine.add(activity, 3);

                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                linearLayout.setLayoutParams(layoutParams);
                linearLayout.setPadding(myUtility.dpToPx(10), myUtility.dpToPx(10), myUtility.dpToPx(10), myUtility.dpToPx(10));
                linearLayout.setOrientation(LinearLayout.HORIZONTAL);
                linearLayout.setGravity(Gravity.CENTER_VERTICAL);
                linearLayout.setTag(data_arr.get(i)[1]);

//                LinearLayout.LayoutParams numTextParams = new LinearLayout.LayoutParams(
//                        myUtility.dpToPx(35), ViewGroup.LayoutParams.WRAP_CONTENT);
//                numTextParams.rightMargin = myUtility.dpToPx(10);
//                numTextView.setLayoutParams(numTextParams);
//                numTextView.setTextSize(16);
//                numTextView.setTypeface(null, Typeface.BOLD);
//                String num = (usersCount) + ".";
//                numTextView.setText(num);

                CardView.LayoutParams cardViewParams = new CardView.LayoutParams(myUtility.dpToPx(50), myUtility.dpToPx(50));
                cardView.setLayoutParams(cardViewParams);
                cardView.setRadius(myUtility.dpToPx(25));
                cardView.setForegroundGravity(Gravity.CENTER);

                CardView.LayoutParams imageParams = new CardView.LayoutParams(
                        CardView.LayoutParams.MATCH_PARENT, CardView.LayoutParams.MATCH_PARENT);
                imageView.setLayoutParams(imageParams);
                if ((data_arr.get(i)[2]).equals("https://lms.ucode.world/api/media/profile_photo/default.png")) {
                    imageView.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.default_avatar));
                } else {
                    imageView.setId(usersCount);
                    StatisticsFragment.GetImageBitmap getImageBitmap = new StatisticsFragment.GetImageBitmap(activity);
                    getImageBitmap.execute((String) data_arr.get(i)[2], Integer.toString(usersCount));
                }

                LinearLayout.LayoutParams textLayoutParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                textLayoutParams.leftMargin = myUtility.dpToPx(10);
                textLayout.setLayoutParams(textLayoutParams);
                textLayout.setOrientation(LinearLayout.HORIZONTAL);
                textLayout.setGravity(Gravity.CENTER_VERTICAL);

                LinearLayout.LayoutParams usernameParams = new LinearLayout.LayoutParams(
                        myUtility.dpToPx(120), LinearLayout.LayoutParams.WRAP_CONTENT);
                usernameTextView.setLayoutParams(usernameParams);
                usernameTextView.setTextSize(16);
                usernameTextView.setTypeface(null, Typeface.BOLD);
                usernameTextView.setText((String) data_arr.get(i)[1]);

                LinearLayout.LayoutParams levelParams = new LinearLayout.LayoutParams(
                        myUtility.dpToPx(80), LinearLayout.LayoutParams.WRAP_CONTENT);
                levelTextView.setLayoutParams(levelParams);
                levelTextView.setTextSize(16);
                String level = Double.toString((Double) data_arr.get(i)[3]);
                levelTextView.setText(level);

                LinearLayout.LayoutParams timeParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                timeTextView.setLayoutParams(timeParams);
                timeTextView.setTextSize(16);
                timeTextView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                String[] time_split = ((String) data_arr.get(i)[4]).split(" ");
                String time = time_split.length == 2 ? time_split[0] + " days\n" +
                        time_split[1].substring(0, 8) : time_split[0].substring(0, 8);
                timeTextView.setText(time);

                cardView.addView(imageView);
                textLayout.addView(usernameTextView);
                textLayout.addView(levelTextView);
                textLayout.addView(timeTextView);
//                linearLayout.addView(numTextView);
                linearLayout.addView(cardView);
                linearLayout.addView(textLayout);
                results_linear_layout.addView(linearLayout);
                results_linear_layout.addView(gray_line);

                linearLayout.setOnClickListener(v -> {
                    String username = (String) v.getTag();
                    Toast.makeText(activity, username, Toast.LENGTH_SHORT).show();
                    String userUrl = "https://lms.ucode.world/api/v0/frontend/users/" + username + "/";

                    Intent intent = new Intent(activity, UserPageActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putString("userUrl", userUrl);
                    intent.putExtras(bundle);
                    activity.startActivity(intent);
                });
            }

            cached = false;
            refresh = false;
            reload = false;
            load_up = false;
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

    private static class StatisticRequestUrl {
        private static String mRequestUrl;
        private static int page = 1;
        private static boolean ordering = false;
        private static boolean game = false;
        private static boolean enrollment = false;
        private static boolean level_from = false;
        private static boolean level_to = false;
        private static boolean username = false;
        private static boolean ordering_down;
        private static int game_num;
        private static int enrollment_num;
        private static int level_from_num;
        private static int level_to_num;
        private static String username_str;

        StatisticRequestUrl(String requestUrl) {
            mRequestUrl = requestUrl;
        }
        protected void setOrdering() {
            if (!ordering) {
                ordering = true;
                ordering_down = true;
            }
            else
                ordering_down = !ordering_down;
        }
        protected static void firstPage() {
            page = 1;
        }
        protected static void nextPage() {
            page++;
        }
        protected void setGame(int game_num) {
            StatisticRequestUrl.game_num = game_num;
            game = true;
        }
        protected void setEnrollment(int enrollment_num) {
            StatisticRequestUrl.enrollment_num = enrollment_num;
            enrollment = true;
        }
        protected void setLevel_from(int level_num) {
            StatisticRequestUrl.level_from_num = level_num;
            level_from = true;
        }
        protected void setLevel_to(int level_num) {
            StatisticRequestUrl.level_to_num = level_num;
            level_to = true;
        }
        protected void setUsername(String username_str) {
            StatisticRequestUrl.username_str = username_str;
            username = true;
        }
        protected boolean getOrdering() {
            return ordering_down;
        }
        protected String getRequestUrl() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(mRequestUrl);
            stringBuilder.append("?page=").append(page);
            if (ordering) {
                stringBuilder.append("&ordering=");
                stringBuilder.append(ordering_down ? "level" : "-level");
            }
            if (game) {
                stringBuilder.append("&game=");
                stringBuilder.append(game_num);
            }
            if (enrollment) {
                stringBuilder.append("&enrollment=");
                stringBuilder.append(enrollment_num);
            }
            if (level_from) {
                stringBuilder.append("&level__gte=");
                stringBuilder.append(level_from_num);
            }
            if (level_to) {
                stringBuilder.append("&level__lte=");
                stringBuilder.append(level_to_num);
            }
            if (username) {
                stringBuilder.append("&username__contains=");
                stringBuilder.append(username_str);
            }
            return stringBuilder.toString();
        }
    }
}