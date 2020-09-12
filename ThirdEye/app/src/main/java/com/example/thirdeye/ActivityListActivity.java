package com.example.thirdeye;


import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;

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


        File activitiesFile = new File(ActivityListActivity.this.getExternalFilesDir(null) + "/day_" + sdf_day.format(new Date()) + "_activities.txt");
        String str = readFromFile( activitiesFile );
        activityListTextView.setText(str);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopThread = true;
    }

    /////////////////// background threading for activity classification
    class ActivityListThread implements Runnable {
        TextView textView;
        String str;
        ActivityListThread (View view){
            this.textView = (TextView) view;
        }
        @Override
        public void run() {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            File activitiesFile = new File(ActivityListActivity.this.getExternalFilesDir(null) + "/day_" + sdf_day.format(new Date()) + "_activities.txt");
            for (int i = 1; i==1 && !stopThread ; ){
                str = readFromFile( activitiesFile );
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        textView.setText(str);
                    }
                });
            }
        }
    }

    public void startThread(View view) {
        stopThread = false;
        ActivityListThread runnable = new ActivityListThread(view);
        new Thread(runnable).start();
    }

    public void stopThread(View view) {
        stopThread = true;
    }
    /////////////////// threading ended -------------


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