package com.example.attendance_monitoring;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class listView_classess_adapter extends BaseAdapter {
    Context context;
    String id[],section_name[];
    LayoutInflater inflater;

    public listView_classess_adapter(Context acontext, ArrayList<String> id,ArrayList<String> section_name){
        this.context = acontext;
        this.id = id.toArray(new String[0]);
        this.section_name = section_name.toArray(new String[0]);
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
    public View getView(final int i, View view, ViewGroup parent) {
        view = inflater.inflate(R.layout.classess_adapter_lay,null);

        final DatabaseHelper db = new DatabaseHelper(view.getRootView().getContext());
        final student_DBManager sdb = new student_DBManager(view.getRootView().getContext());
        sdb.open();

        final TextView classess = (TextView) view.findViewById(R.id.classess);
        final Button update = (Button) view.findViewById(R.id.update);
        final Button delete = (Button) view.findViewById(R.id.delete);
        final TextView count_student = (TextView) view.findViewById(R.id.count_student);

        classess.setText(section_name[i].substring(0,1).toUpperCase()+section_name[i].substring(1));

        int count_stud = sdb.getCount_students(String.valueOf(id[i]));
        count_student.setText(count_stud+" Students");

        final View finalView = view;
        classess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(finalView.getRootView().getContext(),manage_classess_main.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("id",id[i]);
                intent.putExtra("section_name",section_name[i]);


                finalView.getRootView().getContext().startActivity(intent);
            }
        });

        count_student.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(finalView.getRootView().getContext(),manage_classess_main.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("id",id[i]);
                intent.putExtra("section_name",section_name[i]);

                finalView.getRootView().getContext().startActivity(intent);
            }
        });

        final View finalView1 = view;
        update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(finalView1.getRootView().getContext());
                builder.setTitle("Update Section Name");

                final EditText class_name =  new EditText(finalView1.getRootView().getContext());
                class_name.setInputType(InputType.TYPE_CLASS_TEXT);
                class_name.setText(section_name[i]);

                LinearLayout lay = new LinearLayout(finalView1.getRootView().getContext());
                lay.setOrientation(LinearLayout.VERTICAL);
                lay.addView(class_name);

                builder.setView(lay);

                builder.setPositiveButton("Update Changes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(!class_name.getText().toString().equals("")){
                            int count = db.updateSection(class_name.getText().toString(),Integer.parseInt(id[i]));
                            if(count!=0){
                                Toast.makeText(finalView1.getRootView().getContext(),"Successfully Saved",Toast.LENGTH_SHORT).show();
                            }else{
                                Toast.makeText(finalView1.getRootView().getContext(),"Please Try Again Later!",Toast.LENGTH_SHORT).show();
                            }
                        }else{
                            Toast.makeText(finalView1.getRootView().getContext(),"Please Complete All the Fields ",Toast.LENGTH_SHORT).show();
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

        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(finalView1.getRootView().getContext());
                builder.setMessage("Do you want to remove Class name ( "+section_name[i]+" ) ?");

                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        db.removeSection(Integer.parseInt(id[i]));
                        Toast.makeText(finalView1.getRootView().getContext(),"Successfully Remove",Toast.LENGTH_SHORT).show();
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

        return view;
    }
}
