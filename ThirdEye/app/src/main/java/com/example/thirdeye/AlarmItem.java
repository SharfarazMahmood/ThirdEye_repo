package com.example.thirdeye;

import androidx.annotation.NonNull;

public class AlarmItem {
    private String activity;
    private String time="00:00";
    private int pending = 0; // 0 = pending, 1= done
    private int notified = 0; // 0 = not notified, 1 = notified

    public AlarmItem(String text) {
        String[] s = text.split(",");
        activity = s[0];
        time = s[1];
        if (s.length == 4){
            pending = Integer.parseInt(s[2]);
            notified = Integer.parseInt(s[3]);
        }
    }

    public String getActivity() {
        return activity;
    }

    public String getTime() {
        return time;
    }

    public int getHour(){
        int hour  = Integer.parseInt(time.substring(0,2));
        return hour;
    }

    public int getPending() {
        return pending;
    }

    public int getNotified() {
        return notified;
    }

    public void setPending() {
        pending = 1;
    }

    public void setNotified() {
        notified = 1;
    }

    @NonNull
    @Override
    public String toString() {
        return activity + "," + time + "," + pending + "," + notified ;
    }


    public String toString_Activity() {
        return activity + "," + time ;
    }
}
