package com.example.attendance_monitoring;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class students_record_adapter extends BaseAdapter {
    Context context;
    String id[],lastname[],firstname[],middlename[],studentnumber[],pick_status[];
    LayoutInflater inflater;
    String f_id,sections_names,sec_id;

    public students_record_adapter(Context acontext, ArrayList<String> id,ArrayList<String> lastname,ArrayList<String> firstname,ArrayList<String> middlename,ArrayList<String> student_number,ArrayList<String> pick_status,String f_id,String sections_names,String sec_id){
        this.context = acontext;
        this.f_id = f_id;
        this.sections_names = sections_names;
        this.id = id.toArray(new String[0]);
        this.lastname = lastname.toArray(new String[0]);
        this.firstname = firstname.toArray(new String[0]);
        this.middlename = middlename.toArray(new String[0]);
        this.studentnumber = student_number.toArray(new String[0]);
        this.pick_status = pick_status.toArray(new String[0]);
        this.sec_id = sec_id;
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
        view = inflater.inflate(R.layout.students_record_adapter_lay,null);

        final TextView students = (TextView) view.findViewById(R.id.students);
        final TextView student_numbers = (TextView) view.findViewById(R.id.student_number);

        if(!middlename[i].equals("")){
            students.setText(" "+lastname[i].substring(0,1).toUpperCase()+lastname[i].substring(1)+" , "+firstname[i].substring(0,1).toUpperCase()+firstname[i].substring(1)+" "+middlename[i].substring(0,1).toUpperCase()+middlename[i].substring(1));
            student_numbers.setText("Student #: "+studentnumber[i]);
        }else{
            students.setText(" "+lastname[i].substring(0,1).toUpperCase()+lastname[i].substring(1)+" , "+firstname[i].substring(0,1).toUpperCase()+firstname[i].substring(1)+" ");
            student_numbers.setText("Student #: "+studentnumber[i]);
        }

        final View finalView = view;
        student_numbers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent  = new Intent(finalView.getRootView().getContext(),student_info_main.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("id",id[i]);
                intent.putExtra("f_id",f_id);
                intent.putExtra("studentnumber",studentnumber[i]);
                intent.putExtra("section_names",sections_names);
                intent.putExtra("sec_id",sec_id);
                finalView.getRootView().getContext().startActivity(intent);
            }
        });


        students.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent  = new Intent(finalView.getRootView().getContext(),student_info_main.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("id",id[i]);
                intent.putExtra("f_id",f_id);
                intent.putExtra("studentnumber",studentnumber[i]);
                intent.putExtra("section_names",sections_names);
                intent.putExtra("sec_id",sec_id);
                finalView.getRootView().getContext().startActivity(intent);
            }
        });

        return view;
    }

}
