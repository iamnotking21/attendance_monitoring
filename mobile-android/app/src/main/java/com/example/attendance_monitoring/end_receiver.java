package com.example.attendance_monitoring;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

public class end_receiver extends BroadcastReceiver {
    public Vibrator vibrator;
    public record_DBManager record_db;
    public schedule_DBManager sched_db;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v("data","end ------------------------");

        record_db = new record_DBManager(context);
        record_db.open();

        sched_db = new schedule_DBManager(context);
        sched_db.open();

        vibrator = (Vibrator) context
                .getSystemService(Context.VIBRATOR_SERVICE);

        vibrator.vibrate(1000);

        String schedule_id = intent.getStringExtra("schedule_id");
        Log.v("data","schedule id end "+schedule_id);

        int count_val_update = sched_db.update_status_start_end(schedule_id,String.valueOf(0),String.valueOf(0));
        if(count_val_update!=0){
            Log.v("data","updated successfully end ");
            time(context,intent);
        }else{
            Log.v("data","please try again later ");
            time(context,intent);
        }
    }

    public void time(Context context,Intent intent){

        Toast.makeText(context,"attendance", Toast.LENGTH_SHORT).show();
        Log.v("data","hi");
        try {
            //Create a new PendingIntent and add it to the AlarmManager
            intent = new Intent(context, backgroundservice.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                    12345, intent, PendingIntent.FLAG_CANCEL_CURRENT);
            AlarmManager am =
                    (AlarmManager) context.getSystemService(Activity.ALARM_SERVICE);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                am.setExact(AlarmManager.RTC_WAKEUP, 1, pendingIntent);
            } else {
                am.set(AlarmManager.RTC_WAKEUP,1, pendingIntent);
            }

        } catch (Exception e) {}
    }

}
