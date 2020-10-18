package com.example.ucode;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
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

public class ChallengePageActivity extends AppCompatActivity {

    private static Authorization authorization;
    private static boolean newToken = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_challenge_page);
        MyUtility myUtility = new MyUtility(getResources(), this, this);

        try { authorization = myUtility.authorize(); }
        catch (Exception ignored) {}
        if (authorization == null)
            return;

        Bundle bundle = getIntent().getExtras();
        if (bundle == null)
            return;
        String challengeUrl = bundle.getString("challengeUrl");

        ScrollView mainView = this.findViewById(R.id.activity_challenge_page_main_layout);
        mainView.setVisibility(View.GONE);

        final ChallengePageActivity.GetJson getJson = new ChallengePageActivity.GetJson(this);
        getJson.execute(challengeUrl, "authorization", authorization.getToken());

    }

    static class GetJson extends AsyncTask<String, String, String> {
        private final WeakReference<Activity> mActivity;

        public GetJson(Activity activity) {
            mActivity = new WeakReference<>(activity);
        }


        ProgressDialog pd;

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
            MyUtility myUtility = new MyUtility(activity.getResources(), activity, activity);

            if (pd.isShowing())
                pd.dismiss();

            if (result == null) {
                Toast.makeText(activity, "Can't get challenge data...", Toast.LENGTH_LONG).show();
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
                JSONObject challenge = jsonData.getJSONObject("challenge");
                String title = challenge.getString("title");
                String description = challenge.getString("description");
                int experience = challenge.getInt("experience");
                int mark = -1;
                mark = jsonData.getInt("mark");
                JSONArray past_assessments_json = jsonData.getJSONArray("past_assessments");
                ArrayList<Object[]> past_assessments = null;
                if (past_assessments_json.length() != 0)
                    past_assessments = new ArrayList<>();
                for (int i = 0; i < past_assessments_json.length(); i++) {
                    JSONObject assessor_data = past_assessments_json.getJSONObject(i);
                    JSONObject assessor = assessor_data.getJSONObject("assessor");
                    String username = assessor.getString("username");
                    String photoUrl = "https://lms.ucode.world/api/" + assessor.getString("photo");
                    JSONObject feedback = assessor_data.getJSONObject("feedback");
                    float feedback_mark = feedback.getInt("mark");
                    String feedback_text = feedback.getString("text");
                    int assessor_mark = assessor_data.getInt("mark");
                    Object[] objects = new Object[5];
                    objects[0] = username;
                    objects[1] = photoUrl;
                    objects[2] = assessor_mark;
                    objects[3] = feedback_mark;
                    objects[4] = feedback_text;
                    past_assessments.add(objects);
                }
                JSONArray past_automatons_json = jsonData.getJSONArray("past_automatons");
                ArrayList<Object[]> past_automatons = null;
                if (past_automatons_json.length() != 0)
                    past_automatons = new ArrayList<>();
                for (int i = 0; i < past_automatons_json.length(); i++) {
                    JSONObject automaton_data = past_automatons_json.getJSONObject(i);
                    JSONObject automaton = automaton_data.getJSONObject("automaton");
                    String name = automaton.getString("name");
                    int automator_mark = automaton_data.getInt("mark");
                    String comment = automaton_data.getString("comment");
                    Object[] objects = new Object[3];
                    objects[0] = name;
                    objects[1] = automator_mark;
                    objects[2] = comment;
                    past_automatons.add(objects);
                }
                JSONArray past_presentations_json = jsonData.getJSONArray("past_presentations");
                ArrayList<String[]> past_presentations = null;
                if (past_presentations_json.length() != 0)
                    past_presentations = new ArrayList<>();
                for (int i = 0; i < past_presentations_json.length(); i++) {
                    JSONObject assessor = past_presentations_json.getJSONObject(i);
                    String username = assessor.getString("username");
                    String comment = assessor.getString("comment");
                    String[] strings = new String[2];
                    strings[0] = username;
                    strings[1] = comment;
                    past_presentations.add(strings);
                }
                int presentation_mark = 0;
                if (past_presentations != null)
                    presentation_mark = jsonData.getInt("presentation_mark");
                JSONArray teams = jsonData.getJSONArray("teams");
                JSONObject team = teams.getJSONObject(0);
                String team_name = team.getString("name");
                JSONArray team_users_json = team.getJSONArray("team_users");
                ArrayList<Object[]> team_users = new ArrayList<>();
                for (int i = 0; i < team_users_json.length(); i++) {
                    JSONObject team_user = team_users_json.getJSONObject(i);
                    String leader = team_user.getString("leader");
                    String status = team_user.getString("invite_status");
                    JSONObject user = team_user.getJSONObject("user");
                    String username = user.getString("username");
                    String photoUrl = "https://lms.ucode.world/api/" + user.getString("photo");
                    Object[] objects = new Object[4];
                    objects[0] = username;
                    objects[1] = photoUrl;
                    objects[2] = status;
                    objects[3] = leader;
                    team_users.add(objects);
                }

                /*Log.d("TITLE", title);
                Log.d("DESC", description);
                Log.d("EXPERIENCE", String.valueOf(experience));
                Log.d("MARK", String.valueOf(mark));
                Log.d("PRES_MARK", String.valueOf(presentation_mark));
                Log.d("TEAM_NAME", team_name);
                Log.d("ASS", String.valueOf(past_assessments));
                Log.d("AUTO", String.valueOf(past_automatons));
                Log.d("PRESENTATIONS", String.valueOf(past_presentations));
                Log.d("TEAM", String.valueOf(team_users));*/

                TextView title_text_view = activity.findViewById(R.id.challenge_page_title);
                TextView description_text_view = activity.findViewById(R.id.challenge_page_description);
                CardView title_layout = activity.findViewById(R.id.challenge_page_title_layout);
                TextView mark_text_view = activity.findViewById(R.id.challenge_page_mark);
                LinearLayout members_layout = activity.findViewById(R.id.challenge_team_members_layout);
                TextView team_name_text_view = activity.findViewById(R.id.challenge_team_name);
                ScrollView main_layout = activity.findViewById(R.id.activity_challenge_page_main_layout);
                LinearLayout past_assessments_layout = activity.findViewById(R.id.past_assessments_layout);
                LinearLayout past_automatons_layout = activity.findViewById(R.id.past_automatons_layout);
                LinearLayout past_presentations_layout = activity.findViewById(R.id.past_presentations_layout);
                TextView presentation_mark_text_view = activity.findViewById(R.id.past_presentations_layout_text);
                main_layout.setVisibility(View.VISIBLE);

                title_text_view.setText(title);
                description_text_view.setText(description);
                team_name_text_view.setText(team_name);
                if (mark >= 0) {
                    if (mark >= 50) {
                        String value = activity.getResources().getString(R.string.success) + ": " + mark;
                        mark_text_view.setText(value);
                        mark_text_view.setTextColor(myUtility.parseColor(R.color.challenge_success));
                        mark_text_view.setBackground(ContextCompat.getDrawable(activity, R.drawable.ripple_green_bg));
                    }
                    else {
                        String value = activity.getResources().getString(R.string.fail) + ": " + mark;
                        mark_text_view.setText(value);
                        mark_text_view.setTextColor(myUtility.parseColor(R.color.challenge_fail));
                        mark_text_view.setBackground(ContextCompat.getDrawable(activity, R.drawable.ripple_red_bg));
                    }
                }

                title_layout.setOnClickListener(v -> {
                    if (description_text_view.getVisibility() == View.GONE)
                        description_text_view.setVisibility(View.VISIBLE);
                    else
                        description_text_view.setVisibility(View.GONE);
                });
                
                

                members_layout.removeAllViews();
                for (int i = 0; i < team_users.size(); i++) {
                    RelativeLayout relativeLayout = new RelativeLayout(activity);
                    FrameLayout frameLayout = new FrameLayout(activity);
                    CardView cardView = new CardView(activity);
                    ImageView imageView = new ImageView(activity);
                    TextView textView = new TextView(activity);

                    RelativeLayout.LayoutParams relativeParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                    relativeParams.leftMargin = (int)myUtility.dpToPx(5);
                    relativeLayout.setLayoutParams(relativeParams);
                    int relativeLayoutPadding = myUtility.dpToPx(10);
                    relativeLayout.setPadding(relativeLayoutPadding, relativeLayoutPadding, relativeLayoutPadding, relativeLayoutPadding);
                    relativeLayout.setBackground(ContextCompat.getDrawable(activity, R.drawable.ripple_grey_border));
                    relativeLayout.setTag((String)team_users.get(i)[0]);

                    RelativeLayout.LayoutParams relativeParamsForFrame = new RelativeLayout.LayoutParams(myUtility.dpToPx(72), myUtility.dpToPx(72));
                    relativeParamsForFrame.addRule(RelativeLayout.CENTER_HORIZONTAL);
                    frameLayout.setLayoutParams(relativeParamsForFrame);
                    frameLayout.setBackground(ContextCompat.getDrawable(activity, R.drawable.photo_wrapper_green));

                    CardView.LayoutParams cardParams = new CardView.LayoutParams(CardView.LayoutParams.WRAP_CONTENT, CardView.LayoutParams.WRAP_CONTENT);
                    cardParams.gravity = Gravity.CENTER;
                    cardView.setLayoutParams(cardParams);
                    cardView.setRadius(myUtility.dpToPx(30));

                    CardView.LayoutParams cardParamsForImage = new CardView.LayoutParams(myUtility.dpToPx(60), myUtility.dpToPx(60));
                    imageView.setLayoutParams(cardParamsForImage);
                    int id = View.generateViewId();
                    imageView.setId(id);
                    GetImageBitmap getImageBitmap = new GetImageBitmap(activity);
                    getImageBitmap.execute((String)team_users.get(i)[1], String.valueOf(id));

                    RelativeLayout.LayoutParams relativeParamsForText = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                    relativeParamsForText.topMargin = myUtility.dpToPx(75);
                    relativeParamsForText.addRule(RelativeLayout.CENTER_HORIZONTAL);
                    textView.setLayoutParams(relativeParamsForText);
                    textView.setTextSize(15);
                    textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    textView.setText((String)team_users.get(i)[0]);

                    cardView.addView(imageView);
                    frameLayout.addView(cardView);
                    relativeLayout.addView(frameLayout);
                    relativeLayout.addView(textView);
                    members_layout.addView(relativeLayout);

                    relativeLayout.setOnClickListener(v -> {
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
                past_assessments_layout.removeAllViews();
                if (past_assessments != null) {
                    for (int i = 0; i < past_assessments.size(); i++) {
                        LinearLayout linearLayout = new LinearLayout(activity);
                        RelativeLayout relativeLayout = new RelativeLayout(activity);
                        CardView cardView = new CardView(activity);
                        ImageView imageView = new ImageView(activity);
                        TextView textView = new TextView(activity);
                        LinearLayout messageLinearLayout = new LinearLayout(activity);
                        RelativeLayout progressRelativeLayout = new RelativeLayout(activity);
                        RelativeLayout progressWithTextRelativeLayout1 = new RelativeLayout(activity);
                        RelativeLayout progressWithTextRelativeLayout2 = new RelativeLayout(activity);
                        CircularProgressBar progressBar1 = new CircularProgressBar(activity);
                        CircularProgressBar progressBar2 = new CircularProgressBar(activity);
                        TextView progressText1 = new TextView(activity);
                        TextView progressText2 = new TextView(activity);
                        TextView messageTextView = new TextView(activity);

                        // avatar and username generation
                        LinearLayout.LayoutParams linearParams = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        linearLayout.setLayoutParams(linearParams);
                        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
                        int layoutPadding = myUtility.dpToPx(10);
                        linearLayout.setPadding(layoutPadding, layoutPadding, layoutPadding, layoutPadding);
                        linearLayout.setGravity(Gravity.BOTTOM);

                        RelativeLayout.LayoutParams relativeParams = new RelativeLayout.LayoutParams(
                                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                        relativeLayout.setLayoutParams(relativeParams);
                        relativeLayout.setPadding(layoutPadding, layoutPadding, layoutPadding, layoutPadding);
                        relativeLayout.setBackground(ContextCompat.getDrawable(activity, R.drawable.ripple_grey_border));
                        relativeLayout.setTag((String)past_assessments.get(i)[0]);

                        RelativeLayout.LayoutParams relativeParamsForCard = new RelativeLayout.LayoutParams(
                                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                        relativeParamsForCard.addRule(RelativeLayout.CENTER_HORIZONTAL);
                        relativeParamsForCard.addRule(Gravity.CENTER);
                        cardView.setLayoutParams(relativeParamsForCard);
                        cardView.setRadius(myUtility.dpToPx(20));

                        CardView.LayoutParams cardParamsForImage = new CardView.LayoutParams(myUtility.dpToPx(40), myUtility.dpToPx(40));
                        imageView.setLayoutParams(cardParamsForImage);
                        int id = View.generateViewId();
                        imageView.setId(id);
                        GetImageBitmap getImageBitmap = new GetImageBitmap(activity);
                        getImageBitmap.execute((String)past_assessments.get(i)[1], String.valueOf(id));

                        RelativeLayout.LayoutParams relativeParamsForText = new RelativeLayout.LayoutParams(
                                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                        relativeParamsForText.topMargin = myUtility.dpToPx(45);
                        relativeParamsForText.addRule(RelativeLayout.CENTER_HORIZONTAL);
                        textView.setLayoutParams(relativeParamsForText);
                        textView.setTextSize(12);
                        textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                        textView.setText((String)past_assessments.get(i)[0]);

                        // message generation
                        LinearLayout.LayoutParams messageLinearParams = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        messageLinearParams.leftMargin = myUtility.dpToPx(10);
                        messageLinearLayout.setLayoutParams(messageLinearParams);
                        messageLinearLayout.setOrientation(LinearLayout.VERTICAL);
                        messageLinearLayout.setPadding(layoutPadding, layoutPadding, layoutPadding, layoutPadding);
                        messageLinearLayout.setBackground(ContextCompat.getDrawable(activity, R.drawable.challenge_feedback_text));

                        RelativeLayout.LayoutParams progressRelativeParams = new RelativeLayout.LayoutParams(
                                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                        progressRelativeLayout.setLayoutParams(progressRelativeParams);

                        // progress 1
                        progressWithTextRelativeLayout1.setLayoutParams(new RelativeLayout.LayoutParams(myUtility.dpToPx(40), myUtility.dpToPx(40)));

                        int progressBarSize = myUtility.dpToPx(40);
                        progressBar1.setLayoutParams(new LinearLayout.LayoutParams(progressBarSize, progressBarSize));
                        progressBar1.setAnimationDuration(2000);
                        progressBar1.showSubstrate(true);
                        progressBar1.showProgressText(false);
                        progressBar1.setSubstrateColor(myUtility.parseColor(R.color.light_grey));
                        progressBar1.setProgressColor(myUtility.parseColor(R.color.skill_progress));
                        progressBar1.setProgressWidth(myUtility.dpToPx(3));
                        progressBar1.setProgress((int)(past_assessments.get(i)[2]));

                        RelativeLayout.LayoutParams progressText1Params = new RelativeLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                        progressText1Params.rightMargin =  myUtility.dpToPx(3);
                        progressText1Params.bottomMargin =  myUtility.dpToPx(3);
                        progressText1.setLayoutParams(progressText1Params);
                        progressText1.setGravity(Gravity.CENTER);
                        progressText1.setTextSize(12);
                        progressText1.setText(String.valueOf((int)past_assessments.get(i)[2]));

                        // progress 2
                        RelativeLayout.LayoutParams progressWithTextRelativeLayout2Params = new RelativeLayout.LayoutParams(progressBarSize, progressBarSize);
                        progressWithTextRelativeLayout2Params.leftMargin = myUtility.dpToPx(50);
                        progressWithTextRelativeLayout2.setLayoutParams(progressWithTextRelativeLayout2Params);

                        RelativeLayout.LayoutParams progressBar2Params = new RelativeLayout.LayoutParams(progressBarSize, progressBarSize);
                        progressBar2.setLayoutParams(progressBar2Params);
                        progressBar2.setAnimationDuration(2000);
                        progressBar2.showSubstrate(true);
                        progressBar2.showProgressText(false);
                        progressBar2.setSubstrateColor(myUtility.parseColor(R.color.light_grey));
                        progressBar2.setProgressColor(myUtility.parseColor(R.color.skill_progress));
                        progressBar2.setProgressWidth(myUtility.dpToPx(3));
                        progressBar2.setProgress((int)(float)(past_assessments.get(i)[3]) * 100 / 5);

                        RelativeLayout.LayoutParams progressText2Params = new RelativeLayout.LayoutParams(
                                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
                        progressText2Params.rightMargin =  myUtility.dpToPx(3);
                        progressText2Params.bottomMargin =  myUtility.dpToPx(3);
                        progressText2.setLayoutParams(progressText2Params);
                        progressText2.setGravity(Gravity.CENTER);
                        progressText2.setTextSize(12);
                        progressText2.setText(String.valueOf((float)past_assessments.get(i)[3]));

                        // message text
                        RelativeLayout.LayoutParams relativeParamsForMessage = new RelativeLayout.LayoutParams(
                                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                        messageTextView.setLayoutParams(relativeParamsForMessage);
                        messageTextView.setTextSize(18);
                        messageTextView.setText((String)past_assessments.get(i)[4]);

                        cardView.addView(imageView);
                        relativeLayout.addView(cardView);
                        relativeLayout.addView(textView);

                        progressWithTextRelativeLayout1.addView(progressBar1);
                        progressWithTextRelativeLayout1.addView(progressText1);
                        progressWithTextRelativeLayout2.addView(progressBar2);
                        progressWithTextRelativeLayout2.addView(progressText2);

                        progressRelativeLayout.addView(progressWithTextRelativeLayout1);
                        progressRelativeLayout.addView(progressWithTextRelativeLayout2);
                        messageLinearLayout.addView(progressRelativeLayout);
                        messageLinearLayout.addView(messageTextView);
                        linearLayout.addView(relativeLayout);
                        linearLayout.addView(messageLinearLayout);
                        past_assessments_layout.addView(linearLayout);

                        relativeLayout.setOnClickListener(v -> {
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
                }
                past_automatons_layout.removeAllViews();
                if (past_automatons != null) {
                    for (int i = 0; i < past_automatons.size(); i++) {
                        RelativeLayout relativeLayout = new RelativeLayout(activity);
                        RelativeLayout progressRelativeLayout = new RelativeLayout(activity);
                        CircularProgressBar progressBar = new CircularProgressBar(activity);
                        TextView progressText = new TextView(activity);
                        TextView automatonName = new TextView(activity);

                        relativeLayout.setLayoutParams(new RelativeLayout.LayoutParams(
                                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
                        int relativePadding = myUtility.dpToPx(10);
                        relativeLayout.setPadding(relativePadding, relativePadding, relativePadding, relativePadding);
                        relativeLayout.setBackground(ContextCompat.getDrawable(activity, R.drawable.ripple_grey_border));

                        RelativeLayout.LayoutParams progressLayoutParams = new RelativeLayout.LayoutParams(myUtility.dpToPx(55), myUtility.dpToPx(55));
                        progressLayoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
                        progressRelativeLayout.setLayoutParams(progressLayoutParams);

                        RelativeLayout.LayoutParams progressBarParams = new RelativeLayout.LayoutParams(myUtility.dpToPx(55), myUtility.dpToPx(55));
                        progressBar.setLayoutParams(progressBarParams);
                        progressBar.setAnimationDuration(2000);
                        progressBar.showSubstrate(true);
                        progressBar.showProgressText(false);
                        progressBar.setSubstrateColor(myUtility.parseColor(R.color.light_grey));
                        progressBar.setProgressColor(myUtility.parseColor(R.color.skill_progress));
                        progressBar.setProgressWidth(myUtility.dpToPx(4));
                        progressBar.setProgress((int)(past_automatons.get(i)[1]));

                        RelativeLayout.LayoutParams progressTextParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
                        progressTextParams.rightMargin = myUtility.dpToPx(5);
                        progressTextParams.bottomMargin = myUtility.dpToPx(3);
                        progressText.setLayoutParams(progressTextParams);
                        progressText.setGravity(Gravity.CENTER);
                        progressText.setTextSize(15);
                        progressText.setText(String.valueOf((int)(past_automatons.get(i)[1])));

                        RelativeLayout.LayoutParams automatonNameTextParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                        automatonNameTextParams.topMargin = myUtility.dpToPx(60);
                        automatonNameTextParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
                        automatonName.setLayoutParams(automatonNameTextParams);
                        automatonName.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                        automatonName.setTextSize(18);
                        automatonName.setText((String)past_automatons.get(i)[0]);

                        progressRelativeLayout.addView(progressBar);
                        progressRelativeLayout.addView(progressText);
                        relativeLayout.addView(progressRelativeLayout);
                        relativeLayout.addView(automatonName);
                        past_automatons_layout.addView(relativeLayout);
                    }
                }
                else {
                    TextView textView = activity.findViewById(R.id.past_automatons_layout_text);
                    textView.setVisibility(View.GONE);
                    past_automatons_layout.setVisibility(View.GONE);
                }
                past_presentations_layout.removeAllViews();
                if (past_presentations != null) {
                    String presentation_mark_wrapper = activity.getString(R.string.presentation_mark) + ": " + presentation_mark;
                    presentation_mark_text_view.setText(presentation_mark_wrapper);
                    for (int i = 0; i < past_presentations.size(); i++) {
                        LinearLayout linearLayout = new LinearLayout(activity);
                        TextView usernameText = new TextView(activity);
                        TextView messageText = new TextView(activity);

                        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
                        linearLayout.setPadding(
                                myUtility.dpToPx(10), myUtility.dpToPx(15), myUtility.dpToPx(10), 0);
                        linearLayout.setGravity(Gravity.BOTTOM);
                        linearLayout.setOrientation(LinearLayout.VERTICAL);

                        messageText.setLayoutParams(new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
                        int textPadding = myUtility.dpToPx(10);
                        messageText.setPadding(textPadding, textPadding, textPadding, textPadding);
                        messageText.setBackground(ContextCompat.getDrawable(activity, R.drawable.challenge_feedback_text));
                        messageText.setTextSize(18);
                        messageText.setText(past_presentations.get(i)[1]);

                        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                        textParams.topMargin = myUtility.dpToPx(5);
                        usernameText.setLayoutParams(textParams);
                        usernameText.setTextSize(12);
                        usernameText.setText(past_presentations.get(i)[0]);

                        linearLayout.addView(messageText);
                        linearLayout.addView(usernameText);
                        past_presentations_layout.addView(linearLayout);
                    }
                }
                else {
                    past_presentations_layout.setVisibility(View.GONE);
                    presentation_mark_text_view.setVisibility(View.GONE);
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
                ImageView imageView = mActivity.get().findViewById((int)objects[0]);
                imageView.setImageBitmap((Bitmap)objects[1]);
            }
        }
    }
}