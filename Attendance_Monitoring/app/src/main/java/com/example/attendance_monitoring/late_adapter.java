package com.example.attendance_monitoring;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class late_adapter extends BaseAdapter {
    Context context;
    LayoutInflater inflater;
    ArrayList<String> id,firstname,lastname,oras_start,oras_end,oras_end_late,title ;

    public late_adapter(Context acontext, ArrayList<String> id,ArrayList<String> firstname,ArrayList<String> lastname,ArrayList<String> title,ArrayList<String> oras_start,ArrayList<String> oras_end,ArrayList<String> oras_end_late){
        this.context = acontext;
        this.id = id;
        this.firstname = firstname;
        this.lastname = lastname;
        this.title = title;
        this.oras_start = oras_start;
        this.oras_end = oras_end;
        this.oras_end_late = oras_end_late;
        inflater = (LayoutInflater.from(context));

    }

    @Override
    public int getCount() {
        return id.size();
    }


    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup parent) {
        view = inflater.inflate(R.layout.late_adapter_lay,null);

        if(id.size()!=0){
            final TextView boys_present = (TextView) view.findViewById(R.id.boys_present);
            final TextView student_numberx = (TextView) view.findViewById(R.id.student_number);
            final TextView schedule = (TextView) view.findViewById(R.id.schedule_title);
            final TextView schedule_time = (TextView) view.findViewById(R.id.schedule_time);

            boys_present.setText(lastname.get(i)+", "+firstname.get(i));
            student_numberx.setText("Student #:"+id.get(i));
            schedule.setText(title.get(i));
            schedule_time.setText(oras_start.get(i)+" - "+oras_end.get(i));
        }else{
            Log.v("data","no record ----------------------------------------xxxxxxxxxxxxxxxx ");
        }

        return view;
    }
}
