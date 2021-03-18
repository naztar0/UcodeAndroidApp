package com.example.ucode;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import java.util.Comparator;

public class UserPageActivity extends AppCompatActivity {

    private static Authorization authorization;
    private static boolean newToken = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_page);
        MyUtility myUtility = new MyUtility(getResources(), this, this);

        try {
            authorization = myUtility.authorize();
        } catch (Exception ignored) {
        }
        if (authorization == null)
            return;

        Bundle bundle = getIntent().getExtras();
        if (bundle == null)
            return;
        String userUrl = bundle.getString("userUrl");

        ConstraintLayout mainView = this.findViewById(R.id.user_page_main_layout);
        mainView.setVisibility(View.GONE);

        GetJson getJson = new GetJson(this);
        getJson.execute(userUrl, "authorization", authorization.getToken());
    }

    static class GetJson extends AsyncTask<String, String, String> {
        private final WeakReference<Activity> mActivity;

        public GetJson(Activity activity) {
            mActivity = new WeakReference<>(activity);
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

            pd = new ProgressDialog(activity);
            pd.setMessage("Please wait");
            pd.setCancelable(false);
            pd.show();
        }

        @Override
        protected String doInBackground(String... params) {
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

            MyUtility myUtility = new MyUtility(activity.getResources(), activity, activity);
            if (pd.isShowing())
                pd.dismiss();

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
                    location = null, adventure = null, photoUrl = null, phone = null,
                    github_link = null, facebook_link = null, instagram_link = null,
                    linkedin_link = null, workplace = null;
            int user_id = 0, tokens = 0, lives = 0, toxic = 0;
            double level = 0, assessor_mark = 0;
            boolean socials = false;
            ArrayList<Object[]> skills_arr = null;
            try {
                user_id = jsonData.getInt("id");
                username = jsonData.getString("username");
                first_name = jsonData.getString("first_name");
                last_name = jsonData.getString("last_name");
                email = jsonData.getString("email");

                JSONArray location_temp = jsonData.getJSONArray("location_users");
                int last_location = location_temp.length() - 1;
                location = ((JSONObject) location_temp.get(last_location)).getJSONObject("location").getString("name");

                JSONArray adventure_temp = jsonData.getJSONArray("adventure_users");
                int last_adv = adventure_temp.length() - 1;
                adventure = ((JSONObject) adventure_temp.get(last_adv)).getString("adventure_name");
                try {
                    level = ((JSONObject) adventure_temp.get(last_adv)).getDouble("level");
                } catch (org.json.JSONException e) {
                    level = jsonData.getDouble("level");
                }

                photoUrl = jsonData.getString("photo_url");
                if (!jsonData.isNull("phone"))
                    phone = jsonData.getString("phone");
                if (!jsonData.isNull("socials")) {
                    socials = true;
                    JSONObject socialsJson = jsonData.getJSONObject("socials");
                    github_link = socialsJson.getString("github");
                    facebook_link = socialsJson.getString("facebook");
                    instagram_link = socialsJson.getString("instagram");
                    linkedin_link = socialsJson.getString("linkedin");
                }
                if (!jsonData.isNull("current_workplace"))
                    workplace = jsonData.getString("current_workplace");

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
                Comparator<Object[]> comparator = (Object[] a, Object[] b) ->
                        Integer.compare(((int) b[1] * 100 / (int) b[2]), (int) a[1] * 100 / (int) a[2]);
                skills_arr.sort(comparator);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            user.setProfileData(user_id, username, first_name, last_name, email,
                    location, adventure, level, photoUrl, phone, workplace,
                    tokens, lives, assessor_mark, toxic, false, false, false, skills_arr);

            if (newToken)
                MyUtility.saveToken(authorization);

            // set visibility
            ConstraintLayout mainLayout = activity.findViewById(R.id.user_page_main_layout);
            mainLayout.setVisibility(View.VISIBLE);

            // profile image
            final UserPageActivity.GetImageBitmap getImageBitmap = new UserPageActivity.GetImageBitmap(activity);
            getImageBitmap.execute(user.PHOTO_URL());

            // progress bar
            int progress = (int) (user.LEVEL() % 1 * 100);

            CircularProgressBar circularProgressBar = activity.findViewById(R.id.circular_progress);
            circularProgressBar.setAnimationDuration(1900);
            circularProgressBar.setProgressWidth(myUtility.dpToPx(6));
            circularProgressBar.setProgress(progress);
            circularProgressBar.setProgressColor(myUtility.parseColor(R.color.progressFront));

            // progress text
            final TextView progress_text = activity.findViewById(R.id.profile_progress_text);
            ValueAnimator animation2 = ValueAnimator.ofInt(0, progress * 3 / 4, progress);
            animation2.setDuration(2000);
            animation2.setInterpolator(new DecelerateInterpolator());
            animation2.addUpdateListener(animation -> {
                String progress_value = animation.getAnimatedValue().toString() + "%";
                progress_text.setText(progress_value);
            });
            animation2.start();

            // level
            TextView level_text = activity.findViewById(R.id.profile_level_text);
            level_text.setText(String.valueOf((int) user.LEVEL()));
            if (user.LEVEL() >= 1000)
                level_text.setTextSize(20);
            else if (user.LEVEL() >= 100)
                level_text.setTextSize(25);

            // full name
            TextView full_name_text = activity.findViewById(R.id.profile_name);
            String full_name_value = user.FIRST_NAME() + " " + user.LAST_NAME();
            full_name_text.setText(full_name_value);

            // location
            TextView location_text = activity.findViewById(R.id.profile_location);
            location_text.setText(user.LOCATION());

            // slack
            String slackUrl = "https://lms.ucode.world/api/v0/frontend/users/" + user.ID() + "/slack/";
            GetSlack getSlack = new GetSlack(activity);
            getSlack.execute(slackUrl, "authorization", authorization.getToken());

            // socials
            if (socials) {
                ImageView github_icon = activity.findViewById(R.id.icon_github);
                ImageView facebook_icon = activity.findViewById(R.id.icon_facebook);
                ImageView instagram_icon = activity.findViewById(R.id.icon_instagram);
                ImageView linkedin_icon = activity.findViewById(R.id.icon_linkedin);
                assert github_link != null;
                if (!github_link.equals("")) {
                    String finalGithub_link = github_link;
                    github_icon.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.icon_github));
                    github_icon.setOnClickListener(v -> {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(finalGithub_link));
                        activity.startActivity(browserIntent);
                    });
                }
                assert facebook_link != null;
                if (!facebook_link.equals("")) {
                    String finalFacebook_link = facebook_link;
                    facebook_icon.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.icon_facebook));
                    facebook_icon.setOnClickListener(v -> {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(finalFacebook_link));
                        activity.startActivity(browserIntent);
                    });
                }
                assert instagram_link != null;
                if (!instagram_link.equals("")) {
                    String finalInstagram_link = instagram_link;
                    instagram_icon.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.icon_instagram));
                    instagram_icon.setOnClickListener(v -> {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(finalInstagram_link));
                        activity.startActivity(browserIntent);
                    });
                }
                assert linkedin_link != null;
                if (!linkedin_link.equals("")) {
                    String finalLinkedin_link = linkedin_link;
                    linkedin_icon.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.icon_linkedin));
                    linkedin_icon.setOnClickListener(v -> {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(finalLinkedin_link));
                        activity.startActivity(browserIntent);
                    });
                }
            }

            // workplace
            LinearLayout workplace_layout = activity.findViewById(R.id.profile_workplace_layout);
            if (user.WORKPLACE() != null) {
                TextView workplace_text = activity.findViewById(R.id.profile_workplace);
                workplace_text.setText(user.WORKPLACE());
            }
            else
                workplace_layout.setVisibility(View.GONE);

            // phone
            LinearLayout phone_layout = activity.findViewById(R.id.profile_phone_layout);
            if (phone != null) {
                TextView phone_text = activity.findViewById(R.id.profile_phone);
                phone_text.setText(phone);
                String finalPhone = phone;
                phone_layout.setOnClickListener(n -> {
                    Intent intent = new Intent(Intent.ACTION_CALL);
                    intent.setData(Uri.parse("tel:" + finalPhone));
                    if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.CALL_PHONE}, 0);
                        return;
                    }
                    activity.startActivity(intent);
                });
            }
            else
                phone_layout.setVisibility(View.GONE);

            // skills
            if (user.SKILLS() != null) {
                LinearLayout linearLayoutSkills = activity.findViewById(R.id.profile_skills);
                linearLayoutSkills.removeAllViews();
                View gray_line1 = GrayLine.add(activity, 3);
                linearLayoutSkills.addView(gray_line1);
                for(int i = 0; i < user.SKILLS().size(); i++) {
                    View gray_line2 = GrayLine.add(activity, 3);

                    LinearLayout skillLayout = new LinearLayout(activity);
                    skillLayout.setOrientation(LinearLayout.HORIZONTAL);
                    skillLayout.setPadding(myUtility.dpToPx(10), 20, 20, 5);
                    skillLayout.setGravity(Gravity.CENTER_VERTICAL);
                    LinearLayout.LayoutParams skillLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    skillLayout.setLayoutParams(skillLayoutParams);

                    CircularProgressBar skillProgressBar = new CircularProgressBar(activity);
                    skillProgressBar.setLayoutParams(new LinearLayout.LayoutParams(myUtility.dpToPx(60), myUtility.dpToPx(60)));
                    skillProgressBar.setAnimationDuration(2000);
                    skillProgressBar.showProgressText(true);
                    skillProgressBar.showSubstrate(true);
                    skillProgressBar.setSubstrateColor(myUtility.parseColor(R.color.light_grey));
                    skillProgressBar.setProgressColor(myUtility.parseColor(R.color.skill_progress));
                    skillProgressBar.setTextColor(myUtility.parseColor(R.color.colorPrimaryDark));
                    skillProgressBar.setProgressWidth(myUtility.dpToPx(5));
                    skillProgressBar.setProgress((int)(user.SKILLS().get(i)[1]) * 100 / (int)user.SKILLS().get(i)[2]);

                    TextView progressName = new TextView(activity);
                    progressName.setPadding(25, 0, 0, 10);
                    progressName.setTextSize(24);
                    progressName.setText((String)user.SKILLS().get(i)[0]);
                    progressName.setLayoutParams(skillLayoutParams);

                    skillLayout.addView(skillProgressBar);
                    skillLayout.addView(progressName);
                    linearLayoutSkills.addView(skillLayout);

                    linearLayoutSkills.addView(gray_line2);
                }
            }

        }
    }

    static class GetImageBitmap extends AsyncTask<String, String, Bitmap> {
        private final WeakReference<Activity> mActivity;

        public GetImageBitmap(Activity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        protected Bitmap doInBackground(String... strings) {
            HttpURLConnection connection = null;
            Bitmap bitmap = null;

            try {
                URL url = new URL(strings[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();


                InputStream stream = connection.getInputStream();
                bitmap = BitmapFactory.decodeStream(stream);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap img) {
            ImageView imageView = mActivity.get().findViewById(R.id.profile_image);
            imageView.setImageBitmap(img);
        }
    }

    static class GetSlack extends AsyncTask<String, String, String> {
        private final WeakReference<Activity> mActivity;

        public GetSlack(Activity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        protected String doInBackground(String... params) {
            return MyUtility.fetchData(params);
        }

        @Override
        protected void onPostExecute(String slack) {
            if (slack != null) {
                String finalSlack = slack.substring(1, slack.length() - 2);
                ImageView imageView = mActivity.get().findViewById(R.id.icon_slack);
                imageView.setOnClickListener(v -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(finalSlack));
                    try {
                        mActivity.get().startActivity(intent);
                    }
                    catch (android.content.ActivityNotFoundException e) {
                        Toast.makeText(mActivity.get(), "Slack app not installed", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }
}