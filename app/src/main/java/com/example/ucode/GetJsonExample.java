package com.example.ucode;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

public class GetJsonExample extends AsyncTask<String, String, String> {
    private WeakReference<Activity> mActivity;

    public GetJsonExample(Activity activity){
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

        pd = new ProgressDialog(mActivity.get());
        pd.setMessage("Please wait");
        pd.setCancelable(false);
        pd.show();
    }

    @Override
    protected String doInBackground(String... params) {


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

        if (pd.isShowing()){
            pd.dismiss();
        }
    }
}
