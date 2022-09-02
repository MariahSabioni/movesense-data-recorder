package com.example.movesensedatarecorder.model;

import android.os.Parcel;
import android.os.Parcelable;

public class HrPoint implements Parcelable {

    private float hr;
    private int rr;
    private long sysTime;

    public HrPoint(long sysTime, float hr, int rr){
        this.sysTime = sysTime;
        this.hr = hr;
        this.rr = rr;
    }

    public long getSysTime() {
        return sysTime;
    }

    public float getHr() {
        return hr;
    }

    public float getRr() {
        return rr;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(sysTime);
        out.writeFloat(hr);
        out.writeFloat(rr);
    }

    public static final Parcelable.Creator<HrPoint> CREATOR
            = new Parcelable.Creator<HrPoint>() {
        public HrPoint createFromParcel(Parcel in) {
            return new HrPoint(in);
        }

        public HrPoint[] newArray(int size) {
            return new HrPoint[size];
        }
    };

    private HrPoint(Parcel in) {
        sysTime = in.readLong();
        hr = in.readFloat();
        rr = in.readInt();
    }

}
