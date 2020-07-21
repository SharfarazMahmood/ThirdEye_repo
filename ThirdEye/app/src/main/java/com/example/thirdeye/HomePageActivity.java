package com.example.thirdeye;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class HomePageActivity extends AppCompatActivity implements View.OnClickListener {

    private CardView profileCardView, cameraCardView, activityListCardView, notificationsCardView, settingsCardView, signOutCardView ;
    TextView currentUserEmailTextView;

    private FirebaseAuth mAuth;
    private FirebaseUser mUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_page_activity);

        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();

        profileCardView = findViewById(R.id.profileCardViewId);
        cameraCardView = findViewById(R.id.cameraCardViewId);
        activityListCardView = findViewById(R.id.activityListCardViewId);
        notificationsCardView = findViewById(R.id.notificationsCardViewId);
        settingsCardView  = findViewById(R.id.settingsCardViewId);
        signOutCardView  = findViewById(R.id.signOutCardViewID);
        currentUserEmailTextView = findViewById(R.id.currentUserEmailTextViewId);

        profileCardView.setOnClickListener(this); ;
        cameraCardView.setOnClickListener(this);
        activityListCardView.setOnClickListener(this);
        notificationsCardView.setOnClickListener(this);
        settingsCardView.setOnClickListener(this);
        signOutCardView.setOnClickListener(this);

        currentUserEmailTextView.setText(mUser.getEmail());
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.profileCardViewId:
            { break;}
            case R.id.cameraCardViewId:
            {/////takes the user to Camera page -------------------
                //Intent  intent_startCamera  = new Intent(getApplicationContext(), CameraActivityInBuiltApp.class);
                Intent  intent_startCamera  = new Intent(getApplicationContext(), OpenCVCameraActivity.class);
                startActivity(intent_startCamera);
                break; }
            case R.id.activityListCardViewId:
            { break;}
            case R.id.notificationsCardViewId:
            { break;}
            case R.id.settingsCardViewId:
            { break;}
            case R.id.signOutCardViewID:
            {   FirebaseAuth.getInstance().signOut();
                finish();
/////           takes the user to sign in page --------------------
                Intent intent_signOut = new Intent(getApplicationContext(), MainActivity.class);
                intent_signOut.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent_signOut.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);

                startActivity(intent_signOut);
                break; }
        }
    }
}