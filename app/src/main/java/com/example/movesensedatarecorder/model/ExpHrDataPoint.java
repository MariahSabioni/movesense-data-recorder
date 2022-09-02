package com.example.movesensedatarecorder.model;

import android.os.Build;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExpHrDataPoint {

    private String mov, loc, subjID, expID, wod;
    private String hr, rr, sysTime;

    public ExpHrDataPoint(HrPoint hrPoint, String mExpID, String mWod, String mMov, String mSubjID, String mLoc){
        this.hr = String.valueOf(hrPoint.getHr());
        this.rr = String.valueOf(hrPoint.getRr());
        this.sysTime = String.valueOf(hrPoint.getSysTime());
        this.mov = mMov;
        this.expID = mExpID;
        this.subjID = mSubjID;
        this.loc = mLoc;
        this.wod = mWod;
    }

    public String hrDataToCsvRow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Stream.of(hr, rr, sysTime, expID, wod, mov, loc, subjID)
                    .map(value -> value.replaceAll("\"", "\"\""))
                    .map(value -> Stream.of("\"", ",").anyMatch(value::contains) ? "\"" + value + "\"" : value)
                    .collect(Collectors.joining(","));
        } return null;
    }
}

