package com.example.ucode;

import android.os.AsyncTask;

import org.json.JSONException;
import org.json.JSONObject;

public class Authorization {
    private static String username;
    private static String password;
    private static String token;

    public Authorization(String username, String password) {
        Authorization.username = username;
        Authorization.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        Authorization.token = token;
    }

    public void generateAuthToken() {
        String url = "https://lms.ucode.world/api/frontend/o/token";
        GetToken getToken = new GetToken();
        getToken.execute(url, "username", username, "password", password, "grant_type", "password");
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
        protected void onPostExecute(String response) {
            if (response != null) {
                JSONObject jsonObject;
                try {
                    jsonObject = new JSONObject(response);
                    token = "Bearer " + jsonObject.getString("access_token");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Authorization authorization = new Authorization(username, password);
                authorization.setToken(token);
                MyUtility.saveToken(authorization);
            }
        }
    }
}
