package com.example.ucode;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;


public class SearchActivity extends AppCompatActivity {

    private static Authorization authorization;
    private static boolean newToken = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        MyUtility myUtility = new MyUtility(getResources(), this, this);

        try { authorization = myUtility.authorize(); }
        catch (Exception ignored) {}
        if (authorization == null)
            return;

        LinearLayout linearLayout = findViewById(R.id.search_results_layout);
        linearLayout.removeAllViews();

        Button back_button = findViewById(R.id.search_back_button);
        back_button.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });

        EditText editText = findViewById(R.id.search_edit_text);
        editText.addTextChangedListener(new TextListener(this));

        Button close_button = findViewById(R.id.search_close_button);
        close_button.setVisibility(View.GONE);
        close_button.setOnClickListener(v -> {
            close_button.setVisibility(View.GONE);
            editText.setText("");
        });
    }

    static class TextListener implements TextWatcher {
        private final WeakReference<Activity> mActivity;

        public TextListener (Activity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            Button close_button = mActivity.get().findViewById(R.id.search_close_button);
            if (s.toString().equals("")) {
                close_button.setVisibility(View.GONE);
                LinearLayout linearLayout = mActivity.get().findViewById(R.id.search_results_layout);
                linearLayout.removeAllViews();
                return;
            }
            close_button.setVisibility(View.VISIBLE);
            String searchUrl = "https://lms.ucode.world/api/v0/frontend/users/?username__contains=" + s.toString();
            final GetJson getJson = new GetJson(mActivity.get());
            getJson.execute(searchUrl, "authorization", authorization.getToken());
        }
    }

    static class GetJson extends AsyncTask<String, String, String> {
        private final WeakReference<Activity> mActivity;

        public GetJson(Activity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        protected String doInBackground(String... params) {
            String change_page = MyUtility.fetchData(params);
            if (change_page == null) {
                try {
                    authorization.generateAuthToken();
                } catch (Exception ignore) {
                }
                change_page = MyUtility.fetchData(params[0], params[1], authorization.getToken());
                if (change_page != null)
                    newToken = true;
            }
            return change_page;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            Activity activity = mActivity.get();
            if (activity == null || activity.isFinishing()) {
                return;
            }
            MyUtility myUtility = new MyUtility(activity.getResources(), activity, activity);

            if (result == null) {
                Toast.makeText(activity, "Can't get users data...", Toast.LENGTH_LONG).show();
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

            try {
                jsonData = new JSONObject(result);
                JSONArray usersJson = jsonData.getJSONArray("results");
                ArrayList<String[]> users = new ArrayList<>();
                for (int i = 0; i < usersJson.length() && i < 100; i++) {
                    JSONObject userJson = usersJson.getJSONObject(i);
                    String username = userJson.getString("username");
                    String first_name = userJson.getString("first_name");
                    String last_name = userJson.getString("last_name");
                    String photo_url = "https://lms.ucode.world/api/" + userJson.getString("photo_url");

                    String[] strings = new String[4];
                    strings[0] = username;
                    strings[1] = first_name;
                    strings[2] = last_name;
                    strings[3] = photo_url;
                    users.add(strings);
                }

                LinearLayout results_linear_layout = activity.findViewById(R.id.search_results_layout);
                results_linear_layout.removeAllViews();

                for (int i = 0; i < users.size(); i++) {
                    LinearLayout linearLayout = new LinearLayout(activity);
                    CardView cardView = new CardView(activity);
                    ImageView imageView = new ImageView(activity);
                    LinearLayout textLayout = new LinearLayout(activity);
                    TextView fullNameTextView = new TextView(activity);
                    TextView usernameTextView = new TextView(activity);
                    View gray_line = GrayLine.add(activity, 3);

                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    linearLayout.setLayoutParams(layoutParams);
                    linearLayout.setPadding(myUtility.dpToPx(10), myUtility.dpToPx(10), myUtility.dpToPx(10), myUtility.dpToPx(10));
                    linearLayout.setOrientation(LinearLayout.HORIZONTAL);
                    linearLayout.setTag((String)users.get(i)[0]);

                    CardView.LayoutParams cardViewParams = new CardView.LayoutParams(myUtility.dpToPx(50), myUtility.dpToPx(50));
                    cardView.setLayoutParams(cardViewParams);
                    cardView.setRadius(myUtility.dpToPx(25));
                    cardView.setForegroundGravity(Gravity.CENTER);

                    CardView.LayoutParams imageParams = new CardView.LayoutParams(
                            CardView.LayoutParams.MATCH_PARENT, CardView.LayoutParams.MATCH_PARENT);
                    imageView.setLayoutParams(imageParams);
                    if (((String)users.get(i)[3]).equals("https://lms.ucode.world/api/media/profile_photo/default.png")) {
                        imageView.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.default_avatar));
                    }
                    else {
                        imageView.setId(i);
                        GetImageBitmap getImageBitmap = new GetImageBitmap(activity);
                        getImageBitmap.execute((String) users.get(i)[3], Integer.toString(i));
                    }

                    LinearLayout.LayoutParams textLayoutParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
                    textLayout.setLayoutParams(textLayoutParams);
                    textLayout.setOrientation(LinearLayout.VERTICAL);
                    textLayout.setGravity(Gravity.CENTER_VERTICAL);

                    LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    textParams.leftMargin = myUtility.dpToPx(20);
                    fullNameTextView.setLayoutParams(textParams);
                    fullNameTextView.setTextSize(16);
                    fullNameTextView.setTypeface(null, Typeface.BOLD);
                    String full_name = (String)users.get(i)[1] + " " + (String)users.get(i)[2];
                    fullNameTextView.setText(full_name);

                    usernameTextView.setLayoutParams(textParams);
                    usernameTextView.setTextSize(16);
                    usernameTextView.setText((String)users.get(i)[0]);

                    cardView.addView(imageView);
                    textLayout.addView(fullNameTextView);
                    textLayout.addView(usernameTextView);
                    linearLayout.addView(cardView);
                    linearLayout.addView(textLayout);
                    results_linear_layout.addView(linearLayout);
                    results_linear_layout.addView(gray_line);

                    linearLayout.setOnClickListener(v -> {
                        String username = (String)v.getTag();
                        Toast.makeText(activity, username, Toast.LENGTH_SHORT).show();
                        String userUrl = "https://lms.ucode.world/api/v0/frontend/users/" + username + "/";

                        Intent intent = new Intent(activity, UserPageActivity.class);
                        Bundle bundle = new Bundle();
                        bundle.putString("userUrl", userUrl);
                        intent.putExtras(bundle);
                        activity.startActivity(intent);
                    });
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (newToken)
                MyUtility.saveToken(authorization);
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

    @Override
    public void onBackPressed() {
        finish();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
}