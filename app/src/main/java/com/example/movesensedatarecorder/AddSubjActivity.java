package com.example.movesensedatarecorder;

import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.movesensedatarecorder.model.Subject;
import com.example.movesensedatarecorder.utils.MsgUtils;
import com.example.movesensedatarecorder.utils.SavingUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;

public class AddSubjActivity extends AppCompatActivity {

    private static final String TAG = AddSubjActivity.class.getSimpleName();
    private String IDnum;
    private TextView subjID;
    private EditText name, lastName, height, weight, email;
    private Button buttonSaveSubj;
    private Handler handlerSave;
    private List<Subject> subjSet = new ArrayList<>();
    private String FILE_NAME = "subjects_data";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_subj);

        //ui
        subjID = findViewById(R.id.textView_subj_id);
        name = findViewById(R.id.edit_text_name);
        lastName = findViewById(R.id.edit_text_last_name);
        height = findViewById(R.id.edit_text_height);
        weight = findViewById(R.id.edit_text_weight);
        email = findViewById(R.id.edit_text_email);
        buttonSaveSubj = findViewById(R.id.button_save);

        //save button listener
        buttonSaveSubj.setOnClickListener(v -> {
            try {
                saveSubj();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                MsgUtils.showToast(getApplicationContext(),"could not save subject");
            }
        });

        //set subj ID
        IDnum = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        subjID.setText("Subject ID: " + IDnum);
    }

    private void saveSubj() throws IOException, ClassNotFoundException {
        //error handler to fill all fields
        String mName = String.valueOf(name.getText());
        String mLastName = String.valueOf(lastName.getText());
        String mWeight = String.valueOf(weight.getText());
        String mHeight = String.valueOf(height.getText());
        String mEmail = String.valueOf(email.getText());
        double dHeight, dWeight;
        if (TextUtils.isEmpty(mName)) {
            name.setError("Name is required");
            name.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(mLastName)) {
            lastName.setError("Last name is required");
            lastName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(mHeight)) {
            height.setError("Height is required");
            height.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(mWeight)) {
            weight.setError("Weight is required");
            weight.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(mEmail)) {
            email.setError("Email is required");
            email.requestFocus();
            return;
        }
        try {
            dHeight = Double.parseDouble(mHeight);
        } catch (Exception e) {
            height.setError("Wrong input format");
            height.requestFocus();
            return;
        }
        try {
            dWeight = Double.parseDouble(mWeight);
        } catch (Exception e) {
            weight.setError("Wrong input format");
            weight.requestFocus();
            return;
        }
        //create subject object
        Subject subject = new Subject(mName.toLowerCase(), mLastName.toLowerCase(), mEmail.toLowerCase(), dHeight, dWeight, IDnum);
        Log.i(TAG, "subject created: " + subject);
        //read file if it exists, convert to list, add new subject to list and save
        boolean fileExist = fileExist(FILE_NAME);
        if (fileExist) {
            File oldfile = new File(getApplicationContext().getFilesDir(),FILE_NAME);
            subjSet = SavingUtils.readSubjectFile(FILE_NAME, oldfile);
            Log.i(TAG, "read subjects: " + subjSet);
        }
        subjSet.add(subject);
        //write back into the file
        try {
            saveSubjectFile();
            MsgUtils.showToast(getApplicationContext(), "Subject saved");
            finish();
        } catch (Exception e) {
            e.printStackTrace();
        }
        finish();
    }

    private boolean fileExist(String fname){
        File file = getBaseContext().getFileStreamPath(fname);
        return file.exists();
    }

    private void saveSubjectFile() throws IOException {
        File newfile = new File(getApplicationContext().getFilesDir(), FILE_NAME);
        FileOutputStream fos = new FileOutputStream(newfile);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(subjSet);
        oos.close();
    }

}
