package com.example.ucode.data;

import android.util.Log;

import com.example.ucode.Authorization;

import org.json.JSONException;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 */
public class LoginDataSource {

    public Result<Authorization> login(String username, String password) throws InterruptedException, ExecutionException, JSONException {

        // handle loggedInUser authentication
        Authorization authorization = new Authorization(username, password);
        authorization.generateAuthToken();
        String token = authorization.getToken();
        if (token != null) {
            return new Result.Success<>(authorization);
        }
        return new Result.Error(new IOException("Error logging in"));
    }

    public void logout() {
        // revoke authentication
    }
}