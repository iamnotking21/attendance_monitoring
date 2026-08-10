package com.example.attendance_monitoring;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.opencsv.CSVWriter;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class Attendance_record_summary_main extends AppCompatActivity {
    private TextView textView4,startdate,enddate;
    private ImageView back;
    public record_DBManager record_db;
    public student_DBManager stud_db;
    public boys_attendance_record_summary_adapter boys_attendance;
    public girls_attendance_record_summary_adapter girls_attendance;
    public ListView boys_present,girls_present;
    public Button startmonth,endmonth,exportfile;
    public int myear,mmonth,mday;
    public DatePickerDialog datePickerDialog,datePickerDialog1;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.attendance_record_summary_main_lay);

        record_db = new record_DBManager(Attendance_record_summary_main.this);
        record_db.open();

        stud_db = new student_DBManager(Attendance_record_summary_main.this);
        stud_db.open();

        textView4 = (TextView) findViewById(R.id.textView4);
        back = (ImageView) findViewById(R.id.back);

        startmonth = (Button) findViewById(R.id.startmonth);
        endmonth = (Button) findViewById(R.id.endmonth);
        exportfile = (Button) findViewById(R.id.exportfile);
        startdate = (TextView) findViewById(R.id.startdate);
        enddate = (TextView) findViewById(R.id.enddate);

        boys_present = (ListView) findViewById(R.id.list_view_section_name);
        girls_present = (ListView) findViewById(R.id.list_view_section_name_girls);

        Date c = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
        String formattedDate = df.format(c);


        final Calendar calendar = Calendar.getInstance();
        myear = calendar.get(Calendar.YEAR);
        mmonth = calendar.get(Calendar.MONTH);
        mday = calendar.get(Calendar.DAY_OF_MONTH);

        startdate.setText(myear+"-"+(mmonth+1)+"-"+mday);
        enddate.setText(myear+"-"+(mmonth+1)+"-"+mday);


        Intent intent = getIntent();
        final String section_id = intent.getStringExtra("section_id");
        final String section_name = intent.getStringExtra("section_name");

        textView4.setText(section_name);

        exportfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fun_export_csv(section_id,section_name);
            }
        });

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Attendance_record_summary_main.this,Report_Section_main.class));
            }
        });

        startmonth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                datePickerDialog = new DatePickerDialog(Attendance_record_summary_main.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        startdate.setText(year+"-"+(month+1)+"-"+dayOfMonth);
                    }
                },myear,mmonth,mday);
                datePickerDialog.show();

                datePickerDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        Log.v("data","tanginaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa ");
                        fun_boys_list(section_id);
                        fun_girls_list(section_id);
                    }
                });

            }
        });

        endmonth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                datePickerDialog1 = new DatePickerDialog(Attendance_record_summary_main.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        enddate.setText(year+"-"+(month+1)+"-"+dayOfMonth);
                    }
                },myear,mmonth,mday);
                datePickerDialog1.show();

                datePickerDialog1.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        Log.v("data","hello world graduateeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee na  ");
                        fun_boys_list(section_id);
                        fun_girls_list(section_id);
                    }
                });
            }
        });
    }

    public void fun_export_csv(String section_id,String section_name){
        final String csv = (Environment.getExternalStorageDirectory().getAbsolutePath() + "/Download/"+""+section_name+"_"+startdate.getText().toString()+"_"+enddate.getText().toString()+".csv"); // Here csv file name is MyCsvFile.csv
        Log.v("data","file path "+csv);


        final ArrayList<String> present_student_lname = new ArrayList<>();
        final ArrayList<String> present_student_fname = new ArrayList<>();
        final ArrayList<String> present_student_number_bull = new ArrayList<>();
        final ArrayList<String> present_student_middlename = new ArrayList<>();
        final ArrayList<String> present_student_gender = new ArrayList<>();

        CSVWriter writer;

        final List<String[]> data_csv = new ArrayList<String[]>();
        data_csv.add(new String[]{"Student Number","Lastname","Firstname","Middlename","Gender","# of Present","# of Late","# of Absent"});

        Cursor cursor3 = stud_db.getEstudyanteMalebySection(section_id);
        if(cursor3.moveToFirst()){
            do {
                String lname_boy = cursor3.getString(cursor3.getColumnIndex("lastname"));
                String fname_boy = cursor3.getString(cursor3.getColumnIndex("firstname"));
                String student_num = cursor3.getString(cursor3.getColumnIndex("student_number"));
                String middlename_boys = cursor3.getString(cursor3.getColumnIndex("middlename"));
                String gender_boys = cursor3.getString(cursor3.getColumnIndex("gender"));

                present_student_number_bull.add(student_num);
                present_student_lname.add(lname_boy);
                present_student_fname.add(fname_boy);
                present_student_middlename.add(middlename_boys);
                present_student_gender.add(gender_boys);

                Cursor cursor4 = record_db.getCount_present(student_num,startdate.getText().toString(),enddate.getText().toString());

                Cursor cursor5 = record_db.getCount_late(student_num,startdate.getText().toString(),enddate.getText().toString());

                Cursor cursor6 = record_db.getCount_absent(student_num,startdate.getText().toString(),enddate.getText().toString());
                Log.v("data boys ","count present ------"+cursor4.getCount()+"count late ------"+cursor5.getCount()+"count absent ------"+cursor6.getCount()+" student num ------"+student_num+" lastname ------"+lname_boy+" firstname ------"+fname_boy+" middlename ------"+middlename_boys+" gender ------"+gender_boys);

                //data_csv.add(student_num+","+lname_boy+","+fname_boy+","+middlename_boys+","+gender_boys+","+cursor4.getCount()+","+cursor5.getCount()+","+cursor6.getCount());
                data_csv.add(new String[]{student_num,lname_boy,fname_boy,middlename_boys,gender_boys, String.valueOf(cursor4.getCount()),String.valueOf(cursor5.getCount()),String.valueOf(cursor6.getCount())});

                writer = null;
                try {
                    writer = new CSVWriter(new FileWriter(csv));
                    writer.writeAll(data_csv);
                }catch (IOException e){
                    e.printStackTrace();
                }

            }while (cursor3.moveToNext());
        }cursor3.close();

        Cursor cursor_female1 = stud_db.getEstudyanteFemalebySection(section_id);
        if(cursor_female1.moveToFirst()){
            do {
                String lname_boy = cursor_female1.getString(cursor_female1.getColumnIndex("lastname"));
                String fname_boy = cursor_female1.getString(cursor_female1.getColumnIndex("firstname"));
                String student_num = cursor_female1.getString(cursor_female1.getColumnIndex("student_number"));
                String middlename_boys = cursor_female1.getString(cursor_female1.getColumnIndex("middlename"));
                String gender_boys = cursor_female1.getString(cursor_female1.getColumnIndex("gender"));

                Cursor cursor4 = record_db.getCount_present(student_num,startdate.getText().toString(),enddate.getText().toString());

                Cursor cursor5 = record_db.getCount_late(student_num,startdate.getText().toString(),enddate.getText().toString());

                Cursor cursor6 = record_db.getCount_absent(student_num,startdate.getText().toString(),enddate.getText().toString());
                Log.v("data girls","count present ------"+cursor4.getCount()+"count late ------"+cursor5.getCount()+"count absent ------"+cursor6.getCount()+" student num ------"+student_num+" lastname ------"+lname_boy+" firstname ------"+fname_boy+" middlename ------"+middlename_boys+" gender ------"+gender_boys);

                data_csv.add(new String[]{student_num,lname_boy,fname_boy,middlename_boys,gender_boys, String.valueOf(cursor4.getCount()),String.valueOf(cursor5.getCount()),String.valueOf(cursor6.getCount())});

                try {
                    writer = new CSVWriter(new FileWriter(csv));
                    writer.writeAll(data_csv);
                    writer.close();
                }catch (IOException e){
                    e.printStackTrace();
                }

            }while (cursor_female1.moveToNext());
        }cursor_female1.close();

    }

    public void fun_boys_list(String section_id){

        final ArrayList<String> present_student_number = new ArrayList<>();
        final ArrayList<String> present_student_lname = new ArrayList<>();
        final ArrayList<String> present_student_fname = new ArrayList<>();
        final ArrayList<String> present_student_number_bull = new ArrayList<>();

        final ArrayList<String> oras_start = new ArrayList<>();
        final ArrayList<String> oras_end = new ArrayList<>();
        final ArrayList<String> oras_title = new ArrayList<>();
        final ArrayList<String> oras_end_late = new ArrayList<>();


        Cursor cursor3 = stud_db.getEstudyanteMalebySection(section_id);
        if(cursor3.moveToFirst()){
            do {
                String lname_boy = cursor3.getString(cursor3.getColumnIndex("lastname"));
                String fname_boy = cursor3.getString(cursor3.getColumnIndex("firstname"));
                String student_num = cursor3.getString(cursor3.getColumnIndex("student_number"));
                present_student_number_bull.add(student_num);
                present_student_lname.add(lname_boy);
                present_student_fname.add(fname_boy);
            }while (cursor3.moveToNext());

            for(int r = 0 ; r < present_student_number_bull.size(); r++){
                //Log.v("data"," last name boy -------------------------------------------------- "+present_student_lname.get(r));
                Cursor cursor4 = record_db.get_Recordlang(present_student_number_bull.get(r));
                if(cursor4.moveToFirst()){
                    do {
                        String title = cursor4.getString(cursor4.getColumnIndex("title"));
                        String time_start = cursor4.getString(cursor4.getColumnIndex("start_time"));
                        String time_end = cursor4.getString(cursor4.getColumnIndex("end_time"));
                        String time_end_late = cursor4.getString(cursor4.getColumnIndex("end_time_late"));

                        oras_title.add(title);
                        oras_start.add(time_start);
                        oras_end.add(time_end);
                        oras_end_late.add(time_end_late);

                    }while (cursor4.moveToNext());

                    boys_attendance = new boys_attendance_record_summary_adapter(Attendance_record_summary_main.this,present_student_number_bull,present_student_lname,present_student_fname,startdate.getText().toString(),enddate.getText().toString());
                    boys_present.setAdapter(boys_attendance);

                }cursor4.close();
            }
        }cursor3.close();
    }

    public void fun_girls_list(String section_id){

        final ArrayList<String> present_student_number = new ArrayList<>();
        final ArrayList<String> present_student_lname = new ArrayList<>();
        final ArrayList<String> present_student_fname = new ArrayList<>();
        final ArrayList<String> present_student_number_bull = new ArrayList<>();

        final ArrayList<String> oras_start = new ArrayList<>();
        final ArrayList<String> oras_end = new ArrayList<>();
        final ArrayList<String> oras_title = new ArrayList<>();
        final ArrayList<String> oras_end_late = new ArrayList<>();


        Cursor cursor3 = stud_db.getEstudyanteFemalebySection(section_id);
        if(cursor3.moveToFirst()){
            do {
                String lname_boy = cursor3.getString(cursor3.getColumnIndex("lastname"));
                String fname_boy = cursor3.getString(cursor3.getColumnIndex("firstname"));
                String student_num = cursor3.getString(cursor3.getColumnIndex("student_number"));
                present_student_number_bull.add(student_num);
                present_student_lname.add(lname_boy);
                present_student_fname.add(fname_boy);
            }while (cursor3.moveToNext());

            for(int r = 0 ; r < present_student_number_bull.size(); r++){
                //Log.v("data"," last name boy -------------------------------------------------- "+present_student_lname.get(r));
                Cursor cursor4 = record_db.get_Recordlang(present_student_number_bull.get(r));
                if(cursor4.moveToFirst()){
                    do {
                        String title = cursor4.getString(cursor4.getColumnIndex("title"));
                        String time_start = cursor4.getString(cursor4.getColumnIndex("start_time"));
                        String time_end = cursor4.getString(cursor4.getColumnIndex("end_time"));
                        String time_end_late = cursor4.getString(cursor4.getColumnIndex("end_time_late"));

                        oras_title.add(title);
                        oras_start.add(time_start);
                        oras_end.add(time_end);
                        oras_end_late.add(time_end_late);

                    }while (cursor4.moveToNext());

                    girls_attendance = new girls_attendance_record_summary_adapter(Attendance_record_summary_main.this,present_student_number_bull,present_student_lname,present_student_fname,startdate.getText().toString(),enddate.getText().toString());
                    girls_present.setAdapter(girls_attendance);

                }cursor4.close();
            }
        }cursor3.close();

    }

}
