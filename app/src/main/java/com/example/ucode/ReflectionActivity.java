package com.example.ucode;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatRadioButton;
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
import android.widget.RadioButton;
import android.widget.RadioGroup;
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
import java.util.Arrays;

public class ReflectionActivity extends AppCompatActivity {

    private static Authorization authorization;
    private static boolean newToken = false;
    private static String reflectionUrl = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reflection);
        MyUtility myUtility = new MyUtility(getResources(), this, this);

        try { authorization = myUtility.authorize(); }
        catch (Exception ignored) {}
        if (authorization == null)
            return;

        Bundle bundle = getIntent().getExtras();
        if (bundle == null)
            return;
        reflectionUrl = bundle.getString("reflectionUrl");

        ScrollView mainView = this.findViewById(R.id.activity_reflection_page_main_layout);
        mainView.setVisibility(View.GONE);

        final GetJson getJson = new GetJson(this);
        getJson.execute(reflectionUrl, "authorization", authorization.getToken());
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
            String assessment_page = MyUtility.fetchData(params);
            if (assessment_page == null) {
                try {authorization.generateAuthToken();}
                catch (Exception ignore) {}
                assessment_page = MyUtility.fetchData(params[0], params[1], authorization.getToken());
                if (assessment_page != null)
                    newToken = true;
            }
            return assessment_page;
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
                Toast.makeText(activity, "Can't get assessment data...", Toast.LENGTH_LONG).show();
                return;
            }
            Log.d("RESULT", result);

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
                boolean is_reflection = jsonData.getBoolean("is_reflection");
                JSONObject team = jsonData.getJSONObject("team");
                String team_name = team.getString("name");
                JSONObject assessments_json = jsonData.getJSONObject("assessment");
                String description = assessments_json.getString("name");
                JSONArray blocks_json = assessments_json.getJSONArray("blocks");
                ArrayList<Object[]> blocks = new ArrayList<>();
                for (int i = 0; i < blocks_json.length(); i++) {
                    JSONObject block = blocks_json.getJSONObject(i);
                    String blockName = block.getString("name");
                    String blockDesc = block.getString("description");
                    String blockType = block.getString("type");
                    JSONArray blockQuestionsJson = block.getJSONArray("questions");
                    ArrayList<Object[]> blockQuestions = new ArrayList<>();
                    for (int j = 0; j < blockQuestionsJson.length(); j++) {
                        JSONObject blockQuestion = blockQuestionsJson.getJSONObject(j);
                        int questionId = blockQuestion.getInt("id");
                        String questionName = blockQuestion.getString("name");
                        String questionDesc = blockQuestion.getString("description");
                        String questionType = blockQuestion.getString("type");
                        JSONArray questionValuesJson = blockQuestion.getJSONArray("question_values");
                        ArrayList<Object[]> questionValues = new ArrayList<>();
                        for (int k = 0; k < questionValuesJson.length(); k++) {
                            JSONObject valueJson = questionValuesJson.getJSONObject(k);
                            int valueId = valueJson.getInt("id");
                            String valueName = valueJson.getString("value");
                            String valueHint = valueJson.getString("hint");
                            double valuePercent = valueJson.getDouble("percent");

                            Object[] valueObjects = new Object[4];
                            valueObjects[0] = valueId;
                            valueObjects[1] = valueName;
                            valueObjects[2] = valueHint;
                            valueObjects[3] = valuePercent;
                            questionValues.add(valueObjects);
                        }

                        Object[] questionObjects = new Object[5];
                        questionObjects[0] = questionId;
                        questionObjects[1] = questionName;
                        questionObjects[2] = questionDesc;
                        questionObjects[3] = questionType;
                        questionObjects[4] = questionValues;
                        blockQuestions.add(questionObjects);
                    }

                    Object[] blockObjects = new Object[4];
                    blockObjects[0] = blockName;
                    blockObjects[1] = blockDesc;
                    blockObjects[2] = blockType;
                    blockObjects[3] = blockQuestions;
                    blocks.add(blockObjects);
                }


                TextView title_text_view = activity.findViewById(R.id.reflection_page_title);
                TextView description_text_view = activity.findViewById(R.id.reflection_page_description);
                CardView title_layout = activity.findViewById(R.id.reflection_page_title_layout);
                ScrollView main_layout = activity.findViewById(R.id.activity_reflection_page_main_layout);
                LinearLayout questions_layout = activity.findViewById(R.id.reflection_page_questions_layout);
                Button finish_assessment_button = activity.findViewById(R.id.finish_assessment_button);
                main_layout.setVisibility(View.VISIBLE);

                String title;
                if (is_reflection)
                    title = "Reflection for " + team_name;
                else
                    title = "Assessment for " + team_name;
                title_text_view.setText(title);
                description_text_view.setText(description);

                title_layout.setOnClickListener(v -> {
                    if (description_text_view.getVisibility() == View.GONE)
                        description_text_view.setVisibility(View.VISIBLE);
                    else
                        description_text_view.setVisibility(View.GONE);
                });

                questions_layout.removeAllViews();
                for (int i = 0; i < blocks.size(); i++) {
                    @SuppressWarnings("unchecked")
                    ArrayList<Object[]> questions = (ArrayList<Object[]>) blocks.get(i)[3];
                    LinearLayout linearLayout = new LinearLayout(activity);
                    TextView blockNameTextView = new TextView(activity);
                    TextView blockDescTextView = new TextView(activity);

                    LinearLayout.LayoutParams linearLayoutParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    linearLayoutParams.topMargin = myUtility.dpToPx(10);
                    linearLayoutParams.bottomMargin = myUtility.dpToPx(10);
                    linearLayout.setLayoutParams(linearLayoutParams);
                    linearLayout.setPadding(0, myUtility.dpToPx(5), 0, 0);
                    linearLayout.setOrientation(LinearLayout.VERTICAL);
                    linearLayout.setBackground(ContextCompat.getDrawable(activity, R.drawable.border_dark));

                    LinearLayout.LayoutParams blockNameParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    blockNameTextView.setLayoutParams(blockNameParams);
                    blockNameTextView.setPadding(myUtility.dpToPx(15), myUtility.dpToPx(7), myUtility.dpToPx(15), myUtility.dpToPx(7));
                    blockNameTextView.setTextSize(20);
                    blockNameTextView.setTypeface(null, Typeface.BOLD);
                    blockNameTextView.setTextColor(ContextCompat.getColor(activity, R.color.profile_black_text_80));
                    blockNameTextView.setText((String)blocks.get(i)[0]);

                    LinearLayout.LayoutParams blockDeskParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    blockDeskParams.topMargin = myUtility.dpToPx(2);
                    blockDeskParams.bottomMargin = myUtility.dpToPx(2);
                    blockDeskParams.leftMargin = myUtility.dpToPx(10);
                    blockDeskParams.rightMargin = myUtility.dpToPx(10);
                    blockDescTextView.setLayoutParams(blockDeskParams);
                    blockDescTextView.setPadding(myUtility.dpToPx(15), myUtility.dpToPx(5), myUtility.dpToPx(15), myUtility.dpToPx(5));
                    blockDescTextView.setTextSize(15);
                    blockDescTextView.setTextColor(ContextCompat.getColor(activity, R.color.profile_black_text_80));
                    blockDescTextView.setBackground(ContextCompat.getDrawable(activity, R.color.grey_5));
                    blockDescTextView.setText(myUtility.trimN((String)blocks.get(i)[1]));

                    linearLayout.addView(blockNameTextView);
                    linearLayout.addView(blockDescTextView);
                    for (int j = 0; j < questions.size(); j++) {
                        @SuppressWarnings("unchecked")
                        ArrayList<Object[]> values = (ArrayList<Object[]>) questions.get(j)[4];
                        String trimmed = myUtility.trimN((String)questions.get(j)[2]);
                        String[] separated = myUtility.bashCheck(trimmed);

                        LinearLayout questionLayout = new LinearLayout(activity);
                        TextView questionNameTextView = new TextView(activity);
                        TextView questionDescTextView = new TextView(activity);
                        TextView bashTextView = null;
                        RadioGroup radioGroup;
                        LinearLayout checkboxLayout;

                        LinearLayout.LayoutParams questionLayoutParams = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        questionLayoutParams.topMargin = myUtility.dpToPx(15);
                        questionLayout.setLayoutParams(questionLayoutParams);
                        questionLayout.setOrientation(LinearLayout.VERTICAL);

                        LinearLayout.LayoutParams questionNameParams = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        questionNameParams.topMargin = myUtility.dpToPx(2);
                        questionNameParams.bottomMargin = myUtility.dpToPx(2);
                        questionNameParams.leftMargin = myUtility.dpToPx(10);
                        questionNameParams.rightMargin = myUtility.dpToPx(10);
                        questionNameTextView.setLayoutParams(questionNameParams);
                        questionNameTextView.setPadding(myUtility.dpToPx(15), myUtility.dpToPx(5), myUtility.dpToPx(15), myUtility.dpToPx(5));
                        questionNameTextView.setTextSize(16);
                        questionNameTextView.setTypeface(null, Typeface.BOLD);
                        questionNameTextView.setTextColor(ContextCompat.getColor(activity, R.color.profile_black_text_80));
                        questionNameTextView.setBackground(ContextCompat.getDrawable(activity, R.color.grey_5));
                        questionNameTextView.setText((String)questions.get(j)[1]);

                        LinearLayout.LayoutParams questionDeskParams = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        questionDeskParams.topMargin = myUtility.dpToPx(5);
                        questionDeskParams.rightMargin = myUtility.dpToPx(15);
                        questionDeskParams.leftMargin = myUtility.dpToPx(15);
                        questionDescTextView.setLayoutParams(questionDeskParams);
                        questionDescTextView.setTextSize(14);
                        questionDescTextView.setTextColor(ContextCompat.getColor(activity, R.color.profile_black_text_80));
                        questionDescTextView.setText(separated == null ? trimmed : separated[0]);

                        if (separated != null) {
                            bashTextView = new TextView(activity);
                            LinearLayout.LayoutParams bashTextViewParams = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                            bashTextViewParams.topMargin = myUtility.dpToPx(5);
                            bashTextViewParams.rightMargin = myUtility.dpToPx(15);
                            bashTextViewParams.leftMargin = myUtility.dpToPx(15);
                            bashTextView.setLayoutParams(bashTextViewParams);
                            bashTextView.setPadding(myUtility.dpToPx(10), myUtility.dpToPx(10), myUtility.dpToPx(10), myUtility.dpToPx(10));
                            bashTextView.setTextSize(10);
                            bashTextView.setTextColor(ContextCompat.getColor(activity, R.color.profile_white_text_80));
                            bashTextView.setTextIsSelectable(true);
                            bashTextView.setTypeface(Typeface.MONOSPACE);
                            bashTextView.setBackground(ContextCompat.getDrawable(activity, R.drawable.dark_bg));
                            bashTextView.setText(separated[1]);
                        }


                        questionLayout.addView(questionNameTextView);
                        questionLayout.addView(questionDescTextView);
                        if (bashTextView != null)
                            questionLayout.addView(bashTextView);
                        linearLayout.addView(questionLayout);
                        switch ((String)questions.get(j)[3]) {
                            case "list":
                                radioGroup = new RadioGroup(activity);
                                RadioGroup.LayoutParams radioGroupParams = new RadioGroup.LayoutParams(
                                        RadioGroup.LayoutParams.MATCH_PARENT, RadioGroup.LayoutParams.WRAP_CONTENT);
                                radioGroup.setLayoutParams(radioGroupParams);
                                radioGroup.setOrientation(LinearLayout.VERTICAL);
                                radioGroup.setPadding(myUtility.dpToPx(15), myUtility.dpToPx(10), myUtility.dpToPx(15), myUtility.dpToPx(10));
                                radioGroup.setId((int)questions.get(j)[0]);
                                //radioGroup.setTag("list");

                                for (int k = 0; k < values.size(); k++) {
                                    RadioButton radioButton = new AppCompatRadioButton(activity);
                                    TextView radioText = new TextView(activity);

                                    RadioGroup.LayoutParams radioButtonParams = new RadioGroup.LayoutParams(myUtility.dpToPx(120), myUtility.dpToPx(35));
                                    radioButton.setLayoutParams(radioButtonParams);
                                    radioButton.setButtonDrawable(ContextCompat.getDrawable(activity, R.color.transparent));
                                    radioButton.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                                    radioButton.setTextSize(14);
                                    radioButton.setText((String)values.get(k)[1]);
                                    if ((double)values.get(k)[3] > 0)
                                        radioButton.setBackground(ContextCompat.getDrawable(activity, R.drawable.radio_button_true));
                                    else
                                        radioButton.setBackground(ContextCompat.getDrawable(activity, R.drawable.radio_button_false));
                                    radioButton.setId((int)values.get(k)[0]);

                                    RadioGroup.LayoutParams radioTextParams = new RadioGroup.LayoutParams(
                                            RadioGroup.LayoutParams.WRAP_CONTENT, RadioGroup.LayoutParams.WRAP_CONTENT);
                                    radioTextParams.leftMargin = myUtility.dpToPx(135);
                                    radioTextParams.topMargin = myUtility.dpToPx(-35);
                                    radioTextParams.bottomMargin = myUtility.dpToPx(30);
                                    radioText.setLayoutParams(radioTextParams);
                                    radioText.setTextSize(12);
                                    radioText.setTextColor(ContextCompat.getColor(activity, R.color.profile_black_text_80));
                                    radioText.setText(myUtility.trimN((String)values.get(k)[2]));

                                    radioGroup.addView(radioButton);
                                    radioGroup.addView(radioText);
                                }
                                linearLayout.addView(radioGroup);
                                break;
                            case "boolean":
                                radioGroup = new RadioGroup(activity);
                                RadioButton radioButtonF = new AppCompatRadioButton(activity);
                                RadioButton radioButtonT = new AppCompatRadioButton(activity);

                                radioGroupParams = new RadioGroup.LayoutParams(
                                        RadioGroup.LayoutParams.MATCH_PARENT, RadioGroup.LayoutParams.WRAP_CONTENT);
                                radioGroup.setLayoutParams(radioGroupParams);
                                radioGroup.setOrientation(LinearLayout.HORIZONTAL);
                                radioGroup.setPadding(myUtility.dpToPx(15), myUtility.dpToPx(10), myUtility.dpToPx(15), myUtility.dpToPx(10));
                                radioGroup.setId((int)questions.get(j)[0]);
                                //radioGroup.setTag("boolean");

                                RadioGroup.LayoutParams radioButtonFParams = new RadioGroup.LayoutParams(myUtility.dpToPx(70), myUtility.dpToPx(35));
                                radioButtonF.setLayoutParams(radioButtonFParams);
                                radioButtonF.setButtonDrawable(ContextCompat.getDrawable(activity, R.color.transparent));
                                radioButtonF.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                                radioButtonF.setTextSize(14);
                                radioButtonF.setText(activity.getString(R.string.bool_false));
                                radioButtonF.setBackground(ContextCompat.getDrawable(activity, R.drawable.radio_button_false_bool));
                                radioButtonF.setId(10000 * (i + 1) + j);
                                radioButtonF.setTag(false);

                                RadioGroup.LayoutParams radioButtonTParams = new RadioGroup.LayoutParams(myUtility.dpToPx(70), myUtility.dpToPx(35));
                                radioButtonTParams.leftMargin = myUtility.dpToPx(-1);
                                radioButtonT.setLayoutParams(radioButtonTParams);
                                radioButtonT.setButtonDrawable(ContextCompat.getDrawable(activity, R.color.transparent));
                                radioButtonT.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                                radioButtonT.setTextSize(14);
                                radioButtonT.setText(activity.getString(R.string.bool_true));
                                radioButtonT.setBackground(ContextCompat.getDrawable(activity, R.drawable.radio_button_true_bool));
                                radioButtonT.setId(20000 * (i + 1) + j);
                                radioButtonT.setTag(true);

                                radioGroup.addView(radioButtonF);
                                radioGroup.addView(radioButtonT);
                                linearLayout.addView(radioGroup);
                                break;
                            case "checkbox":
                                checkboxLayout = new LinearLayout(activity);
                                LinearLayout.LayoutParams checkboxLayoutParams = new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                                checkboxLayout.setLayoutParams(checkboxLayoutParams);
                                checkboxLayout.setOrientation(LinearLayout.VERTICAL);
                                checkboxLayout.setPadding(myUtility.dpToPx(15), myUtility.dpToPx(10), myUtility.dpToPx(15), myUtility.dpToPx(10));
                                //checkboxLayout.setTag("checkbox");

                                for (int k = 0; k < values.size(); k++) {
                                    CheckBox checkBox = new CheckBox(activity);

                                    LinearLayout.LayoutParams checkboxParams = new LinearLayout.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                                    checkBox.setLayoutParams(checkboxParams);
                                    checkBox.setTextSize(12);
                                    checkBox.setText((String)values.get(k)[2]);
                                    checkBox.setId((int)values.get(k)[0]);

                                    checkboxLayout.addView(checkBox);
                                }
                                linearLayout.addView(checkboxLayout);
                                break;
                            case "range":
                                SeekBar seekBar = new SeekBar(activity);
                                LinearLayout.LayoutParams seekBarParams = new LinearLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                                seekBarParams.leftMargin = myUtility.dpToPx(5);
                                seekBarParams.rightMargin = myUtility.dpToPx(5);
                                seekBarParams.topMargin = myUtility.dpToPx(10);
                                seekBarParams.bottomMargin = myUtility.dpToPx(10);
                                seekBar.setLayoutParams(seekBarParams);
                                seekBar.setMax(10);
                                seekBar.setProgress(5);
                                seekBar.setId((int)questions.get(j)[0]);
                                //seekBar.setTag("range");

                                linearLayout.addView(seekBar);
                                break;
                            case "comment":
                                EditText editText = new EditText(activity);
                                LinearLayout.LayoutParams editTextParams = new LinearLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                                editTextParams.leftMargin = myUtility.dpToPx(15);
                                editTextParams.rightMargin = myUtility.dpToPx(15);
                                editTextParams.topMargin = myUtility.dpToPx(10);
                                editTextParams.bottomMargin = myUtility.dpToPx(10);
                                editText.setLayoutParams(editTextParams);
                                editText.setPadding(myUtility.dpToPx(10), myUtility.dpToPx(10), myUtility.dpToPx(10), myUtility.dpToPx(10));
                                editText.setBackground(ContextCompat.getDrawable(activity, R.drawable.border_grey));
                                editText.setHint("Comment");
                                editText.setId((int)questions.get(j)[0]);
                                //editText.setTag("comment");

                                linearLayout.addView(editText);
                                break;
                        }
                    }
                    questions_layout.addView(linearLayout);
                }
                finish_assessment_button.setOnClickListener(v -> {
                    boolean error = false;
                    JSONObject jsonObject = new JSONObject();
                    for (int i = 0; i < blocks.size(); i++) {
                        if (error)
                            break;
                        @SuppressWarnings("unchecked")
                        ArrayList<Object[]> questions = (ArrayList<Object[]>) blocks.get(i)[3];
                        for (int j = 0; j < questions.size(); j++) {
                            if (error)
                                break;
                            int questionId = (int)questions.get(j)[0];
                            switch ((String)questions.get(j)[3]) {
                                case "list":
                                    RadioGroup radioGroup = activity.findViewById(questionId);
                                    int radioId = radioGroup.getCheckedRadioButtonId();
                                    if (radioId == -1) {
                                        error = true;
                                        break;
                                    }
                                    double valuePercent = 0;
                                    @SuppressWarnings("unchecked")
                                    ArrayList<Object[]> values = (ArrayList<Object[]>) questions.get(j)[4];
                                    for (int k = 0; k < values.size(); k++) {
                                        if ((int)values.get(k)[0] == radioId) {
                                            valuePercent = (double)values.get(k)[3];
                                            break;
                                        }
                                    }
                                    try { jsonObject.put(Integer.toString(questionId), valuePercent); }
                                    catch (JSONException ignore) {}
                                    break;
                                case "boolean":
                                    radioGroup = activity.findViewById(questionId);
                                    radioId = radioGroup.getCheckedRadioButtonId();
                                    if (radioId == -1) {
                                        error = true;
                                        break;
                                    }
                                    RadioButton radioButton = activity.findViewById(radioId);
                                    int valueBool = (boolean)radioButton.getTag() ? 1 : 0;
                                    try { jsonObject.put(Integer.toString(questionId), valueBool); }
                                    catch (JSONException ignore) {}
                                    break;
                                case "checkbox":
                                    valuePercent = 0;
                                    @SuppressWarnings("unchecked")
                                    ArrayList<Object[]> values1 = (ArrayList<Object[]>) questions.get(j)[4];
                                    for (int k = 0; k < values1.size(); k++) {
                                        CheckBox checkBox = activity.findViewById((int)values1.get(k)[0]);
                                        if (checkBox.isChecked())
                                            valuePercent += (double)values1.get(k)[3];
                                    }
                                    try { jsonObject.put(Integer.toString(questionId), Double.toString(valuePercent)); }
                                    catch (JSONException ignore) {}
                                    break;
                                case "range":
                                    SeekBar seekBar = activity.findViewById((int)questions.get(j)[0]);
                                    valuePercent = seekBar.getProgress() * 0.1;
                                    try { jsonObject.put(Integer.toString(questionId), valuePercent); }
                                    catch (JSONException ignore) {}
                                    break;
                                case "comment":
                                    EditText editText = activity.findViewById((int)questions.get(j)[0]);
                                    String commentText = editText.getText().toString();
                                    if (commentText.equals(""))
                                        commentText = " ";
                                    try { jsonObject.put(Integer.toString(questionId), commentText); }
                                    catch (JSONException ignore) {}
                                    break;
                            }
                        }
                    }
                    if (!error) {
                        JSONObject answerJson = new JSONObject();
                        try { answerJson.put("answers", jsonObject); }
                        catch (JSONException ignore) {}
                        Log.d("DATA", answerJson.toString());
                        FinishReflection finishReflection = new FinishReflection(activity);
                        finishReflection.execute(answerJson);
                        activity.finish();
                    }
                    else
                        Toast.makeText(activity, "Fill in all the fields!", Toast.LENGTH_SHORT).show();
                });

                /* === Cheat zone begin === */
                String temp_comment = "Nice";
                boolean cheat = true;
                if (!is_reflection)
                    if (cheat) {
                        Button make_all_true = activity.findViewById(R.id.finish_assessment_all_true_button);
                        make_all_true.setVisibility(View.VISIBLE);
                        make_all_true.setOnClickListener(v -> {
                            for (int i = 0; i < blocks.size(); i++) {
                                @SuppressWarnings("unchecked")
                                ArrayList<Object[]> questions = (ArrayList<Object[]>) blocks.get(i)[3];
                                for (int j = 0; j < questions.size(); j++) {
                                    switch ((String)questions.get(j)[3]) {
                                        case "list":
                                            @SuppressWarnings("unchecked")
                                            ArrayList<Object[]> values = (ArrayList<Object[]>) questions.get(j)[4];
                                            for (int k = 0; k < values.size(); k++) {
                                                if ((double)values.get(k)[3] > 0) {
                                                    RadioButton radioButton = activity.findViewById((int)values.get(k)[0]);
                                                    radioButton.setChecked(true);
                                                }
                                            }
                                            break;
                                        case "boolean":
                                            RadioButton radioButton = activity.findViewById(20000 * (i + 1) + j);
                                            radioButton.setChecked(true);
                                            break;
                                        case "checkbox":
                                            @SuppressWarnings("unchecked")
                                            ArrayList<Object[]> values1 = (ArrayList<Object[]>) questions.get(j)[4];
                                            for (int k = 0; k < values1.size(); k++) {
                                                double percent = (double)values1.get(k)[3];
                                                if (percent > 0) {
                                                    CheckBox checkBox = activity.findViewById((int)values1.get(k)[0]);
                                                    checkBox.setChecked(true);
                                                }
                                            }
                                            break;
                                        case "range":
                                            SeekBar seekBar = activity.findViewById((int)questions.get(j)[0]);
                                            seekBar.setProgress(10);
                                            break;
                                        case "comment":
                                            EditText editText = activity.findViewById((int)questions.get(j)[0]);
                                            editText.setText(temp_comment);
                                            break;
                                    }
                                }
                            }
                        });
                    }
                /* === Cheat zone end === */

            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (newToken)
                MyUtility.saveToken(authorization);
        }
    }
    static class FinishReflection extends AsyncTask<JSONObject, String, String> {
        WeakReference<Activity> mActivity;

        FinishReflection (Activity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        protected String doInBackground(JSONObject... jsonObject) {
            return MyUtility.fetchPostData("PUT", true, jsonObject[0], reflectionUrl + "finish-assessment/", "authorization", authorization.getToken());
        }

        @Override
        protected void onPostExecute(String s) {
            if (s.charAt(0) == '"')
                Toast.makeText(mActivity.get(), s, Toast.LENGTH_LONG).show();
        }
    }
}