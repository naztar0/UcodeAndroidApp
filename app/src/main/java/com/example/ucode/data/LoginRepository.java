package com.example.ucode.data;


import com.example.ucode.Authorization;

import org.json.JSONException;

import java.util.concurrent.ExecutionException;

/**
 * Class that requests authentication and user information from the remote data source and
 * maintains an in-memory cache of login status and user credentials information.
 */
public class LoginRepository {

    private static volatile LoginRepository instance;

    private LoginDataSource dataSource;

    // If user credentials will be cached in local storage, it is recommended it be encrypted
    // @see https://developer.android.com/training/articles/keystore
    private Authorization authorization = null;

    // private constructor : singleton access
    private LoginRepository(LoginDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public static LoginRepository getInstance(LoginDataSource dataSource) {
        if (instance == null) {
            instance = new LoginRepository(dataSource);
        }
        return instance;
    }

    public boolean isLoggedIn() {
        return authorization != null;
    }

    public void logout() {
        authorization = null;
        dataSource.logout();
    }

    private void setLoggedInUser(Authorization authorization) {
        this.authorization = authorization;
        // If user credentials will be cached in local storage, it is recommended it be encrypted
        // @see https://developer.android.com/training/articles/keystore
    }

    public Result<Authorization> login(String username, String password) throws InterruptedException, ExecutionException, JSONException {
        // handle login
        Result<Authorization> result = dataSource.login(username, password);
        if (result instanceof Result.Success) {
            setLoggedInUser(((Result.Success<Authorization>) result).getData());
        }
        return result;
    }
}