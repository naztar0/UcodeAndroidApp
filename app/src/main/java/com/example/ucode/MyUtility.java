package com.example.ucode;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.icu.text.SimpleDateFormat;
import android.util.Log;
import android.util.TypedValue;

import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
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
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;

public class MyUtility {
    private static Resources mResources;
    private final Context mContext;
    private final Activity mActivity;

    public MyUtility(Resources resources, Context context, Activity activity) {
        mResources = resources;
        this.mContext = context;
        this.mActivity = activity;
    }

    public int dpToPx(float dp) {
        return (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, mResources.getDisplayMetrics());
    }
//    public static int dpToPx(float dp, Resources resources) {
//        return (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.getDisplayMetrics());
//    }
    public int parseColor(int color) {
        return Color.parseColor("#" + Integer.toHexString (ContextCompat.getColor(mContext, color)));
    }
//    public static int parseColor(int color, Context context) {
//        return Color.parseColor("#" + Integer.toHexString (ContextCompat.getColor(context, color)));
//    }

    public static String[] parseDateTime(String s) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat sdf_res = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        Date date = sdf.parse(s.substring(0, s.indexOf("T")));
        String begin_date = sdf_res.format(date);
        String begin_time = s.substring(s.indexOf("T") + 1, s.indexOf("T") + 6);
        return new String[] {begin_date, begin_time};
    }

    public String trimN(String s) {
        StringBuilder res = new StringBuilder();
        String[] sep = s.split("\n");
        for (String sub: sep) {
            sub = sub.trim();
            res.append(sub).append("\n");
        }
        res.delete(res.length() - 1, res.length());
        return res.toString();
    }
    public String[] bashCheck(String s) {
        int monoStart = s.indexOf("```bash");
        int monoEnd = s.indexOf("```\n");
        if (s.charAt(s.length() - 1) == '`')
            monoEnd = s.length() - 4;
        if (monoStart == -1 || monoEnd == -1)
            return null;
        String mono = s.substring(monoStart + 8, monoEnd - 1);
        String before = s.substring(0, monoStart - 1);
        String after = s.substring(monoEnd + 4);
        return new String[]{before, mono, after};
    }

    public String trimEllipsis(String s, int n) {
        if (s.length() > n)
            return s.substring(0, n).concat("...");
        return s;
    }

    public static void saveData(Object object, int string_id) {
        final File file = new File(mResources.getString(string_id));

        try (FileOutputStream fos = new FileOutputStream(file);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(object);
        } catch (Exception i) {
            Log.d("ERROR", String.valueOf(i));
        }
    }

    public static Object getData(int string_id) {
        final File suspend_f = new File(mResources.getString(string_id));

        Object object = null;

        try (FileInputStream fis = new FileInputStream(suspend_f);
             ObjectInputStream is = new ObjectInputStream(fis)) {
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
        try (FileInputStream fis = new FileInputStream(suspend_f);
            ObjectInputStream is = new ObjectInputStream(fis)) {
            token = (String) is.readObject();
        } catch (java.io.FileNotFoundException e) {
            return null;
        } catch (Exception i) {
            Log.d("ERROR", String.valueOf(i));
        }
        return token;
    }

    public static void saveBitmap(Bitmap bitmap, int string_id, Bitmap.CompressFormat compressFormat) {
        final File file = new File(mResources.getString(string_id));

        try (FileOutputStream fos = new FileOutputStream(file);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            boolean success = bitmap.compress(compressFormat, 100, byteStream);
            if (success)
                oos.writeObject(byteStream.toByteArray());
        } catch (Exception i) {
            Log.d("ERROR", String.valueOf(i));
        }
    }

    public static Bitmap getBitmap(int string_id) {
        final File suspend_f = new File(mResources.getString(string_id));
        Bitmap bitmap = null;
        try (FileInputStream fis = new FileInputStream(suspend_f);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            byte[] image = (byte[]) ois.readObject();
            if (image != null && image.length > 0)
                bitmap = BitmapFactory.decodeByteArray(image, 0, image.length);
        } catch (Exception i) {
            Log.d("ERROR", String.valueOf(i));
        }
        return bitmap;
    }

    public Authorization authorize() {
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

    /* String params
     * 0 = requested URL
     * odd = parameter
     * even = value
     */
    public static String fetchPostData(String method, boolean auth, JSONObject json, String... params) {
        HttpURLConnection connection = null;
        BufferedReader reader = null, err_reader;

        try {
            byte[] postDataBytes;
            int postDataLength;
            if (json == null) {
                StringBuilder postData = new StringBuilder();
                for (int i = 1; i < (params.length - 1); i += 2) {
                    postData.append(params[i]).append("=").append(params[i + 1]);
                    if (params.length - 1 - i > 1)
                        postData.append("&");
                }
                postDataBytes = postData.toString().getBytes(StandardCharsets.UTF_8);
                postDataLength = postData.length();
            }
            else {
                postDataBytes = json.toString().getBytes(StandardCharsets.UTF_8);
                postDataLength = json.toString().length();
            }
            URL url = new URL(params[0]);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            if (auth) {
                connection.setRequestProperty(params[1], params[2]);
                connection.setRequestProperty("Content-Type", "application/json");
            }
            else
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("charset", "utf-8");
            connection.setRequestProperty("Content-Length", Integer.toString(postDataLength));
            connection.setInstanceFollowRedirects(false);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.getOutputStream().write(postDataBytes);

            InputStream err = connection.getErrorStream();
            if (err != null) {
                err_reader = new BufferedReader(new InputStreamReader(err));

                StringBuilder errbuffer = new StringBuilder();
                String errline;

                while ((errline = err_reader.readLine()) != null)
                    errbuffer.append(errline).append("\n");

                Log.d("ERR", errbuffer.toString());
                return errbuffer.toString();
            }
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
