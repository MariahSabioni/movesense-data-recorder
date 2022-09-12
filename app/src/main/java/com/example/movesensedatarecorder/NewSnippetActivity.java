package com.example.movesensedatarecorder;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.example.movesensedatarecorder.model.Subject;
import com.example.movesensedatarecorder.utils.MsgUtils;
import com.example.movesensedatarecorder.utils.SavingUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;

import static com.example.movesensedatarecorder.DataActivity.EXTRAS_EXP_LOC;
import static com.example.movesensedatarecorder.DataActivity.EXTRAS_EXP_MOV;
import static com.example.movesensedatarecorder.DataActivity.EXTRAS_EXP_SUBJ;
import static com.example.movesensedatarecorder.DataActivity.EXTRAS_EXP_TIME;

public class NewSnippetActivity extends AppCompatActivity {

    private static final String TAG = NewSnippetActivity.class.getSimpleName();
    private Button buttonRecord;
    private Spinner subjSpinner, movSpinner, locSpinner, timeSpinner;
    private String FILE_NAME = "subjects_data";
    private List<Subject> subjSet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_snippet);

        boolean fileExist = fileExist(FILE_NAME);
        if (fileExist) {
            File oldfile = new File(getApplicationContext().getFilesDir(),FILE_NAME);
            try {
                subjSet = SavingUtils.readSubjectFile(FILE_NAME, oldfile);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                MsgUtils.showToast(getApplicationContext(),"add subjects");
                finish();
            }
        } else {
            MsgUtils.showToast(getApplicationContext(),"add subjects");
            finish();
            return;
        }

        ArrayList<String> subjects = new ArrayList<>();
        for (Subject s:subjSet){
            String subject = s.getName() + "_"+s.getLastName()+"_"+ s.getSubjID();
            subjects.add(subject);
        }

        //ui
        subjSpinner = findViewById(R.id.spinner_subject);
        ArrayAdapter adapter_subject = new ArrayAdapter(this, android.R.layout.simple_spinner_item, subjects);
        adapter_subject.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        subjSpinner.setAdapter(adapter_subject);

        movSpinner = findViewById(R.id.spinner_movement);
        String[] movementsRes = getResources().getStringArray(R.array.movement_values);
        List<String> movements = Arrays.asList(movementsRes);
        Collections.sort(movements);
        ArrayAdapter adapter_movement = new ArrayAdapter(this, android.R.layout.simple_spinner_item, movements);
        adapter_movement.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        movSpinner.setAdapter(adapter_movement);

        locSpinner = findViewById(R.id.spinner_location);
        ArrayAdapter<CharSequence> adapter_location = ArrayAdapter.createFromResource(this, R.array.location_values, android.R.layout.simple_spinner_item);
        adapter_location.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        locSpinner.setAdapter(adapter_location);

        timeSpinner = findViewById(R.id.spinner_time);
        ArrayAdapter<CharSequence> adapter_time = ArrayAdapter.createFromResource(this, R.array.time_values, android.R.layout.simple_spinner_item);
        adapter_time.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        timeSpinner.setAdapter(adapter_time);

        //save button listener
        buttonRecord = findViewById(R.id.button_record);
        buttonRecord.setOnClickListener(v -> {
            String subjConcat = subjSpinner.getSelectedItem().toString();
            String subjID = subjConcat.substring(subjConcat.lastIndexOf('_', subjConcat.lastIndexOf('_') - 1) + 1);
            setResult(Activity.RESULT_OK,
                    new Intent().putExtra(EXTRAS_EXP_SUBJ, subjID)
                            .putExtra(EXTRAS_EXP_MOV, movSpinner.getSelectedItem().toString())
                            .putExtra(EXTRAS_EXP_LOC, locSpinner.getSelectedItem().toString())
                            .putExtra(EXTRAS_EXP_TIME, timeSpinner.getSelectedItem().toString()));
            finish();
        });

    }
    private boolean fileExist(String fname) {
        File file = getBaseContext().getFileStreamPath(fname);
        return file.exists();
    }
}
