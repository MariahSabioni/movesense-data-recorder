package com.example.movesensedatarecorder.model;

import android.os.Build;

import com.example.movesensedatarecorder.utils.DataUtils;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExpDataPoint {

    private String mov, loc, subjID, expID, wod;
    private String accX, accY, accZ, accCombined, gyroX, gyroY, gyroZ, gyroCombined, time, sysTime, sysTimeMillis, hr;

    public ExpDataPoint(IMU6Point IMU6Point, String mExpID, String mWod, String mMov, String mSubjID, String mLoc, String mHr){
        this.accX = String.valueOf(IMU6Point.getAccX());
        this.accY = String.valueOf(IMU6Point.getAccY());
        this.accZ = String.valueOf(IMU6Point.getAccZ());
        this.accCombined = String.valueOf(IMU6Point.getAccCombined());
        this.gyroX = String.valueOf(IMU6Point.getGyroX());
        this.gyroY = String.valueOf(IMU6Point.getGyroY());
        this.gyroZ = String.valueOf(IMU6Point.getGyroZ());
        this.gyroCombined = String.valueOf(IMU6Point.getGyroCombined());
        this.time = String.valueOf(IMU6Point.getTime());
        this.sysTimeMillis = String.valueOf(IMU6Point.getSysTime());
        this.sysTime = DataUtils.getPrettyDate(IMU6Point.getSysTime());
        this.mov = mMov;
        this.expID = mExpID;
        this.subjID = mSubjID;
        this.loc = mLoc;
        this.wod = mWod;
        this.hr = mHr;
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
