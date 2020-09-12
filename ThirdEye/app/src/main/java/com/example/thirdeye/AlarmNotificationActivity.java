package com.example.thirdeye;


import androidx.appcompat.app.AppCompatActivity;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;


public class AlarmNotificationActivity extends AppCompatActivity {
    public static final String  TAG = "AlarmNotificationActivity";

    private RecyclerView alarmRecyclerView;
    private RecyclerView.Adapter alarmAdapter;
    private RecyclerView.LayoutManager alarmListLayoutManager;

    private FirebaseAuth mAuth;
    private FirebaseUser mUser;

    private ArrayList<AlarmItem> alarmList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.alarm_notification_activity);

        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();


        alarmRecyclerView = findViewById(R.id.recyclerViewId);
        alarmRecyclerView.setHasFixedSize(true);
        alarmListLayoutManager = new LinearLayoutManager(this);
        alarmAdapter = new AlarmAdapter( this ,alarmList);

        alarmRecyclerView.setLayoutManager(alarmListLayoutManager);
        alarmRecyclerView.setAdapter(alarmAdapter);

        loadActivitiList( alarmRecyclerView );
    }

    @SuppressLint("LongLogTag")
    public void loadActivitiList(View view) {
        File activityFile = new File(AlarmNotificationActivity.this.getExternalFilesDir(null) + "/" + mUser.getUid() + "_todos.txt");

        if (!activityFile.exists()) {
            Log.e(TAG, "Error! file not found.");
        } else {
            readFromFile(alarmList, activityFile);
        }
    }

    ///// read pending pending activity notifications from file to in notification page
    private void readFromFile(ArrayList<AlarmItem> alarmList, File activityFile) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(activityFile);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder sb = null;
            String text;

            while ((text = br.readLine()) != null) {
                sb = new StringBuilder();
                sb.append(text);
                AlarmItem alarm = new AlarmItem(sb.toString());
                if (alarm.getNotified() == 0) {
                    alarmList.add(alarm);
                }
//              Log.d(TAG, ": "+ sb.toString() );
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}