package com.example.attendance_monitoring;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;

import static android.content.Context.ALARM_SERVICE;

public class CheckingTime extends BroadcastReceiver {
    public schedule_DBManager sched_db ;
    public student_DBManager sdb;

    @Override
    public void onReceive(Context context, Intent intent) {
        //Toast.makeText(context,"Checking time bitchess ",Toast.LENGTH_SHORT).show();
        sched_db = new schedule_DBManager(context);
        sched_db.open();

        sdb = new student_DBManager(context);
        sdb.open();

        ArrayList<String> id = new ArrayList<>();
        ArrayList<String> una = new ArrayList<>();
        ArrayList<String> tapos = new ArrayList<>();
        ArrayList<String> section_id = new ArrayList<>();
        ArrayList<String> time_late_start = new ArrayList<>();
        ArrayList<String> time_late_end = new ArrayList<>();

        String ids,unas,taposs,section_ids,start_time_late_due,end_time_late_due;

        Cursor cursor = sched_db.getAllActiveSchedule();

        if(cursor.moveToFirst()){
            do{

                ids = cursor.getString(cursor.getColumnIndex("id"));
                unas = cursor.getString(cursor.getColumnIndex("start_time"));
                taposs = cursor.getString(cursor.getColumnIndex("end_time"));
                section_ids = cursor.getString(cursor.getColumnIndex("section_id"));
                start_time_late_due = cursor.getString(cursor.getColumnIndex("start_time_late"));
                end_time_late_due = cursor.getString(cursor.getColumnIndex("end_time_late"));

                id.add(ids);
                una.add(unas);
                tapos.add(taposs);
                section_id.add(section_ids);
                time_late_start.add(start_time_late_due);
                time_late_end.add(end_time_late_due);

            }while (cursor.moveToNext());

            for(int i = 0 ; i < id.size(); i++){
                String[] split = una.get(i).split(":");
                String[] split1 = tapos.get(i).split(":");
                String[] split_late = time_late_start.get(i).split(":");
                String[] split1_late = time_late_end.get(i).split(":");
                startOfTimer(id.get(i),context,split[0],split[1],intent,split[2]);
                endOfTimer(id.get(i),context,split1[0],split1[1],intent,split1[2]);

                startOfTimerLate(id.get(i),context,split_late[0],split_late[1],intent,split_late[2]);
                endOfTimerLate(id.get(i),context,split1_late[0],split1_late[1],intent,split1_late[2]);
            }
        }cursor.close();
    }

    //am = 0
    //pm = 1
    public void startOfTimer(String schedule_id,Context context,String hour,String min,Intent intent,String am_pm){
        intent = new Intent(context, sample.class);
        intent.setAction(Long.toString(System.currentTimeMillis()));
        intent.putExtra("schedule_id",schedule_id);
        int dummyuniqueInt = new Random().nextInt(47523);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, dummyuniqueInt, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Log.v("data","hour "+hour+" min "+min);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR, Integer.parseInt(hour));
        calendar.set(Calendar.MINUTE, Integer.parseInt(min));
        if(am_pm.equals("AM")){
            calendar.set(Calendar.AM_PM,Calendar.AM);
        }else{
            calendar.set(Calendar.AM_PM,Calendar.PM);
        }


        try{
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }catch (IllegalStateException e){
            e.printStackTrace();
        }
    }

    public void endOfTimer(String schedule_id,Context context,String hour,String min,Intent intent,String am_pm){
        intent = new Intent(context, end_sample.class);
        intent.setAction(Long.toString(System.currentTimeMillis()));
        intent.putExtra("schedule_id",schedule_id);
        int dummyuniqueInt = new Random().nextInt(47523);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, dummyuniqueInt, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Log.v("data"," end hour "+hour+" min "+min);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR, Integer.parseInt(hour));
        calendar.set(Calendar.MINUTE, Integer.parseInt(min));
        if(am_pm.equals("AM")){
            calendar.set(Calendar.AM_PM,Calendar.AM);
        }else{
            calendar.set(Calendar.AM_PM,Calendar.PM);
        }

        try{

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);

        }catch (IllegalStateException e){
            e.printStackTrace();
        }
    }

    public void startOfTimerLate(String schedule_id,Context context,String hour,String min,Intent intent,String am_pm){
        intent = new Intent(context, start_sample_late.class);
        intent.setAction(Long.toString(System.currentTimeMillis()));
        intent.putExtra("schedule_id",schedule_id);
        int dummyuniqueInt = new Random().nextInt(47523);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, dummyuniqueInt, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Log.v("data"," start late hour "+hour+" min "+min);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR, Integer.parseInt(hour));
        calendar.set(Calendar.MINUTE, Integer.parseInt(min));
        if(am_pm.equals("AM")){
            calendar.set(Calendar.AM_PM,Calendar.AM);
        }else{
            calendar.set(Calendar.AM_PM,Calendar.PM);
        }

        try{

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);

        }catch (IllegalStateException e){
            e.printStackTrace();
        }
    }

    public void endOfTimerLate(String schedule_id,Context context,String hour,String min,Intent intent,String am_pm){
        intent = new Intent(context, end_sample_late.class);
        intent.setAction(Long.toString(System.currentTimeMillis()));
        intent.putExtra("schedule_id",schedule_id);
        int dummyuniqueInt = new Random().nextInt(47523);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, dummyuniqueInt, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Log.v("data","end late hour "+hour+" min "+min);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR, Integer.parseInt(hour));
        calendar.set(Calendar.MINUTE, Integer.parseInt(min));
        if(am_pm.equals("AM")){
            calendar.set(Calendar.AM_PM,Calendar.AM);
        }else{
            calendar.set(Calendar.AM_PM,Calendar.PM);
        }

        try{
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }catch (IllegalStateException e){
            e.printStackTrace();
        }
    }
}
