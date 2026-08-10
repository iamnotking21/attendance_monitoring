package com.example.attendance_monitoring;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class student_DBManager {
    private Context context;
    private SQLiteDatabase db;
    private DatabaseHelper myDB;

    public student_DBManager(Context context){
        this.context = context;
    }

    public student_DBManager open() throws SQLException{
        this.myDB = new DatabaseHelper(this.context);
        this.db = this.myDB.getWritableDatabase();
        return this;
    }

    public long insert_Student(int id,String lastnames,String firstnames,String middlenames,String student_numbers,String gender,String status){
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseHelper.lastname,lastnames);
        contentValues.put(DatabaseHelper.firstname,firstnames);
        contentValues.put(DatabaseHelper.middlename,middlenames);
        contentValues.put(DatabaseHelper.gender,gender);
        contentValues.put(DatabaseHelper.student_number,student_numbers);
        contentValues.put(DatabaseHelper.student_status,status);
        contentValues.put(DatabaseHelper.pick_status,"active");
        contentValues.put(DatabaseHelper.section_id,id);

        long count = this.db.insert(DatabaseHelper.TABLE_NAME_STUDENT,null,contentValues);
        return count;
    }

    public long import_Student(String uidx,String lastnames,String firstnames,String middlenames,String gender,String student_numbers,String pickstatus,String status,String sectionid){
        ContentValues contentValues = new ContentValues();

        contentValues.put(DatabaseHelper.uid,uidx);
        contentValues.put(DatabaseHelper.lastname,lastnames);
        contentValues.put(DatabaseHelper.firstname,firstnames);
        contentValues.put(DatabaseHelper.middlename,middlenames);
        contentValues.put(DatabaseHelper.gender,gender);
        contentValues.put(DatabaseHelper.qr_code_bitmap,"");
        contentValues.put(DatabaseHelper.student_number,student_numbers);
        contentValues.put(DatabaseHelper.student_status,status);
        contentValues.put(DatabaseHelper.pick_status,pickstatus);
        contentValues.put(DatabaseHelper.section_id,sectionid);

        long count = this.db.insert(DatabaseHelper.TABLE_NAME_STUDENT,null,contentValues);
        return count;
    }

    public int Validate_same_studentnumber(String student_numbers){
        String[] selectionArgs = {student_numbers,String.valueOf(1)};
        Cursor cursor = this.db.query(DatabaseHelper.TABLE_NAME_STUDENT,DatabaseHelper.col_student,DatabaseHelper.student_number+"=? and "+DatabaseHelper.student_status+"=?",selectionArgs,null,null,null,null);
        return cursor.getCount();
    }

    public int attendance_studentnumber(String student_numbers){
        String[] selectionArgs = {student_numbers,String.valueOf(1)};
        Cursor cursor = this.db.query(DatabaseHelper.TABLE_NAME_STUDENT,DatabaseHelper.col_student,DatabaseHelper.student_number+"=? and "+DatabaseHelper.student_status+"=?",selectionArgs,null,null,null,null);
        return cursor.getCount();
    }

    public Cursor getStudents(String id){
        String[] selectionArgs = {id,String.valueOf(1)};
        Cursor cursor = this.db.query(DatabaseHelper.TABLE_NAME_STUDENT,DatabaseHelper.col_student,DatabaseHelper.section_id+"=? and "+DatabaseHelper.student_status+"=?",selectionArgs,null,null,null,null);
        if(cursor != null){
            cursor.moveToFirst();
        }
        return cursor;
    }

    public Cursor getEstudyante(String student_number){
        String[] selectionArgs = {student_number,String.valueOf(1)};
        Cursor cursor = this.db.query(DatabaseHelper.TABLE_NAME_STUDENT,DatabaseHelper.col_student,DatabaseHelper.student_number+"=? and "+DatabaseHelper.student_status+"=?",selectionArgs,null,null,null,null);
        if(cursor != null){
            cursor.moveToFirst();
        }
        return cursor;
    }

    public Cursor getEstudyanteMale(String student_number){
        String[] selectionArgs = {student_number,String.valueOf(1),"Male"};
        Cursor cursor = this.db.query(DatabaseHelper.TABLE_NAME_STUDENT,DatabaseHelper.col_student,DatabaseHelper.student_number+"=? and "+DatabaseHelper.student_status+"=? and "+DatabaseHelper.gender+"=?",selectionArgs,null,null,null,null);
        if(cursor != null){
            cursor.moveToFirst();
        }
        return cursor;
    }

    public Cursor getEstudyanteMalebySection(String section_id){
        String[] selectionArgs = {section_id,String.valueOf(1),"Male"};
        Cursor cursor = this.db.query(DatabaseHelper.TABLE_NAME_STUDENT,DatabaseHelper.col_student,DatabaseHelper.section_id+"=? and "+DatabaseHelper.student_status+"=? and "+DatabaseHelper.gender+"=?",selectionArgs,null,null,DatabaseHelper.lastname + " ASC",null);
        if(cursor != null){
            cursor.moveToFirst();
        }
        return cursor;
    }

    public Cursor getEstudyanteFemalebySection(String section_id){
        String[] selectionArgs = {section_id,String.valueOf(1),"Female"};
        Cursor cursor = this.db.query(DatabaseHelper.TABLE_NAME_STUDENT,DatabaseHelper.col_student,DatabaseHelper.section_id+"=? and "+DatabaseHelper.student_status+"=? and "+DatabaseHelper.gender+"=?",selectionArgs,null,null,DatabaseHelper.lastname + " ASC",null);
        if(cursor != null){
            cursor.moveToFirst();
        }
        return cursor;
    }

    public Cursor getEstudyanteFemale(String student_number){
        String[] selectionArgs = {student_number,String.valueOf(1),"Female"};
        Cursor cursor = this.db.query(DatabaseHelper.TABLE_NAME_STUDENT,DatabaseHelper.col_student,DatabaseHelper.student_number+"=? and "+DatabaseHelper.student_status+"=? and "+DatabaseHelper.gender+"=?",selectionArgs,null,null,null,null);
        if(cursor != null){
            cursor.moveToFirst();
        }
        return cursor;
    }

    public int getCount_students(String id){
        String[] selectionArgs = {id,String.valueOf(1)};
        Cursor cursor = this.db.query(DatabaseHelper.TABLE_NAME_STUDENT,DatabaseHelper.col_student,DatabaseHelper.section_id+"=? and "+DatabaseHelper.student_status+"=?",selectionArgs,null,null,null,null);
        return cursor.getCount();
    }

    public Cursor get_record_students(int id){
        String[] selectionArgs = {String.valueOf(id),String.valueOf(1)};
        Cursor cursor1 = this.db.query(DatabaseHelper.TABLE_NAME_STUDENT,DatabaseHelper.col_student,DatabaseHelper.uid+"=? and "+DatabaseHelper.student_status+"=?",selectionArgs,null,null,null,null);
        if(cursor1 != null){
            cursor1.moveToFirst();
        }
        return cursor1;
    }

    public Cursor getAllSection(){
        String[] selectionArgs = {String.valueOf(1)};
        Cursor cursor = this.db.query(DatabaseHelper.TABLE_NAME_SECTION,DatabaseHelper.col_section,DatabaseHelper.status_section+"=?",selectionArgs,null,null,null,null);
        if(cursor != null){
            cursor.moveToFirst();
        }
        return cursor;
    }

    public Cursor getidSection(String section_name){
        String[] selectionArgs = {String.valueOf(1),section_name};
        Cursor cursor = this.db.query(DatabaseHelper.TABLE_NAME_SECTION,DatabaseHelper.col_section,DatabaseHelper.status_section+"=? and "+DatabaseHelper.section_name+" =? ",selectionArgs,null,null,null,null);
        if(cursor != null){
            cursor.moveToFirst();
        }
        return cursor;
    }

    public Cursor section_import_student(String section_name){
        String[] selectionArgs = {String.valueOf(1),section_name};
        Cursor cursor = this.db.query(DatabaseHelper.TABLE_NAME_SECTION,DatabaseHelper.col_section,DatabaseHelper.status_section+"=? and "+DatabaseHelper.uid+" =? ",selectionArgs,null,null,null,null);
        if(cursor != null){
            cursor.moveToFirst();
        }
        return cursor;
    }

    public int update_student_record(int ids,String lastnames,String firstnames,String middlenames,String picks,String sections,String gender){
        ContentValues contentValues = new ContentValues();

        contentValues.put(DatabaseHelper.lastname,lastnames);
        contentValues.put(DatabaseHelper.firstname,firstnames);
        contentValues.put(DatabaseHelper.middlename,middlenames);
        contentValues.put(DatabaseHelper.pick_status,picks);
        contentValues.put(DatabaseHelper.section_id,sections);
        contentValues.put(DatabaseHelper.gender,gender);

        String[] whereArgs = {String.valueOf(ids)};
        int count = this.db.update(DatabaseHelper.TABLE_NAME_STUDENT,contentValues,DatabaseHelper.uid+"=?",whereArgs);
        return count;

    }

    public int update_student_number(int ids,String student_numbers){
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseHelper.student_number,student_numbers);
        String[] whereArgs = {String.valueOf(ids)};
        int count = this.db.update(DatabaseHelper.TABLE_NAME_STUDENT,contentValues,DatabaseHelper.uid+"=?",whereArgs);
        return count;

    }

    public int remove_student(int ids){
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseHelper.student_status,0);
        String[] whereArgs = {String.valueOf(ids)};
        int count = this.db.update(DatabaseHelper.TABLE_NAME_STUDENT,contentValues,DatabaseHelper.uid+"=?",whereArgs);
        return count;

    }

    public Cursor search_student(String inputText,String id) throws SQLException{
        Log.v("data","input text "+inputText);
        String[] selectionArgs = {inputText,id};
        String query = "SELECT * FROM students WHERE student_number LIKE '%"+inputText+"%' AND section_id = '"+id+"' AND student_status = '1' ";
        Cursor cursor = this.db.rawQuery(query,null);
        if(cursor != null){
            cursor.moveToFirst();
        }
        return cursor;
    }

    public Cursor selectAllStudents(){
        Cursor cursor3 = this.db.rawQuery("SELECT * FROM students",null);
        if(cursor3 != null){
            cursor3.moveToFirst();
        }
        return cursor3;
    }

}
