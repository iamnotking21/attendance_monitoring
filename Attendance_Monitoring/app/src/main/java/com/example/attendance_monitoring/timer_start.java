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

public class timer_start extends BroadcastReceiver {
    public schedule_DBManager sched_db ;
    public student_DBManager sdb;
    public Cursor cursorfix;
    public int val_active;
    @Override
    public void onReceive(Context context, Intent intent) {
        sched_db = new schedule_DBManager(context);
        sched_db.open();

        sdb = new student_DBManager(context);
        sdb.open();

        String student_number = intent.getStringExtra("student_number");

        ArrayList<String> id = new ArrayList<>();
        ArrayList<String> una = new ArrayList<>();
        ArrayList<String> tapos = new ArrayList<>();
        ArrayList<String> section_id = new ArrayList<>();
        ArrayList<String> students = new ArrayList<>();
        ArrayList<String> students_section = new ArrayList<>();

        String ids,unas,taposs,section_ids,estudyante,estudyante_section;

        Cursor cursor = sched_db.getAllActiveSchedule();
        if(cursor.moveToFirst()){
            do {
                ids = cursor.getString(cursor.getColumnIndex("id"));
                unas = cursor.getString(cursor.getColumnIndex("start_time"));
                taposs = cursor.getString(cursor.getColumnIndex("end_time"));
                section_ids = cursor.getString(cursor.getColumnIndex("section_id"));
                id.add(ids);
                una.add(unas);
                tapos.add(taposs);
                section_id.add(section_ids);
            }while (cursor.moveToNext());
           for(int i = 0 ; i < id.size(); i++){
               Cursor cursor1 = sdb.getStudents(section_id.get(i));
               if(cursor1.moveToFirst()){
                   do {
                       estudyante = cursor1.getString(cursor1.getColumnIndex("student_number"));
                       estudyante_section = cursor1.getString(cursor1.getColumnIndex("section_id"));
                       students.add(estudyante);
                       students_section.add(estudyante_section);
                   }while (cursor1.moveToNext());
                   for(int x = 0 ; x < students.size(); x++){
                       if(student_number.equals(students.get(x))){
                           //true
                           Log.v("data","start timer ");
                           for(int y = 0 ; y < id.size(); y++){
                               Log.v("data","lalala ");
                               String[] split = una.get(y).split(":");
                               String[] split1 = tapos.get(y).split(":");

                               try{
                                   cursorfix = sched_db.check_if_start_end_notactive(id.get(y),String.valueOf(0),String.valueOf(0));
                                   val_active = cursorfix.getCount();
                                   cursorfix.close();
                               }finally {
                                   cursorfix.close();
                               }

                               if(val_active==0){
                                   Log.v("data","tangina timer start -------------------------->"+val_active);
                                   startOfTimer(id.get(y),context,split[0],split[1],students_section.get(x),student_number);
                               }else{
                                   Log.v("data","tangina timer end -------------------------->"+val_active);
                                   endOfTimer(id.get(y),context,split1[0],split1[1],students_section.get(x),student_number);
                               }
                           }
                       }else{
                           Log.v("data","don't start timer "+student_number+"  "+students.get(x));
                       }
                   }
               }cursor1.close();
           }
        }cursor.close();
    }


    public void startOfTimer(String schedule_id,Context context,String hour,String min,String section_id,String student_number){
        Log.v("data","start hahahhaha ");
        Intent intent = new Intent(context,do_daily_receiver.class);
        //1 milleseconds
        intent.setAction(Long.toString(1));
        Log.v("data","current millis "+System.currentTimeMillis());

        intent.putExtra("schedule_id",schedule_id);
        intent.putExtra("section_id",section_id);
        intent.putExtra("student_number",student_number);

        int dummyuniqueInt = new Random().nextInt(543254);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, dummyuniqueInt, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        Calendar firingCal= Calendar.getInstance();
        Calendar currentCal = Calendar.getInstance();

        firingCal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hour)); // At the hour you wanna fire
        firingCal.set(Calendar.MINUTE, Integer.parseInt(min)); // Particular minute
        firingCal.set(Calendar.AM_PM, Integer.parseInt(hour) < 12 ? Calendar.AM : Calendar.PM);
        firingCal.setTimeInMillis(System.currentTimeMillis());

        if(firingCal.compareTo(currentCal) < 0) {
            firingCal.add(Calendar.DAY_OF_MONTH, +1);
        }

        Long intendedTime = firingCal.getTimeInMillis();
        Log.v("data","intended time "+intendedTime);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, intendedTime, pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, intendedTime, pendingIntent);
        }

        //alarmManager.setRepeating(AlarmManager.RTC, intendedTime , AlarmManager.INTERVAL_DAY, pendingIntent);

        /*
        Intent intent = new Intent(context,do_daily_receiver.class);
        intent.setAction(Long.toString(System.currentTimeMillis()));

        intent.putExtra("schedule_id",schedule_id);

        int dummyuniqueInt = new Random().nextInt(543254);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(),dummyuniqueInt,intent,PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        */

    }


    public void endOfTimer(String schedule_id,Context context,String hour,String min,String section_id,String student_number){
        Log.v("data"," end hahahhaha ");
        Intent intent = new Intent(context,end_receiver.class);
        //1 milleseconds
        intent.setAction(Long.toString(System.currentTimeMillis()));
        Log.v("data","current millis "+System.currentTimeMillis());

        intent.putExtra("schedule_id",schedule_id);
        intent.putExtra("section_id",section_id);
        intent.putExtra("student_number",student_number);

        int dummyuniqueInt = new Random().nextInt(758854);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, dummyuniqueInt, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        Calendar firingCal= Calendar.getInstance();
        Calendar currentCal = Calendar.getInstance();

        firingCal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hour)); // At the hour you wanna fire
        firingCal.set(Calendar.MINUTE, Integer.parseInt(min)); // Particular minute
        firingCal.set(Calendar.AM_PM, Integer.parseInt(hour) < 12 ? Calendar.AM : Calendar.PM);
        firingCal.setTimeInMillis(System.currentTimeMillis());

        if(firingCal.compareTo(currentCal) < 0) {
            firingCal.add(Calendar.DAY_OF_MONTH, 1);
        }

        Long intendedTime = firingCal.getTimeInMillis();
        Log.v("data","intended time "+intendedTime);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, intendedTime, pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, intendedTime, pendingIntent);
        }

        //alarmManager.setRepeating(AlarmManager.RTC, intendedTime , AlarmManager.INTERVAL_DAY, pendingIntent);

        /*
        Intent intent = new Intent(context,do_daily_receiver.class);
        intent.setAction(Long.toString(System.currentTimeMillis()));

        intent.putExtra("schedule_id",schedule_id);

        int dummyuniqueInt = new Random().nextInt(543254);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(),dummyuniqueInt,intent,PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        */

    }

}
