package com.example.ucode.ui.home;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.animation.ObjectAnimator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.ucode.GrayLine;
import com.example.ucode.MainActivity;
import com.example.ucode.R;
import com.example.ucode.ShadowBottom;
import com.example.ucode.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

public class HomeFragment extends Fragment {

    private HomeViewModel homeViewModel;

    SwipeRefreshLayout swipeRefreshLayout;
    boolean cached = false;
    boolean refresh = false;

    public View onCreateView(@NonNull LayoutInflater inflater,  ViewGroup container, Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.fragment_home, container, false);

        final String authorization_key = "authorization";
        final String authorization_value = "Bearer AWSH2skNRwRVYZ6YRqK7sFgdkz6K60";
        final String request_url = "https://lms.ucode.world/api/v0/frontend/user/self/";

        final GetJson getJson = new GetJson(getActivity(), root);
        getJson.execute(request_url, authorization_key, authorization_value);

        // refresh
        swipeRefreshLayout = root.findViewById(R.id.profile_refresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshData(getActivity(), root, request_url, authorization_key, authorization_value);
            }
        });

        return root;
    }

    public void saveData(User user) {
        final File file = new File(getResources().getString(R.string.profile_cash_path));

        try (FileOutputStream fos = new FileOutputStream(file);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(user);
        }
        catch (Exception i) {
            Log.d("ERROR", String.valueOf(i));
        }
    }

    public User getData() {
        final File suspend_f = new File(getResources().getString(R.string.profile_cash_path));

        User user = null;

        try (FileInputStream fis = new FileInputStream(suspend_f); ObjectInputStream is = new ObjectInputStream(fis)) {
            user = (User) is.readObject();
        }
        catch (java.io.FileNotFoundException e) {
            return null;
        }
        catch (Exception i) {
            Log.d("ERROR", String.valueOf(i));
        }

        return user;
    }

    public void refreshData(Activity activity, View view, String... params) {
        refresh = true;
        GetJson getJson = new GetJson(activity, view);
        getJson.execute(params[0], params[1], params[2]);
    }

    public void authorize() {

    }

    public void onChallengeClick(View v) {
        Toast.makeText(getContext(), v.getTag().toString(), Toast.LENGTH_SHORT).show();
    }

    public void onActivityClick(View v) {
        Toast.makeText(getContext(), "Show activity", Toast.LENGTH_SHORT).show();
    }

    class GetJson extends AsyncTask<String, String, String> {
        private WeakReference<Activity> mActivity;
        private WeakReference<View> root;

        public GetJson (Activity activity, View view){
            mActivity = new WeakReference<>(activity);
            root = new WeakReference<>(view);
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
                user = getData();
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

            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                URL url = new URL(params[0]);
                connection = (HttpURLConnection) url.openConnection();
                for (int i = 1; i < (params.length + 1) / 2; i += 2)
                    connection.setRequestProperty(params[i], params[i + 1]);
                connection.connect();


                InputStream stream = connection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(stream));

                StringBuilder buffer = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null)
                    buffer.append(line).append("\n");

                return buffer.toString();


            }
            catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    if (reader != null) {
                        reader.close();
                    }
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            Activity activity = mActivity.get();
            if (activity == null || activity.isFinishing()) {
                return;
            }

            if (!cached) {
                if (!refresh) {
                    if (pd.isShowing())
                        pd.dismiss();
                }
                else {
                    if (swipeRefreshLayout.isRefreshing())
                        swipeRefreshLayout.setRefreshing(false);
                }

                /////////////////////////////
                if (result == null) {
                    Toast.makeText(mActivity.get(), "Can't get profile data...", Toast.LENGTH_LONG).show();
                    return;
                }

                JSONObject jsonData = null;
                try {
                    jsonData = new JSONObject(result);
                }
                catch (JSONException e) {
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
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }

                user.setProfileData(user_id, username, first_name, last_name, email,
                        location, adventure, level, "https://lms.ucode.world/api/media/profile_photo/default.png", phone,
                        tokens, lives, assessor_mark, toxic, n_mail, n_push, n_slack);

                saveData(user);
            }

            String[] challenges = new String[]{"Connect Sprint00", "Connect Race01", "Connect Sprint02"};
            String[] challenge_status = new String[]{"91", "assessment", "in progress"};

            String[] assessments = new String[]{};

            // progress bar
            int progress = (int)(user.LEVEL() % 1 * 100);
            ProgressBar progressBar = root.get().findViewById(R.id.progress_profile);
            ObjectAnimator animation1 = ObjectAnimator.ofInt (progressBar, "progress", 0, progress); // progress
            animation1.setDuration (1800); // in milliseconds
            animation1.setInterpolator (new DecelerateInterpolator ());
            animation1.start ();

            // progress text
            final TextView progress_text = root.get().findViewById(R.id.profile_progress_text);
            ValueAnimator animation2 = ValueAnimator.ofInt(0, progress * 3 / 4, progress);
            animation2.setDuration(2000);
            animation1.setInterpolator (new DecelerateInterpolator ());
            animation2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    String progress_value = animation.getAnimatedValue().toString() + " %";
                    progress_text.setText(progress_value);
                }
            });
            animation2.start();

            TextView level_text = root.get().findViewById(R.id.profile_level_text);
            level_text.setText(String.valueOf((int)user.LEVEL()));

            // full name
            TextView full_name_text = root.get().findViewById(R.id.profile_name);
            String full_name_value = user.FIRST_NAME() + " " + user.LAST_NAME();
            full_name_text.setText(full_name_value);

            // hearts & coins
            TextView hearts_text = root.get().findViewById(R.id.hearts);
            hearts_text.setText(String.valueOf(user.LIVES()));
            TextView coins_text =  root.get().findViewById(R.id.coins);
            coins_text.setText(String.valueOf(user.TOKENS()));

            // location
            TextView location_text = root.get().findViewById(R.id.profile_location);
            location_text.setText(user.LOCATION());

            // toxic_feedbacks
            TextView toxic_text = root.get().findViewById(R.id.profile_toxic);
            toxic_text.setText(String.valueOf(user.TOXIC()));

            // average check rate
            TextView rate_text = root.get().findViewById(R.id.profile_rate);
            rate_text.setText(String.valueOf(user.ASSESSOR_MARK()));


            // activity
            LinearLayout activity_btn = root.get().findViewById(R.id.profile_activity);
            activity_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onActivityClick(v);
                }
            });

            final String[] catNames = new String[] {
                    "Рыжик", "Барсик", "Мурзик", "Мурка", "Васька",
                    "Томасина", "Кристина", "Пушок", "Дымка", "Кузя",
                    "Китти", "Масяня", "Симба"
            };

            // skills

            LinearLayout linearLayout = root.get().findViewById(R.id.profile_skills);
            //ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(root.get(), listView, catNames);


            cached = false;
            refresh = false;
        }
    }
}