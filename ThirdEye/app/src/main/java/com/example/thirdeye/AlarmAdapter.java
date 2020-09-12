package com.example.thirdeye;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class AlarmAdapter extends RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder> {
    Context context;
    ArrayList<AlarmItem> alarmsList;

    public AlarmAdapter(Context c, ArrayList<AlarmItem> list) {
        context = c;
        alarmsList = list;
    }

    @NonNull
    @Override
    public AlarmViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.alarm_list_item, parent, false);
        AlarmViewHolder alarmViewHolder = new AlarmViewHolder(itemView);
        return alarmViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull AlarmViewHolder holder, int position) {
        holder.alarmActivityTextView.setText(alarmsList.get(position).getActivity());
        holder.alarmTimeTextView.setText(alarmsList.get(position).getTime());

    }


    @Override
    public int getItemCount() {
        return alarmsList.size();
    }


    class AlarmViewHolder extends RecyclerView.ViewHolder{
        private TextView alarmActivityTextView, alarmTimeTextView, alarmPendingTextView;
        public AlarmViewHolder(View itemView) {
            super(itemView);
            alarmActivityTextView = itemView.findViewById(R.id.alarm_activity_textView_id);
            alarmTimeTextView = itemView.findViewById(R.id.alarm_time_textView_id);
        }
    }

}
