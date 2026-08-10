package com.example.attendance_monitoring;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.ArrayList;

public class scanner_main extends AppCompatActivity {
    private ImageView back;
    private IntentIntegrator qrScan;
    private int brightness;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*
        setContentView(R.layout.scanner_main_lay);

        back = (ImageView) findViewById(R.id.back);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(scanner_main.this,panel_main.class));
            }
        });
        */
        qrScan = new IntentIntegrator(scanner_main.this);

        qrScan.initiateScan();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode,resultCode,data);
        Log.v("data","contents "+result.getContents());
        if(result != null){
            if(result.getContents() == null){
                //startActivity(new Intent(scanner_main.this,panel_main.class));
                Intent backs = new Intent(scanner_main.this,panel_main.class);
                backs.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(backs);
                //Toast.makeText(scanner_main.this,"QR Code not registered ",Toast.LENGTH_SHORT).show();
            }else{

                //fetch from database student number yan
                student_DBManager sdb = new student_DBManager(scanner_main.this);
                sdb.open();
                schedule_DBManager sched_db = new schedule_DBManager(scanner_main.this);
                sched_db.open();
                record_DBManager record_db = new record_DBManager(scanner_main.this);
                record_db.open();

                ArrayList<String> klase_name = new ArrayList<>();
                String section_id;

                int count_val = sdb.attendance_studentnumber(result.getContents());
                if(count_val!=0){

                    Intent intent = new Intent(scanner_main.this,attendance_record.class);
                    intent.putExtra("student_number",result.getContents());
                    sendBroadcast(intent);

                   // Log.v("data","attendance ka na bobo");
                    Toast.makeText(scanner_main.this,"Successfully Attendance !"+result.getContents(),Toast.LENGTH_SHORT).show();
                    qrScan.initiateScan();
                }else{
                    //Log.v("data","hindi mahanap");
                    Toast.makeText(scanner_main.this,"QR Code Doesn't Registered ! "+result.getContents(),Toast.LENGTH_SHORT).show();
                    qrScan.initiateScan();
                }
            }
        }else{
            super.onActivityResult(requestCode, resultCode, data);

        }
    }

}
