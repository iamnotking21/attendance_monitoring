package com.example.attendance_monitoring;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class schedule_DBManager {
    private Context context;
    private SQLiteDatabase db;
    private DatabaseHelper myDB;

    public schedule_DBManager(Context context){
        this.context = context;
    }

    public schedule_DBManager open() throws SQLException {
        this.myDB = new DatabaseHelper(this.context);
        this.db = this.myDB.getWritableDatabase();
        return this;
    }

    public long insert_schedule(int section_id,String title,String venue,String starttime,String endtime,String status_schedule,String start_late,String end_late){
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseHelper.venue,venue);
        contentValues.put(DatabaseHelper.title,title);
        contentValues.put(DatabaseHelper.start_time,starttime);
        contentValues.put(DatabaseHelper.end_time,endtime);
        contentValues.put(DatabaseHelper.section_id,section_id);
        contentValues.put(DatabaseHelper.status_schedule,status_schedule);
        contentValues.put(DatabaseHelper.start_time_late,start_late);
        contentValues.put(DatabaseHelper.end_time_late,end_late);

        contentValues.put(DatabaseHelper.status_start,"wala");
        contentValues.put(DatabaseHelper.status_end,"wala");
        contentValues.put(DatabaseHelper.status_start_late,"wala");
        contentValues.put(DatabaseHelper.status_end_late,"wala");

        long count = this.db.insert(DatabaseHelper.TABLE_NAME_SCHEDULE,null,contentValues);
        return count;
    }

    public long import_schedule(String uidx,String venue,String title,String starttime,String endtime,String start_late,String end_late,String section_id,String status_schedule,String status_start_latex,String status_end_latex,String status_startx,String status_endx){
        ContentValues contentValues = new ContentValues();

        contentValues.put(DatabaseHelper.uid,uidx);
        contentValues.put(DatabaseHelper.venue,venue);
        contentValues.put(DatabaseHelper.title,title);
        contentValues.put(DatabaseHelper.start_time,starttime);
        contentValues.put(DatabaseHelper.end_time,endtime);
        contentValues.put(DatabaseHelper.section_id,section_id);
        contentValues.put(DatabaseHelper.status_schedule,status_schedule);
        contentValues.put(DatabaseHelper.start_time_late,start_late);
        contentValues.put(DatabaseHelper.end_time_late,end_late);

        contentValues.put(DatabaseHelper.status_start,status_startx);
        contentValues.put(DatabaseHelper.status_end,status_endx);
        contentValues.put(DatabaseHelper.status_start_late,status_start_latex);
        contentValues.put(DatabaseHelper.status_end_late,status_end_latex);

        long count = this.db.insert(DatabaseHelper.TABLE_NAME_SCHEDULE,null,contentValues);
        return count;
    }

    public long insert_current_day(String current_day){
        ContentValues contentValues = new ContentValues();

        contentValues.put(DatabaseHelper.current_day,current_day);

        long count = this.db.insert(DatabaseHelper.TABLE_NAME_DAY,null,contentValues);
        return count;
    }

    public long import_current_day(String uidx,String current_day){
        ContentValues contentValues = new ContentValues();

        contentValues.put(DatabaseHelper.uid,uidx);
        contentValues.put(DatabaseHelper.current_day,current_day);

        long count = this.db.insert(DatabaseHelper.TABLE_NAME_DAY,null,contentValues);
        return count;
    }


    public Cursor selectcurrentday(){
        Cursor cursor = this.db.rawQuery("SELECT * FROM days",null);
        if(cursor != null){
            cursor.moveToFirst();
        }
        return cursor;
    }

    public Cursor getCurrentday(){
        Cursor cursor = this.db.query(DatabaseHelper.TABLE_NAME_DAY,DatabaseHelper.col_days,null,null,null,null,DatabaseHelper.uid + " DESC",null);
        if(cursor != null){
            cursor.moveToFirst();
        }
        return cursor;
    }

    public int valCurrentday(String days){
        String[] selectionArgs = {days};
        Cursor cursor = this.db.query(DatabaseHelper.TABLE_NAME_DAY,DatabaseHelper.col_days,DatabaseHelper.current_day+"=?",selectionArgs,null,null,DatabaseHelper.uid + " DESC",null);
        return cursor.getCount();
    }

    public Cursor getSchedules(String section_id){
        String[] selectionArgs = {section_id,String.valueOf(1)};
        Cursor cursor = this.db.query(DatabaseHelper.TABLE_NAME_SCHEDULE,DatabaseHelper.col_schedule,DatabaseHelper.section_id+"=? and "+DatabaseHelper.status_schedule+"=?",selectionArgs,null,null,null,null);
        if(cursor != null){
            cursor.moveToFirst();
        }
        return cursor;
    }

    public Cursor getfuckingschedules(){
        String[] selectionArgs = {String.valueOf(1)};
        Cursor cursor = this.db.query(DatabaseHelper.TABLE_NAME_SCHEDULE,DatabaseHelper.col_schedule,DatabaseHelper.status_schedule+"=?",selectionArgs,null,null,null,null);
        if(cursor != null){
            cursor.moveToFirst();
        }
        return cursor;
    }

    public Cursor getSchedules_for_adbsent(String schedule_id){
        String[] selectionArgs = {schedule_id,String.valueOf(1)};
        Cursor cursor = this.db.query(DatabaseHelper.TABLE_NAME_SCHEDULE,DatabaseHelper.col_schedule,DatabaseHelper.uid+"=? and "+DatabaseHelper.status_schedule+"=?",selectionArgs,null,null,null,null);
        if(cursor!=null){
            cursor.moveToFirst();
        }
        return cursor;
    }

    public Cursor getAllActiveSchedule(){
        String[] selectionArgs = {String.valueOf(1)};
        Cursor cursor = this.db.query(DatabaseHelper.TABLE_NAME_SCHEDULE,DatabaseHelper.col_schedule,DatabaseHelper.status_schedule+"=?",selectionArgs,null,null,null,null);
        if(cursor != null){
            cursor.moveToFirst();
        }
        return cursor;
    }

    public int update_schedule(int primary_id,String venue,String title,String start_time,String end_time,String start_late,String end_late){
        ContentValues contentValues = new ContentValues();

        contentValues.put(DatabaseHelper.venue,venue);
        contentValues.put(DatabaseHelper.title,title);
        contentValues.put(DatabaseHelper.start_time,start_time);
        contentValues.put(DatabaseHelper.end_time,end_time);
        contentValues.put(DatabaseHelper.start_time_late,start_late);
        contentValues.put(DatabaseHelper.end_time_late,end_late);

        String[] whereArgs = {String.valueOf(primary_id)};

        int count = this.db.update(DatabaseHelper.TABLE_NAME_SCHEDULE,contentValues,DatabaseHelper.uid+"=?",whereArgs);
        return count;
    }

    public void remove_schedule(int primary_id){
        ContentValues contentValues = new ContentValues();

        contentValues.put(DatabaseHelper.status_schedule,String.valueOf(0));

        String[] whereArgs = {String.valueOf(primary_id)};

        this.db.update(DatabaseHelper.TABLE_NAME_SCHEDULE,contentValues,DatabaseHelper.uid+"=?",whereArgs);
    }

    public Cursor search_schedule(String inputText,String section_id) throws SQLException{
        String query = "SELECT * FROM schedule WHERE title LIKE '%"+inputText+"%' AND section_id = '"+section_id+"' AND status_schedule = '1' ";
        Cursor cursor = this.db.rawQuery(query,null);
        if(cursor != null){
            cursor.moveToFirst();
        }
        return cursor;
    }

    public int update_status_start(String schedule_id,String status){
        ContentValues contentValues = new ContentValues();

        contentValues.put(DatabaseHelper.status_start,status);

        String[] whereArgs = {schedule_id};
        int count = this.db.update(DatabaseHelper.TABLE_NAME_SCHEDULE,contentValues,DatabaseHelper.uid+"=?",whereArgs);
        return count;
    }

    public int update_status_start_late(String schedule_id,String status){
        ContentValues contentValues = new ContentValues();

        contentValues.put(DatabaseHelper.status_start_late,status);

        String[] whereArgs = {schedule_id};
        int count = this.db.update(DatabaseHelper.TABLE_NAME_SCHEDULE,contentValues,DatabaseHelper.uid+"=?",whereArgs);
        return count;
    }

    public int update_status_start_end(String schedule_id,String status_start,String status_end){
        ContentValues contentValues = new ContentValues();

        contentValues.put(DatabaseHelper.status_start,status_start);
        contentValues.put(DatabaseHelper.status_end,status_end);

        String[] whereArgs = {schedule_id};

        int count = this.db.update(DatabaseHelper.TABLE_NAME_SCHEDULE,contentValues,DatabaseHelper.uid+"=?",whereArgs);
        return count;
    }

    public int refresh_status_start_late(String schedule_id){
        String[] whereargs = {schedule_id};
        ContentValues contentValues = new ContentValues();

        contentValues.put(DatabaseHelper.status_start,"wala");
        contentValues.put(DatabaseHelper.status_end,"wala");
        contentValues.put(DatabaseHelper.status_start_late,"wala");
        contentValues.put(DatabaseHelper.status_end_late,"wala");

        int count = this.db.update(DatabaseHelper.TABLE_NAME_SCHEDULE,contentValues,DatabaseHelper.uid+"=?",whereargs);
        return count;
    }

    public int update_status_start_end_late(String schedule_id,String status_start,String status_end){
        ContentValues contentValues = new ContentValues();

        contentValues.put(DatabaseHelper.status_start_late,status_start);
        contentValues.put(DatabaseHelper.status_end_late,status_end);

        String[] whereArgs = {schedule_id};

        int count = this.db.update(DatabaseHelper.TABLE_NAME_SCHEDULE,contentValues,DatabaseHelper.uid+"=?",whereArgs);
        return count;

    }

    public Cursor check_if_start_end_notactive(String schedule_id,String status_start,String status_end){
        String[] selectionArgs = {schedule_id,status_start,status_end};
        Cursor cursor = this.db.query(DatabaseHelper.TABLE_NAME_SCHEDULE,DatabaseHelper.col_schedule,DatabaseHelper.uid+"=? and "+DatabaseHelper.status_start+"=? and "+DatabaseHelper.status_end+"=? ",selectionArgs,null,null,null,null);
        return cursor;
    }

    public int check_if_start(String schedule_id,String status_start){
        String[] selectionArgs = {schedule_id,status_start};
        Cursor cursor = this.db.query(DatabaseHelper.TABLE_NAME_SCHEDULE,DatabaseHelper.col_schedule,DatabaseHelper.uid+"=? and "+DatabaseHelper.status_start+"=?",selectionArgs,null,null,null,null);
        return cursor.getCount();
    }

    public int check_if_start_late(String schedule_id,String status_start){
        String[] selectionArgs = {schedule_id,status_start};
        Cursor cursor = this.db.query(DatabaseHelper.TABLE_NAME_SCHEDULE,DatabaseHelper.col_schedule,DatabaseHelper.uid+"=? and "+DatabaseHelper.status_start_late+"=?",selectionArgs,null,null,null,null);
        return cursor.getCount();
    }

    public int check_if_start_end_notactive_late(String schedule_id,String status_start,String status_end){
        String[] selectionArgs = {schedule_id,status_start,status_end};
        Cursor cursor = this.db.query(DatabaseHelper.TABLE_NAME_SCHEDULE,DatabaseHelper.col_schedule,DatabaseHelper.uid+"=? and "+DatabaseHelper.status_start_late+"=? and "+DatabaseHelper.status_end_late+"=? ",selectionArgs,null,null,null,null);
        return cursor.getCount();
    }

    public Cursor selectAllSchedule(){
        Cursor cursor3 = this.db.rawQuery("SELECT * FROM schedule",null);
        if(cursor3 != null){
            cursor3.moveToFirst();
        }
        return cursor3;
    }
}
