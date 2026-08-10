package com.example.attendance_monitoring;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class panel_main extends AppCompatActivity {
    private Button dashboard,scan,sections,reports,databases,schedule;
    private ImageView exit;
    public schedule_DBManager sched_db;
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.panel);

        sched_db = new schedule_DBManager(panel_main.this);
        sched_db.open();

        verifyStoragePermissions();

        dashboard  = (Button) findViewById(R.id.dashborad);
        scan  = (Button) findViewById(R.id.scan);
        sections  = (Button) findViewById(R.id.sections);
        reports = (Button) findViewById(R.id.reports);
        databases = (Button) findViewById(R.id.database);
        exit = (ImageView) findViewById(R.id.exit);
        schedule = (Button) findViewById(R.id.schedule);

        dashboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intentx = new Intent(panel_main.this,backgroundservice.class);
                sendBroadcast(intentx);


                //startActivity(new Intent(panel_main.this,MainActivity.class));
                Intent dash = new Intent(panel_main.this,MainActivity.class);
                dash.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(dash);

                Intent intent = new Intent(panel_main.this,backgroundservice.class);
                sendBroadcast(intent);

            }
        });

        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(panel_main.this,backgroundservice.class);
                sendBroadcast(intent);


                Intent startMain = new Intent(Intent.ACTION_MAIN);
                startMain.addCategory(Intent.CATEGORY_HOME);
                startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(startMain);


            }
        });

        sections.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                Intent intent = new Intent(panel_main.this,backgroundservice.class);
                sendBroadcast(intent);


                //startActivity(new Intent(panel_main.this,classess.class));
                Intent sections = new Intent(panel_main.this,classess.class);
                sections.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(sections);
            }
        });

        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                Intent intent = new Intent(panel_main.this,backgroundservice.class);
                sendBroadcast(intent);


                //startActivity(new Intent(panel_main.this,scanner_main.class));
                Intent scan = new Intent(panel_main.this,scanner_main.class);
                scan.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(scan);
            }
        });

        schedule.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                Intent intent = new Intent(panel_main.this,backgroundservice.class);
                sendBroadcast(intent);


                //startActivity(new Intent(panel_main.this,schedule_main.class));
                Intent sched = new Intent(panel_main.this,schedule_main.class);
                sched.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(sched);
            }
        });

        reports.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                Intent intent = new Intent(panel_main.this,backgroundservice.class);
                sendBroadcast(intent);


                //startActivity(new Intent(panel_main.this,Report_Section_main.class));
                Intent rep = new Intent(panel_main.this,Report_Section_main.class);
                rep.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(rep);
            }
        });

        databases.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                Intent intent = new Intent(panel_main.this,backgroundservice.class);
                sendBroadcast(intent);


                //startActivity(new Intent(panel_main.this,Database_main.class));
                Intent data = new Intent(panel_main.this,Database_main.class);
                data.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(data);
            }
        });

        Calendar firingCal= Calendar.getInstance();
        try {

            //Create a new PendingIntent and add it to the AlarmManager
            Intent intent = new Intent(panel_main.this, backgroundservice.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(panel_main.this,
                    12345, intent, PendingIntent.FLAG_CANCEL_CURRENT);
            AlarmManager am =
                    (AlarmManager) panel_main.this.getSystemService(Activity.ALARM_SERVICE);
            Long intendedTime = firingCal.getTimeInMillis();

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                am.setExact(AlarmManager.RTC_WAKEUP, 1, pendingIntent);

            } else {
                am.set(AlarmManager.RTC_WAKEUP,1, pendingIntent);
            }

        } catch (Exception e) {}

        Intent intent = new Intent(panel_main.this,backgroundservice.class);
        sendBroadcast(intent);



        Date c = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
        String formattedDate = df.format(c);
        Log.v("data","current date "+formattedDate);

        ArrayList<String> val_date = new ArrayList<>();
        ArrayList<String> schedule_id = new ArrayList<>();

        Cursor cursor = sched_db.getCurrentday();
        Log.v("data","count "+cursor.getCount());

        if(cursor.getCount()==0){

            long count = sched_db.insert_current_day(formattedDate);
            if(count!=0){
                Log.v("data","insert successfully");
            }else{
                Log.v("data","please try again later");
            }

        }else{

            if(cursor.moveToFirst()){
                do {
                    String old_days = cursor.getString(cursor.getColumnIndex("current_day"));
                    Log.v("data","old days "+old_days);
                    val_date.add(old_days);
                }while (cursor.moveToNext());
                int count_val ;
                count_val = sched_db.valCurrentday(formattedDate);
                if(count_val==0){
                    Log.v("data","val "+count_val);

                    long count = sched_db.insert_current_day(formattedDate);
                    if(count!=0){
                        Log.v("data","insert successfully");

                        Cursor cursor1 = sched_db.getfuckingschedules();
                        if(cursor1.moveToFirst()){
                            do {
                                String nakatakda = cursor1.getString(cursor1.getColumnIndex("id"));
                                schedule_id.add(nakatakda);
                            }while (cursor1.moveToNext());

                            for(int y = 0 ; y < schedule_id.size(); y++){
                                int count_val_update = sched_db.refresh_status_start_late(schedule_id.get(y));
                                if(count_val_update!=0){
                                    Log.v("data","successfully refresh ");
                                }else{
                                    Log.v("data","please try again later");
                                }
                            }

                        }cursor1.close();

                    }else{
                        Log.v("data","please try again later");
                    }

                }else{
                    Log.v("data"," not val "+count_val);
                }

            }cursor.close();
        }

    }


    public void verifyStoragePermissions() {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(panel_main.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    panel_main.this,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

}
