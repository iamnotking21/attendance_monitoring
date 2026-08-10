package com.example.attendance_monitoring;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;

public class schedules_list_main_adapter extends BaseAdapter {

    Context context;
    String id[],venue[],title[],starttime[],endtime[],starttime_late[],endtime_late[];
    LayoutInflater inflater;
    public TextView titles;

    public schedules_list_main_adapter(Context acontext, ArrayList<String> id, ArrayList<String> venue, ArrayList<String> title, ArrayList<String> starttime, ArrayList<String> endtime,ArrayList<String> starttime_late,ArrayList<String> endtime_late){
        this.context = acontext;
        this.id = id.toArray(new String[0]);
        this.venue = venue.toArray(new String[0]);
        this.title = title.toArray(new String[0]);
        this.starttime = starttime.toArray(new String[0]);
        this.endtime = endtime.toArray(new String[0]);
        this.starttime_late = starttime_late.toArray(new String[0]);
        this.endtime_late = endtime_late.toArray(new String[0]);
        inflater = (LayoutInflater.from(context));
    }

    @Override
    public int getCount() {
        return id.length;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView( final int i, View view, ViewGroup parent) {
        view = inflater.inflate(R.layout.schedules_list_main_adapter_lay,null);

        final schedule_DBManager sched_db = new schedule_DBManager(view.getRootView().getContext());
        sched_db.open();

        final TextView titles = (TextView) view.findViewById(R.id.title);
        final TextView starttimes = (TextView) view.findViewById(R.id.starttime);
        final TextView endtimes = (TextView) view.findViewById(R.id.endtime);

        titles.setText(title[i].substring(0,1).toUpperCase()+title[i].substring(1));
        starttimes.setText(starttime[i]);
        endtimes.setText(endtime[i]);

        final View finalView = view;
        titles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
             modify(i,finalView,sched_db);
            }
        });

        starttimes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                modify(i,finalView,sched_db);
            }
        });

        endtimes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                modify(i,finalView,sched_db);
            }
        });


        return view;
    }

    public void modify(final int i,View view,final schedule_DBManager sched_db){
        final View finalView = view;

        View alert = inflater.inflate(R.layout.custom_dialog_modify_schedule,null);

        final TextView timer_start = (TextView) alert.findViewById(R.id.time_start);
        final TextView timer_end = (TextView) alert.findViewById(R.id.time_end);
        final TextView min_late_start = (TextView) alert.findViewById(R.id.min_late_start);
        final TextView min_late_end = (TextView) alert.findViewById(R.id.min_late_end);
        final EditText titlesv = alert.findViewById(R.id.titles);
        final EditText venues = alert.findViewById(R.id.venues);
        final TextView una = alert.findViewById(R.id.una);
        final TextView tapos = alert.findViewById(R.id.tapos);

        titlesv.setText(title[i].substring(0,1).toUpperCase()+title[i].substring(1));
        venues.setText(venue[i].substring(0,1).toUpperCase()+venue[i].substring(1));

        una.setText(starttime[i]);
        tapos.setText(endtime[i]);
        min_late_start.setText(endtime[i]);
        min_late_end.setText(endtime_late[i]);

        final Button update_start = alert.findViewById(R.id.update_start);
        final Button update_end  = alert.findViewById(R.id.update_end);
        final Button remove  = alert.findViewById(R.id.remove);
        final Button update_late = alert.findViewById(R.id.late);

        final Calendar c = Calendar.getInstance();
        final int hour = c.get(Calendar.HOUR_OF_DAY);
        final int minute = c.get(Calendar.MINUTE);

        final String[] start_oras = starttime[i].split(":");
        Log.v("data","split start oras "+start_oras[0]+":"+start_oras[1]+":"+start_oras[2]);

        final int start_hour = Integer.parseInt(start_oras[0]);
        final int start_minute = Integer.parseInt(start_oras[1]);

        final String[] end_oras = endtime[i].split(":");
        Log.v("data","split start oras "+end_oras[0]+":"+end_oras[1]+":"+end_oras[2]);

        final String[] end_oras_late = endtime_late[i].split(":");
        Log.v("data","split start oras "+end_oras_late[0]+":"+end_oras_late[1]+":"+end_oras_late[2]);


        final int end_hour_late = Integer.parseInt(end_oras_late[0]);
        final int end_minute_late = Integer.parseInt(end_oras_late[1]);

        final int end_hour = Integer.parseInt(end_oras[0]);
        final int end_minute = Integer.parseInt(end_oras[1]);

        update_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final TimePickerDialog timePickerDialog = new TimePickerDialog(finalView.getRootView().getContext(), new TimePickerDialog.OnTimeSetListener() {
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

                        timer_start.setText(hour_of_12_hour_format+":"+minute1+":"+status);

                        Log.v("data","set time "+hour_of_12_hour_format+":"+minute1+":"+status);

                    }
                }, start_hour, start_minute, false);
                timePickerDialog.setTitle("Start");
                timePickerDialog.show();

                timePickerDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        timer_start.setText("");
                    }
                });

                timePickerDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if(!timer_start.getText().toString().equals("")){
                            una.setText(timer_start.getText().toString());
                        }
                    }
                });


            }
        });

        update_end.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v("data","pota "+timer_start.getText().toString());
                TimePickerDialog timePickerDialog1 = new TimePickerDialog(finalView.getRootView().getContext(), new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay2, int minute2) {
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

                        timer_end.setText(hour_of_12_hour_format2+":"+minute2+":"+status2);
                        Log.v("data","set time 2 "+hour_of_12_hour_format2+":"+minute2+":"+status2);

                        Log.v("data","start time "+timer_start.getText().toString());
                        Log.v("data","end time "+timer_end.getText().toString());
                    }
                },end_hour,end_minute,false);

                timePickerDialog1.setTitle("End");
                timePickerDialog1.show();


                timePickerDialog1.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if(!timer_end.getText().toString().equals("")){
                            tapos.setText(timer_end.getText().toString());
                            min_late_start.setText(timer_end.getText().toString());
                        }
                    }
                });

            }
        });

        update_late.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TimePickerDialog timePickerDialog_late = new TimePickerDialog(finalView.getRootView().getContext(), new TimePickerDialog.OnTimeSetListener() {
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
                },end_hour_late,end_minute_late,false);
                timePickerDialog_late.show();
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(finalView.getRootView().getContext());


        builder.setView(alert);

        builder.setPositiveButton("Update Changes ", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                /*
                Log.v("data","timer start "+timer_start.getText().toString());
                Log.v("data","timer end "+timer_end.getText().toString());
                Log.v("data","una start "+una.getText().toString());
                Log.v("data","tapos end "+tapos.getText().toString());
                */


                String msg_save = "";
                String msg_try = "";
                String msg_complete = "";

                if(titlesv.getText().toString().equals("") || venues.getText().toString().equals("") || tapos.getText().toString().equals("") || una.getText().toString().equals("") || min_late_start.getText().toString().equals("") || min_late_end.getText().toString().equals("")){
                    Log.v("data","inc");
                    msg_complete = "Please complete all the fields";
                }else{
                    Log.v("data","complete");

                    int count_update = sched_db.update_schedule(Integer.parseInt(id[i]),venues.getText().toString(),titlesv.getText().toString(),una.getText().toString(),tapos.getText().toString(),min_late_start.getText().toString(),min_late_end.getText().toString());
                    if(count_update!=0){
                        //Toast.makeText(finalView.getRootView().getContext(),"Schedule Successfully Updated !",Toast.LENGTH_SHORT).show();
                        msg_save = "Schedule Successfully Updated !";
                    }else{
                        //Toast.makeText(finalView.getRootView().getContext(),"Please try again later !",Toast.LENGTH_SHORT).show();
                        msg_try = "Please try again later !";
                    }
                }

                if(!msg_save.equals("")){
                    Toast.makeText(finalView.getRootView().getContext(),msg_save,Toast.LENGTH_SHORT).show();
                }else if(!msg_try.equals("")){
                    Toast.makeText(finalView.getRootView().getContext(),msg_try,Toast.LENGTH_SHORT).show();
                }else if(!msg_complete.equals("")){
                    Toast.makeText(finalView.getRootView().getContext(),msg_complete,Toast.LENGTH_SHORT).show();
                }


            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        final AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        dialog.show();


        remove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder1 = new AlertDialog.Builder(finalView.getRootView().getContext());

                builder1.setMessage("Are you sure? Do you want to remove this ?");

                builder1.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog1, int which) {
                        sched_db.remove_schedule(Integer.parseInt(id[i]));
                        dialog.dismiss();
                        Toast.makeText(finalView.getRootView().getContext(),"Schedule Successfully Removed !",Toast.LENGTH_SHORT).show();
                    }
                });

                builder1.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });

                builder1.show();

            }
        });


    }

}
