package com.example.ucode;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.Log;
import android.util.TypedValue;

import androidx.core.content.ContextCompat;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutionException;

public class MyUtility {
    private static Resources mResources;
    private Context mContext;
    private Activity mActivity;

    public MyUtility(Resources resources, Context context, Activity activity) {
        mResources = resources;
        this.mContext = context;
        this.mActivity = activity;
    }

    public float dpToPx(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, mResources.getDisplayMetrics());
    }
    public int parseColor(int color) {
        return Color.parseColor("#" + Integer.toHexString (ContextCompat.getColor(mContext, color)));
    }


    public static void saveData(Object object) {
        final File file = new File(mResources.getString(R.string.home_cache_path));

        try (FileOutputStream fos = new FileOutputStream(file);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(object);
        } catch (Exception i) {
            Log.d("ERROR", String.valueOf(i));
        }
    }

    public static Object getData() {
        final File suspend_f = new File(mResources.getString(R.string.home_cache_path));

        Object object = null;

        try (FileInputStream fis = new FileInputStream(suspend_f); ObjectInputStream is = new ObjectInputStream(fis)) {
            object = is.readObject();
        } catch (java.io.FileNotFoundException e) {
            return null;
        } catch (Exception i) {
            Log.d("ERROR", String.valueOf(i));
        }

        return object;
    }

    public static void saveToken(Authorization authorization) {
        final File file = new File(mResources.getString(R.string.profile_token_path));

        try (FileOutputStream fos = new FileOutputStream(file);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(authorization.getToken());
        } catch (Exception i) {
            Log.d("ERROR", String.valueOf(i));
        }
    }

    public static String getToken() {
        final File suspend_f = new File(mResources.getString(R.string.profile_token_path));
        String token = null;
        try (FileInputStream fis = new FileInputStream(suspend_f); ObjectInputStream is = new ObjectInputStream(fis)) {
            token = (String) is.readObject();
        } catch (java.io.FileNotFoundException e) {
            return null;
        } catch (Exception i) {
            Log.d("ERROR", String.valueOf(i));
        }
        return token;
    }

    public Authorization authorize() throws InterruptedException, ExecutionException, JSONException {
        SharedPreferences sharedPreferences = mActivity.getSharedPreferences("auth", Context.MODE_PRIVATE);
        String username = sharedPreferences.getString("username", null);
        String password = sharedPreferences.getString("password", null);
        if (username == null || password == null)
            return null;
        Authorization authorization = new Authorization(username, password);
        String token = getToken();
        if (token != null)
            authorization.setToken(token);
        else
            authorization.generateAuthToken();
        return authorization;
    }

    public static String fetchData(String... params) {
        HttpURLConnection connection = null;
        BufferedReader reader = null;

        try {
            URL url = new URL(params[0]);
            connection = (HttpURLConnection) url.openConnection();
            for (int i = 1; i < (params.length - 1); i += 2)
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
}
