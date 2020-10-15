package com.example.ucode;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.Menu;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ucode.ui.activity.ActivityFragment;
import com.example.ucode.ui.assessments.AssessmentsFragment;
import com.example.ucode.ui.home.HomeFragment;
import com.example.ucode.ui.login.LoginActivity;
import com.example.ucode.ui.my_challenges.MyChallengesFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;
import com.google.gson.Gson;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.view.GravityCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    DrawerLayout drawer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Authorization authorization = getAuthorization();
        if (authorization == null) {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            MainActivity.this.startActivity(intent);
        }

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        drawer = findViewById(R.id.drawer_layout);

        NavigationView navigationView = findViewById(R.id.nav_view);

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_events, R.id.nav_assessments, R.id.nav_my_challenges,
                R.id.nav_all_challenges, R.id.nav_slots, R.id.nav_media, R.id.nav_cluster, R.id.nav_events)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        User user = getData();
        if (user != null) {
            TextView username_text = findViewById(R.id.menu_profile_username);
            username_text.setText(user.USERNAME());
            TextView email_text = findViewById(R.id.menu_profile_email);
            email_text.setText(user.EMAIL());
        }

        return NavigationUI.navigateUp(navController, mAppBarConfiguration) || super.onSupportNavigateUp();
    }

    public User getData() {
        final File suspend_f = new File(getResources().getString(R.string.home_cache_path));

        User user = null;

        try (FileInputStream fis = new FileInputStream(suspend_f); ObjectInputStream is = new ObjectInputStream(fis)) {
            user = (User) is.readObject();
        }
        catch (java.io.FileNotFoundException e) {
            return null;
        }
        catch (Exception i) {
            Log.d("ERROR", String.valueOf(i));
        }

        return user;
    }
    public Authorization getAuthorization() {
        SharedPreferences sharedPreferences = getSharedPreferences("auth", MODE_PRIVATE);
        String username = sharedPreferences.getString("username", null);
        String password = sharedPreferences.getString("password", null);
        if (username == null || password == null)
            return null;
        return new Authorization(username, password);
    }
}