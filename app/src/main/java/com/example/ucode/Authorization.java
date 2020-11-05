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
    private final String username;
    private final String password;
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
            super.onPreExecute(); // make wait screen
        }

        @Override
        protected String doInBackground(String... params) {
            return MyUtility.fetchPostData("POST", false, null, params);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
        }
    }
}
