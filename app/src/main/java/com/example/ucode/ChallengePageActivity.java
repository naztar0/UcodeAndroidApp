package com.example.ucode;

import androidx.appcompat.app.AlertDialog;
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
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
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
    private static String challengeUrl = null;

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
        challengeUrl = bundle.getString("challengeUrl");
        String scheduledUrl = "https://lms.ucode.world/api/v0/frontend/slots/scheduled/";
        String questionsUrl = "https://lms.ucode.world/api/v0/frontend/feedback_questions/";


        ScrollView mainView = this.findViewById(R.id.activity_challenge_page_main_layout);
        mainView.setVisibility(View.GONE);

        final GetJson getJson = new GetJson(this);
        getJson.execute("authorization", authorization.getToken(), challengeUrl, scheduledUrl, questionsUrl);
    }

    static class GetJson extends AsyncTask<String, String, String[]> {
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
        protected String[] doInBackground(String... params) {
            String change_page = MyUtility.fetchData(params[2], params[0], params[1]);
            String scheduled = MyUtility.fetchData(params[3], params[0], params[1]);
            String questions = MyUtility.fetchData(params[4], params[0], params[1]);
            if (change_page == null) {
                try {authorization.generateAuthToken();}
                catch (Exception ignore) {}
                change_page = MyUtility.fetchData(params[2], params[0], authorization.getToken());
                scheduled = MyUtility.fetchData(params[3], params[0], authorization.getToken());
                questions = MyUtility.fetchData(params[4], params[0], authorization.getToken());
                if (change_page != null)
                    newToken = true;
            }
            return new String[]{change_page, scheduled, questions};
        }

        @Override
        protected void onPostExecute(String[] result) {
            super.onPostExecute(result);

            Activity activity = mActivity.get();
            if (activity == null || activity.isFinishing()) {
                return;
            }
            MyUtility myUtility = new MyUtility(activity.getResources(), activity, activity);

            if (pd.isShowing())
                pd.dismiss();

            if (result[0] == null) {
                Toast.makeText(activity, "Can't get challenge data...", Toast.LENGTH_LONG).show();
                return;
            }
            if (result[1] == null) {
                Toast.makeText(activity, "Can't get assessments data...", Toast.LENGTH_LONG).show();
                return;
            }
            if (result[2] == null) {
                Toast.makeText(activity, "Can't get questions data...", Toast.LENGTH_LONG).show();
                return;
            }

            JSONObject jsonData = null, scheduledJson = null, questionsJson = null;
            try {
                jsonData = new JSONObject(result[0]);
                // TODO: schedule in challenge realization
                scheduledJson = new JSONObject(result[1]);
                questionsJson = new JSONObject(result[2]);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (jsonData == null) {
                Toast.makeText(activity, "An error occurred, please try later...", Toast.LENGTH_LONG).show();
                return;
            }

            try {
                String status = jsonData.getString("status");
                JSONObject challenge = jsonData.getJSONObject("challenge");
                String title = challenge.getString("title");
                String description = challenge.getString("description");
                int experience = challenge.getInt("experience");
                int mark = -1, reflection_id = 0;
                if (!jsonData.isNull("mark"))
                    mark = jsonData.getInt("mark");
                if (!jsonData.isNull("reflection_team_id"))
                    reflection_id = jsonData.getInt("reflection_team_id");
                boolean can_give_up = jsonData.getBoolean("can_give_up");
                JSONArray past_assessments_json = jsonData.getJSONArray("past_assessments");
                ArrayList<Object[]> past_assessments = null;
                if (past_assessments_json.length() != 0)
                    past_assessments = new ArrayList<>();
                for (int i = 0; i < past_assessments_json.length(); i++) {
                    JSONObject assessor_data = past_assessments_json.getJSONObject(i);
                    JSONObject assessor = assessor_data.getJSONObject("assessor");
                    String username = assessor.getString("username");
                    String photoUrl = "https://lms.ucode.world/api/" + assessor.getString("photo");
                    int assessor_mark = 0;
                    if (!assessor_data.isNull("mark"))
                        assessor_mark = assessor_data.getInt("mark");
                    int reflection_mark = 0;
                    if (!assessor_data.isNull("reflection_mark"))
                        reflection_mark = assessor_data.getInt("reflection_mark");
                    double feedback_mark = -1;
                    String feedback_text = "";
                    if (!assessor_data.isNull("feedback")) {
                        JSONObject feedback = assessor_data.getJSONObject("feedback");
                        if (!feedback.isNull("mark"))
                            feedback_mark = feedback.getDouble("mark");
                        if (!feedback.isNull("text"))
                            feedback_text = feedback.getString("text");
                    }
                    int assessment_id = assessor_data.getInt("id");

                    Object[] objects = new Object[7];
                    objects[0] = username;
                    objects[1] = photoUrl;
                    objects[2] = assessor_mark;
                    objects[3] = reflection_mark;
                    objects[4] = feedback_mark;
                    objects[5] = feedback_text;
                    objects[6] = assessment_id;
                    assert past_assessments != null;
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
                    assert past_automatons != null;
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
                    assert past_presentations != null;
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
                    String invite_status = team_user.getString("invite_status");
                    JSONObject user = team_user.getJSONObject("user");
                    String username = user.getString("username");
                    String photoUrl = "https://lms.ucode.world/api/" + user.getString("photo");
                    Object[] objects = new Object[4];
                    objects[0] = username;
                    objects[1] = photoUrl;
                    objects[2] = invite_status;
                    objects[3] = leader;
                    team_users.add(objects);
                }


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
                Button finish_button = activity.findViewById(R.id.challenge_finish_button);
                Button give_up_button = activity.findViewById(R.id.challenge_give_up_button);
                Button reflection_button = activity.findViewById(R.id.challenge_reflection_button);
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
                else {
                    mark_text_view.setText(status.replace("_", " "));
                    mark_text_view.setTextColor(myUtility.parseColor(R.color.challenge_success));
                    mark_text_view.setBackground(ContextCompat.getDrawable(activity, R.drawable.ripple_green_bg));
                }

                title_layout.setOnClickListener(v -> {
                    if (description_text_view.getVisibility() == View.GONE)
                        description_text_view.setVisibility(View.VISIBLE);
                    else
                        description_text_view.setVisibility(View.GONE);
                });

                if (status.equals("in_progress")) {
                    finish_button.setVisibility(View.VISIBLE);
                    finish_button.setOnClickListener(v -> {
                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);
                        alertDialog.setTitle("Finish");
                        alertDialog.setMessage("Are you sure you want to finish?");
                        alertDialog.setPositiveButton("Yes", (dialog, whichButton) -> {
                            String finishUrl = challengeUrl + "close/";
                            JSONObject jsonObject = new JSONObject();
                            try { jsonObject.put("status", "reflection"); }
                            catch (JSONException ignore) {}
                            FinishChallenge finishChallenge = new FinishChallenge(activity);
                            finishChallenge.execute(jsonObject, new String[]{ finishUrl, "authorization", authorization.getToken() });
                            Toast.makeText(activity, "Finished", Toast.LENGTH_SHORT).show();
                            activity.startActivity(activity.getIntent());
                            activity.finish();
                        });
                        alertDialog.setNegativeButton("No", null);
                        alertDialog.show();
                    });
                }
                else if (status.equals("reflection")) {
                    reflection_button.setVisibility(View.VISIBLE);
                    int finalReflection_id = reflection_id;
                    reflection_button.setOnClickListener(v -> {
                        String reflectionUrl = "https://lms.ucode.world/api/v0/frontend/assessment_teams/" + finalReflection_id + "/";

                        Intent intent = new Intent(activity, ReflectionActivity.class);
                        Bundle bundle = new Bundle();
                        bundle.putString("reflectionUrl", reflectionUrl);
                        intent.putExtras(bundle);
                        activity.startActivity(intent);
                        activity.finish();
                    });
                }
                if (can_give_up) {
                    give_up_button.setVisibility(View.VISIBLE);
                    give_up_button.setOnClickListener(v -> {
                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);
                        alertDialog.setTitle("Give up");
                        alertDialog.setMessage("Are you sure you want to give up?");
                        alertDialog.setPositiveButton("Yes", (dialog, whichButton) -> {
                            String finishUrl = challengeUrl + "close/";
                            JSONObject jsonObject = new JSONObject();
                            try { jsonObject.put("status", "cancelled"); }
                            catch (JSONException ignore) {}
                            FinishChallenge finishChallenge = new FinishChallenge(activity);
                            finishChallenge.execute(jsonObject, new String[]{ finishUrl, "authorization", authorization.getToken() });
                            Toast.makeText(activity, "Gave up", Toast.LENGTH_SHORT).show();
                            activity.startActivity(activity.getIntent());
                            activity.finish();
                        });
                        alertDialog.setNegativeButton("No", null);
                        alertDialog.show();
                    });
                }

                members_layout.removeAllViews();
                for (int i = 0; i < team_users.size(); i++) {
                    RelativeLayout relativeLayout = new RelativeLayout(activity);
                    FrameLayout frameLayout = new FrameLayout(activity);
                    CardView cardView = new CardView(activity);
                    ImageView imageView = new ImageView(activity);
                    TextView textView = new TextView(activity);

                    RelativeLayout.LayoutParams relativeParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                    relativeParams.leftMargin = myUtility.dpToPx(5);
                    relativeLayout.setLayoutParams(relativeParams);
                    int relativeLayoutPadding = myUtility.dpToPx(10);
                    relativeLayout.setPadding(relativeLayoutPadding, relativeLayoutPadding, relativeLayoutPadding, relativeLayoutPadding);
                    relativeLayout.setBackground(ContextCompat.getDrawable(activity, R.drawable.ripple_grey_border));
                    relativeLayout.setTag(team_users.get(i)[0]);

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
                    getImageBitmap.execute((String)team_users.get(i)[1], Integer.toString(id));

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
                        RelativeLayout progressWithTextRelativeLayout3 = new RelativeLayout(activity);
                        CircularProgressBar progressBar1 = new CircularProgressBar(activity);
                        CircularProgressBar progressBar2 = new CircularProgressBar(activity);
                        CircularProgressBar progressBar3 = new CircularProgressBar(activity);
                        TextView progressText1 = new TextView(activity);
                        TextView progressText2 = new TextView(activity);
                        TextView progressText3 = new TextView(activity);
                        TextView messageTextView = null;
                        LinearLayout feedbackLinearLayout = null;

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
                        relativeLayout.setTag(past_assessments.get(i)[0]);

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
                        getImageBitmap.execute((String)past_assessments.get(i)[1], Integer.toString(id));

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
                        progressBar2.setProgress((int)(past_assessments.get(i)[3]));

                        RelativeLayout.LayoutParams progressText2Params = new RelativeLayout.LayoutParams(
                                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
                        progressText2Params.rightMargin =  myUtility.dpToPx(3);
                        progressText2Params.bottomMargin =  myUtility.dpToPx(3);
                        progressText2.setLayoutParams(progressText2Params);
                        progressText2.setGravity(Gravity.CENTER);
                        progressText2.setTextSize(12);
                        progressText2.setText(String.valueOf((int)past_assessments.get(i)[3]));

                        // progress 3
                        RelativeLayout.LayoutParams progressWithTextRelativeLayout3Params = new RelativeLayout.LayoutParams(progressBarSize, progressBarSize);
                        progressWithTextRelativeLayout3Params.leftMargin = myUtility.dpToPx(100);
                        progressWithTextRelativeLayout3.setLayoutParams(progressWithTextRelativeLayout3Params);

                        double progress3 = (double)past_assessments.get(i)[4];
                        if (progress3 == -1)
                            progress3 = 0;
                        RelativeLayout.LayoutParams progressBar3Params = new RelativeLayout.LayoutParams(progressBarSize, progressBarSize);
                        progressBar3.setLayoutParams(progressBar3Params);
                        progressBar3.setAnimationDuration(2000);
                        progressBar3.showSubstrate(true);
                        progressBar3.showProgressText(false);
                        progressBar3.setSubstrateColor(myUtility.parseColor(R.color.light_grey));
                        progressBar3.setProgressColor(myUtility.parseColor(R.color.skill_progress));
                        progressBar3.setProgressWidth(myUtility.dpToPx(3));
                        progressBar3.setProgress((int)(progress3 * 100 / 5));

                        RelativeLayout.LayoutParams progressText3Params = new RelativeLayout.LayoutParams(
                                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
                        progressText3Params.rightMargin = myUtility.dpToPx(3);
                        progressText3Params.bottomMargin = myUtility.dpToPx(3);
                        progressText3.setLayoutParams(progressText3Params);
                        progressText3.setGravity(Gravity.CENTER);
                        progressText3.setTextSize(12);
                        progressText3.setText(String.valueOf(progress3));

                        // assessment if needs
                        if ((double)past_assessments.get(i)[4] != -1) {
                            // message text
                            RelativeLayout.LayoutParams relativeParamsForMessage = new RelativeLayout.LayoutParams(
                                    RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                            messageTextView = new TextView(activity);
                            messageTextView.setLayoutParams(relativeParamsForMessage);
                            messageTextView.setTextSize(18);
                            messageTextView.setTextColor(ContextCompat.getColor(activity, R.color.profile_black_text_80));
                            messageTextView.setText((String) past_assessments.get(i)[5]);

                        }
                        else {
                            // toxicCheckbox    id 100 + i
                            // commentText      id 200 + i
                            // seekBar          id 300 * (i + 1) + j

                            int toxicCheckboxId = 100 + i;
                            int commentEditTextId = 200 + i;
                            int seekBarId = 300 * (i + 1);

                            LinearLayout.LayoutParams feedbackLinearParams = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                            feedbackLinearLayout = new LinearLayout(activity);
                            feedbackLinearLayout.setLayoutParams(feedbackLinearParams);
                            feedbackLinearLayout.setOrientation(LinearLayout.VERTICAL);

                            JSONArray questionsArray = questionsJson.getJSONArray("results");
                            for (int j = 0; j < questionsArray.length(); j++) {
                                JSONObject questionJson = questionsArray.getJSONObject(j);
                                String questionTitle = questionJson.getString("title");
                                int questionId = questionJson.getInt("id");
                                int questionMaxMark = questionJson.getInt("max_mark");

                                TextView questionTextView = new TextView(activity);
                                SeekBar seekBar = new SeekBar(activity);

                                LinearLayout.LayoutParams questionLinearParams = new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                                questionLinearParams.topMargin = myUtility.dpToPx(10);
                                questionLinearParams.bottomMargin = myUtility.dpToPx(5);
                                questionTextView.setLayoutParams(questionLinearParams);
                                questionTextView.setPadding(myUtility.dpToPx(10), 0, myUtility.dpToPx(10), 0);
                                questionTextView.setTextSize(16);
                                questionTextView.setText(questionTitle);

                                LinearLayout.LayoutParams seekLinearParams = new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                                seekBar.setLayoutParams(seekLinearParams);
                                seekBar.setMax(questionMaxMark);
                                seekBar.setProgress(questionMaxMark/2+1);
                                seekBar.setId(seekBarId + j);
                                seekBar.setTag(questionId);

                                feedbackLinearLayout.addView(questionTextView);
                                feedbackLinearLayout.addView(seekBar);
                            }
                            CheckBox toxicCheckbox = new CheckBox(activity);
                            EditText commentEditText = new EditText(activity);
                            Button submitButton = new Button(activity);

                            LinearLayout.LayoutParams toxicLinearParams = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                            toxicLinearParams.topMargin = myUtility.dpToPx(5);
                            toxicCheckbox.setLayoutParams(toxicLinearParams);
                            toxicCheckbox.setTextSize(16);
                            toxicCheckbox.setText(activity.getString(R.string.toxic));
                            toxicCheckbox.setId(toxicCheckboxId);

                            LinearLayout.LayoutParams commentLinearParams = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                            commentLinearParams.topMargin = myUtility.dpToPx(10);
                            commentEditText.setLayoutParams(commentLinearParams);
                            int editPadding = myUtility.dpToPx(10);
                            commentEditText.setPadding(editPadding, editPadding, editPadding, editPadding);
                            commentEditText.setBackground(ContextCompat.getDrawable(activity, R.drawable.border_grey));
                            commentEditText.setHint(activity.getString(R.string.comment));
                            commentEditText.setId(commentEditTextId);

                            LinearLayout.LayoutParams submitLinearParams = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT, myUtility.dpToPx(40));
                            submitLinearParams.topMargin = myUtility.dpToPx(10);
                            submitButton.setLayoutParams(submitLinearParams);
                            submitButton.setTextSize(16);
                            submitButton.setAllCaps(false);
                            submitButton.setTextColor(ContextCompat.getColor(activity, R.color.profile_white_text));
                            submitButton.setBackground(ContextCompat.getDrawable(activity, R.drawable.ripple_dark_bg));
                            submitButton.setText(activity.getString(R.string.submit));
                            int finalI = i;
                            ArrayList<Object[]> finalPast_assessments = past_assessments;
                            submitButton.setOnClickListener(v -> {
                                JSONObject jsonPostData = new JSONObject();
                                JSONObject answersJson = new JSONObject();
                                for (int j = 0; j < questionsArray.length(); j++) {
                                    SeekBar seekBarForJson = activity.findViewById(seekBarId + j);
                                    int seekBarForJsonId = (int)seekBarForJson.getTag();
                                    int seekBarForJsonValue = seekBarForJson.getProgress();
                                    try {
                                        answersJson.put(Integer.toString(seekBarForJsonId), seekBarForJsonValue);
                                    } catch (JSONException ignore) {}
                                }
                                EditText editTextForJson = activity.findViewById(commentEditTextId);
                                CheckBox checkBoxForJson = activity.findViewById(toxicCheckboxId);
                                String commentText = editTextForJson.getText().toString();
                                if (commentText.equals(""))
                                    commentText = " ";
                                boolean isToxic = checkBoxForJson.isChecked();
                                try {
                                    jsonPostData.put("answers", answersJson);
                                    jsonPostData.put("text", commentText);
                                    jsonPostData.put("toxic", isToxic);
                                } catch (JSONException ignore) {}

                                String feedbackUrl = "https://lms.ucode.world/api/v0/frontend/assessment_teams/" + finalPast_assessments.get(finalI)[6] + "/create-feedback/";
                                CreateFeedback createFeedback = new CreateFeedback(activity, jsonPostData);
                                createFeedback.execute(feedbackUrl, "authorization", authorization.getToken());
                                Log.d("JSON_DATA", jsonPostData.toString() + ", url: " + feedbackUrl);

                                activity.startActivity(activity.getIntent());
                                activity.finish();
                            });

                            feedbackLinearLayout.addView(toxicCheckbox);
                            feedbackLinearLayout.addView(commentEditText);
                            feedbackLinearLayout.addView(submitButton);
                        }

                        cardView.addView(imageView);
                        relativeLayout.addView(cardView);
                        relativeLayout.addView(textView);

                        progressWithTextRelativeLayout1.addView(progressBar1);
                        progressWithTextRelativeLayout1.addView(progressText1);
                        progressWithTextRelativeLayout2.addView(progressBar2);
                        progressWithTextRelativeLayout2.addView(progressText2);
                        progressWithTextRelativeLayout3.addView(progressBar3);
                        progressWithTextRelativeLayout3.addView(progressText3);

                        progressRelativeLayout.addView(progressWithTextRelativeLayout1);
                        progressRelativeLayout.addView(progressWithTextRelativeLayout2);
                        progressRelativeLayout.addView(progressWithTextRelativeLayout3);
                        messageLinearLayout.addView(progressRelativeLayout);
                        if ((double)past_assessments.get(i)[4] != -1)
                            messageLinearLayout.addView(messageTextView);
                        else
                            messageLinearLayout.addView(feedbackLinearLayout);
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
                else {
                    TextView textView = activity.findViewById(R.id.past_assessments_layout_text);
                    textView.setVisibility(View.GONE);
                    past_assessments_layout.setVisibility(View.GONE);
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
                        messageText.setTextColor(ContextCompat.getColor(activity, R.color.profile_black_text_80));
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
                ImageView imageView = mActivity.get().findViewById((int) objects[0]);
                imageView.setImageBitmap((Bitmap) objects[1]);
            }
        }
    }

    static class CreateFeedback extends AsyncTask<String, String, String> {
        WeakReference<Activity> mActivity;
        private final JSONObject jsonObject;
        ProgressDialog pd;

        CreateFeedback (Activity activity, JSONObject jsonObject) {
            mActivity = new WeakReference<>(activity);
            this.jsonObject = jsonObject;
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
            return MyUtility.fetchPostData("POST", true, jsonObject, strings);
        }

        @Override
        protected void onPostExecute(String result) {
            if (pd.isShowing())
                pd.dismiss();
            Log.d("RESULT", String.valueOf(result));
        }
    }

    static class FinishChallenge extends AsyncTask<Object, String, String> {
        WeakReference<Activity> mActivity;
        ProgressDialog pd;

        FinishChallenge (Activity activity) {
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
            return MyUtility.fetchPostData("PATCH", true, (JSONObject)objects[0], (String[])objects[1]);
        }

        @Override
        protected void onPostExecute(String result) {
            if (pd.isShowing())
                pd.dismiss();
            Log.d("RESULT", String.valueOf(result));
        }
    }
}