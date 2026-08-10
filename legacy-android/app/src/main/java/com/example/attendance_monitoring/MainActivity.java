package com.example.attendance_monitoring;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements SearchView.OnQueryTextListener, SearchView.OnCloseListener {
    private ListView listview_boys_present,listview_girls_present,late,listview_absent;
    private ImageView back;
    public DatabaseHelper db;
    public schedule_DBManager sched_db;
    public record_DBManager record_db;
    public student_DBManager stud_db;
    public SearchView searchView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new DatabaseHelper(MainActivity.this);

        sched_db = new schedule_DBManager(MainActivity.this);
        sched_db.open();

        record_db = new record_DBManager(MainActivity.this);
        record_db.open();

        stud_db = new student_DBManager(MainActivity.this);
        stud_db.open();

        ArrayList<String> section_name = new ArrayList<>();
        ArrayList<String> days = new ArrayList<>();
        ArrayList<String> month = new ArrayList<>();
        ArrayList<String> c_day = new ArrayList<>();

        String[] arawan;

        final Cursor cursor = db.getAllSection();
        if(cursor.moveToFirst()){
            do {
                String klase = cursor.getString(cursor.getColumnIndex("sections_name"));
                section_name.add(klase);
            }while (cursor.moveToNext());
        }cursor.close();

        int gh1 = 0,gh2 = 0,gh3 = 0,gh4 = 0,gh5 = 0,gh6 = 0,gh7 = 0,gh8 = 0,gh9 = 0,gh10 = 0,gh11 = 0,gh12 = 0;
        Cursor cursor1 = sched_db.getCurrentday();

        if(cursor1.getCount()!=0){
            if(cursor1.moveToFirst()){
                do {
                    String araw = cursor1.getString(cursor1.getColumnIndex("current_day"));
                    c_day.add(araw);
                }while (cursor1.moveToNext());

                if(c_day.size()!=0){
                    for(int b = 0 ; b < c_day.size(); b++){
                        arawan = c_day.get(b).split("-");
                        String f_day = arawan[0]+"-"+arawan[1]+"-"+arawan[2];
                        days.add(f_day);

                        if("Dec".equals(arawan[1])){
                            gh1++;
                            if(gh1==1){
                                month.add(arawan[1]+"-"+arawan[2]);
                            }
                        }

                        if("Jan".equals(arawan[1])){
                            gh2++;
                            if(gh2==1){
                                month.add(arawan[1]+"-"+arawan[2]);
                            }
                        }

                        if("Feb".equals(arawan[1])){
                            gh3++;
                            if(gh3==1){
                                month.add(arawan[1]+"-"+arawan[2]);
                            }
                        }

                        if("Mar".equals(arawan[1])){
                            gh4++;
                            if(gh4==1){
                                month.add(arawan[1]+"-"+arawan[2]);
                            }
                        }

                        if("Apr".equals(arawan[1])){
                            gh5++;
                            if(gh5==1){
                                month.add(arawan[1]+"-"+arawan[2]);
                            }
                        }

                        if("May".equals(arawan[1])){
                            gh6++;
                            if(gh6==1){
                                month.add(arawan[1]+"-"+arawan[2]);
                            }
                        }

                        if("Jun".equals(arawan[1])){
                            gh7++;
                            if(gh7==1){
                                month.add(arawan[1]+"-"+arawan[2]);
                            }
                        }

                        if("Sep".equals(arawan[1])){
                            gh8++;
                            if(gh8==1){
                                month.add(arawan[1]+"-"+arawan[2]);
                            }
                        }

                        if("Oct".equals(arawan[1])){
                            gh9++;
                            if(gh9==1){
                                month.add(arawan[1]+"-"+arawan[2]);
                            }
                        }

                        if("Jul".equals(arawan[1])){
                            gh10++;
                            if(gh10==1){
                                month.add(arawan[1]+"-"+arawan[2]);
                            }
                        }

                        if("Aug".equals(arawan[1])){
                            gh11++;
                            if(gh11==1){
                                month.add(arawan[1]+"-"+arawan[2]);
                            }
                        }

                        if("Nov".equals(arawan[1])){
                            gh12++;
                            if(gh12==1){
                                month.add(arawan[1]+"-"+arawan[2]);
                            }
                        }

                    }
                }
            }cursor1.close();
        }


        back = (ImageView) findViewById(R.id.back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //startActivity(new Intent(MainActivity.this,panel_main.class));
                Intent backs = new Intent(MainActivity.this,panel_main.class);
                backs.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(backs);
            }
        });


        listview_boys_present = (ListView) findViewById(R.id.list_view_present_boys);
        listview_girls_present = (ListView) findViewById(R.id.list_view_present_girls);
        late = (ListView) findViewById(R.id.listview_late);
        listview_absent = (ListView) findViewById(R.id.listview_absent);

        searchView = (SearchView) findViewById(R.id.search);
        searchView.setIconifiedByDefault(false);
        searchView.setOnQueryTextListener(this);
        searchView.setOnCloseListener(this);

    }


    public void spinner_fun(String query,ArrayList<String> present_student_number,ArrayList<String> present_student_number_bull,ArrayList<String> present_student_fname,ArrayList<String> present_student_lname,ArrayList<String> oras_title,ArrayList<String> oras_start,ArrayList<String> oras_end,ArrayList<String> oras_end_late,ArrayList<String> present_student_number_bull_girls, ArrayList<String> present_student_lname_girls,ArrayList<String> present_student_fname_girls,ArrayList<String> oras_titleg,ArrayList<String> oras_startg,ArrayList<String> oras_endg,ArrayList<String> oras_end_lateg,TextView count_present,ArrayList<String> present_student_number_bull_late,ArrayList<String> present_student_lname_late,ArrayList<String> present_student_fname_late,ArrayList<String> oras_titlel,ArrayList<String> oras_startl,ArrayList<String> oras_endl,ArrayList<String> oras_end_latel,TextView count_late,ArrayList<String> present_student_number_bulla,ArrayList<String> present_student_lnamea,ArrayList<String> present_student_fnamea,ArrayList<String> oras_titlea,ArrayList<String> oras_starta,ArrayList<String> oras_enda,ArrayList<String> oras_end_latea,TextView count_absent){
        int count = 0,count_g = 0;
        Log.v("data","query ----------------------------------------------------------------------- "+query);

        Date c = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
        String formattedDate = df.format(c);
        //String formattedDate = "02-Jan-2020";
        Log.v("data","current date "+formattedDate);


        boys_present_adapter boys_present;
        girsl_present_adapter girls_present;
        late_adapter lates;
        absent_adapter absent;

        if(!query.equals("")){

            ArrayList<String> section_id_bitch = db.searchSectionnamebitch(query);
            present_student_number.clear();
            for(int i = 0; i < section_id_bitch.size(); i++){
                Cursor cursor2 = record_db.get_Record(section_id_bitch.get(i),formattedDate);
                if(cursor2.moveToFirst()){
                    do {
                        String student_number = cursor2.getString(cursor2.getColumnIndex("student_number"));
                        Log.v("data","student number ---------------------------------------------- "+student_number);
                        present_student_number.add(student_number);
                    }while (cursor2.moveToNext());

                    for(int x = 0 ; x < present_student_number.size(); x++){
                        Cursor cursor3 = stud_db.getEstudyanteMale(present_student_number.get(x));
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
                                Cursor cursor4 = record_db.get_RecordbyStudentnumber(present_student_number_bull.get(r),formattedDate);
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

                                    boys_present = new boys_present_adapter(MainActivity.this,present_student_number_bull,present_student_fname,present_student_lname,oras_title,oras_start,oras_end,oras_end_late);
                                    listview_boys_present.setAdapter(boys_present);
                                    count = boys_present.getCount();

                                }cursor4.close();
                            }
                        }cursor3.close();
                    }
                    count_present.setText(String.valueOf(count));

                    for(int y = 0; y < present_student_number.size(); y++){
                        Cursor cursor3g = stud_db.getEstudyanteFemale(present_student_number.get(y));
                        if(cursor3g.moveToFirst()){
                            do {
                                String lname_girl = cursor3g.getString(cursor3g.getColumnIndex("lastname"));
                                String fname_girl = cursor3g.getString(cursor3g.getColumnIndex("firstname"));
                                String student_num_girl = cursor3g.getString(cursor3g.getColumnIndex("student_number"));

                                present_student_number_bull_girls.add(student_num_girl);
                                present_student_lname_girls.add(lname_girl);
                                present_student_fname_girls.add(fname_girl);

                                Cursor cursor4g = record_db.get_RecordbyStudentnumber(student_num_girl,formattedDate);

                                if(cursor4g.moveToFirst()){
                                    do {
                                        String titleg = cursor4g.getString(cursor4g.getColumnIndex("title"));
                                        String time_startg = cursor4g.getString(cursor4g.getColumnIndex("start_time"));
                                        String time_endg = cursor4g.getString(cursor4g.getColumnIndex("end_time"));
                                        String time_end_lateg = cursor4g.getString(cursor4g.getColumnIndex("end_time_late"));

                                        oras_titleg.add(titleg);
                                        oras_startg.add(time_startg);
                                        oras_endg.add(time_endg);
                                        oras_end_lateg.add(time_end_lateg);
                                    }while (cursor4g.moveToNext());

                                    girls_present = new girsl_present_adapter(MainActivity.this,present_student_number_bull_girls,present_student_fname_girls,present_student_lname_girls,oras_titleg,oras_startg,oras_endg,oras_end_lateg);
                                    listview_girls_present.setAdapter(girls_present);

                                    count_g = girls_present.getCount();
                                    int total = count+count_g;
                                    Log.v("data","countxx present ------------------------------------------------------------------------------ "+total);
                                    //count_present.setText(""+count_g);

                                }cursor4g.close();
                            }while (cursor3g.moveToNext());

                        }cursor3g.close();
                        //count_present.setText(""+count_g);
                    }

                    int tots = Integer.parseInt(count_present.getText().toString())+count_g;
                    count_present.setText(""+tots);

                }cursor2.close();
                //


                Cursor cursorlate = record_db.get_RecordLate(section_id_bitch.get(i),formattedDate);
                if (cursorlate.moveToFirst()) {
                    do {
                        String student_number_late = cursorlate.getString(cursorlate.getColumnIndex("student_number"));

                        Cursor cursor2late = stud_db.getEstudyante(student_number_late);
                        if(cursor2late.moveToFirst()){
                            do {

                                String lname_late = cursor2late.getString(cursor2late.getColumnIndex("lastname"));
                                String fname_late = cursor2late.getString(cursor2late.getColumnIndex("firstname"));
                                String student_num_late = cursor2late.getString(cursor2late.getColumnIndex("student_number"));

                                present_student_number_bull_late.add(student_num_late);
                                present_student_lname_late.add(lname_late);
                                present_student_fname_late.add(fname_late);

                                Cursor cursor3late = record_db.get_RecordbyStudentnumberLate(student_num_late,formattedDate);

                                if(cursor3late.moveToFirst()){
                                    do {
                                        String titlelate = cursor3late.getString(cursor3late.getColumnIndex("title"));
                                        String time_startlate = cursor3late.getString(cursor3late.getColumnIndex("start_time"));
                                        String time_endlate = cursor3late.getString(cursor3late.getColumnIndex("end_time"));
                                        String time_end_latelate = cursor3late.getString(cursor3late.getColumnIndex("end_time_late"));

                                        oras_titlel.add(titlelate);
                                        oras_startl.add(time_startlate);
                                        oras_endl.add(time_endlate);
                                        oras_end_latel.add(time_end_latelate);

                                        lates = new late_adapter(MainActivity.this,present_student_number_bull_late,present_student_fname_late,present_student_lname_late,oras_titlel,oras_startl,oras_endl,oras_end_latel);
                                        lates.notifyDataSetChanged();
                                        late.setAdapter(lates);
                                        int count_latex = lates.getCount();

                                        count_late.setText(String.valueOf(count_latex));

                                    }while (cursor3late.moveToNext());
                                }cursor3late.close();

                            }while (cursor2late.moveToNext());
                        }cursor2late.close();

                    } while (cursorlate.moveToNext());
                }cursorlate.close();
                //

                Cursor cursorabsent = record_db.get_RecordbyStudentnumberAbsent(section_id_bitch.get(i),formattedDate);
                if(cursorabsent.moveToFirst()){
                    do {
                        String student_number_absent = cursorabsent.getString(cursorabsent.getColumnIndex("student_number"));

                        Cursor cursor2absent = stud_db.getEstudyante(student_number_absent);
                        if(cursor2absent.moveToFirst()){
                            do {
                                String lname_boya = cursor2absent.getString(cursor2absent.getColumnIndex("lastname"));
                                String fname_boya = cursor2absent.getString(cursor2absent.getColumnIndex("firstname"));
                                String student_numa = cursor2absent.getString(cursor2absent.getColumnIndex("student_number"));

                                present_student_number_bulla.add(student_numa);
                                present_student_lnamea.add(lname_boya);
                                present_student_fnamea.add(fname_boya);

                                Cursor cursor3absent = record_db.get_RecordbyStudentnumberAbsentnumber(student_numa,formattedDate);
                                if(cursor3absent.moveToFirst()){
                                    do {


                                        String titlea = cursor3absent.getString(cursor3absent.getColumnIndex("title"));
                                        String time_starta = cursor3absent.getString(cursor3absent.getColumnIndex("start_time"));
                                        String time_enda = cursor3absent.getString(cursor3absent.getColumnIndex("end_time"));
                                        String time_end_latea = cursor3absent.getString(cursor3absent.getColumnIndex("end_time_late"));

                                        oras_titlea.add(titlea);
                                        oras_starta.add(time_starta);
                                        oras_enda.add(time_enda);
                                        oras_end_latea.add(time_end_latea);

                                    }while (cursor3absent.moveToNext());

                                    absent = new absent_adapter(MainActivity.this,present_student_number_bulla,present_student_fnamea,present_student_lnamea,oras_titlea,oras_starta,oras_enda,oras_end_latea);
                                    absent.notifyDataSetChanged();
                                    listview_absent.setAdapter(absent);

                                    int count_absentx = absent.getCount();
                                    count_absent.setText(String.valueOf(count_absentx));

                                }cursor3absent.close();
                            }while (cursor2absent.moveToNext());
                        }cursor2absent.close();
                    }while (cursorabsent.moveToNext());
                }cursorabsent.close();
            }

            if(section_id_bitch.size()==0){
                boys_present = new boys_present_adapter(MainActivity.this,present_student_number_bull,present_student_fname,present_student_lname,oras_title,oras_start,oras_end,oras_end_late);
                listview_boys_present.setAdapter(boys_present);
                count = boys_present.getCount();

                girls_present = new girsl_present_adapter(MainActivity.this,present_student_number_bull_girls,present_student_fname_girls,present_student_lname_girls,oras_titleg,oras_startg,oras_endg,oras_end_lateg);
                listview_girls_present.setAdapter(girls_present);

                count_g = girls_present.getCount();
                int total = count+count_g;
                count_present.setText(String.valueOf(total));

                lates = new late_adapter(MainActivity.this,present_student_number_bull_late,present_student_fname_late,present_student_lname_late,oras_titlel,oras_startl,oras_endl,oras_end_latel);
                late.setAdapter(lates);
                int count_latex = lates.getCount();

                count_late.setText(String.valueOf(count_latex));

                absent = new absent_adapter(MainActivity.this,present_student_number_bulla,present_student_fnamea,present_student_lnamea,oras_titlea,oras_starta,oras_enda,oras_end_latea);
                listview_absent.setAdapter(absent);

                int count_absentx = absent.getCount();
                count_absent.setText(String.valueOf(count_absentx));

            }

        }else{
            boys_present = new boys_present_adapter(MainActivity.this,present_student_number_bull,present_student_fname,present_student_lname,oras_title,oras_start,oras_end,oras_end_late);
            listview_boys_present.setAdapter(boys_present);
            count = boys_present.getCount();

            girls_present = new girsl_present_adapter(MainActivity.this,present_student_number_bull_girls,present_student_fname_girls,present_student_lname_girls,oras_titleg,oras_startg,oras_endg,oras_end_lateg);
            listview_girls_present.setAdapter(girls_present);

            count_g = girls_present.getCount();
            int total = count+count_g;
            count_present.setText(String.valueOf(total));

            lates = new late_adapter(MainActivity.this,present_student_number_bull_late,present_student_fname_late,present_student_lname_late,oras_titlel,oras_startl,oras_endl,oras_end_latel);
            late.setAdapter(lates);
            int count_latex = lates.getCount();

            count_late.setText(String.valueOf(count_latex));

            absent = new absent_adapter(MainActivity.this,present_student_number_bulla,present_student_fnamea,present_student_lnamea,oras_titlea,oras_starta,oras_enda,oras_end_latea);
            listview_absent.setAdapter(absent);

            int count_absentx = absent.getCount();
            count_absent.setText(String.valueOf(count_absentx));


        }


    }

    public boolean onQueryTextChange(String newText) {
        showResults(newText);
        Log.v("data","1");
        return false;
    }

    public boolean onQueryTextSubmit(String query) {
        showResults(query);
        Log.v("data","2");
        return false;
    }

    public boolean onClose() {
        showResults("");
        Log.v("data","2");
        return false;
    }

    private void showResults(String query){
        final TextView count_present = (TextView) findViewById(R.id.count_present);
        final TextView count_late = (TextView) findViewById(R.id.count_late);
        final TextView count_absent = (TextView) findViewById(R.id.count_absent);


        final ArrayList<String> present_student_number = new ArrayList<>();
        final ArrayList<String> present_student_lname = new ArrayList<>();
        final ArrayList<String> present_student_fname = new ArrayList<>();
        final ArrayList<String> present_student_number_bull = new ArrayList<>();


        final ArrayList<String> present_student_lnamea = new ArrayList<>();
        final ArrayList<String> present_student_fnamea = new ArrayList<>();
        final ArrayList<String> present_student_number_bulla = new ArrayList<>();

        final ArrayList<String> present_student_lname_late = new ArrayList<>();
        final ArrayList<String> present_student_fname_late = new ArrayList<>();
        final ArrayList<String> present_student_number_bull_late = new ArrayList<>();


        final ArrayList<String> present_student_lname_girls = new ArrayList<>();
        final ArrayList<String> present_student_fname_girls = new ArrayList<>();
        final ArrayList<String> present_student_number_bull_girls = new ArrayList<>();
        final ArrayList<String> oras_start = new ArrayList<>();
        final ArrayList<String> oras_end = new ArrayList<>();
        final ArrayList<String> oras_title = new ArrayList<>();
        final ArrayList<String> oras_end_late = new ArrayList<>();

        final ArrayList<String> oras_startl = new ArrayList<>();
        final ArrayList<String> oras_endl = new ArrayList<>();
        final ArrayList<String> oras_titlel = new ArrayList<>();
        final ArrayList<String> oras_end_latel = new ArrayList<>();

        final ArrayList<String> oras_startg = new ArrayList<>();
        final ArrayList<String> oras_endg = new ArrayList<>();
        final ArrayList<String> oras_titleg = new ArrayList<>();
        final ArrayList<String> oras_end_lateg = new ArrayList<>();

        final ArrayList<String> oras_starta = new ArrayList<>();
        final ArrayList<String> oras_enda = new ArrayList<>();
        final ArrayList<String> oras_titlea = new ArrayList<>();
        final ArrayList<String> oras_end_latea = new ArrayList<>();

        if(query.equals("")){
            Log.v("data","wala laman ------xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
            spinner_fun(query,present_student_number,present_student_number_bull,present_student_fname,present_student_lname,oras_title,oras_start,oras_end,oras_end_late,present_student_number_bull_girls,present_student_lname_girls,present_student_fname_girls,oras_titleg,oras_startg,oras_endg,oras_end_lateg,count_present,present_student_number_bull_late,present_student_lname_late,present_student_fname_late,oras_titlel,oras_startl,oras_endl,oras_end_latel,count_late,present_student_number_bulla,present_student_lnamea,present_student_fnamea,oras_titlea,oras_starta,oras_enda,oras_end_latea,count_absent);
        }else{
            spinner_fun(query,present_student_number,present_student_number_bull,present_student_fname,present_student_lname,oras_title,oras_start,oras_end,oras_end_late,present_student_number_bull_girls,present_student_lname_girls,present_student_fname_girls,oras_titleg,oras_startg,oras_endg,oras_end_lateg,count_present,present_student_number_bull_late,present_student_lname_late,present_student_fname_late,oras_titlel,oras_startl,oras_endl,oras_end_latel,count_late,present_student_number_bulla,present_student_lnamea,present_student_fnamea,oras_titlea,oras_starta,oras_enda,oras_end_latea,count_absent);
        }


    }
}
