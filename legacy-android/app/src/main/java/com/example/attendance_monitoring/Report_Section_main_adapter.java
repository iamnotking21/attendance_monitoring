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

public class Report_Section_main_adapter extends BaseAdapter {
    Context context;
    String id[],section_name[];
    LayoutInflater inflater;
    public record_DBManager record_db;

    public Report_Section_main_adapter(Context acontext, ArrayList<String> id, ArrayList<String> sections_name){
        this.context = acontext;
        this.id = id.toArray(new String[0]);
        this.section_name = sections_name.toArray(new String[0]);
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
        view = inflater.inflate(R.layout.report_section_main_adapter_lay,null);

        record_db = new record_DBManager(view.getRootView().getContext());
        record_db.open();

        final TextView classess = (TextView) view.findViewById(R.id.classess);
        final TextView count_studetnt = (TextView) view.findViewById(R.id.count_student);

        classess.setText(section_name[i]);

        Cursor cursor3 = record_db.get_RecordCount(id[i]);
        count_studetnt.setText(cursor3.getCount()+" Records ");

        final ArrayList<String> sectionx = new ArrayList<>();
        final ArrayList<String> schedulex = new ArrayList<>();

        final View finalView = view;

        if(cursor3.moveToFirst()){
            do {
                String section_id = cursor3.getString(cursor3.getColumnIndex("section_id"));
                String schedule_id = cursor3.getString(cursor3.getColumnIndex("schedule_id"));
                sectionx.add(section_id);
                schedulex.add(schedule_id);
            }while (cursor3.moveToNext());

            for( int s = 0 ; s < sectionx.size(); s++){

                final int finalS = s;
                classess.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(finalView.getRootView().getContext(),Attendance_record_summary_main.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra("section_id",sectionx.get(finalS));
                        intent.putExtra("section_name",section_name[i]);
                        finalView.getRootView().getContext().startActivity(new Intent(intent));
                    }
                });

                final int finalS1 = s;
                count_studetnt.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(finalView.getRootView().getContext(),Attendance_record_summary_main.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra("section_id",sectionx.get(finalS1));
                        intent.putExtra("section_name",section_name[i]);
                        finalView.getRootView().getContext().startActivity(new Intent(intent)); }
                });
            }

        }cursor3.close();

        return view;
    }
}
