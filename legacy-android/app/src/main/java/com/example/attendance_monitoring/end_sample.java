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

public class end_sample extends BroadcastReceiver {
    public schedule_DBManager sched_db;
    public Cursor cursorfix3;
    public int count_val_unactive;
    @Override
    public void onReceive(Context context, Intent intent) {
        sched_db = new schedule_DBManager(context);
        sched_db.open();

        String schedule_id = intent.getStringExtra("schedule_id");
        //Toast.makeText(context,"walang kwenta",Toast.LENGTH_SHORT).show();
        Log.v("data","loser end "+schedule_id);

        try{
            cursorfix3 = sched_db.check_if_start_end_notactive(schedule_id,String.valueOf(0),String.valueOf(0));
            count_val_unactive = cursorfix3.getCount();
            cursorfix3.close();
        }finally {
            cursorfix3.close();
        }

        if(count_val_unactive==0){
            int count_val_update = sched_db.update_status_start_end(schedule_id,String.valueOf(0),String.valueOf(0));
            if(count_val_update!=0){
                Log.v("data","updated successfully end ");
            }else{
                Log.v("data","please try again later ");
            }
        }else{
            Log.v("data","patay na ang alarm");
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
