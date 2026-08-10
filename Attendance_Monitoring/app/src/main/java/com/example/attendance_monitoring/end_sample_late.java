package com.example.attendance_monitoring;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class end_sample_late extends BroadcastReceiver {
    public schedule_DBManager sched_db;
    public student_DBManager stud_db;
    public record_DBManager record_db;
    public int myear,mmonth,mday,absent_val;
    public Cursor cursor,cursor1,cursor2;
    @Override
    public void onReceive(Context context, Intent intent) {
        sched_db = new schedule_DBManager(context);
        sched_db.open();

        stud_db = new student_DBManager(context);
        stud_db.open();

        record_db = new record_DBManager(context);
        record_db.open();

        Date c = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
        String formattedDate = df.format(c);
        Log.v("data","current date "+formattedDate);

        String schedule_id = intent.getStringExtra("schedule_id");
        Log.v("data","late end id -------------------------------------------------"+schedule_id);
        //Toast.makeText(context,"late end ",Toast.LENGTH_SHORT).show();

        final Calendar calendar = Calendar.getInstance();
        myear = calendar.get(Calendar.YEAR);
        mmonth = calendar.get(Calendar.MONTH);
        mday = calendar.get(Calendar.DAY_OF_MONTH);

        String record_day = myear+"-"+(mmonth+1)+"-"+mday;

        ArrayList<String> section_primary = new ArrayList<>();
        ArrayList<String> student_number = new ArrayList<>();
        ArrayList<String> section_gago = new ArrayList<>();

        ArrayList<String> oras_start = new ArrayList<>();
        ArrayList<String> oras_end = new ArrayList<>();
        ArrayList<String> oras_title = new ArrayList<>();
        ArrayList<String> oras_end_late = new ArrayList<>();


        if(!schedule_id.equals("")){

            int count_val_unactive = sched_db.check_if_start_end_notactive_late(schedule_id,String.valueOf(0),String.valueOf(0));
            if(count_val_unactive==0){
                int count_val_update = sched_db.update_status_start_end_late(schedule_id,String.valueOf(0),String.valueOf(0));
                if(count_val_update!=0){
                    try{
                        cursor = sched_db.getSchedules_for_adbsent(schedule_id);

                        if(cursor.moveToFirst()){
                            do {
                                String section_id = cursor.getString(cursor.getColumnIndex("section_id"));

                                String time_start = cursor.getString(cursor.getColumnIndex("start_time"));
                                String time_end = cursor.getString(cursor.getColumnIndex("end_time"));
                                String title = cursor.getString(cursor.getColumnIndex("title"));
                                String timer_end_late = cursor.getString(cursor.getColumnIndex("end_time_late"));

                                oras_start.add(time_start);
                                oras_end.add(time_end);
                                oras_title.add(title);
                                oras_end_late.add(timer_end_late);

                                section_primary.add(section_id);
                            }while (cursor.moveToNext());

                            for(int i = 0 ; i < section_primary.size(); i++){

                                try{
                                    cursor1 = stud_db.getStudents(section_primary.get(i));

                                    if(cursor1.moveToFirst()){
                                        do {
                                            String student_id = cursor1.getString(cursor1.getColumnIndex("student_number"));
                                            String section_id_pota = cursor1.getString(cursor1.getColumnIndex("section_id"));
                                            student_number.add(student_id);
                                            section_gago.add(section_id_pota);
                                        }while (cursor1.moveToNext());
                                        if(student_number.size()!=0){
                                            for(int a = 0 ; a < student_number.size(); a++){

                                                try{
                                                    cursor2 = record_db.val_record(student_number.get(a),schedule_id,formattedDate);
                                                    absent_val = cursor2.getCount();
                                                    cursor2.close();
                                                }finally {
                                                    cursor2.close();
                                                }

                                                 if(absent_val==0){
                                                    long absent_insert = record_db.insert_record(record_day,schedule_id,section_gago.get(a),student_number.get(a),formattedDate,"Absent",oras_title.get(i),oras_start.get(i),oras_end.get(i),oras_end_late.get(i));
                                                    if(absent_insert!=0){
                                                        Log.v("data","successfully save absent -------------------------------------");
                                                    }else{
                                                        Log.v("data","please try again later------------------------------------");
                                                    }
                                                }else{
                                                    Log.v("data","absent --------------------------------------------------- "+student_number.get(a)+" ----- "+schedule_id);
                                                }
                                            }
                                        }
                                    }cursor1.close();

                                }finally {
                                 cursor1.close();
                                }
                            }
                        }cursor.close();

                    }finally {
                        cursor.close();
                    }

                }else{
                    Log.v("data","please try again later ");
                }
            }else{
            }
        }
        balik(intent,context);
    }

    public void balik(Intent intent,Context context){
        Calendar firingCal= Calendar.getInstance();
        try {
            //Create a new PendingIntent and add it to the AlarmManager
            intent = new Intent(context, CheckingTime.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                    12345, intent, PendingIntent.FLAG_CANCEL_CURRENT);
            AlarmManager am =
                    (AlarmManager) context.getSystemService(Activity.ALARM_SERVICE);
            Long intendedTime = firingCal.getTimeInMillis();

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                am.setExact(AlarmManager.RTC_WAKEUP, 1, pendingIntent);

            } else {
                am.set(AlarmManager.RTC_WAKEUP,1, pendingIntent);
            }

        } catch (Exception e) {}
    }

}
