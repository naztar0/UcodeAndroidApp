package com.example.ucode;

import android.os.AsyncTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutionException;

public class Authorization {
    private String username;
    private String password;

    public Authorization(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getLogin() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public String getAuthToken() throws ExecutionException, InterruptedException, JSONException {
        String url = "https://lms.ucode.world/api/frontend/o/token";
        GetToken getToken = new GetToken();
        getToken.execute(url, "username", this.username, "password", this.password, "grant_type", "password");
        String response = getToken.get();
        JSONObject jsonObject = new JSONObject(response);
        return jsonObject.getString("access_token");
    }


    static class GetToken extends AsyncTask<String, String, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();//////////////////////////////////////////// make wait screen
        }

        @Override
        protected String doInBackground(String... params) {
            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                URL url = new URL(params[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
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


            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
        }
    }
}
