package com.example.attendance_monitoring;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.util.ArrayList;

public class classess extends Activity implements SearchView.OnQueryTextListener, SearchView.OnCloseListener {
    private ImageView add_class,back;
    private ListView list_view_section_name;
    private SearchView searchView;
    public DatabaseHelper db;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.classess_main_lay);

         db = new DatabaseHelper(classess.this);

        searchView = (SearchView) findViewById(R.id.search);
        searchView.setIconifiedByDefault(false);
        searchView.setOnQueryTextListener(this);
        searchView.setOnCloseListener(this);

        add_class = (ImageView) findViewById(R.id.add_class);
        back = (ImageView) findViewById(R.id.back);
        list_view_section_name = (ListView) findViewById(R.id.list_view_section_name);

        add_class.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(classess.this);

                builder.setTitle("Section Name");

                final EditText class_name =  new EditText(classess.this);
                class_name.setInputType(InputType.TYPE_CLASS_TEXT);
                class_name.setHint("Class Name:                                                   *");

                LinearLayout lay = new LinearLayout(classess.this);
                lay.setOrientation(LinearLayout.VERTICAL);
                lay.addView(class_name);

                builder.setView(lay);

                builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(!class_name.getText().toString().equals("")){
                            int count = db.Validate_if_Same(class_name.getText().toString());
                            if(count!=0){
                                Toast.makeText(classess.this,"Section Name is Exists! Please Input Another",Toast.LENGTH_SHORT).show();
                            }else{
                                long id = db.insertSection(class_name.getText().toString(),"1");
                                if(id!=0){
                                    Toast.makeText(classess.this,"Successfully Saved",Toast.LENGTH_SHORT).show();
                                }else{
                                    Toast.makeText(classess.this,"Please try again later",Toast.LENGTH_SHORT).show();
                                }
                            }
                        }else{
                            Toast.makeText(classess.this,"Please Complete All the Fields ",Toast.LENGTH_SHORT).show();
                        }
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

        back = (ImageView) findViewById(R.id.back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //startActivity(new Intent(classess.this,panel_main.class));
                Intent backs = new Intent(classess.this,panel_main.class);
                backs.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(backs);
            }
        });

        final ArrayList<String> ids = new ArrayList<>();
        final ArrayList<String> sections_name = new ArrayList<>();

        Cursor cursor = db.getAllSection();
        if(cursor.moveToFirst()){
            do {
                int id = cursor.getInt(cursor.getColumnIndex("id"));
                String section_name = cursor.getString(cursor.getColumnIndex("sections_name"));
                ids.add(String.valueOf(id));
                sections_name.add(section_name);

            }while (cursor.moveToNext());

            listView_classess_adapter classess_adapter = new listView_classess_adapter(classess.this,ids,sections_name);
            list_view_section_name.setAdapter(classess_adapter);

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
        ArrayList<String> id_s = new ArrayList<>();
        ArrayList<String> sec_names_s = new ArrayList<>();
        ArrayList<String> sec_status_s = new ArrayList<>();

        if(query.equals("")){
            Log.v("data","3");

            final ArrayList<String> ids = new ArrayList<>();
            final ArrayList<String> sections_name = new ArrayList<>();

            Cursor cursor = db.getAllSection();
            if(cursor.moveToFirst()){
                do {
                    int id = cursor.getInt(cursor.getColumnIndex("id"));
                    String section_name = cursor.getString(cursor.getColumnIndex("sections_name"));
                    ids.add(String.valueOf(id));
                    sections_name.add(section_name);

                }while (cursor.moveToNext());

                listView_classess_adapter classess_adapter = new listView_classess_adapter(classess.this,ids,sections_name);
                list_view_section_name.setAdapter(classess_adapter);

            }cursor.close();

        }else{
            Cursor cursor = db.search(query);
            if(cursor != null){
                if(cursor.moveToFirst()){
                    do {
                        final String id_s_s = cursor.getString(cursor.getColumnIndex("id"));
                        final String sec_names_s_s = cursor.getString(cursor.getColumnIndex("sections_name"));
                        final String sec_status_s_s = cursor.getString(cursor.getColumnIndex("status_section"));

                        id_s.add(id_s_s);
                        sec_names_s.add(sec_names_s_s);
                        sec_status_s.add(sec_status_s_s);

                    }while (cursor.moveToNext());

                    if(id_s.size()==0){
                        Log.v("data","null");
                    }

                    listView_classess_adapter classess_adapter = new listView_classess_adapter(classess.this,id_s,sec_names_s);
                    list_view_section_name.setAdapter(classess_adapter);

                }cursor.close();
            }
        }


    }

}
