package com.example.thirdeye;

import android.app.Activity;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class FrequnetActivity {
    public static final String TAG = "FrequentActivity";

    private FirebaseAuth mAuth;
    private FirebaseUser mUser;

    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");

    /** label file stored in Assets. */
    private int total_number_of_activities = 13;
    private static final String LABEL_PATH = "labels.txt";
    private static final int RESULTS_TO_SHOW = 3;
    private List<String> labelList;
    private int[][] activityFrequncyArray = new int[1][total_number_of_activities];
    private PriorityQueue<Map.Entry<String, Integer>> sortedActivities =
            new PriorityQueue<>(
                    RESULTS_TO_SHOW,
                    new Comparator<Map.Entry<String, Integer>>() {
                        @Override
                        public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                            return (o1.getValue()).compareTo(o2.getValue());
                        }
                    });

    ArrayList<AlarmItem> alarmList = null;
    int days_count=0;
    ArrayList<ArrayList<AlarmItem>> activitiesPerHour = new ArrayList<>();
    File folder = null;


    public FrequnetActivity(Activity activity){
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();

        //////// load the pre defined activities
        try {
            labelList = loadLabelList(activity);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ////////// initialize an arrylist of 24 arraylist
        for (int i = 0; i <24  ; i++) {
            alarmList = new ArrayList<>();
            activitiesPerHour.add(alarmList);
        }


        //////////  find all the activities file (files with name that starts with "days_")
        folder = new File(activity.getExternalFilesDir(null)+ "");
        File[] activityFiles = folder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String name) {
                return name.startsWith("day_");
            }
        });
        if(activityFiles.length == 0){
            return;
        }
        /////////// sort the activities file accorfing to last modified date
        Arrays.sort(activityFiles, new Comparator<File>() {
            public int compare(File f1, File f2) {
                return Long.compare(f1.lastModified(), f2.lastModified());
            }
        });

        ///////// create arraylist of activities per day
        //////////// and group the activities of all in a "per hour" catagories (a list of 24 list)
        for (days_count = 0; days_count < activityFiles.length; days_count++) {
            Log.d(TAG, ": "+activityFiles[days_count].getName());
            alarmList = new ArrayList<>();
            readFromFile(alarmList, activityFiles[days_count].getAbsoluteFile());
            for (int i = 0; i < alarmList.size(); i++) {
//                Log.d(TAG, ": \t"+ alarmList.get(i).toString_Activity() );
                int hour = alarmList.get(i).getHour();
                activitiesPerHour.get(hour).add(alarmList.get(i));
            }
        }

        ///////// find the THREE most frequent activities per each hour (  )
        for (int i = 0; i < activitiesPerHour.size(); i++) {

            activityFrequncyArray = new int[1][total_number_of_activities];
            for (int j = 0; j < activitiesPerHour.get(i).size(); j++) {
                int index = 0 ;
                index = labelList.indexOf(activitiesPerHour.get(i).get(j).getActivity());
                activityFrequncyArray[0][index]++;
            }

            //// find activity frequency
            List<String> frequentActivityList = printTopKActivities();
            if( frequentActivityList.size()>0 ){
                ArrayList<AlarmItem> alarmList = new ArrayList<>();
                String str = "no_act,99:99";
//                Log.d(TAG, "in hour "+ i +" "+  frequentActivityList);

                for (int j = 0; j < frequentActivityList.size(); j++) {
                    String time = String.format( "%02d", i )+":00" ;
                    str =  frequentActivityList.get(j).split(",")[0] + ","+ time;
                    AlarmItem item = new AlarmItem( str );

//                    Log.d(TAG, "hour "+i+" "+ item.toString() );
                    alarmList.add(item);
                }
                ////// todo_list file
                File totoFile = new File(activity.getExternalFilesDir(null)+"/"+mUser.getUid()+"_todos.txt");
                writeToFile(alarmList, totoFile);
            }
        }
    }






    /** Reads label list from Assets. */
    private List<String> loadLabelList(Activity activity) throws IOException {
        List<String> labelList = new ArrayList<String>();
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(activity.getAssets().open(LABEL_PATH)));
        String line;
        while ((line = reader.readLine()) != null) {
            labelList.add(line);
        }

        reader.close();
        return labelList;
    }

    /** find top-K Activities, to be written as TODOs. */
    private List<String> printTopKActivities() {
        for (int i = 0; i < labelList.size(); ++i) {
            sortedActivities.add(
                    new AbstractMap.SimpleEntry<>(labelList.get(i), activityFrequncyArray[0][i]));
            if (sortedActivities.size() > RESULTS_TO_SHOW) {
                sortedActivities.poll();
            }
        }
        String textToShow = "";
        List<String> most_frequent_k_activities = new ArrayList<String>();
        final int size = sortedActivities.size();
        for (int i = 0; i < size; ++i) {
            Map.Entry<String, Integer> activity = sortedActivities.poll();
            if (activity.getValue()>0){
                most_frequent_k_activities.add(0, String.format( "%s,%d",activity.getKey(), activity.getValue() ));
            }
        }
        return most_frequent_k_activities;
    }


    ///////////// read activities from file
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
//               Log.d(TAG, ">>>" + alarmList.get( alarmList.size()-1 ).toString() );
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


    ///////////// write to file with updated notified alarm list
    private void writeToFile(ArrayList<AlarmItem> alarmList, File f) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(f, true);
            OutputStreamWriter osw = new OutputStreamWriter(fos);
            StringBuilder sb = new StringBuilder();
            for (int itemCount = 0; itemCount < alarmList.size(); itemCount++) {
                AlarmItem alarm = alarmList.get(itemCount);
                sb.append(alarm.toString()).append("\n");
            }
            Log.d(TAG, " write "+sb.toString());
            osw.write(sb.toString());
            osw.flush();
            fos.getFD().sync();
            osw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
