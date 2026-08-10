package com.example.attendance_monitoring;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.WriterException;
import com.opencsv.CSVReader;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

import androidmads.library.qrgenearator.QRGContents;
import androidmads.library.qrgenearator.QRGEncoder;
import androidmads.library.qrgenearator.QRGSaver;

public class manage_classess_main extends AppCompatActivity implements SearchView.OnQueryTextListener, SearchView.OnCloseListener {
    private ImageView back,add_student,excel,qrImage;
    private ListView list_view_section_name;
    public student_DBManager sdb;
    public String id,section_names;
    private SearchView searchView;
    private String save_path = Environment.getExternalStorageDirectory().getPath() + "/Download/";
    public Bitmap bitmap;
    private TextView class_name;
    public QRGEncoder qrgEncoder;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.manage_classess_main_lay);

        sdb = new student_DBManager(manage_classess_main.this);
        sdb.open();

        Intent intent = getIntent();
         id = intent.getStringExtra("id");
        section_names = intent.getStringExtra("section_name");

        searchView = (SearchView) findViewById(R.id.search);
        searchView.setIconifiedByDefault(false);
        searchView.setOnQueryTextListener(this);
        searchView.setOnCloseListener(this);

        back = (ImageView) findViewById(R.id.back);
        add_student = (ImageView) findViewById(R.id.add_student);
        list_view_section_name = (ListView) findViewById(R.id.list_view_section_name);
        excel = (ImageView) findViewById(R.id.excel);
        class_name = (TextView) findViewById(R.id.class_name);

        class_name.setText(section_names);

        excel.setOnClickListener(new View.OnClickListener() {
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
                                234);

                        // MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE is an
                        // app-defined int constant that should be quite unique

                        return;
                    }
                }

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("text/comma-separated-values");
                startActivityForResult(Intent.createChooser(intent, "Open CSV"), 234);
            }
        });

        add_student.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LayoutInflater inflater = getLayoutInflater();
                View alertLayout = inflater.inflate(R.layout.custom_dialog_add_students,null);
                final TextView section_namex = alertLayout.findViewById(R.id.section_namex);
                final EditText lastname = alertLayout.findViewById(R.id.lastname);
                final EditText firstname = alertLayout.findViewById(R.id.firstname);
                final EditText middlename = alertLayout.findViewById(R.id.middlename);
                final Spinner gender = alertLayout.findViewById(R.id.gender);
                final EditText student_number = alertLayout.findViewById(R.id.student_number);

                section_namex.setText("       Section Name: "+section_names);

                ArrayList<String> kasarian = new ArrayList<>();
                kasarian.add("Male");
                kasarian.add("Female");

                ArrayAdapter<String> adapter = new ArrayAdapter<String>(manage_classess_main.this,android.R.layout.simple_spinner_item,kasarian);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                gender.setAdapter(adapter);

                AlertDialog.Builder builder = new AlertDialog.Builder(manage_classess_main.this);
                builder.setTitle("Add Students");

                builder.setView(alertLayout);

                builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(!lastname.getText().toString().equals("") || !firstname.getText().toString().equals("") || !middlename.getText().toString().equals("") || !student_number.getText().toString().equals("")){
                            int count_student_number = sdb.Validate_same_studentnumber(student_number.getText().toString());
                            if(count_student_number!=0){
                                Toast.makeText(manage_classess_main.this,"Student number exists",Toast.LENGTH_SHORT).show();
                            }else{
                                long count = sdb.insert_Student(Integer.parseInt(id),lastname.getText().toString(),firstname.getText().toString(),middlename.getText().toString(),student_number.getText().toString(),gender.getSelectedItem().toString(),String.valueOf(1));
                                if(count!=0){
                                    Toast.makeText(manage_classess_main.this,"Save Successfully and generate QR Code",Toast.LENGTH_SHORT).show();
                                    Log.v("data","save");

                                    //generate qr code

                                    //qrImage = (ImageView) findViewById(R.id.qrImage);


                                    WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
                                    Display display = windowManager.getDefaultDisplay();
                                    Point point = new Point();
                                    display.getSize(point);
                                    int width = point.x;
                                    int height = point.y;
                                    int smallerDimension = width < height ? width : height;
                                    smallerDimension = smallerDimension * 3 / 4;

                                    Log.v("data","student number "+student_number.getText().toString() +" "+ smallerDimension);
                                    qrgEncoder = new QRGEncoder(student_number.getText().toString(),null, QRGContents.Type.TEXT,smallerDimension);
                                    try{

                                        bitmap = qrgEncoder.encodeAsBitmap();
                                        //save sa database ang student.getText().toString() at smallerDimension
                                        // qrImage.setImageBitmap(bitmap);

                                        boolean save;
                                        String result;

                                        try{
                                            save = QRGSaver.save(save_path,student_number.getText().toString(),bitmap,QRGContents.ImageType.IMAGE_JPEG);
                                            result = save ? "Image saved" : "Image not saved ";
                                            Toast.makeText(manage_classess_main.this,result,Toast.LENGTH_SHORT).show();
                                        }catch (Exception e){
                                            e.printStackTrace();
                                        }

                                    }catch (WriterException e){
                                        e.printStackTrace();
                                    }

                                }else{
                                    Toast.makeText(manage_classess_main.this,"Please try again later",Toast.LENGTH_SHORT).show();
                                }
                            }
                        }else{
                            Toast.makeText(manage_classess_main.this,"Please Complete All the Fields",Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });



                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //startActivity(new Intent(manage_classess_main.this,classess.class));
                Intent backs = new Intent(manage_classess_main.this,classess.class);
                backs.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(backs);
            }
        });

        Cursor cursor = sdb.getStudents(id);

        ArrayList<String> lastname = new ArrayList<>();
        ArrayList<String> firstname = new ArrayList<>();
        ArrayList<String> middlename = new ArrayList<>();
        ArrayList<String> gender = new ArrayList<>();
        ArrayList<String> qr_code_bitmap = new ArrayList<>();
        ArrayList<String> student_number = new ArrayList<>();
        ArrayList<String> pick_status = new ArrayList<>();
        ArrayList<String> primary_id = new ArrayList<>();
        ArrayList<String> section_id = new ArrayList<>();
        String sec_id;
        if(cursor.moveToFirst()){
            do {
                final String idx = cursor.getString(cursor.getColumnIndex("id"));
                final String lastnames = cursor.getString(cursor.getColumnIndex("lastname"));
                final String firstnames = cursor.getString(cursor.getColumnIndex("firstname"));
                final String middlenames = cursor.getString(cursor.getColumnIndex("middlename"));
                final String student_numbers = cursor.getString(cursor.getColumnIndex("student_number"));
                final String pick_statuss = cursor.getString(cursor.getColumnIndex("pick_status"));
                sec_id = cursor.getString(cursor.getColumnIndex("section_id"));

                primary_id.add(idx);
                lastname.add(lastnames);
                firstname.add(firstnames);
                middlename.add(middlenames);
                student_number.add(student_numbers);
                pick_status.add(pick_statuss);
                section_id.add(sec_id);

            }while (cursor.moveToNext());

            students_record_adapter student = new students_record_adapter(manage_classess_main.this,primary_id,lastname,firstname,middlename,student_number,pick_status,id,section_names,sec_id);
            list_view_section_name.setAdapter(student);

        }cursor.close();


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 234: {
                if (resultCode == RESULT_OK) {
                    //Log.v("Data","full path ------------------ "+data.getData().getPath());
                    File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath());
                    String[] split = data.getData().getPath().split(":");
                     Log.v("data","split data "+split[1]);
                    //Log.v("data","path "+file+"/"+"documents/SalesJan2009.csv");
                    import_csv(new File(file+"/"+split[1]),new File(split[1]));
                }
            }
        }
    }



    public void import_csv(File from,File bullshit){

        try{
            student_DBManager sdb = new student_DBManager(manage_classess_main.this);
            sdb.open();

            Log.v("data","from ---------------------------------------------------"+from);

            CSVReader csvReader = null;

            if(from.exists()){
                csvReader = new CSVReader(new FileReader(from));
               // Log.v("data","split value ----------------------------------------------------- "+bullshit);
            }else{
                csvReader = new CSVReader(new FileReader(bullshit));
            }

            String[] f_csv;
            String err_msg = "";
            String err_section = "";
            String good_msg = "";
            String please_msg = "";
            String student_exists_msg = "";
            String student_exists_number_msg = "";
            while((f_csv = csvReader.readNext())!= null){
                Log.v("data","students name --------------- "+f_csv[0]+"-----------"+f_csv[1]+"-----------"+f_csv[2]+"-----------"+f_csv[3]+"-----------"+f_csv[4]+"-----------"+f_csv[5]+"\n");

                if(!"Sections".equals(f_csv[5])){
                    Cursor cursor = sdb.getidSection(f_csv[5]);
                    Log.v("data","count section "+cursor.getCount());
                    if(cursor.getCount()==0){
                        err_msg = "Section name doesn't exists  !  Please sure this section is already saved !! ";
                        err_section = f_csv[5];
                    }else{
                        //save
                        if(section_names.equals(f_csv[5])){
                            if(!"Student Number".equals(f_csv[4])){
                                int count_student_number = sdb.Validate_same_studentnumber(f_csv[4]);
                                if(count_student_number==0){
                                    long count_save = sdb.insert_Student(Integer.parseInt(id),f_csv[0],f_csv[1],f_csv[2],f_csv[4],f_csv[3],String.valueOf(1));
                                    if(count_save!=0){
                                        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
                                        Display display = windowManager.getDefaultDisplay();
                                        Point point = new Point();
                                        display.getSize(point);
                                        int width = point.x;
                                        int height = point.y;
                                        int smallerDimension = width < height ? width : height;
                                        smallerDimension = smallerDimension * 3 / 4;

                                        Log.v("data","student number "+f_csv[4] +" "+ smallerDimension);
                                        qrgEncoder = new QRGEncoder(f_csv[4],null, QRGContents.Type.TEXT,smallerDimension);
                                        try{

                                            bitmap = qrgEncoder.encodeAsBitmap();
                                            //save sa database ang student.getText().toString() at smallerDimension
                                            // qrImage.setImageBitmap(bitmap);

                                            boolean save;
                                            String result;

                                            try{
                                                save = QRGSaver.save(save_path,f_csv[4],bitmap,QRGContents.ImageType.IMAGE_JPEG);
                                                result = save ? "Image saved" : "Image not saved ";
                                                Toast.makeText(manage_classess_main.this,result,Toast.LENGTH_SHORT).show();
                                            }catch (Exception e){
                                                e.printStackTrace();
                                            }

                                        }catch (WriterException e){
                                            e.printStackTrace();
                                        }


                                        //Toast.makeText(manage_classess_main.this,"Import Students Successfully ",Toast.LENGTH_SHORT).show();
                                        good_msg = "Import Students Successfully ";
                                    }else{
                                        //Toast.makeText(manage_classess_main.this,"Please try again later !",Toast.LENGTH_SHORT).show();
                                        please_msg = "Please try again later !";
                                    }
                                }else{
                                    Toast.makeText(manage_classess_main.this,"Student Number "+f_csv[4]+" exists ",Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    }
                }
            }
            if(!err_msg.equals("")){
                Toast.makeText(manage_classess_main.this,err_msg+" "+err_section,Toast.LENGTH_SHORT).show();
            }
            if(!good_msg.equals("")){
                Toast.makeText(manage_classess_main.this,good_msg,Toast.LENGTH_SHORT).show();
            }
            if(!please_msg.equals("")){
                Toast.makeText(manage_classess_main.this,please_msg,Toast.LENGTH_SHORT).show();
            }
        }catch (Exception e){
            e.printStackTrace();
        }

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
        ArrayList<String> lastname_s = new ArrayList<>();
        ArrayList<String> firstname_s = new ArrayList<>();
        ArrayList<String> middlename_s = new ArrayList<>();
        ArrayList<String> student_number_s = new ArrayList<>();
        ArrayList<String> pick_status_s = new ArrayList<>();
        ArrayList<String> primary_id_s = new ArrayList<>();
        ArrayList<String> section_id_s = new ArrayList<>();
        String sec_id_s;

        if(query.equals("")){
            Log.v("data","3");

            Cursor cursor = sdb.getStudents(id);

            ArrayList<String> lastname = new ArrayList<>();
            ArrayList<String> firstname = new ArrayList<>();
            ArrayList<String> middlename = new ArrayList<>();
            ArrayList<String> student_number = new ArrayList<>();
            ArrayList<String> pick_status = new ArrayList<>();
            ArrayList<String> primary_id = new ArrayList<>();
            ArrayList<String> section_id = new ArrayList<>();
            String sec_id;
            if(cursor.moveToFirst()){
                do {
                    final String idx = cursor.getString(cursor.getColumnIndex("id"));
                    final String lastnames = cursor.getString(cursor.getColumnIndex("lastname"));
                    final String firstnames = cursor.getString(cursor.getColumnIndex("firstname"));
                    final String middlenames = cursor.getString(cursor.getColumnIndex("middlename"));
                    final String student_numbers = cursor.getString(cursor.getColumnIndex("student_number"));
                    final String pick_statuss = cursor.getString(cursor.getColumnIndex("pick_status"));
                    sec_id = cursor.getString(cursor.getColumnIndex("section_id"));

                    primary_id.add(idx);
                    lastname.add(lastnames);
                    firstname.add(firstnames);
                    middlename.add(middlenames);
                    student_number.add(student_numbers);
                    pick_status.add(pick_statuss);
                    section_id.add(sec_id);

                }while (cursor.moveToNext());

                students_record_adapter student = new students_record_adapter(manage_classess_main.this,primary_id,lastname,firstname,middlename,student_number,pick_status,id,section_names,sec_id);
                list_view_section_name.setAdapter(student);

            }cursor.close();

        }else{
            Cursor cursor = sdb.search_student(query,id);
            if(cursor != null){
                if(cursor.moveToFirst()){
                    do {
                        final String idx_s = cursor.getString(cursor.getColumnIndex("id"));
                        final String lastnames_s = cursor.getString(cursor.getColumnIndex("lastname"));
                        final String firstnames_s = cursor.getString(cursor.getColumnIndex("firstname"));
                        final String middlenames_s = cursor.getString(cursor.getColumnIndex("middlename"));
                        final String student_numbers_s = cursor.getString(cursor.getColumnIndex("student_number"));
                        final String pick_statuss_s = cursor.getString(cursor.getColumnIndex("pick_status"));
                        sec_id_s = cursor.getString(cursor.getColumnIndex("section_id"));

                        primary_id_s.add(idx_s);
                        lastname_s.add(lastnames_s);
                        firstname_s.add(firstnames_s);
                        middlename_s.add(middlenames_s);
                        student_number_s.add(student_numbers_s);
                        pick_status_s.add(pick_statuss_s);
                        section_id_s.add(sec_id_s);

                    }while (cursor.moveToNext());


                    students_record_adapter student = new students_record_adapter(manage_classess_main.this,primary_id_s,lastname_s,firstname_s,middlename_s,student_number_s,pick_status_s,id,section_names,sec_id_s);
                    list_view_section_name.setAdapter(student);

                }cursor.close();
            }
        }


    }

}
