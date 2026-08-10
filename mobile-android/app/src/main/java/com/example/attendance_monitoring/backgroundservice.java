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

public class backgroundservice extends BroadcastReceiver {

    static final String ACTION = "android.intent.action.BOOT_COMPLETED";

    @Override
    public void onReceive(Context context, Intent intent) {
        schedule_DBManager sched_db = new schedule_DBManager(context);
        sched_db.open();
        if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())){
            balik(intent,context);
            updateCal(sched_db,context);

            //Toast.makeText(context,"bitchesssss background service ",Toast.LENGTH_SHORT).show();
            Log.v("data","bitchessss");


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
                    Log.v("data","first condition");

                } else {
                    am.set(AlarmManager.RTC_WAKEUP,1, pendingIntent);
                    Log.v("data","second condition");
                }

            } catch (Exception e) {}

        }else{
            balik(intent,context);
            updateCal(sched_db,context);

            //Toast.makeText(context,"bitchesssss background service ",Toast.LENGTH_SHORT).show();
            Log.v("data","bitchessss");


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
                    Log.v("data","third condition");
                    //balik(intent,context);
                } else {
                    am.set(AlarmManager.RTC_WAKEUP,1, pendingIntent);
                    Log.v("data","forth condition");

                }

            } catch (Exception e) {}


        }
    }

    public void balik(Intent intent,Context context){
        Calendar firingCal= Calendar.getInstance();
        try {
            //Create a new PendingIntent and add it to the AlarmManager
            intent = new Intent(context, backgroundservice.class);
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

    public void updateCal(schedule_DBManager sched_db,Context context){
        sched_db = new schedule_DBManager(context);
        sched_db.open();

        Date c = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
        String formattedDate = df.format(c);
        Log.v("data","current date "+formattedDate);

        ArrayList<String> val_date = new ArrayList<>();
        ArrayList<String> schedule_id = new ArrayList<>();

        Cursor cursor = sched_db.getCurrentday();
        Log.v("data","count "+cursor.getCount());

        if(cursor.getCount()==0){

            long count = sched_db.insert_current_day(formattedDate);
            if(count!=0){
                Log.v("data","insert successfully");
            }else{
                Log.v("data","please try again later");
            }

        }else{

            if(cursor.moveToFirst()){
                do {
                    String old_days = cursor.getString(cursor.getColumnIndex("current_day"));
                    Log.v("data","old days "+old_days);
                    val_date.add(old_days);
                }while (cursor.moveToNext());
                int count_val ;
                count_val = sched_db.valCurrentday(formattedDate);
                if(count_val==0){
                    Log.v("data","val "+count_val);

                    long count = sched_db.insert_current_day(formattedDate);
                    if(count!=0){
                        Log.v("data","insert successfully");

                        Cursor cursor1 = sched_db.getfuckingschedules();
                        if(cursor1.moveToFirst()){
                            do {
                                String nakatakda = cursor1.getString(cursor1.getColumnIndex("id"));
                                schedule_id.add(nakatakda);
                            }while (cursor1.moveToNext());

                            for(int y = 0 ; y < schedule_id.size(); y++){
                                int count_val_update = sched_db.refresh_status_start_late(schedule_id.get(y));
                                if(count_val_update!=0){
                                    Log.v("data","successfully refresh ");
                                }else{
                                    Log.v("data","please try again later");
                                }
                            }

                        }cursor1.close();

                    }else{
                        Log.v("data","please try again later");
                    }

                }else{
                    Log.v("data"," not val "+count_val);
                }

            }cursor.close();
        }

    }
}
