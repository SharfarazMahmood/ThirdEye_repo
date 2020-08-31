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
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.io.IOException;


public class HomePageActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "HomePageActivity";

    /////// Handler is from android class
    private Handler mainHandler = new Handler(); //////// this handler only works with this HomePageActivity class

    // This should be a volatile variable so that the value isn't a cached but instead the most updated one
    private Button buttonStartThread ;

    private volatile boolean stopThread = false;


    private CardView profileCardView, cameraCardView, activityListCardView, notificationsCardView, settingsCardView, signOutCardView ;
    TextView currentUserEmailTextView;

    private FirebaseAuth mAuth;
    private FirebaseUser mUser;

    ////// activity classifire variable
    private volatile boolean runClassifier = false;
    private ActivityClassifier classifier = null;

    private static final String HANDLE_THREAD_NAME = "ActivityClassifyBackground";


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


        ///////// image and data folder create when/if not found
        File folder = new File(String.valueOf(this.getExternalFilesDir(null)));
        File nomediaFile = new File(String.valueOf(this.getExternalFilesDir(null))+"/.nomedia");

        if(!folder.exists()){
            folder.mkdirs();
            Log.e(TAG, "Homepage: folder created: "+folder.getAbsolutePath());
        }
        if(!nomediaFile.exists()){
            try {
                nomediaFile.createNewFile();
                Log.e(TAG, "Homepage: nomedia file created: ");
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Homepage: nomedia file NOT created: "+nomediaFile.getName());
            }
        }

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

            profileCardView.setOnClickListener(this);
            cameraCardView.setOnClickListener(this);
            activityListCardView.setOnClickListener(this);
            notificationsCardView.setOnClickListener(this);
            settingsCardView.setOnClickListener(this);
            signOutCardView.setOnClickListener(this);
            currentUserEmailTextView.setText(mUser.getEmail());
        }


        buttonStartThread = findViewById(R.id.button_start_threadId);

    }


    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.profileCardViewId:
            { break;}
            case R.id.cameraCardViewId:
            {
/////           takes the user to Camera page -------------------
                Intent  intent_startCamera  = new Intent(getApplicationContext(), OpenCVCameraActivity.class);
                startActivity(intent_startCamera);
                break;
            }
            case R.id.activityListCardViewId:
            {
                Intent intent_activityList = new Intent(getApplicationContext(), ActivityListActivity.class);
                startActivity(intent_activityList);
                break;
            }
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

    //////////////////////////////////////////



    @Override
    public void onResume() {
        super.onResume();

        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M   &&   arePermisionsDenied() ){
            requestPermissions(PERMISSIONS, REQUEST_PERMISSIONS);
            return;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {

        runClassifier =  false;
        if (classifier != null){
            classifier.close();
            classifier=null;

        }
        ///////// this is to avoid creating eombe thread
        super.onDestroy();

    }





    ///////// background thread for activity classifier
    class BackgroundThreadImplementsRunnable implements Runnable {
        int seconds ;
        BackgroundThreadImplementsRunnable (int seconds){
            this.seconds= seconds;
        }
        @Override
        public void run() {
            File imgFolder = new File(HomePageActivity.this.getExternalFilesDir(null) + "/imageData/");
            for (int i = 1; i==1 ; ){

                if (runClassifier && buttonStartThread.getText().equals("Start") ) {
                    //// thread messaging with the main thread handler
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            buttonStartThread.setText("Background processing...");
                        }
                    });

                    Log.e(TAG, "Background Activity classifier started.");
                    classifier = new ActivityClassifier(HomePageActivity.this, true);
                    Log.e(TAG, "Background Activity classifier done.");
                    classifier.close();
                    classifier = null;
                }

                if (runClassifier && classifier == null  && imgFolder.listFiles().length > 20) {
                    //// thread messaging with the main thread handler
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            buttonStartThread.setText("Start");
                        }
                    });
                }

                if (!runClassifier && classifier != null){
                    classifier.close();
                    classifier = null;
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            buttonStartThread.setText("Start");
                        }
                    });
                    break;

                }
            }
        }
    }



    public void startThread(View view) {
        stopThread = false;
        runClassifier = true;
        Log.d(TAG, "startThread: mainUIthread homepage 1"+ Thread.currentThread().getName() );

        BackgroundThreadImplementsRunnable runnable = new BackgroundThreadImplementsRunnable(10);
        new Thread(runnable).start();

        Log.d(TAG, "startThread: mainUIthread homepage 2"+ Thread.currentThread().getName() );
    }

    public void stopThread(View view) {
        stopThread = true;
        runClassifier = false;
    }

    /////////////////// threading ended -------------

}