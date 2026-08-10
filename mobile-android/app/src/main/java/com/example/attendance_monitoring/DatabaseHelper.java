package com.example.attendance_monitoring;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.ArrayList;

public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "attendance_monitoring";

    public static final String TABLE_NAME_SECTION = "sections";
    public static final String TABLE_NAME_STUDENT = "students";
    public static final String TABLE_NAME_SCHEDULE = "schedule";
    public static final String TABLE_NAME_DAY = "days";
    public static final String TABLE_NAME_ATTENDANCE_RECORD = "attendance_record";

    public static final String uid = "id";
    public static final String section_name = "sections_name";
    public static final String status_section = "status_section";

    public static final String lastname = "lastname";
    public static final String firstname = "firstname";
    public static final String middlename = "middlename";
    public static final String student_number = "student_number";
    public static final String pick_status = "pick_status";
    public static final String student_status = "student_status";
    public static final String gender = "gender";
    public static final String qr_code_bitmap = "qr_code_bitmap";
    public static final String section_id = "section_id";

    public static final String venue = "venue";

    public static final String title = "title";
    public static final String start_time = "start_time";
    public static final String end_time = "end_time";

    public static final String status_schedule = "status_schedule";
    public static final String status_start = "status_start";
    public static final String status_end = "status_end";
    public static final String start_time_late = "start_time_late";

    public static final String end_time_late = "end_time_late";

    public static final String status_start_late = "status_start_late";
    public static final String status_end_late = "status_end_late";

    public static final String current_day = "current_day";
    public static final String record_day = "record_day";

    public static final String status_record = "status_record";
    public static final String schedule_id = "schedule_id";


    public static final String[] col_record = {uid,schedule_id,section_id,student_number,current_day,status_record,title,start_time,end_time,end_time_late};

    public static final String[] col_schedule = {uid,venue,title,start_time,end_time,section_id,status_schedule,status_start,status_end,start_time_late,end_time_late,status_start_late,status_end_late};

    public static final String[] col_days = {uid,current_day};

    public static final String[] col_student = {uid,lastname,firstname,middlename,gender,qr_code_bitmap,student_number,pick_status,student_status,section_id};

    public static final String[] col_section = {uid,section_name,status_section};

    public static final String Create_table_schedule = "CREATE TABLE "+TABLE_NAME_SCHEDULE+" ("+uid+" INTEGER PRIMARY KEY AUTOINCREMENT , "+
            venue+" VARCHAR(50), "+
            title+" VARCHAR(50), "+
            start_time+" VARCHAR(50), "+
            end_time+" VARCHAR(50), "+
            start_time_late+" VARCHAR(50), "+
            end_time_late+" VARCHAR(50), "+
            section_id+" VARCHAR(50), "+
            status_schedule+" VARCHAR(50), "+
            status_start_late+" VARCHAR(50), "+
            status_end_late+" VARCHAR(50), "+
            status_start+" VARCHAR(50), "+
            status_end+" VARCHAR(50)"+")";

    public static final String Create_table_record = "CREATE TABLE "+TABLE_NAME_ATTENDANCE_RECORD+" ("+uid+" INTEGER PRIMARY KEY AUTOINCREMENT , "+
            schedule_id+" VARCHAR(50), "+
            section_id+" VARCHAR(50), "+
            student_number+" VARCHAR(50), "+
            current_day+" VARCHAR(50), "+
            record_day+" VARCHAR(50), "+
            title+" VARCHAR(50), "+
            start_time+" VARCHAR(50), "+
            end_time+" VARCHAR(50), "+
            end_time_late+" VARCHAR(50), "+
            status_record+" VARCHAR(50)"+")";

    public static final String Create_table_day = "CREATE TABLE "+TABLE_NAME_DAY+" ("+uid+" INTEGER PRIMARY KEY AUTOINCREMENT, "+
            current_day+" VARCHAR(50)"+") ";

    public static final String Create_table_section = "CREATE TABLE "+TABLE_NAME_SECTION+" ("+uid+" INTEGER PRIMARY KEY AUTOINCREMENT , "+
            section_name+" VARCHAR(100),"+
            status_section+" VARCHAR(10)"+ ")";

    public static final String Create_table_students = "CREATE TABLE "+TABLE_NAME_STUDENT+" ("+uid+" INTEGER PRIMARY KEY AUTOINCREMENT , "+
            lastname+" VARCHAR(50), "+
            firstname+" VARCHAR(50), "+
            middlename+" VARCHAR(50), "+
            gender+" VARCHAR(50), "+
            qr_code_bitmap+" VARCHAR(50), "+
            student_number+" VARCHAR(100), "+
            pick_status+" VARCHAR(50), "+
            student_status+" VARCHAR(50), "+
            section_id+" VARCHAR(50)"+")";

    public static final String drop_table_section = "DROP TABLE IF EXISTS "+TABLE_NAME_SECTION+" ";
    public static final String drop_table_student = "DROP TABLE IF EXISTS "+TABLE_NAME_STUDENT+" ";
    public static final String drop_table_schedule = "DROP TABLE IF EXISTS "+TABLE_NAME_SCHEDULE+" ";
    public static final String drop_table_day = "DROP TABLE IF EXISTS "+TABLE_NAME_DAY+" ";
    public static final String drop_table_record = "DROP TABLE IF EXISTS "+TABLE_NAME_ATTENDANCE_RECORD+" ";

    public DatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, 1);
        SQLiteDatabase db = this.getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(Create_table_section);
        db.execSQL(Create_table_students);
        db.execSQL(Create_table_schedule);
        db.execSQL(Create_table_day);
        db.execSQL(Create_table_record);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(drop_table_section);
        db.execSQL(drop_table_student);
        db.execSQL(drop_table_schedule);
        db.execSQL(drop_table_day);
        db.execSQL(drop_table_record);
    }

    public long insertSection(String section_names,String status_sections){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put(section_name,section_names);
        contentValues.put(status_section,status_sections);

        long id = db.insert(TABLE_NAME_SECTION,null,contentValues);
        return id;

    }

    public long importSection(String uidx,String section_names,String status_sections){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put(uid,uidx);
        contentValues.put(section_name,section_names);
        contentValues.put(status_section,status_sections);

        long id = db.insert(TABLE_NAME_SECTION,null,contentValues);
        return id;

    }

    public int Validate_if_Same(String section){
        SQLiteDatabase db = this.getWritableDatabase();
        String[] selectionArgs = {section};
        Cursor cursor = db.query(TABLE_NAME_SECTION,col_section,section_name+"=? ",selectionArgs,null,null,null,null);
        return cursor.getCount();
    }

    public Cursor getAllSection(){
        SQLiteDatabase db = this.getWritableDatabase();
        String[] selectionArgs = {String.valueOf(1)};
        Cursor cursor = db.query(TABLE_NAME_SECTION,col_section,status_section+"=?",selectionArgs,null,null,null,null);
        if(cursor != null){
            cursor.moveToFirst();
        }
        return cursor;
    }

    public Cursor getSectionname(String section_name){
        SQLiteDatabase db = this.getWritableDatabase();
        String[] selectionArgs = {String.valueOf(1),section_name};
        Cursor cursor = db.query(TABLE_NAME_SECTION,col_section,status_section+"=? and "+DatabaseHelper.section_name+"=?",selectionArgs,null,null,null,null);
        if(cursor != null){
            cursor.moveToFirst();
        }
        return cursor;
    }

    public ArrayList<String> getSectionnamebitch(String section_name){
        ArrayList<String> data = new ArrayList<>();
        SQLiteDatabase db = this.getWritableDatabase();
        String[] selectionArgs = {String.valueOf(1),section_name};
        Cursor cursor = db.query(TABLE_NAME_SECTION,col_section,status_section+"=? and "+DatabaseHelper.section_name+"=?",selectionArgs,null,null,null,null);
        while (cursor.moveToNext()){
            String id  = cursor.getString(cursor.getColumnIndex("id"));
            data.add(id);
        }
        return data;
    }

    public ArrayList<String> searchSectionnamebitch (String inputText) throws SQLException{
        ArrayList<String> data = new ArrayList<>();
        Log.v("data","input text "+inputText);
        String pota_section  = inputText.toLowerCase();
        SQLiteDatabase db = this.getWritableDatabase();
        String[] selectionArgs = {inputText};
        String query = "SELECT * FROM sections WHERE sections_name = '"+inputText+"' AND status_section = '1' ";
        Cursor cursor = db.rawQuery(query,null);
        while (cursor.moveToNext()){
            String id  = cursor.getString(cursor.getColumnIndex("id"));
            Log.v("data","id form search bitch ------------------------------- "+id);
            data.add(id);
        }
        return data;
    }


    public int updateSection(String new_section,int idx){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put(section_name,new_section);

        String[] whereArgs = {String.valueOf(idx)};

        int count = db.update(TABLE_NAME_SECTION,contentValues,uid+"=?",whereArgs);
        return count;
    }

    public void removeSection(int idx){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(status_section,0);
        String[] whereArgs = {String.valueOf(idx)};
        db.update(TABLE_NAME_SECTION,contentValues,uid+"=?",whereArgs);
    }

    public Cursor search (String inputText) throws SQLException{
        Log.v("data","input text "+inputText);
        SQLiteDatabase db = this.getWritableDatabase();
        String[] selectionArgs = {inputText};
        String query = "SELECT * FROM sections WHERE sections_name LIKE '%"+inputText+"%' AND status_section = '1' ";
        Cursor cursor = db.rawQuery(query,null);
        if(cursor != null){
            cursor.moveToFirst();
        }
        return cursor;
    }

    public void fucking_delete_all_data(){
        SQLiteDatabase db = this.getWritableDatabase();

        db.delete(DatabaseHelper.TABLE_NAME_SCHEDULE,null,null);
        db.delete(DatabaseHelper.TABLE_NAME_ATTENDANCE_RECORD,null,null);
        db.delete(DatabaseHelper.TABLE_NAME_DAY,null,null);
        db.delete(DatabaseHelper.TABLE_NAME_SECTION,null,null);
        db.delete(DatabaseHelper.TABLE_NAME_STUDENT,null,null);
    }

    public boolean checkDb(String DB_FULL_PATH){
        SQLiteDatabase db = null;
        try{
            db = SQLiteDatabase.openDatabase(DB_FULL_PATH,null,SQLiteDatabase.OPEN_READONLY);
            db.close();
        }catch (SQLiteException e){
            e.printStackTrace();
        }
        return db != null;
    }

    public Cursor selectAllsections(){
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor3 = db.query(TABLE_NAME_SECTION,col_section,null,null,null,null,null,null);
        if(cursor3 != null){
            cursor3.moveToFirst();
        }
        return cursor3;
    }


}
