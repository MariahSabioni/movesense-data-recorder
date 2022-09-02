package com.example.movesensedatarecorder.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.ArrayList;

public class DataPoint implements Parcelable {

    private float accX, accY, accZ, gyroX, gyroY, gyroZ;
    private int time;
    private long sysTime;

    public DataPoint(int time, float accX,float accY,float accZ,float gyroX,float gyroY,float gyroZ, long sysTime ){
        this.time = time;
        this.accX = accX;
        this.accY = accY;
        this.accZ = accZ;
        this.gyroX = gyroX;
        this.gyroY = gyroY;
        this.gyroZ = gyroZ;
        this.sysTime = sysTime;
    }

    public int getTime() {
        return time;
    }

    public float getAccX() {
        return accX;
    }

    public float getAccY() {
        return accY;
    }

    public float getAccZ() {
        return accZ;
    }

    public float getGyroX() {
        return gyroX;
    }

    public float getGyroY() {
        return gyroY;
    }

    public float getGyroZ() {
        return gyroZ;
    }

    public long getSysTime() {
        return sysTime;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(time);
        out.writeFloat(accX);
        out.writeFloat(accY);
        out.writeFloat(accZ);
        out.writeFloat(gyroX);
        out.writeFloat(gyroY);
        out.writeFloat(gyroZ);
        out.writeLong(sysTime);
    }

    public static final Parcelable.Creator<DataPoint> CREATOR
            = new Parcelable.Creator<DataPoint>() {
        public DataPoint createFromParcel(Parcel in) {
            return new DataPoint(in);
        }

        public DataPoint[] newArray(int size) {
            return new DataPoint[size];
        }
    };

    private DataPoint(Parcel in) {
        time = in.readInt();
        accX = in.readFloat();
        accY = in.readFloat();
        accZ = in.readFloat();
        gyroX = in.readFloat();
        gyroY = in.readFloat();
        gyroZ = in.readFloat();
        sysTime = in.readLong();
    }

}
