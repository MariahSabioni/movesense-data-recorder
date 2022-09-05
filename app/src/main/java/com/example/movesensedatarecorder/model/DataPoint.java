package com.example.movesensedatarecorder.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.ArrayList;

public class DataPoint implements Parcelable {

    private float accX, accY, accZ, gyroX, gyroY, gyroZ, accCombined, gyroCombined;
    private int time;
    private long sysTime;

    public DataPoint(int time, float accX,float accY,float accZ,float gyroX,float gyroY,float gyroZ, long sysTime ){
        this.time = time;
        this.accX = accX;
        this.accY = accY;
        this.accZ = accZ;
        this.accCombined = (float) (Math.sqrt(Math.pow(accX,2)+Math.pow(accY,2)+Math.pow(accZ,2)) - 9.81);
        this.gyroX = gyroX;
        this.gyroY = gyroY;
        this.gyroZ = gyroZ;
        this.gyroCombined = (float) (Math.sqrt(Math.pow(gyroX,2)+Math.pow(gyroY,2)+Math.pow(gyroZ,2)));
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

    public float getAccCombined() {
        return accCombined;
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

    public float getGyroCombined() {
        return gyroCombined;
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
        out.writeFloat(accCombined);
        out.writeFloat(gyroX);
        out.writeFloat(gyroY);
        out.writeFloat(gyroZ);
        out.writeFloat(gyroCombined);
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
        accCombined = in.readFloat();
        gyroX = in.readFloat();
        gyroY = in.readFloat();
        gyroZ = in.readFloat();
        gyroCombined = in.readFloat();
        sysTime = in.readLong();
    }

}
