package com.example.thirdeye;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;


public class HomePageActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "HomePageActivity";


    private CardView profileCardView, cameraCardView, activityListCardView;
    private CardView notificationsCardView, settingsCardView, signOutCardView;
    TextView currentUserEmailTextView;

    private NotificationManagerCompat notificationManagerCompat;
    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
    SimpleDateFormat sdf_day = new SimpleDateFormat("dd-MM");

    private FirebaseAuth mAuth;
    private FirebaseUser mUser;

    private Button buttonStartThread;
    private volatile boolean stopThread = false;
    private volatile boolean runClassifier = false;
    private ActivityClassifier classifier = null;
    /////// Handler is from android class
    private Handler mainHandler = new Handler(); //////// this handler only works with this HomePageActivity class

    private FrequnetActivity frequnetActivity = null;

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

        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();

        ///////// data and image folder create when/if not found
        File folder = new File(String.valueOf(this.getExternalFilesDir(null) ));
        File nomediaFile = new File(this.getExternalFilesDir(null) + "/.nomedia");

        createFilesOrDirectory(folder);
        createFilesOrDirectory(nomediaFile);

        {////////// homepage card view objects initialization
            profileCardView = findViewById(R.id.profileCardViewId);
            cameraCardView = findViewById(R.id.cameraCardViewId);
            activityListCardView = findViewById(R.id.activityListCardViewId);
            notificationsCardView = findViewById(R.id.notificationsCardViewId);
            settingsCardView = findViewById(R.id.settingsCardViewId);
            signOutCardView = findViewById(R.id.signOutCardViewID);
            currentUserEmailTextView = findViewById(R.id.currentUserEmailTextViewId);

            buttonStartThread = findViewById(R.id.button_start_threadId);

            profileCardView.setOnClickListener(this);
            cameraCardView.setOnClickListener(this);
            activityListCardView.setOnClickListener(this);
            notificationsCardView.setOnClickListener(this);
            settingsCardView.setOnClickListener(this);
            signOutCardView.setOnClickListener(this);
            currentUserEmailTextView.setText(mUser.getEmail());
        }

        //// create a notification manager
        notificationManagerCompat = NotificationManagerCompat.from(this);
        //// start all the threads
        startThread(buttonStartThread);

    }

    @Override
    public void onResume() {
        stopThread = false;
        runClassifier = true;
        super.onResume();
        //////// check permission based on android version
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && arePermisionsDenied() ){
            requestPermissions(PERMISSIONS, REQUEST_PERMISSIONS);
            return;
        }
    }

    @Override
    protected void onDestroy() {
        stopThread = true;
        runClassifier = false;
        if (classifier != null) {
            classifier.close();
            classifier = null;
        }

        super.onDestroy();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.profileCardViewId: {
                break;
            }
            case R.id.cameraCardViewId: {
/////           takes the user to Camera page -------------------
                Intent intent_startCamera = new Intent(getApplicationContext(), OpenCVCameraActivity.class);
                startActivity(intent_startCamera);
                break;
            }
            case R.id.activityListCardViewId: {
                Intent intent_activityList = new Intent(getApplicationContext(), ActivityListActivity.class);
                startActivity(intent_activityList);
                break;
            }
            case R.id.notificationsCardViewId: {
                Intent intent_alarmNotificationList = new Intent(getApplicationContext(), AlarmNotificationActivity.class);
                startActivity(intent_alarmNotificationList);
                break;
            }
            case R.id.settingsCardViewId: {
                break;
            }
            case R.id.signOutCardViewID: {
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
    private void createFilesOrDirectory(File f) {
        if (f.isDirectory() && !f.exists()) {
            f.mkdirs();
            Log.e(TAG, " folder created: " + f.getAbsolutePath());
        }
        else if (!f.exists()) {
            try {
                f.createNewFile();
                Log.e(TAG, " file created: "+ f.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, " file NOT created: " + f.getAbsolutePath());
            }
        }
    }

    ///////////// send notifications using the app notification channel
    public void sendNotification(String a, String t, int id) {
        Notification notification = new NotificationCompat.Builder(this, App.CHANNEL_1_ID)
                .setSmallIcon(R.drawable.ic_round_notification_important_24)
                .setContentTitle(a)
                .setContentText("missed at " + t)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
                .setGroup("Activity missed")
                .build();
        notificationManagerCompat.notify(id, notification);
    }

    ///////////// check alarm list to send notification and update alarmlist
    private void checkToSendNotification(ArrayList<AlarmItem> list) {
        try {
            Calendar current = Calendar.getInstance();
            Calendar within = Calendar.getInstance();
            within.add(Calendar.MINUTE, 12);

            Date currentTime = sdf.parse(current.get(Calendar.HOUR_OF_DAY) + ":" + current.get(Calendar.MINUTE));
            Date withinTime = sdf.parse(within.get(Calendar.HOUR_OF_DAY) + ":" + within.get(Calendar.MINUTE));
            Date alarmTime ;
            for (int itemCount = 0; itemCount < list.size(); itemCount++) {
                AlarmItem alarm = list.get(itemCount);
                alarmTime = sdf.parse(alarm.getTime());
                // getPending:: 0 = pending, 1= done
                // getNotified:: 0 = not notified, 1 = notified
                if (( /*alarm.getPending() == 0 && */alarm.getNotified() == 0) &&
                        ( alarmTime.before(currentTime) && alarmTime.before(withinTime))) {
                    sendNotification(alarm.getActivity(), alarm.getTime(), itemCount);
                    list.get(itemCount).setNotified();
                    Log.d(TAG,  alarmTime + " " +currentTime);
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    ///////////// check alarm list to send notification and update alarmlist
    private void checkIfActivityIsDetected(ArrayList<AlarmItem> list) {

        ///create activity list from today's activity file
        ArrayList<AlarmItem> todaysActivitiesList = new ArrayList<>();
        File todaysActivitiesFile = new File(HomePageActivity.this.getExternalFilesDir(null) + "/day_" + sdf_day.format(new Date()) + "_activities.txt");
        readFromFile(todaysActivitiesList, todaysActivitiesFile);

        try {
            for (int todoCount = 0; todoCount < list.size(); todoCount++) {
                Date todoTime ;
                AlarmItem todo = list.get(todoCount);
                todoTime = sdf.parse(todo.getTime());

                Log.d(TAG, "todo: "+todo.getActivity() +","+ todo.getPending());
                for (int activityCount = 0; activityCount < todaysActivitiesList.size(); activityCount++) {
                    Date aTime, withinStart, withinEnd;
                    AlarmItem a = todaysActivitiesList.get(activityCount);

                    if(todo.getActivity().equals(a.getActivity())){
                        aTime = sdf.parse(a.getTime());
                        Calendar c = Calendar.getInstance();//// finding a range around activity time
                        c.setTime(todoTime);
                        c.add(Calendar.MINUTE, -10); /// take the time 10 minutes before todoTime
                        withinStart = sdf.parse(c.get(Calendar.HOUR_OF_DAY) + ":" + c.get(Calendar.MINUTE));
                        c.add(Calendar.MINUTE, 20); /// take the time 20 minutes after todoTime
                        withinEnd = sdf.parse(c.get(Calendar.HOUR_OF_DAY) + ":" + c.get(Calendar.MINUTE));
                        ///////// check if the activity time is within the range of todoTime
//                            getPending:: 0 = pending, 1= done
//                            getNotified:: 0 = not notified, 1 = notified
                        if ( aTime.after(withinStart) && aTime.before(withinEnd) ) {
                            list.get(todoCount).setPending();
                            list.get(todoCount).setNotified();
                            Log.d(TAG,  todoTime + ">>>> at >>>>" +aTime);
                            Log.d(TAG,  withinStart + ">>>> within >>>>" +withinEnd);
                        }
                        Log.d(TAG, "activity: "+a.getActivity());
                    }else{
                        Log.d(TAG, ": no match");
                    }
                }
                Log.d(TAG, "todo: "+todo.getActivity() +","+ todo.getPending());


            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    ///////////// write to file with updated notified alarm list
    private void writeToFile(ArrayList<AlarmItem> alarmList, File f) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(f);
            OutputStreamWriter osw = new OutputStreamWriter(fos);
            StringBuilder sb = new StringBuilder();
            for (int itemCount = 0; itemCount < alarmList.size(); itemCount++) {
                AlarmItem alarm = alarmList.get(itemCount);
                sb.append(alarm.toString()).append("\n");
            }
//            Log.d(TAG, "notifThread : alarm write "+sb.toString() + itemCount);
            osw.write(sb.toString());
            osw.flush();
            fos.getFD().sync();
            osw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    ///////////// read notification alarms from a text file to given arraylist
    private void readFromFile(ArrayList<AlarmItem> alarmList, File f) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(f);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder sb ;
            String text;
            while ((text = br.readLine()) != null ) {
                sb = new StringBuilder();
                sb.append(text);
                alarmList.add(new AlarmItem(sb.toString()));
//                Log.d(TAG, ">>>" + alarmList.get( alarmList.size()-1 ).toString() );
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
    //////////////////////////////////////////




    /////////////////// background threading for activity classification
    class ActivityClassifyThread implements Runnable {
        @Override
        public void run() {
            File imgFolder = new File(HomePageActivity.this.getExternalFilesDir(null) + "/imageData/");
            for (int i = 1; i == 1; ) {
                if (runClassifier && buttonStartThread.getText().equals("Start") ) {
                    //// thread messaging with the main thread handler
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            buttonStartThread.setText("Background processing...");
                        }
                    });

//                    Log.e(TAG, "Background Activity classifier started.");
                    classifier = new ActivityClassifier(HomePageActivity.this, true);
//                    Log.e(TAG, "Background Activity classifier done.");
                    classifier.close();
                    classifier = null;
                }
                if (runClassifier && classifier == null && imgFolder.listFiles().length > 20) {
                    //// thread messaging with the main thread handler
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            buttonStartThread.setText("Start");
                        }
                    });
                }
                if (!runClassifier && classifier != null) {
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

    class NotificationThread implements Runnable {
        Activity activity;
        public NotificationThread(Activity activity) {
            this.activity = activity;
        }

        @Override
        public void run() {
            for (int i = 1; i == 1 && stopThread == false; ) {
                ArrayList<AlarmItem> todoList = new ArrayList<>();

                File todoFile = new File(HomePageActivity.this.getExternalFilesDir(null) + "/" + mUser.getUid() + "_todos.txt");
                try {
                    Thread.sleep(2000);
                    if (!todoFile.exists()) {
                        Log.e(TAG, "Error! file not found.");
                    } else {
                        readFromFile(todoList, todoFile);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                checkIfActivityIsDetected(todoList);

                checkToSendNotification(todoList);

                writeToFile(todoList, todoFile);
            }
        }
    }

    class FrequentActivityThread implements Runnable{
        Activity activity;
        public FrequentActivityThread(Activity activity) {
            this.activity = activity;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(2000);

                File todoFile = new File(activity.getExternalFilesDir(null) + "/" + mUser.getUid() + "_todos.txt");
                if( todoFile.exists() ){
                    todoFile.delete();
                }
                if (!todoFile.exists()){
                    todoFile.createNewFile();
                    frequnetActivity = new FrequnetActivity(activity);
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ///////// here we can delete all previous todoFiles if needed0
        }
    }

    public void startThread(View view) {
        stopThread = false;
        runClassifier = true;

        ActivityClassifyThread runnable_activityClassifier = new ActivityClassifyThread();
        new Thread(runnable_activityClassifier).start();

        FrequentActivityThread runnable_FrequentActivity = new FrequentActivityThread(this);
        Thread t = new Thread(runnable_FrequentActivity);
        t.start();

        while( t.isAlive() ){ }

        NotificationThread runnable_notification = new NotificationThread(this);
        new Thread(runnable_notification).start();


    }

    public void stopThread(View view) {
        stopThread = true;
        runClassifier = false;
    }

    /////////////////// threading ended -------------

}