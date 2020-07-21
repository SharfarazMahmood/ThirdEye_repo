package com.example.thirdeye;

import android.app.Application;
import android.content.Intent;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

///////// this class is used for directly taking to the homepage if
//////// an user has a session in the firebase log in
public class HomeCurrentUser extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("log_testing"," main page act onCreate()");

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser mUser = mAuth.getCurrentUser();

        if(mUser != null){
            Intent intent_current_user_homeActivity = new Intent(getApplicationContext() , HomePageActivity.class);
            intent_current_user_homeActivity.addFlags(FLAG_ACTIVITY_NEW_TASK);
        }

    }
}
