package com.example.attendance_monitoring;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

import java.util.Calendar;

public class start_sample_late extends BroadcastReceiver {
    public schedule_DBManager sched_db ;
    public Cursor cursor5;
    public int count_val_unactive_late;
    @Override
    public void onReceive(Context context, Intent intent) {
        sched_db = new schedule_DBManager(context);
        sched_db.open();
        String schedule_id = intent.getStringExtra("schedule_id");
        Log.v("data","id late start ----------------------------------------------"+schedule_id);

        if(!schedule_id.equals("")){
            try{

                cursor5 = sched_db.check_if_start_end_notactive(schedule_id,String.valueOf(0),String.valueOf(0));
                count_val_unactive_late = cursor5.getCount();
                cursor5.close();
            }finally {
                cursor5.close();
            }

            if(count_val_unactive_late==1){
                int count_val_start_late = sched_db.check_if_start_late(schedule_id,String.valueOf(1));
                if(count_val_start_late==0){

                    int count = sched_db.update_status_start_late(schedule_id,String.valueOf(1));
                    if(count!=0){
                        Log.v("data","updated start late -----------------");
                    }else{
                        Log.v("data","please try again later");
                    }
                }else{
                    Log.v("data","nag start na ang late");
                }
            }else{
                Log.v("data","patay na ang late --------------------------------- ");
            }

            balik(intent,context);

        }
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
