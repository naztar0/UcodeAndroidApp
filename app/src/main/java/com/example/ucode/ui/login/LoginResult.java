package com.example.ucode.ui.login;

import androidx.annotation.Nullable;

import com.example.ucode.Authorization;

/**
 * Authentication result : success (user details) or error message.
 */
class LoginResult {
    @Nullable
    private Authorization success;
    @Nullable
    private Integer error;

    LoginResult(@Nullable Integer error) {
        this.error = error;
    }

    LoginResult(@Nullable Authorization success) {
        this.success = success;
    }

    @Nullable
    Authorization getSuccess() {
        return success;
    }

    @Nullable
    Integer getError() {
        return error;
    }
}