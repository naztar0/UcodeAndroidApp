package com.example.ucode;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

public class Authorization {
    private String username;
    private String password;
    private String token;

    public Authorization(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public String getToken() {
        return this.token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void generateAuthToken() throws ExecutionException, InterruptedException, JSONException {
        String url = "https://lms.ucode.world/api/frontend/o/token";
        GetToken getToken = new GetToken();
        getToken.execute(url, "username", this.username, "password", this.password, "grant_type", "password");
        String response = getToken.get();
        if (response != null) {
            JSONObject jsonObject = new JSONObject(response);
            this.token = "Bearer " + jsonObject.getString("access_token");
        }
    }


    private static class GetToken extends AsyncTask<String, String, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();//////////////////////////////////////////// make wait screen
        }

        @Override
        protected String doInBackground(String... params) {
            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                StringBuilder postData = new StringBuilder();
                for (int i = 1; i < (params.length - 1); i += 2) {
                    postData.append(params[i]).append("=").append(params[i + 1]);
                    if (params.length - 1 - i > 1)
                        postData.append("&");
                }
                byte[] postDataBytes = postData.toString().getBytes(StandardCharsets.UTF_8);
                Log.d("postData", postData.toString());
                URL url = new URL(params[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setRequestProperty("charset", "utf-8");
                connection.setRequestProperty("Content-Length", Integer.toString(postData.length()));
                connection.setInstanceFollowRedirects(false);
                connection.setDoOutput(true);
                connection.setUseCaches(false);
                connection.getOutputStream().write(postDataBytes);


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
