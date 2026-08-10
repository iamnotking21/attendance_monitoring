package com.example.attendance_monitoring;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Vibrator;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class start_late_receiver extends BroadcastReceiver {
    public Vibrator vibrator;
    public record_DBManager record_db;
    public schedule_DBManager sched_db;
    @Override
    public void onReceive(Context context, Intent intent) {
        Date c = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
        String formattedDate = df.format(c);

        record_db = new record_DBManager(context);
        record_db.open();

        sched_db = new schedule_DBManager(context);
        sched_db.open();

        vibrator = (Vibrator) context
                .getSystemService(Context.VIBRATOR_SERVICE);

        vibrator.vibrate(1000);

        String schedule_id = intent.getStringExtra("schedule_id");
        String section_id = intent.getStringExtra("section_id");
        String student_number = intent.getStringExtra("student_number");

        Log.v("data","start late time ");

        if(!schedule_id.equals("") || !section_id.equals("") || !student_number.equals("")){
            int val_active = sched_db.check_if_start_end_notactive_late(schedule_id,String.valueOf(0),String.valueOf(0));
            if(val_active==0){

                int count_update_start = sched_db.update_status_start_late(schedule_id,String.valueOf(1));
                if(count_update_start!=0){
                    Log.v("data","updated succesfully");

                    /*
                    int count_same_val = record_db.val_record(student_number);
                    if(count_same_val==0){

                        long count_val = record_db.insert_record(schedule_id,section_id,student_number,formattedDate,"late");
                        if(count_val != 0 ){
                            Log.v("data","present save successfully ");
                        }else{
                            Log.v("data","please try again later");
                        }
                    }
                    */
                }else{
                    Log.v("data","please try again later ");
                }

            }

        }

    }
}
