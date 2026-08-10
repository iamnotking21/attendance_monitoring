package com.example.attendance_monitoring;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class boys_attendance_record_summary_adapter extends BaseAdapter {
    Context context;
    String student_number[],lastname[],firstname[];
    String startdate,enddate;
    LayoutInflater inflater;
    public record_DBManager record_db;
    public boys_attendance_record_summary_adapter(Context acontext, ArrayList<String> student_number,ArrayList<String> lastname,ArrayList<String> firstname,String startdate,String enddate){
        this.context = acontext;
        this.startdate = startdate;
        this.enddate = enddate;
        this.student_number = student_number.toArray(new String[0]);
        this.lastname = lastname.toArray(new String[0]);
        this.firstname = firstname.toArray(new String[0]);
        inflater = (LayoutInflater.from(context));
    }

    @Override
    public int getCount() {
        return student_number.length;
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
    public View getView(int i, View view, ViewGroup parent) {
        view = inflater.inflate(R.layout.boys_attendance_record_summary_adapter_lay,null);

        record_db = new record_DBManager(view.getRootView().getContext());
        record_db.open();

        final TextView boys_name = (TextView) view.findViewById(R.id.boys_name);
        final TextView present = (TextView) view.findViewById(R.id.present);
        final TextView late = (TextView) view.findViewById(R.id.late);
        final TextView absent = (TextView) view.findViewById(R.id.absent);

        ArrayList<String> count_present = new ArrayList<>();
        ArrayList<String> count_late = new ArrayList<>();
        ArrayList<String> count_absent = new ArrayList<>();

        boys_name.setText(lastname[i]+", "+firstname[i]+" ");
        Log.v("data","start date ----------------------------------- "+startdate+" ------------------- "+enddate);

        Cursor cursor3 = record_db.getCount_present_late_absent(student_number[i],startdate,enddate);
        if(cursor3.moveToFirst()){
            do {

                String cpresent = cursor3.getString(cursor3.getColumnIndex("status_record"));
                Log.v("data","status record --------------------------------------- "+cpresent);
                if(cpresent.equals("Present")){
                    count_present.add(cpresent);
                }else if(cpresent.equals("Late")){
                    count_late.add(cpresent);
                }else if(cpresent.equals("Absent")){
                    count_absent.add(cpresent);
                }

            }while (cursor3.moveToNext());

            Log.v("data","count present --------------------------------------------------------------------- "+count_present.size());

            present.setText("# of Present "+"\n        "+String.valueOf(count_present.size()));
            late.setText("# of Late "+"\n        "+String.valueOf(count_late.size()));
            absent.setText("# of Absent "+"\n        "+String.valueOf(count_absent.size()));

        }cursor3.close();

        if(cursor3.getCount()==0){
            present.setText("# of Present "+"\n        "+0);
            late.setText("# of Late "+"\n        "+0);
            absent.setText("# of Absent "+"\n        "+0);

        }

        return view;
    }
}
