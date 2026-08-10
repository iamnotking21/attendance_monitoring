package com.example.attendance_monitoring;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.zxing.WriterException;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import androidmads.library.qrgenearator.QRGContents;
import androidmads.library.qrgenearator.QRGEncoder;
import androidmads.library.qrgenearator.QRGSaver;

public class Database_main extends AppCompatActivity {
    private ImageView back;
    private Button backup,checkdb,importdb,restoredb;
    public DatabaseHelper db;
    public student_DBManager sdb ;
    public schedule_DBManager scheddb;
    public record_DBManager recDb;
    private TextView dbsize;
    public QRGEncoder qrgEncoder;
    public Bitmap bitmap;
    private String save_path = Environment.getExternalStorageDirectory().getPath() + "/Download/";
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.database_main_lay);

        db = new DatabaseHelper(Database_main.this);

        sdb = new student_DBManager(Database_main.this);
        sdb.open();

        scheddb = new schedule_DBManager(Database_main.this);
        scheddb.open();

        recDb = new record_DBManager(Database_main.this);
        recDb.open();

        File sd = Environment.getExternalStorageDirectory();
        File data = Environment.getDataDirectory();
        Log.v("data","sd path "+sd+" data "+data);

        final String currentPath = "/data/"+"com.example.attendance_monitoring"+"/databases/"+"attendance_monitoring";
        final String backupDBpath = "attendance_monitoring";

        final File currentDbpath = new File(data,currentPath);
        final File backupDb = new File(sd,"/Download/attendance_monitoring");

        back = (ImageView) findViewById(R.id.back);
        dbsize = (TextView) findViewById(R.id.dbsize);

        backup = (Button) findViewById(R.id.backup);
        checkdb = (Button) findViewById(R.id.checkdb);
        importdb = (Button) findViewById(R.id.importdb);
        restoredb = (Button) findViewById(R.id.restoredb);

        File f = Database_main.this.getDatabasePath("attendance_monitoring");
        long dbSize = f.length();
        Log.v("data","db size "+dbSize);

        if(dbSize>=1024){
            long totalBytes = dbSize/1024;
            Log.v("data","db size "+totalBytes+" KBs");
            dbsize.setText("Database size:  "+totalBytes+" KBs");
            if(totalBytes>=1024){
                long totalmb = totalBytes/1024;
                Log.v("data","db size "+Long.toString(totalmb)+" MBs");
                dbsize.setText("Database size:  "+totalmb+" MBs");
                if(totalmb>=1024){
                    long totalgb = totalmb/1024;
                    Log.v("data","db size "+totalgb+" Gb");
                    dbsize.setText("Database size:  "+totalgb+" Gb");
                }
            }
        }else{
            Log.v("data","db size "+dbSize+" bytes");
            dbsize.setText("Database size:  "+dbSize+" bytes");
        }

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //startActivity(new Intent(Database_main.this,panel_main.class));
                Intent intent = new Intent(Database_main.this,panel_main.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });

        checkdb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Boolean val_check = db.checkDb(String.valueOf(currentDbpath));

                if(val_check==true){
                    Toast.makeText(Database_main.this,"Database is in Good Condition!",Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(Database_main.this,"Database is in Bad Condition!",Toast.LENGTH_SHORT).show();
                }

            }
        });

        backup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ArrayList<String> section_id = new ArrayList<>();
                ArrayList<String> section_namesx = new ArrayList<>();
                ArrayList<String> status_sectionx = new ArrayList<>();

                if(Build.VERSION.SDK_INT > Build.VERSION_CODES.M){

                    verifyStoragePermissions();
                    isStoragePermissionGrantedRead();
                    isStoragePermissionGranted();

                    String csv = (Environment.getExternalStorageDirectory().getAbsolutePath()+"/Download/AttendanceMonitoringDatabasePFNHS.csv");
                    //Log.v("data","csv imported file db --------------------------- "+ csv);

                    Log.v("data","main direct --------------------------- "+Environment.getExternalStorageDirectory().getAbsolutePath());

                    CSVWriter writer = null;

                    final List<String[]> data_csv = new ArrayList<>();
                    data_csv.add(new String[]{"col1","col2","col3","col4","col5","col6","col7","col8","col9","col10","col11","col12","col13","col14"});

                    Cursor sectioncsv = db.selectAllsections();
                    if(sectioncsv.moveToFirst()){
                        do {
                            String uid_sec = sectioncsv.getString(sectioncsv.getColumnIndex("id"));
                            String section_name = sectioncsv.getString(sectioncsv.getColumnIndex("sections_name"));
                            String section_status = sectioncsv.getString(sectioncsv.getColumnIndex("status_section"));

                            section_id.add(uid_sec);
                            section_namesx.add(section_name);
                            status_sectionx.add(section_status);

                            data_csv.add(new String[]{uid_sec,section_name,section_status,"col4","col5","col6","col7","col8","col9","col10","col11","col12","col13","col14"});
                            try{
                                writer = new CSVWriter(new FileWriter(Environment.getExternalStorageDirectory().getAbsolutePath()+"/Download/AttendanceMonitoringDatabasePFNHS.csv"));
                                writer.writeAll(data_csv);

                            }catch (IOException e){
                                e.printStackTrace();
                            }

                        }while (sectioncsv.moveToNext());
                    }sectioncsv.close();

                    //students

                    Cursor curstuds = sdb.selectAllStudents();
                    if(curstuds.moveToFirst()){
                        do {
                            String studid = curstuds.getString(curstuds.getColumnIndex("id"));
                            String lastname = curstuds.getString(curstuds.getColumnIndex("lastname"));
                            String firstname = curstuds.getString(curstuds.getColumnIndex("firstname"));
                            String middlename = curstuds.getString(curstuds.getColumnIndex("middlename"));
                            String gender = curstuds.getString(curstuds.getColumnIndex("gender"));
                            String student_number = curstuds.getString(curstuds.getColumnIndex("student_number"));
                            String pick_status = curstuds.getString(curstuds.getColumnIndex("pick_status"));
                            String student_status = curstuds.getString(curstuds.getColumnIndex("student_status"));
                            String section_idxz = curstuds.getString(curstuds.getColumnIndex("section_id"));

                            data_csv.add(new String[]{studid,lastname,firstname,middlename,gender,student_number,pick_status,student_status,section_idxz,"mali*1","mali*2","mali*3","mali*4","mali*5"});
                            try{
                                writer = new CSVWriter(new FileWriter(csv));
                                writer.writeAll(data_csv);
                            }catch (IOException e){
                                e.printStackTrace();
                            }

                        }while (curstuds.moveToNext());
                    }curstuds.close();

                    //schedule
                    Cursor curssched = scheddb.selectAllSchedule();
                    if(curssched.moveToFirst()){
                        do {
                            String idsched = curssched.getString(curssched.getColumnIndex("id"));
                            String venue = curssched.getString(curssched.getColumnIndex("venue"));
                            String title = curssched.getString(curssched.getColumnIndex("title"));
                            String start_time = curssched.getString(curssched.getColumnIndex("start_time"));
                            String end_time = curssched.getString(curssched.getColumnIndex("end_time"));
                            String start_time_late = curssched.getString(curssched.getColumnIndex("start_time_late"));
                            String end_time_late = curssched.getString(curssched.getColumnIndex("end_time_late"));
                            String section_idy = curssched.getString(curssched.getColumnIndex("section_id"));
                            String status_schedule = curssched.getString(curssched.getColumnIndex("status_schedule"));
                            String status_start_late =curssched.getString(curssched.getColumnIndex("status_start_late"));
                            String status_end_late = curssched.getString(curssched.getColumnIndex("status_end_late"));
                            String status_start = curssched.getString(curssched.getColumnIndex("status_start"));
                            String status_end = curssched.getString(curssched.getColumnIndex("status_end"));

                            data_csv.add(new String[]{idsched,venue,title,start_time,end_time,start_time_late,end_time_late,section_idy,status_schedule,status_start_late,status_end_late,status_start,status_end,"sched*1"});

                            try{
                                writer = new CSVWriter(new FileWriter(csv));
                                writer.writeAll(data_csv);

                            }catch (IOException e){
                                e.printStackTrace();
                            }

                        }while (curssched.moveToNext());
                    }curssched.close();

                    //schedule
                    Cursor cursorrecord = recDb.selectAllrecord();
                    if(cursorrecord.moveToFirst()){
                        do {
                            String rid = cursorrecord.getString(cursorrecord.getColumnIndex("id"));
                            String schedule_id = cursorrecord.getString(cursorrecord.getColumnIndex("schedule_id"));
                            String section_ida = cursorrecord.getString(cursorrecord.getColumnIndex("section_id"));
                            String studentnumber = cursorrecord.getString(cursorrecord.getColumnIndex("student_number"));
                            String current_day = cursorrecord.getString(cursorrecord.getColumnIndex("current_day"));
                            String record_day = cursorrecord.getString(cursorrecord.getColumnIndex("record_day"));
                            String titlex = cursorrecord.getString(cursorrecord.getColumnIndex("title"));
                            String start_time = cursorrecord.getString(cursorrecord.getColumnIndex("start_time"));
                            String end_time = cursorrecord.getString(cursorrecord.getColumnIndex("end_time"));
                            String end_time_late = cursorrecord.getString(cursorrecord.getColumnIndex("end_time_late"));
                            String status_record = cursorrecord.getString(cursorrecord.getColumnIndex("status_record"));

                            data_csv.add(new String[]{rid,schedule_id,section_ida,studentnumber,current_day,record_day,titlex,start_time,end_time,end_time_late,status_record,"record*12","record*13","record*14"});


                            try{
                                writer = new CSVWriter(new FileWriter(csv));
                                writer.writeAll(data_csv);
                            }catch (IOException e){
                                e.printStackTrace();
                            }

                        }while (cursorrecord.moveToNext());
                    }cursorrecord.close();

                    //day

                    Cursor curday = scheddb.selectcurrentday();
                    if(curday.moveToFirst()){
                        do {
                            String did = curday.getString(curday.getColumnIndex("id"));
                            String cur = curday.getString(curday.getColumnIndex("current_day"));

                            data_csv.add(new String[]{did,cur,"day*3","day*4","day*5","day*6","day*7","day*8","day*9","day*10","day*11","day*12"});

                            try{
                                writer = new CSVWriter(new FileWriter(csv));
                                writer.writeAll(data_csv);
                                writer.close();
                            }catch (IOException e){
                                e.printStackTrace();
                            }

                        }while (curday.moveToNext());
                    }curday.close();


                }else{

                    try{

                        checkExternalStoragePermissions();

                        FileChannel source = new FileInputStream(currentDbpath).getChannel();
                        FileChannel destination = new FileOutputStream(backupDb).getChannel();

                        destination.transferFrom(source,0,source.size());
                        source.close();
                        destination.close();
                        Toast.makeText(Database_main.this,"Successfully Backup the Database!",Toast.LENGTH_SHORT).show();
                    }catch (IOException e){
                        e.printStackTrace();
                    }

                }
                Toast.makeText(Database_main.this,"Successfully Backup the Database",Toast.LENGTH_SHORT).show();
            }
        });

        importdb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {

                        // Should we show an explanation?
                        if (shouldShowRequestPermissionRationale(
                                Manifest.permission.READ_EXTERNAL_STORAGE)) {
                            // Explain to the user why we need to read the contacts
                        }

                        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                4534);

                        // MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE is an
                        // app-defined int constant that should be quite unique

                        return;
                    }
                }

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                startActivityForResult(Intent.createChooser(intent,"Choose *.sqlite"),4534);
                if(Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                    Toast.makeText(Database_main.this,"Please Find AttendanceMonitoringDatabasePFNHS file name ",Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(Database_main.this,"Please Find attendance_monitoring file name ",Toast.LENGTH_SHORT).show();
                }

            }
        });

        restoredb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(Database_main.this);
                builder.setMessage("Are you sure? Do you want to wipe all the data ?");

                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        db.fucking_delete_all_data();
                        Toast.makeText(Database_main.this,"Successfully clear the data! ",Toast.LENGTH_SHORT).show();
                    }
                });

                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                builder.show();
            }
        });

    }

    public void verifyStoragePermissions() {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(Database_main.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    Database_main.this,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    int REQUEST_STORAGE = 1;

    private void checkExternalStoragePermissions() {
        if (hasStoragePermissionGranted()) {
            //You can do what whatever you want to do as permission is granted
        } else {
            requestExternalStoragePermission();
        }
    }

    public boolean hasStoragePermissionGranted(){
        return  ContextCompat.checkSelfPermission(Database_main.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    public void requestExternalStoragePermission() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            ActivityCompat.requestPermissions(Database_main.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_STORAGE);
        }
    }

    public  boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v("data","write granted -------------------------------------------- ");
                return true;
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                Log.v("data","not granted ------------------------------------------------");
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            return true;
        }
    }

    public  boolean isStoragePermissionGrantedRead() {
        if (Build.VERSION.SDK_INT >= 23) {

            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v("data","Permission is granted --------------------------------------- ");
                return true;
            } else {

                Log.v("data","Permission is revoked --------------------------------------------");
                ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v("data","Permission is granted ----------------------------------------------");
            return true;
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case 4534:{
                if(resultCode == RESULT_OK){

                    SQLiteDatabase dbx = db.getWritableDatabase();
                    File datax = Environment.getDataDirectory();
                    final String currentPath = "/data/"+"com.example.attendance_monitoring"+"/databases/"+"attendance_monitoring";
                    final File currentDbpath = new File(datax,currentPath);

                    File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath());
                    String[] split = data.getData().getPath().split(":");

                    Log.v("data","file --------------------- "+file+"------------------- "+split[1]);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                        try {

                            File fileexists = new File(file+"/"+split[1]);
                            CSVReader csvReader = null;
                            if(fileexists.exists()){
                                csvReader = new CSVReader(new FileReader(file+"/"+split[1]));
                            }else{
                                csvReader = new CSVReader(new FileReader(split[1]));
                            }

                            String[] f_csv;

                            while((f_csv = csvReader.readNext())!= null){
                                if(f_csv[3].equals("col4") && !f_csv[0].equals("col1") && !f_csv[3].equals("")){
                                    int count = db.Validate_if_Same(f_csv[1]);
                                    if(count!=0){
                                        //Toast.makeText(Database_main.this,"Section Name is Exists! Please Input Another",Toast.LENGTH_SHORT).show();
                                    }else{
                                        long id = db.importSection(f_csv[0],f_csv[1],f_csv[2]);
                                        if(id!=0){
                                            //Toast.makeText(Database_main.this,"Successfully Saved",Toast.LENGTH_SHORT).show();
                                        }else{
                                            //Toast.makeText(Database_main.this,"Please try again later",Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }

                                if(f_csv[9].equals("mali*1") && !f_csv[9].equals("")){
                                    Log.v("data","positive------------------------------ ");

                                    Log.v("data","second contion ----------------------------");
                                    int count_student_number = sdb.Validate_same_studentnumber(f_csv[5]);
                                    if(count_student_number==0){
                                        long count_save = sdb.import_Student(f_csv[0],f_csv[1],f_csv[2],f_csv[3],f_csv[4],f_csv[5],f_csv[6],f_csv[7],f_csv[8]);
                                        if(count_save!=0){

                                            WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
                                            Display display = windowManager.getDefaultDisplay();
                                            Point point = new Point();
                                            display.getSize(point);
                                            int width = point.x;
                                            int height = point.y;
                                            int smallerDimension = width < height ? width : height;
                                            smallerDimension = smallerDimension * 3 / 4;

                                            qrgEncoder = new QRGEncoder(f_csv[5],null, QRGContents.Type.TEXT,smallerDimension);
                                            try{

                                                bitmap = qrgEncoder.encodeAsBitmap();
                                                //save sa database ang student.getText().toString() at smallerDimension
                                                // qrImage.setImageBitmap(bitmap);

                                                boolean save;
                                                String result;

                                                try{
                                                    save = QRGSaver.save(save_path,f_csv[5],bitmap,QRGContents.ImageType.IMAGE_JPEG);
                                                    result = save ? "Image saved" : "Image not saved ";
                                                    Toast.makeText(Database_main.this,result,Toast.LENGTH_SHORT).show();
                                                }catch (Exception e){
                                                    e.printStackTrace();
                                                }

                                            }catch (WriterException e){
                                                e.printStackTrace();
                                            } } }  }

                                if(f_csv[13].equals("sched*1") && !f_csv[13].equals("")){
                                    Log.v("data","pos sched -------------------------------------- ");
                                    scheddb.import_schedule(f_csv[0],f_csv[1],f_csv[2],f_csv[3],f_csv[4],f_csv[5],f_csv[6],f_csv[7],f_csv[8],f_csv[9],f_csv[10],f_csv[11],f_csv[12]);
                                }

                                if(f_csv[11].equals("record*12") && !f_csv[11].equals("")){
                                    recDb.import_record(f_csv[0],f_csv[1],f_csv[2],f_csv[3],f_csv[4],f_csv[10],f_csv[6],f_csv[7],f_csv[8],f_csv[9],f_csv[5]);
                                }

                                if(f_csv[2].equals("day*3") && !f_csv[2].equals("")){
                                    int count = scheddb.valCurrentday(f_csv[1]);
                                    if(count==0){
                                        scheddb.import_current_day(f_csv[0],f_csv[1]);
                                    }
                                }

                            }

                        }catch (Exception e){
                            e.printStackTrace();
                        }

                    }else{

                        if(!split[1].equals("")){
                            try{
                                File sd = Environment.getExternalStorageDirectory();
                                // File data = Environment.getDataDirectory();

                                Log.v("data","sd can write ----------------------------- "+sd.canWrite());

                                if(sd.canWrite()){
                                    File currentDbx = new File(file+"/"+split[1]);
                                    Log.v("data","import path db --------------- "+currentDbx);

                                    FileChannel src = new FileInputStream(currentDbx).getChannel();
                                    FileChannel dst = new FileOutputStream(currentDbpath).getChannel();

                                    Log.v("data","src ----------------- "+src+" ---------------------- "+dst);

                                    dst.transferFrom(src,0,src.size());
                                    src.close();
                                    dst.close();
                                    Toast.makeText(Database_main.this,"Successfully Imported Database !",Toast.LENGTH_SHORT).show();
                                }else{
                                    Log.v("data","kuppalllllllllllllllllllllllllll");
                                }

                            }catch (IOException e){
                                e.printStackTrace();
                            }
                        }

                    }
                    Toast.makeText(Database_main.this,"Successfully Restore ",Toast.LENGTH_SHORT).show();
                }else{
                    Log.v("data","aksdkasdkkadk-------------------------------------------------------");
                }
            }
        }
    }
}
