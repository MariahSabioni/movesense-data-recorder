package com.example.movesensedatarecorder;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.example.movesensedatarecorder.model.IMU6Point;
import com.example.movesensedatarecorder.model.ExpDataPoint;
import com.example.movesensedatarecorder.model.ExpHrDataPoint;
import com.example.movesensedatarecorder.model.HrPoint;
import com.example.movesensedatarecorder.model.TempPoint;
import com.example.movesensedatarecorder.service.BleIMUService;
import com.example.movesensedatarecorder.service.GattActions;
import com.example.movesensedatarecorder.utils.DataUtils;
import com.example.movesensedatarecorder.utils.MsgUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import androidx.core.content.res.ResourcesCompat;

import static com.example.movesensedatarecorder.service.GattActions.ACTION_GATT_MOVESENSE_EVENTS;
import static com.example.movesensedatarecorder.service.GattActions.EVENT;
import static com.example.movesensedatarecorder.service.GattActions.MOVESENSE_DATA;
import static com.example.movesensedatarecorder.service.GattActions.MOVESENSE_HR_DATA;
import static com.example.movesensedatarecorder.service.GattActions.MOVESENSE_TEMP_DATA;

public class DataActivity extends Activity {

    private final static String TAG = DataActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private static final int REQUEST_SNIPPET_SETTINGS = 1;
    private static final int REQUEST_FULL_WOD_SETTINGS = 2;
    public static final String EXTRAS_EXP_SUBJ = "EXP_SUBJ";
    public static final String EXTRAS_EXP_MOV = "EXP_MOV";
    public static final String EXTRAS_EXP_WOD = "EXP_WOD";
    public static final String EXTRAS_EXP_LOC = "EXP_LOC";
    public static final String EXTRAS_EXP_TIME = "EXP_TIME";

    private TextView mAccView, mGyroView, mHrView, mTempView, mStatusView, deviceView, expTitleView;
    private ImageButton buttonRecord;
    private ToggleButton toggleSnippet, toggleFull;

    private String mDeviceAddress;
    private BleIMUService mBluetoothLeService;

    private String mSubjID, mMov, mLoc, mTimeRecording, mExpID, mWod, mRecordingType, mHr, mTemp;
    private Drawable startRecordDrawable;
    private Drawable stopRecordDrawable;
    private TimerTask timerTask;
    private Timer timer;
    private boolean record = false;
    private List<ExpDataPoint> expDataSet = new ArrayList<>();
    private List<ExpHrDataPoint> expHrDataSet = new ArrayList<>();
    private String content;
    private static final int CREATE_FILE = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data);

        // the intent from BleIMUService, that started this activity
        final Intent intent = getIntent();
        String deviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // set up ui references
        deviceView = findViewById(R.id.device_view);
        deviceView.setText("Connected to:\n" + deviceName);
        mAccView = findViewById(R.id.acc_view);
        mGyroView = findViewById(R.id.gyro_view);
        mHrView = findViewById(R.id.hr_view);
        mTempView = findViewById(R.id.temp_view);
        mStatusView = findViewById(R.id.status_view);
        buttonRecord = findViewById(R.id.button_recording);
        toggleSnippet = findViewById(R.id.toggle_record_exp);
        toggleFull = findViewById(R.id.toggle_record_full);
        expTitleView = findViewById(R.id.exp_title_view);

        Resources resources = getResources();
        startRecordDrawable = ResourcesCompat.getDrawable(resources, R.drawable.start_record_icon, null);
        stopRecordDrawable = ResourcesCompat.getDrawable(resources, R.drawable.stop_record_icon, null);
        buttonRecord.setBackground(startRecordDrawable);
        expTitleView.setText(R.string.record_exp);

        // Bind to the BleIMUService - onResume or onStart register a BroadcastReceiver.
        Intent gattServiceIntent = new Intent(this, BleIMUService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        toggleFull.setChecked(false);
        toggleSnippet.setChecked(true);
        toggleFull.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b) {
                toggleSnippet.setChecked(false);
            } else {
                toggleSnippet.setChecked(true);
            }
        });
        toggleSnippet.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b) {
                toggleFull.setChecked(false);
            } else {
                toggleFull.setChecked(true);
            }
        });

        //record button listener
        buttonRecord.setOnClickListener(v -> {
            if (!record) {
                if (!toggleFull.isChecked()) {
                    Intent intentExp = new Intent(getApplicationContext(), NewSnippetActivity.class);
                    startActivityForResult(intentExp, REQUEST_SNIPPET_SETTINGS);
                } else {
                    Intent intentExp = new Intent(getApplicationContext(), NewFullWodActivity.class);
                    startActivityForResult(intentExp, REQUEST_FULL_WOD_SETTINGS);
                }
            } else {
                timer.cancel();
                try {
                    exportData();
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                    MsgUtils.showToast(getApplicationContext(), "unable to save data");
                }
                buttonRecord.setBackground(startRecordDrawable);
                expTitleView.setText(R.string.record_exp);
                toggleFull.setEnabled(true);
                toggleSnippet.setEnabled(true);
                record = false;
            }
        });

        resetTimerAndTimerTask();
    }

    private void resetTimerAndTimerTask() {
        if (timer != null) {
            timer.cancel();
        }
        if (timerTask != null) {
            timerTask.cancel();
        }
        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    buttonRecord.setBackground(startRecordDrawable);
                    expTitleView.setText(R.string.record_exp);
                    toggleFull.setEnabled(true);
                    toggleSnippet.setEnabled(false);
                });
                record = false;
                timer.cancel();
                Log.i(TAG, "TimerTask finished");
                try {
                    exportData();
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SNIPPET_SETTINGS && resultCode == Activity.RESULT_OK) {
            expDataSet.clear();
            mRecordingType = "snippet";
            mWod = "undefined";
            mSubjID = data.getStringExtra(EXTRAS_EXP_SUBJ);
            mMov = data.getStringExtra(EXTRAS_EXP_MOV);
            mLoc = data.getStringExtra(EXTRAS_EXP_LOC);
            mTimeRecording = data.getStringExtra(EXTRAS_EXP_TIME);
            mExpID = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            record = true;
            buttonRecord.setBackground(stopRecordDrawable);
            expTitleView.setText(R.string.recording_exp);
            toggleFull.setEnabled(false);
            toggleSnippet.setEnabled(false);
            //automatic stop
            resetTimerAndTimerTask();
            timer.schedule(timerTask, 1000 * Long.parseLong(mTimeRecording));
        } else if (requestCode == REQUEST_FULL_WOD_SETTINGS && resultCode == Activity.RESULT_OK) {
            expDataSet.clear();
            mRecordingType = "wod";
            mMov = "undefined";
            mSubjID = data.getStringExtra(EXTRAS_EXP_SUBJ);
            mWod = data.getStringExtra(EXTRAS_EXP_WOD);
            mLoc = data.getStringExtra(EXTRAS_EXP_LOC);
            mTimeRecording = data.getStringExtra(EXTRAS_EXP_TIME);
            mExpID = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            record = true;
            buttonRecord.setBackground(stopRecordDrawable);
            expTitleView.setText(R.string.recording_exp);
            toggleFull.setEnabled(false);
            toggleSnippet.setEnabled(false);
            //automatic stop
            resetTimerAndTimerTask();
            timer.schedule(timerTask, 1000 * Long.parseLong(mTimeRecording));
        } else if (resultCode == RESULT_OK && requestCode == CREATE_FILE) {
            OutputStream fileOutputStream = null;
            try {
                fileOutputStream = getContentResolver().openOutputStream(data.getData());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            try {
                assert fileOutputStream != null;
                fileOutputStream.write(content.getBytes()); //Write the obtained string to csv
                fileOutputStream.flush();
                fileOutputStream.close();
                MsgUtils.showToast(getApplicationContext(), "file saved!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
        timer.cancel();
    }

    //Callback methods to manage the Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BleIMUService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.i(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
            Log.i(TAG, "onServiceConnected");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
            Log.i(TAG, "onServiceDisconnected");
        }
    };

    //BroadcastReceiver handling various events fired by the Service, see GattActions.Event.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (ACTION_GATT_MOVESENSE_EVENTS.equals(action)) {
                GattActions.Event event = (GattActions.Event) intent.getSerializableExtra(EVENT);
                if (event != null) {
                    switch (event) {
                        case GATT_CONNECTED:
                        case GATT_DISCONNECTED:
                        case GATT_SERVICES_DISCOVERED:
                        case MOVESENSE_NOTIFICATIONS_ENABLED:
                        case MOVESENSE_SERVICE_DISCOVERED:
                            mStatusView.setText(event.toString());
                            mStatusView.setText(R.string.requesting);
                            mAccView.setText(R.string.no_info);
                            mGyroView.setText(R.string.no_info);
                            mHrView.setText(R.string.no_info);
                            mTempView.setText(R.string.no_info);
                            break;
                        case IMU6_DATA_AVAILABLE:
                            ArrayList<IMU6Point> IMU6PointList = intent.getParcelableArrayListExtra(MOVESENSE_DATA);
                            Log.i(TAG, "got data IMU6");
                            IMU6Point IMU6Point = IMU6PointList.get(0); //UI is updated with the first measurement only...

                            if (record) {
                                for (IMU6Point d : IMU6PointList) {
                                    //... but data for all points is saved
                                    ExpDataPoint expDataPoint = new ExpDataPoint(d, mExpID, mWod, mMov, mSubjID, mLoc);
                                    try{
                                        expDataPoint.setHr(mHr); //read HR stored in global variable
                                        expDataPoint.setTemp(mTemp);
                                    }catch(Exception e){
                                        expDataPoint.setHr("undefined");
                                    }
                                    expDataSet.add(expDataPoint);
                                }
                            }
                            mStatusView.setText(R.string.received);
                            String accStr = DataUtils.getAccAsStr(IMU6Point);
                            String gyroStr = DataUtils.getGyroAsStr(IMU6Point);
                            mAccView.setText(accStr);
                            mGyroView.setText(gyroStr);

                            break;
                        case HR_DATA_AVAILABLE:
                            ArrayList<HrPoint> hrPointList = intent.getParcelableArrayListExtra(MOVESENSE_HR_DATA);
                            Log.i(TAG, "got data HR");
                            HrPoint hrPoint = hrPointList.get(0);
                            mHr = String.valueOf(hrPoint.getHr()); //store HR in global variable

//                            if (record) {
//                                for (HrPoint d : hrPointList) {
//                                    ExpHrDataPoint expHrDataPoint = new ExpHrDataPoint(d, mExpID, mWod, mMov, mSubjID, mLoc);
//                                    expHrDataSet.add(expHrDataPoint);
//                                }
//                            }
                            mStatusView.setText(R.string.received);
                            String hrStr = DataUtils.getHrAsStr(hrPoint);
                            mHrView.setText(hrStr);
                            break;
                        case TEMP_DATA_AVAILABLE:
                            ArrayList<TempPoint> tempPointList = intent.getParcelableArrayListExtra(MOVESENSE_TEMP_DATA);
                            Log.i(TAG, "got temp HR");
                            TempPoint tempPoint = tempPointList.get(0);
                            mTemp = String.valueOf(tempPoint.getTemp()); //store Temp in global variable

                            mStatusView.setText(R.string.received);
                            String tempStr = DataUtils.getTempAsStr(tempPoint);
                            mTempView.setText(tempStr);
                            break;
                        case DOUBLE_TAP_DETECTED:
                            Log.i(TAG, "double tap detected!");
                            MsgUtils.showToast(getApplicationContext(), "Double tap!");
                            break;
                        case MOVESENSE_SERVICE_NOT_AVAILABLE:
                            mStatusView.setText(R.string.no_service);
                            break;
                        default:
                            mStatusView.setText(R.string.error);
                            mAccView.setText(R.string.no_info);
                            mGyroView.setText(R.string.no_info);

                    }
                }
            }
        }
    };

    private void exportData() throws IOException, ClassNotFoundException {
        if (expDataSet.isEmpty()) {
            MsgUtils.showToast(getApplicationContext(), "unable to get IMU6 data");
        }
        try {
            String heading = "accX,accY,accZ,accCombined,gyroX,gyroY,gyroZ,gyroCombined,hr,temp," +
                    "time,sysTimeMillis,sysTime,expID,wod,mov,loc,subjID";
            content = heading + "\n" + recordAsCsv();
            saveToExternalStorage();
        } catch (Exception e) {
            e.printStackTrace();
            MsgUtils.showToast(getApplicationContext(), "unable to export IMU6 data");
        }
    }

    private void saveToExternalStorage() {
        String filename;
        if (mWod.equals("undefined")) {
            filename = ("snippet_" + mMov + "_" + mLoc + "_" + mExpID + ".csv").replaceAll(" ", "_").toLowerCase();
        } else {
            filename = ("wod_" + mWod + "_" + mLoc + "_" + mExpID + ".csv").replaceAll(" ", "_").toLowerCase();
        }
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_TITLE, filename);

        startActivityForResult(intent, CREATE_FILE);
    }

    private String recordAsCsv() {
        //https://stackoverflow.com/questions/35057456/how-to-write-arraylistobject-to-a-csv-file
        String recordAsCsv = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            recordAsCsv = expDataSet.stream()
                    .map(ExpDataPoint::dataToCsvRow)
                    .collect(Collectors.joining(System.getProperty("line.separator")));
        }
        return recordAsCsv;
    }

    // Intent filter for broadcast updates from BleHeartRateServices
    private IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_GATT_MOVESENSE_EVENTS);
        return intentFilter;
    }
}

