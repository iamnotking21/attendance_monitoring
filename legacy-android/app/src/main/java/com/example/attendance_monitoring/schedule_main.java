package com.example.attendance_monitoring;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class schedule_main extends AppCompatActivity implements SearchView.OnQueryTextListener, SearchView.OnCloseListener {
    private ImageView back;
    private ListView list_view_section_name;
    private SearchView searchView;
    public DatabaseHelper db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scheduler_main_lay);



        db = new DatabaseHelper(schedule_main.this);

        searchView = (SearchView) findViewById(R.id.search);
        searchView.setIconifiedByDefault(false);
        searchView.setOnQueryTextListener(this);
        searchView.setOnCloseListener(this);


        back = (ImageView) findViewById(R.id.back);
        list_view_section_name = (ListView) findViewById(R.id.list_view_section_name);



        back = (ImageView) findViewById(R.id.back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //startActivity(new Intent(schedule_main.this,panel_main.class));
                Intent backs =  new Intent(schedule_main.this,panel_main.class);
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

            scheduler_adapter_main_lay classess_adapter = new scheduler_adapter_main_lay(schedule_main.this,ids,sections_name);
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

                scheduler_adapter_main_lay classess_adapter = new scheduler_adapter_main_lay(schedule_main.this,ids,sections_name);
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

                    scheduler_adapter_main_lay classess_adapter = new scheduler_adapter_main_lay(schedule_main.this,id_s,sec_names_s);
                    list_view_section_name.setAdapter(classess_adapter);

                }cursor.close();
            }
        }


    }


}
