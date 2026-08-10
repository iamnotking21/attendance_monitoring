package com.example.attendance_monitoring;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;

public class schedules_list_main extends AppCompatActivity implements SearchView.OnQueryTextListener, SearchView.OnCloseListener {
    private ImageView back,add_student;
    private TextView settime,endtime,class_name;
    public String id,section_names;
    public TimePickerDialog timePickerDialog,timePickerDialog1,late_time,late_time1;
    public schedule_DBManager sdb;
    private SearchView searchView;
    private ListView list_view_section_name;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.schedules_list_main_lay);

        sdb = new schedule_DBManager(schedules_list_main.this);
        sdb.open();

        Intent intent = getIntent();
        id = intent.getStringExtra("id");
        section_names = intent.getStringExtra("section_name");

        searchView = (SearchView) findViewById(R.id.search);
        searchView.setIconifiedByDefault(false);
        searchView.setOnQueryTextListener(this);
        searchView.setOnCloseListener(schedules_list_main.this);


        back = (ImageView) findViewById(R.id.back);
        add_student = (ImageView) findViewById(R.id.add_student);
        settime = (TextView) findViewById(R.id.starttimex);
        endtime = (TextView) findViewById(R.id.endtime);
        class_name = (TextView) findViewById(R.id.class_name);
        list_view_section_name = (ListView) findViewById(R.id.list_view_section_name);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //startActivity(new Intent(schedules_list_main.this,schedule_main.class));
                Intent backs = new Intent(schedules_list_main.this,schedule_main.class);
                backs.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(backs);
            }
        });

        class_name.setText(section_names);

        add_student.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Calendar c = Calendar.getInstance();
                final int hour = c.get(Calendar.HOUR_OF_DAY);
                final int minute = c.get(Calendar.MINUTE);

                timePickerDialog = new TimePickerDialog(schedules_list_main.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute1) {
                        String status = "AM";
                        if(hourOfDay > 11){
                            status = "PM";
                        }
                        int hour_of_12_hour_format;

                        if (hourOfDay == 12){
                            hour_of_12_hour_format = hourOfDay;
                        }
                        else if(hourOfDay > 11){
                            hour_of_12_hour_format = hourOfDay - 12;
                        }else{
                            hour_of_12_hour_format = hourOfDay;
                        }

                        settime.setText(hour_of_12_hour_format+":"+minute1+":"+status);
                        Log.v("data","set time "+hour_of_12_hour_format+":"+minute1+":"+status);

                    }
                }, hour, minute, false);
                timePickerDialog.show();

                timePickerDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        settime.setText("");
                        timePickerDialog1 = new TimePickerDialog(schedules_list_main.this, new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {

                            }
                        },hour,minute,false);
                        timePickerDialog1.cancel();
                        Log.v("data","cancel");
                    }
                });

                timePickerDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if(!settime.getText().toString().equals("")){
                            Log.v("data","pota "+settime.getText().toString());
                            timePickerDialog1 = new TimePickerDialog(schedules_list_main.this, new TimePickerDialog.OnTimeSetListener() {
                                @Override
                                public void onTimeSet(TimePicker view, final int hourOfDay2, int minute2) {
                                    String status2 = "AM";
                                    if(hourOfDay2 > 11){
                                        status2 = "PM";
                                    }
                                    int hour_of_12_hour_format2;

                                    if (hourOfDay2 == 12){
                                        hour_of_12_hour_format2 = hourOfDay2;
                                    }
                                    else if(hourOfDay2 > 11){
                                        hour_of_12_hour_format2 = hourOfDay2 - 12;
                                    }
                                    else{
                                        hour_of_12_hour_format2 = hourOfDay2;
                                    }

                                    endtime.setText(hour_of_12_hour_format2+":"+minute2+":"+status2);
                                    Log.v("data","set time 2 "+hour_of_12_hour_format2+":"+minute2+":"+status2);

                                    Log.v("data","start time "+settime.getText().toString());
                                    Log.v("data","end time "+endtime.getText().toString());

                                    if(!settime.getText().toString().equals("") && !endtime.getText().toString().equals("")){
                                        LayoutInflater inflater = getLayoutInflater();
                                        View view1 = inflater.inflate(R.layout.custom_dialog_add_schedule_info,null);

                                        final TextView sections_namex = view1.findViewById(R.id.section_namex);
                                        final TextView una = view1.findViewById(R.id.una);
                                        final TextView tapos = view1.findViewById(R.id.tapos);
                                        final EditText titles = view1.findViewById(R.id.titles);
                                        final EditText venues = view1.findViewById(R.id.venues);
                                        final Button late = view1.findViewById(R.id.late);
                                        final TextView min_late_start = view1.findViewById(R.id.min_late_start);
                                        final TextView min_late_end = view1.findViewById(R.id.min_late_end);

                                        min_late_start.setText(endtime.getText().toString());

                                        late.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {

                                                late_time = new TimePickerDialog(schedules_list_main.this, new TimePickerDialog.OnTimeSetListener() {
                                                    @Override
                                                    public void onTimeSet(TimePicker view, int hourOfDay_late, int minute_late) {

                                                        String status2_late = "AM";
                                                        if(hourOfDay_late > 11){
                                                            status2_late = "PM";
                                                        }
                                                        int hour_of_12_hour_format2_late;

                                                        if (hourOfDay_late == 12){
                                                            hour_of_12_hour_format2_late = hourOfDay_late;
                                                        }
                                                        else if(hourOfDay_late > 11){
                                                            hour_of_12_hour_format2_late = hourOfDay_late - 12;
                                                        }
                                                        else{
                                                            hour_of_12_hour_format2_late = hourOfDay_late;
                                                        }
                                                        min_late_end.setText(hour_of_12_hour_format2_late+":"+minute_late+":"+status2_late);

                                                    }
                                                },hour,minute,false);
                                                late_time.show();
                                            }
                                        });

                                        sections_namex.setText("  Section Name: "+section_names);
                                        una.setText("  Start: "+settime.getText().toString());
                                        tapos.setText("  End: "+endtime.getText().toString());

                                        AlertDialog.Builder builder = new AlertDialog.Builder(schedules_list_main.this);
                                        builder.setTitle("Add Schedule Info.");

                                        builder.setView(view1);

                                        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {

                                                if(!min_late_end.getText().toString().equals("")){
                                                    long count_save = sdb.insert_schedule(Integer.parseInt(id),titles.getText().toString(),venues.getText().toString(),settime.getText().toString(),endtime.getText().toString(),String.valueOf(1),endtime.getText().toString(),min_late_end.getText().toString());
                                                    if(count_save!=0){
                                                        Toast.makeText(schedules_list_main.this,"Schedule Successfully Save !",Toast.LENGTH_SHORT).show();
                                                    }else{
                                                        Toast.makeText(schedules_list_main.this,"Please try again later !",Toast.LENGTH_SHORT).show();
                                                    }
                                                }else{
                                                    Toast.makeText(schedules_list_main.this,"Please Complete all the fields !",Toast.LENGTH_SHORT).show();
                                                }

                                            }
                                        });

                                        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {

                                            }
                                        });

                                        AlertDialog dialog1 = builder.create();
                                        dialog1.show();
                                    }

                                }
                            },hour,minute,false);

                            timePickerDialog1.setTitle("End");
                            timePickerDialog1.show();
                        }else{
                            timePickerDialog1 = new TimePickerDialog(schedules_list_main.this, new TimePickerDialog.OnTimeSetListener() {
                                @Override
                                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {

                                }
                            },hour,minute,false);
                            timePickerDialog1.cancel();
                        }

                        Log.v("data","dismiss");
                    }
                });
            }
        });

        Cursor cursor4 = sdb.getSchedules(id);
        fetch_schedules(cursor4);

    }

    public void fetch_schedules(Cursor cursor){
        ArrayList<String> idx = new ArrayList<>();
        ArrayList<String> venuesx = new ArrayList<>();
        ArrayList<String> titlesx = new ArrayList<>();
        ArrayList<String> start_timesx = new ArrayList<>();
        ArrayList<String> end_timesx = new ArrayList<>();
        ArrayList<String> start_timesx_late = new ArrayList<>();
        ArrayList<String> end_timesx_late = new ArrayList<>();

        //Cursor cursor = sdb.getSchedules(id);
        if(cursor.moveToFirst()){
            do {

                final String ids = cursor.getString(cursor.getColumnIndex("id"));
                final String venues = cursor.getString(cursor.getColumnIndex("venue"));
                final String titles = cursor.getString(cursor.getColumnIndex("title"));
                final String start_times = cursor.getString(cursor.getColumnIndex("start_time"));
                final String end_times = cursor.getString(cursor.getColumnIndex("end_time"));
                final String start_times_late = cursor.getString(cursor.getColumnIndex("start_time_late"));
                final String end_times_late = cursor.getString(cursor.getColumnIndex("end_time_late"));

                idx.add(ids);
                venuesx.add(venues);
                titlesx.add(titles);
                start_timesx.add(start_times);
                end_timesx.add(end_times);
                start_timesx_late.add(start_times_late);
                end_timesx_late.add(end_times_late);

            }while (cursor.moveToNext());

            schedules_list_main_adapter sched = new schedules_list_main_adapter(schedules_list_main.this,idx,venuesx,titlesx,start_timesx,end_timesx,start_timesx_late,end_timesx_late);
            list_view_section_name.setAdapter(sched);

        }cursor.close();

    }


    public boolean onQueryTextChange(String newText) {
        showResults(newText );
        Log.v("data","1");
        return false;
    }

    public boolean onQueryTextSubmit(String query) {
        showResults(query );
        Log.v("data","2");
        return false;
    }

    public boolean onClose() {
        showResults("");
        Log.v("data","2");
        return false;
    }

    private void showResults(String query){

        if(query.equals("")){
            Cursor cursor = sdb.getSchedules(id);
            fetch_schedules(cursor);
        }else{
            Cursor cursor3 = sdb.search_schedule(query,id);
            fetch_schedules(cursor3);
        }


    }


}
