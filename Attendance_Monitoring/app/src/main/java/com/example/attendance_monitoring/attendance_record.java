package com.example.attendance_monitoring;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class attendance_record extends BroadcastReceiver {
    public student_DBManager sdb ;
    public schedule_DBManager sched_db;
    public record_DBManager record_db;
    public int myear,mmonth,mday,count_val_same_record;
    public Cursor cursorfix;
    @Override
    public void onReceive(Context context, Intent intent) {
        sdb = new student_DBManager(context);
        sdb.open();

        sched_db = new schedule_DBManager(context);
        sched_db.open();

        record_db = new record_DBManager(context);
        record_db.open();


        Date c = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
        String formattedDate = df.format(c);

        String student_number = intent.getStringExtra("student_number");
        Log.v("data","student number --------------------"+student_number);

        ArrayList<String> klase_name = new ArrayList<>();
        ArrayList<String> present_start = new ArrayList<>();
        ArrayList<String> schedule_id = new ArrayList<>();
        ArrayList<String> late_start = new ArrayList<>();
        ArrayList<String> section_primary_bitch = new ArrayList<>();

        ArrayList<String> oras_start = new ArrayList<>();
        ArrayList<String> oras_end = new ArrayList<>();
        ArrayList<String> oras_title = new ArrayList<>();
        ArrayList<String> oras_end_late = new ArrayList<>();

        final Calendar calendar = Calendar.getInstance();
        myear = calendar.get(Calendar.YEAR);
        mmonth = calendar.get(Calendar.MONTH);
        mday = calendar.get(Calendar.DAY_OF_MONTH);

        String record_day = myear+"-"+(mmonth+1)+"-"+mday;

        Cursor cursor = sdb.getEstudyante(student_number);
        if(cursor.moveToFirst()){
            do {
                String section_id = cursor.getString(cursor.getColumnIndex("section_id"));
                Log.v("data","section id ------------------ "+section_id);
                klase_name.add(section_id);
            }while (cursor.moveToNext());

            for(int i = 0 ; i < klase_name.size(); i++){
                Cursor cursor1 = sched_db.getSchedules(klase_name.get(i));
                if(cursor1.moveToFirst()){
                    do {
                        String id = cursor1.getString(cursor1.getColumnIndex("id"));
                        String status_start = cursor1.getString(cursor1.getColumnIndex("status_start"));
                        String status_start_late = cursor1.getString(cursor1.getColumnIndex("status_start_late"));
                        String section_primary = cursor1.getString(cursor1.getColumnIndex("section_id"));

                        String time_start = cursor1.getString(cursor1.getColumnIndex("start_time"));
                        String time_end = cursor1.getString(cursor1.getColumnIndex("end_time"));
                        String title = cursor1.getString(cursor1.getColumnIndex("title"));
                        String timer_end_late = cursor1.getString(cursor1.getColumnIndex("end_time_late"));

                        present_start.add(status_start);
                        schedule_id.add(id);
                        late_start.add(status_start_late);
                        section_primary_bitch.add(section_primary);

                        oras_start.add(time_start);
                        oras_end.add(time_end);
                        oras_title.add(title);
                        oras_end_late.add(timer_end_late);

                    }while (cursor1.moveToNext());


                    for(int x = 0 ; x < present_start.size() ; x++){

                        try{
                            cursorfix =  record_db.val_record(student_number,schedule_id.get(x),formattedDate);
                            count_val_same_record = cursorfix.getCount();
                            cursorfix.close();
                        }finally {
                            cursorfix.close();
                        }
                        if(count_val_same_record==0){
                            if(!present_start.get(x).equals("wala")){
                                if(present_start.get(x).equals("1")){
                                    //save present record with student number
                                    long present = record_db.insert_record(record_day,schedule_id.get(x),section_primary_bitch.get(x),student_number,formattedDate,"Present",oras_title.get(x),oras_start.get(x),oras_end.get(x),oras_end_late.get(x));

                                    if(present!=0){
                                        Log.v("data","present successfully");
                                    }else{
                                        Log.v("data","please try again later present ");
                                    }
                                }
                            }

                            if(!late_start.get(x).equals("wala")){
                                if(late_start.get(x).equals("1")){
                                    //save late record with student number
                                    long late_bitch = record_db.insert_record(record_day,schedule_id.get(x),section_primary_bitch.get(x),student_number,formattedDate,"Late",oras_title.get(x),oras_start.get(x),oras_end.get(x),oras_end_late.get(x));

                                    if(late_bitch!=0){
                                        Log.v("data","late successfully");
                                    }else{
                                        Log.v("data","please try again later late");
                                    }
                                }
                            }
                        }else{
                            Log.v("data","naka pag attendance na ");
                        }
                    }

                }cursor1.close();
            }

        }cursor.close();
    }
}
