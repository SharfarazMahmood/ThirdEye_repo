package com.example.thirdeye;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class ActivityListActivity extends AppCompatActivity {
    private static final String TAG = "ActivityListActivity";
    TextView activityListTextrView ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_activity);


        activityListTextrView = findViewById(R.id.activity_List_Text_viewId);
    }

    public void loadActivitiList(View view) {
        FileInputStream fis = null;
        File ActivityFile = new File(ActivityListActivity.this.getExternalFilesDir(null)+"/ActivityFile.txt");
        try {
            fis = new FileInputStream( ActivityFile );
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String text;

            int activityNumber = 1;
            while( (text = br.readLine()) != null ){
                String addNumber = String.valueOf(activityNumber + ". ") ;
                sb.append(addNumber).append(text).append("\n");
                activityNumber++;
            }
            activityListTextrView.setText(sb.toString());

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

    }
}