package com.example.movesensedatarecorder.model;

import android.os.Build;
import android.provider.ContactsContract;

import com.example.movesensedatarecorder.utils.DataUtils;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExpDataPoint {

    private String mov, loc, subjID, expID, wod;
    private String accX, accY, accZ, accCombined, gyroX, gyroY, gyroZ, gyroCombined, time, sysTime, sysTimeMillis, hr;

    public ExpDataPoint(DataPoint dataPoint, String mExpID, String mWod, String mMov, String mSubjID, String mLoc){
        this.accX = String.valueOf(dataPoint.getAccX());
        this.accY = String.valueOf(dataPoint.getAccY());
        this.accZ = String.valueOf(dataPoint.getAccZ());
        this.accCombined = String.valueOf(dataPoint.getAccCombined());
        this.gyroX = String.valueOf(dataPoint.getGyroX());
        this.gyroY = String.valueOf(dataPoint.getGyroY());
        this.gyroZ = String.valueOf(dataPoint.getGyroZ());
        this.gyroCombined = String.valueOf(dataPoint.getGyroCombined());
        this.time = String.valueOf(dataPoint.getTime());
        this.sysTimeMillis = String.valueOf(dataPoint.getSysTime());
        this.sysTime = DataUtils.getPrettyDate(dataPoint.getSysTime());
        this.mov = mMov;
        this.expID = mExpID;
        this.subjID = mSubjID;
        this.loc = mLoc;
        this.wod = mWod;
    }

    public void setHr(String hr) {
        this.hr = hr;
    }

    public String dataToCsvRow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Stream.of(accX, accY, accZ, accCombined, gyroX, gyroY, gyroZ, gyroCombined,
                            hr, time, sysTimeMillis, sysTime, expID, wod, mov, loc, subjID)
                    .map(value -> value.replaceAll("\"", "\"\""))
                    .map(value -> Stream.of("\"", ",").anyMatch(value::contains) ? "\"" + value + "\"" : value)
                    .collect(Collectors.joining(","));
        } return null;
    }
}
