package com.example.attendance_monitoring;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.WriterException;

import java.io.File;
import java.util.ArrayList;

import androidmads.library.qrgenearator.QRGContents;
import androidmads.library.qrgenearator.QRGEncoder;
import androidmads.library.qrgenearator.QRGSaver;

public class student_info_main extends AppCompatActivity {
    private ImageView back,edit,delete,qrImage;
    private EditText student_number,lastname,firstname,middlename;
    private Spinner pick_status,section,genderx;
    private Button update;
    public QRGEncoder qrgEncoder;
    private String save_path = Environment.getExternalStorageDirectory().getPath() + "/Download/";
    public Bitmap bitmap;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.student_info_main_lay);

        final student_DBManager sdb = new student_DBManager(student_info_main.this);
        sdb.open();

        back = (ImageView) findViewById(R.id.back);
        edit = (ImageView) findViewById(R.id.edit);
        student_number = (EditText) findViewById(R.id.student_number);
        lastname = (EditText) findViewById(R.id.lastname);
        firstname = (EditText) findViewById(R.id.firstname);
        middlename = (EditText) findViewById(R.id.middlename);

        pick_status = (Spinner) findViewById(R.id.pick_status);
        section = (Spinner) findViewById(R.id.section);
        genderx = (Spinner) findViewById(R.id.gender);

        update = (Button) findViewById(R.id.update);
        delete = (ImageView) findViewById(R.id.delete);

        qrImage = (ImageView) findViewById(R.id.qrImage);

        Intent intent = getIntent();
        final String id = intent.getStringExtra("id");
        final String f_id = intent.getStringExtra("f_id");
        final String student_numberwww = intent.getStringExtra("studentnumber");
        final String sec_ids = intent.getStringExtra("sec_id");
        final String sections_names = intent.getStringExtra("section_names");
        Log.v("data","id----->"+id);

        qrImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LayoutInflater inflater = getLayoutInflater();
                View alert = inflater.inflate(R.layout.custom_dialog_show_qrimage,null);

                final ImageView showqrimage = (ImageView) alert.findViewById(R.id.showqrimage);

                File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath());
                Log.v("data","directory "+file);

                String direc = file+"/Download/"+student_numberwww+".jpg";

                Bitmap yourSelectedImage1 = BitmapFactory.decodeFile(direc);
                showqrimage.setImageBitmap(yourSelectedImage1);

                AlertDialog.Builder builder = new AlertDialog.Builder(student_info_main.this);
                builder.setView(alert);

                AlertDialog dialog = builder.create();
                dialog.show();

            }
        });

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent1 = new Intent(student_info_main.this,manage_classess_main.class);
                intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent1.putExtra("id",f_id);
                intent1.putExtra("section_name",sections_names);
                student_info_main.this.startActivity(intent1);
            }
        });
        ArrayList<String> sections_class = new ArrayList<>();

        sections_class.add(sections_names);
        Cursor cursor1 = sdb.getAllSection();
        if(cursor1.moveToFirst()){
            do {
                String sec = cursor1.getString(cursor1.getColumnIndex("sections_name"));
                if(!sections_names.equals(sec)){
                    sections_class.add(sec);
                }
            }while (cursor1.moveToNext());

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,sections_class);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            section.setAdapter(adapter);

        }cursor1.close();


        String pick_statuss,student_numbers,lastnames,firstnames,middlenames,genders,qr_code_bitmaps;
        ArrayList<String> pick = new ArrayList<>();
        ArrayList<String> gender_pick = new ArrayList<>();

        Cursor cursor = sdb.get_record_students(Integer.parseInt(id));
        if(cursor.moveToFirst()){
            do {
                student_numbers = cursor.getString(cursor.getColumnIndex("student_number"));
                lastnames = cursor.getString(cursor.getColumnIndex("lastname"));
                firstnames = cursor.getString(cursor.getColumnIndex("firstname"));
                middlenames = cursor.getString(cursor.getColumnIndex("middlename"));
                genders = cursor.getString(cursor.getColumnIndex("gender"));
                qr_code_bitmaps = cursor.getString(cursor.getColumnIndex("qr_code_bitmap"));
                pick_statuss = cursor.getString(cursor.getColumnIndex("pick_status"));
                if(pick_statuss.equals("active")){
                    pick.add(pick_statuss);
                    pick.add("InActive");
                    pick.add("Excuse");
                }else if(pick_statuss.equals("InActive")){
                    pick.add(pick_statuss);
                    pick.add("active");
                    pick.add("Excuse");
                }else if(pick_statuss.equals("Excuse")){
                    pick.add(pick_statuss);
                    pick.add("active");
                    pick.add("InActive");
                }

                if(genders.equals("Male")){
                    gender_pick.add(genders);
                    gender_pick.add("Female");
                }else{
                    gender_pick.add(genders);
                    gender_pick.add("Male");
                }

                student_number.setText(student_numbers);
                lastname.setText(lastnames);
                firstname.setText(firstnames);
                middlename.setText(middlenames);

            }while (cursor.moveToNext());

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,pick);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            pick_status.setAdapter(adapter);

            ArrayAdapter<String> gen_adap = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,gender_pick);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            genderx.setAdapter(gen_adap);


        }cursor.close();

        student_number.setEnabled(false);
        lastname.setEnabled(false);
        firstname.setEnabled(false);
        middlename.setEnabled(false);
        pick_status.setEnabled(false);
        section.setEnabled(false);
        genderx.setEnabled(false);
        update.setEnabled(false);
        qrImage.setEnabled(false);

        edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(student_number.isEnabled()==false){
                    student_number.setEnabled(true);
                    lastname.setEnabled(true);
                    firstname.setEnabled(true);
                    middlename.setEnabled(true);
                    pick_status.setEnabled(true);
                    section.setEnabled(true);
                    update.setEnabled(true);
                    qrImage.setEnabled(true);
                    genderx.setEnabled(true);
                }else{
                    student_number.setEnabled(false);
                    lastname.setEnabled(false);
                    firstname.setEnabled(false);
                    middlename.setEnabled(false);
                    pick_status.setEnabled(false);
                    section.setEnabled(false);
                    genderx.setEnabled(false);
                    update.setEnabled(false);
                    qrImage.setEnabled(false);
                }

            }
        });

        update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(student_number.isEnabled()==true){
                    final AlertDialog.Builder builder = new AlertDialog.Builder(student_info_main.this);

                    builder.setMessage("Are you sure ? Do you want to change this ?");

                    builder.setPositiveButton("Update Changes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if(!student_number.getText().toString().equals("") || !lastname.getText().toString().equals("") || !firstname.getText().toString().equals("") || !middlename.getText().toString().equals("") ){
                                Cursor cursor2 = sdb.getidSection(section.getSelectedItem().toString());
                                String id_sec;
                                if(cursor2.moveToFirst()){
                                    do {
                                        id_sec = cursor2.getString(cursor2.getColumnIndex("id"));
                                    }while (cursor2.moveToNext());
                                    Log.v("data","id "+id_sec);

                                    int val_id = sdb.Validate_same_studentnumber(student_number.getText().toString());
                                    if(val_id!=0){
                                        Toast.makeText(student_info_main.this,"Student Number Exists! Please Choose Other !",Toast.LENGTH_SHORT).show();
                                    }else{
                                        int count_number = sdb.update_student_number(Integer.parseInt(id),student_number.getText().toString());
                                        Log.v("data","old student number "+student_numberwww);

                                        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath());
                                        Log.v("data","directory "+file);

                                        String direc = file+"/Download/"+student_numberwww+".jpg";

                                        File file1 = new File(direc);
                                        Boolean val_del = file1.delete();
                                        if(val_del==true){

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
                                                    Toast.makeText(student_info_main.this,result,Toast.LENGTH_SHORT).show();
                                                }catch (Exception e){
                                                    e.printStackTrace();
                                                }


                                            }catch (WriterException e){
                                                e.printStackTrace();
                                            }

                                        }else{
                                            Log.v("data","can't delete image ");
                                        }

                                        //delete old qr code image from internal

                                        //create new qr code with new student number

                                        if(count_number == 0){
                                            Toast.makeText(student_info_main.this,"Please try again Later !",Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                    int count = sdb.update_student_record(Integer.parseInt(id),lastname.getText().toString(),firstname.getText().toString(),middlename.getText().toString(),pick_status.getSelectedItem().toString(),id_sec,genderx.getSelectedItem().toString());
                                    if(count!=0){
                                        Toast.makeText(student_info_main.this,"Successfully Saved",Toast.LENGTH_SHORT).show();
                                    }else{
                                        Toast.makeText(student_info_main.this,"Please try again Later !",Toast.LENGTH_SHORT).show();
                                    }
                                }cursor2.close();
                            }else{
                                Toast.makeText(student_info_main.this,"PLease complete all the fields !",Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });

                    builder.show();

                }else{
                    Toast.makeText(student_info_main.this,"Please click the edit button !",Toast.LENGTH_SHORT).show();
                }
            }
        });

        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(student_info_main.this);

                builder.setTitle("Do you want to remove this student number "+student_number.getText().toString()+" ?");

                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sdb.remove_student(Integer.parseInt(id));
                        Toast.makeText(student_info_main.this,"Successfully Remove !",Toast.LENGTH_SHORT).show();
                    }
                });

                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });

                builder.show();
            }
        });

    }
}
