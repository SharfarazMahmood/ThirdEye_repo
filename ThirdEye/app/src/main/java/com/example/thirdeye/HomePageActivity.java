package com.example.thirdeye;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class HomePageActivity extends AppCompatActivity implements View.OnClickListener {

    private CardView profileCardView, cameraCardView, activityListCardView, notificationsCardView, settingsCardView, signOutCardView ;
    TextView currentUserEmailTextView;

    private FirebaseAuth mAuth;
    private FirebaseUser mUser;


    ///////Checking?Asking for permission------------------
    private static final String[] PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };
    private static final int PERMISSIONS_COUNT = 3; //////////////should be equal to num of permissions
    private static final int REQUEST_PERMISSIONS = 39;  //////this is a request code

    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean arePermisionsDenied(){
        for (int i=0 ; i < PERMISSIONS_COUNT ; i++){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if( checkSelfPermission(PERMISSIONS[i]) != PackageManager.PERMISSION_GRANTED ){
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if( requestCode == REQUEST_PERMISSIONS && grantResults.length > 0){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if( arePermisionsDenied() ){
                    Intent intent_camToHomePage  = new Intent(getApplicationContext(), HomePageActivity.class);
                    //to resume the main activity use the following line -----------
                    intent_camToHomePage.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent_camToHomePage);
                    finish();
                }else{
                    onResume();
                }
            }
        }
    }
    ///////Checking/Asking for permission ENDED------------------


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_page_activity);

        {////////// homepage cardview
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
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.profileCardViewId:
            { break;}

            case R.id.cameraCardViewId:
            {
/////           takes the user to Camera page -------------------
                //Intent  intent_startCamera  = new Intent(getApplicationContext(), CameraActivityInBuiltApp.class);

                Intent  intent_startCamera  = new Intent(getApplicationContext(), OpenCVCameraActivity.class);
                startActivity(intent_startCamera);
                break;
            }

            case R.id.activityListCardViewId:
            { break;}
            case R.id.notificationsCardViewId:
            { break;}
            case R.id.settingsCardViewId:
            { break;}

            case R.id.signOutCardViewID:
            {
                FirebaseAuth.getInstance().signOut();
                finish();

/////           takes the user to sign in page --------------------
                Intent intent_signOut = new Intent(getApplicationContext(), MainActivity.class);
                intent_signOut.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent_signOut.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);

                startActivity(intent_signOut);
                break;
            }

        }
    }
}