package com.example.attendance_monitoring;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class scheduler_adapter_main_lay extends BaseAdapter {

    Context context;
    String id[],section_name[];
    LayoutInflater inflater;

    public scheduler_adapter_main_lay(Context acontext, ArrayList<String> id, ArrayList<String> section_name){
        this.context = acontext;
        this.id = id.toArray(new String[0]);
        this.section_name = section_name.toArray(new String[0]);
        inflater = (LayoutInflater.from(context));
    }

    @Override
    public int getCount() {
        return id.length;
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
    public View getView(final int i, View view, ViewGroup parent) {
        view = inflater.inflate(R.layout.scheduler_adapter_main_lay_design,null);

        final DatabaseHelper db = new DatabaseHelper(view.getRootView().getContext());
        final student_DBManager sdb = new student_DBManager(view.getRootView().getContext());
        sdb.open();

        final schedule_DBManager sched_db = new schedule_DBManager(view.getRootView().getContext());
        sched_db.open();

        final TextView classess = (TextView) view.findViewById(R.id.classess);
        final TextView count_student = (TextView) view.findViewById(R.id.count_student);

        classess.setText(section_name[i].substring(0,1).toUpperCase()+section_name[i].substring(1));

        Cursor cursor = sched_db.getSchedules(id[i]);

        count_student.setText(cursor.getCount()+" Schedules");

        final View finalView = view;
        classess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(finalView.getRootView().getContext(),schedules_list_main.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("id",id[i]);
                intent.putExtra("section_name",section_name[i]);
                finalView.getRootView().getContext().startActivity(intent);
            }
        });

        count_student.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(finalView.getRootView().getContext(),schedules_list_main.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("id",id[i]);
                intent.putExtra("section_name",section_name[i]);
                finalView.getRootView().getContext().startActivity(intent);
            }
        });

        final View finalView1 = view;
        return view;
    }
}
