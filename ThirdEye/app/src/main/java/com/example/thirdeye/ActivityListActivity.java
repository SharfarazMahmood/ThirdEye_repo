package com.example.thirdeye;


import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;

import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;


public class ActivityListActivity extends AppCompatActivity  {
    private static final String TAG = "ActivityListActivity";
    TextView activityListTextView ;
    private volatile boolean stopThread;
    private Handler mainHandler = new Handler();

    private FirebaseAuth mAuth;
    private FirebaseUser mUser;

    SimpleDateFormat sdf_day = new SimpleDateFormat("dd-MM") ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_activity);

        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();

        activityListTextView = findViewById(R.id.activity_List_Text_viewId);

        loadActivitiList( activityListTextView );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopThread = true;
    }


    public void loadActivitiList(View view) {
        File activitiesFile = new File(ActivityListActivity.this.getExternalFilesDir(null) + "/day_" + sdf_day.format(new Date()) + "_activities.txt");
        if (!activitiesFile.exists()) {
            Log.e(TAG, "Error! file not found.");
        } else {
            String str = readFromFile( activitiesFile );
            activityListTextView.setText(str);
        }

    }

    //// read detected activities from file
    private String readFromFile( File activityFile) {
        StringBuilder sb = null;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream( activityFile );
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            sb = new StringBuilder();
            String text;

            int activityNumber = 1;
            while( (text = br.readLine()) != null ){
                String addNumber = activityNumber + ". " ;
                sb.append(addNumber).append(text).append("\n");
                activityNumber++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if (fis != null){
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return sb.toString();
    }

}