package com.example.attendance_monitoring;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class record_DBManager {
    private Context context;
    private SQLiteDatabase db;
    private DatabaseHelper myDB;

    public record_DBManager(Context context){
        this.context = context;
    }

    public record_DBManager open() throws SQLException {
        this.myDB = new DatabaseHelper(this.context);
        this.db = this.myDB.getWritableDatabase();
        return this;
    }

    public long insert_record(String record_day,String schedule_id,String section_id,String student_number,String current_day,String status_record,String title,String start_time,String end_time,String end_time_late){
        ContentValues contentValues = new ContentValues();

        contentValues.put(DatabaseHelper.schedule_id,schedule_id);
        contentValues.put(DatabaseHelper.section_id,section_id);
        contentValues.put(DatabaseHelper.student_number,student_number);
        contentValues.put(DatabaseHelper.current_day,current_day);
        contentValues.put(DatabaseHelper.status_record,status_record);
        contentValues.put(DatabaseHelper.title,title);
        contentValues.put(DatabaseHelper.start_time,start_time);
        contentValues.put(DatabaseHelper.end_time,end_time);
        contentValues.put(DatabaseHelper.end_time_late,end_time_late);
        contentValues.put(DatabaseHelper.record_day,record_day);

        long count = this.db.insert(DatabaseHelper.TABLE_NAME_ATTENDANCE_RECORD,null,contentValues);
        return count;

    }

    public long import_record(String pid,String schedule_id,String section_id,String student_number,String current_day,String status_record,String title,String start_time,String end_time,String end_time_late,String record_day){
        ContentValues contentValues = new ContentValues();

        contentValues.put(DatabaseHelper.uid,pid);
        contentValues.put(DatabaseHelper.schedule_id,schedule_id);
        contentValues.put(DatabaseHelper.section_id,section_id);
        contentValues.put(DatabaseHelper.student_number,student_number);
        contentValues.put(DatabaseHelper.current_day,current_day);
        contentValues.put(DatabaseHelper.status_record,status_record);
        contentValues.put(DatabaseHelper.title,title);
        contentValues.put(DatabaseHelper.start_time,start_time);
        contentValues.put(DatabaseHelper.end_time,end_time);
        contentValues.put(DatabaseHelper.end_time_late,end_time_late);
        contentValues.put(DatabaseHelper.record_day,record_day);

        long count = this.db.insert(DatabaseHelper.TABLE_NAME_ATTENDANCE_RECORD,null,contentValues);
        return count;

    }

    public Cursor val_record(String student_number,String scheduleid,String current_day){

        String[] selectionArgs = {student_number,scheduleid,current_day};
        Cursor cursor = this.db.query(DatabaseHelper.TABLE_NAME_ATTENDANCE_RECORD,DatabaseHelper.col_record,DatabaseHelper.student_number+"=? and "+DatabaseHelper.schedule_id+"=? and "+DatabaseHelper.current_day+"=?",selectionArgs,null,null,null,null);
        return cursor;
    }


    public Cursor get_Record (String section_id,String current_day) throws SQLException{
        String[] selectionArgs = {section_id,"Present",current_day};
        Cursor cursor = this.db.query(DatabaseHelper.TABLE_NAME_ATTENDANCE_RECORD,DatabaseHelper.col_record,DatabaseHelper.section_id+"=? and "+DatabaseHelper.status_record+"=? and "+DatabaseHelper.current_day+"=?",selectionArgs,null,null,null);
        if(cursor!=null){
            cursor.moveToFirst();
        }
        return cursor;
    }

    public Cursor get_Recordboys (String section_id) throws SQLException{
        String[] selectionArgs = {section_id};
        Cursor cursor = this.db.query(DatabaseHelper.TABLE_NAME_ATTENDANCE_RECORD,DatabaseHelper.col_record,DatabaseHelper.section_id+"=?",selectionArgs,null,null,null);
        if(cursor!=null){
            cursor.moveToFirst();
        }
        return cursor;
    }


    public Cursor get_RecordCount(String section_id){
        String[] selectionArgs = {section_id};
        Cursor cursor3 = this.db.query(DatabaseHelper.TABLE_NAME_ATTENDANCE_RECORD,DatabaseHelper.col_record,DatabaseHelper.section_id+"=?",selectionArgs,null,null,null);
        if(cursor3 != null){
            cursor3.moveToFirst();
        }
        return cursor3;
    }

    /*
    public Cursor get_Record(String section_id,String daily,String monthly){
        String query = "SELECT * FROM attendance_record WHERE section_id LIKE '%"+section_id+"%' AND current_day LIKE '%"+daily+"%' AND current_day LIKE '%"+monthly+"%' ";
        Cursor cursor = this.db.rawQuery(query,null);
        if(cursor!=null){
            cursor.moveToFirst();
        }
        return cursor;
    }
    */

    public Cursor getCount_present_late_absent(String student_number,String startdate,String enddate){
        String query = "SELECT * FROM attendance_record WHERE student_number = '"+student_number+"' AND record_day BETWEEN '"+startdate+"' AND '"+enddate+"' ";
        Cursor cursor3 = this.db.rawQuery(query,null);
        if(cursor3!=null){
            cursor3.moveToFirst();
        }
        return cursor3;
    }


    public Cursor getCount_present(String student_number,String startdate,String enddate){
        String query = "SELECT * FROM attendance_record WHERE student_number = '"+student_number+"' AND record_day BETWEEN '"+startdate+"' AND '"+enddate+"' AND status_record = 'Present' ";
        Cursor cursor3 = this.db.rawQuery(query,null);
        if(cursor3!=null){
            cursor3.moveToFirst();
        }
        return cursor3;
    }

    public Cursor getCount_late(String student_number,String startdate,String enddate){
        String query = "SELECT * FROM attendance_record WHERE student_number = '"+student_number+"' AND record_day BETWEEN '"+startdate+"' AND '"+enddate+"' AND status_record = 'Late' ";
        Cursor cursor3 = this.db.rawQuery(query,null);
        if(cursor3!=null){
            cursor3.moveToFirst();
        }
        return cursor3;
    }

    public Cursor getCount_absent(String student_number,String startdate,String enddate){
        String query = "SELECT * FROM attendance_record WHERE student_number = '"+student_number+"' AND record_day BETWEEN '"+startdate+"' AND '"+enddate+"' AND status_record = 'Absent' ";
        Cursor cursor3 = this.db.rawQuery(query,null);
        if(cursor3!=null){
            cursor3.moveToFirst();
        }
        return cursor3;
    }


    public Cursor get_RecordLate(String section_id,String current_day){
        String[] selectionArgs = {section_id,"Late",current_day};
        Cursor cursor = this.db.query(DatabaseHelper.TABLE_NAME_ATTENDANCE_RECORD,DatabaseHelper.col_record,DatabaseHelper.section_id+"=? and "+DatabaseHelper.status_record+"=? and "+DatabaseHelper.current_day+"=?",selectionArgs,null,null,null);
        if(cursor!=null){
            cursor.moveToFirst();
        }
        return cursor;
    }

    public Cursor get_RecordbyStudentnumber(String student_number,String current_day){
        String[] selectionArgs = {student_number,"Present",current_day};
        Cursor cursor = this.db.query(DatabaseHelper.TABLE_NAME_ATTENDANCE_RECORD,DatabaseHelper.col_record,DatabaseHelper.student_number+"=? and "+DatabaseHelper.status_record+"=? and "+DatabaseHelper.current_day+"=?",selectionArgs,null,null,null);
        if(cursor!=null){
            cursor.moveToFirst();
        }
        return cursor;
    }

    public Cursor get_Recordlang(String student_number){
        String[] selectionArgs = {student_number};
        Cursor cursor = this.db.query(DatabaseHelper.TABLE_NAME_ATTENDANCE_RECORD,DatabaseHelper.col_record,DatabaseHelper.student_number+"=?",selectionArgs,null,null,null);
        if(cursor!=null){
            cursor.moveToFirst();
        }
        return cursor;
    }

    public Cursor get_RecordbyStudentnumberAbsent(String section_id,String current_day){
        String[] selectionArgs = {section_id,"Absent",current_day};
        Cursor cursor = this.db.query(DatabaseHelper.TABLE_NAME_ATTENDANCE_RECORD,DatabaseHelper.col_record,DatabaseHelper.section_id+"=? and "+DatabaseHelper.status_record+"=? and "+DatabaseHelper.current_day+"=?",selectionArgs,null,null,null);
        if(cursor!=null){
            cursor.moveToFirst();
        }
        return cursor;
    }

    public Cursor get_RecordbyStudentnumberAbsentnumber(String student_number,String current_day){
        String[] selectionArgs = {student_number,"Absent",current_day};
        Cursor cursor = this.db.query(DatabaseHelper.TABLE_NAME_ATTENDANCE_RECORD,DatabaseHelper.col_record,DatabaseHelper.student_number+"=? and "+DatabaseHelper.status_record+"=? and "+DatabaseHelper.current_day+"=?",selectionArgs,null,null,null);
        if(cursor!=null){
            cursor.moveToFirst();
        }
        return cursor;
    }

    public Cursor get_RecordbyStudentnumberLate(String student_number,String current_day){
        String[] selectionArgs = {student_number,"Late",current_day};
        Cursor cursor = this.db.query(DatabaseHelper.TABLE_NAME_ATTENDANCE_RECORD,DatabaseHelper.col_record,DatabaseHelper.student_number+"=? and "+DatabaseHelper.status_record+"=? and "+DatabaseHelper.current_day+"=?",selectionArgs,null,null,null);
        if(cursor!=null){
            cursor.moveToFirst();
        }
        return cursor;
    }

    public Cursor selectAllrecord(){
        Cursor cursor3 = this.db.rawQuery("SELECT * FROM attendance_record",null);
        if(cursor3 != null){
            cursor3.moveToFirst();
        }
        return cursor3;
    }

}
