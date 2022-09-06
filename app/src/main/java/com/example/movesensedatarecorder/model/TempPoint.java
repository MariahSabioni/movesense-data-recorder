package com.example.movesensedatarecorder.model;

import android.os.Parcel;
import android.os.Parcelable;

public class TempPoint implements Parcelable {

    private float temp;
    private int time;
    private long sysTime;

    public TempPoint(long sysTime, int time, float temp){
        this.sysTime = sysTime;
        this.time = time;
        this.temp = temp - (float) 273.15;
    }

    public long getSysTime() {
        return sysTime;
    }

    public float getTemp() {
        return temp;
    }

    public int getTime() {
        return time;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(sysTime);
        out.writeInt(time);
        out.writeFloat(temp);
    }

    public static final Parcelable.Creator<TempPoint> CREATOR
            = new Parcelable.Creator<TempPoint>() {
        public TempPoint createFromParcel(Parcel in) {
            return new TempPoint(in);
        }

        public TempPoint[] newArray(int size) {
            return new TempPoint[size];
        }
    };

    private TempPoint(Parcel in) {
        sysTime = in.readLong();
        time = in.readInt();
        temp = in.readFloat();
    }

}
