package com.example.movesensedatarecorder.utils;

import android.util.Log;

import com.example.movesensedatarecorder.model.DataPoint;
import com.example.movesensedatarecorder.model.HrPoint;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class DataUtils {

    private final static String TAG = DataUtils.class.getSimpleName();

    public static ArrayList<HrPoint> HrDataConverter(byte[] data) {

        ArrayList<HrPoint> hrPointList = new ArrayList<>();
        int len = data.length;
        int offset = 2; //byte 1 = op code; byte 2 = request_id
        int hrDataSize = 4; //32 bit (4 bytes)
        int rrDataSize = 2; //16 bit (2 bytes)
        int dataSize = hrDataSize + rrDataSize;
        int numOfSamples = (len - offset) / dataSize;
        // parse and interpret the data, ...
        if ((len - offset) % dataSize == 0) {

            for (int i = 0; i < numOfSamples; i++) {

                int sampleOffset = offset + (i * dataSize);
                float hr = DataUtils.fourBytesToFloat(data, sampleOffset);
                int rr = DataUtils.twoBytesToInt(data, sampleOffset + hrDataSize);
                long sysTime = System.currentTimeMillis();

                HrPoint hrPoint = new HrPoint(sysTime, hr, rr);
                hrPointList.add(hrPoint);
            }
            return hrPointList;
        } else {
            Log.i(TAG, "package with inconsistent length:" + len);
            return null;
        }
    }


    public static ArrayList<DataPoint> IMU6DataConverter(byte[] data) {
        ArrayList<DataPoint> dataPointList = new ArrayList<>();
        int len = data.length;
        int sensorNum = 2; //IMU6 has 2 sensors: acc and gyro
        int offset = 2; //byte 1 = op code; byte 2 = request_id
        int timeDataSize = 4; //32 bit - 4 bytes
        int imuDataSize = 4; //32 bit - 4 bytes
        int coordinates = 3; //x,y,z
        int numOfSamples = (len - 6) / (sensorNum * coordinates * imuDataSize); //number os samples in data package
        // parse and interpret the data, ...
        if (((len - 6f) / (sensorNum * coordinates * imuDataSize)) % 1 == 0) {

            for (int i = 0; i < numOfSamples; i++) {
                int time = DataUtils.fourBytesToInt(data, offset);

                int sampleOffset = offset + timeDataSize + (i * coordinates * imuDataSize);
                float accX = DataUtils.fourBytesToFloat(data, sampleOffset + 0 * imuDataSize);
                float accY = DataUtils.fourBytesToFloat(data, sampleOffset + 1 * imuDataSize);
                float accZ = DataUtils.fourBytesToFloat(data, sampleOffset + 2 * imuDataSize);

                float gyroX = DataUtils.fourBytesToFloat(data, sampleOffset + 0 * imuDataSize + (numOfSamples * coordinates * imuDataSize));
                float gyroY = DataUtils.fourBytesToFloat(data, sampleOffset + 1 * imuDataSize + (numOfSamples * coordinates * imuDataSize));
                float gyroZ = DataUtils.fourBytesToFloat(data, sampleOffset + 2 * imuDataSize + (numOfSamples * coordinates * imuDataSize));

                long sysTime = System.currentTimeMillis();

                DataPoint datapoint = new DataPoint(time, accX, accY, accZ, gyroX, gyroY, gyroZ, sysTime);
                dataPointList.add(datapoint);
            }
            return dataPointList;
        } else {
            Log.i(TAG, "package with inconsistent length:" + len);
            return null;
        }
    }

    public static int fourBytesToInt(byte[] bytes, int offset) {
        return ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    public static int twoBytesToInt(byte[] bytes, int offset) {
//        int value = 0;
//        for (byte b : bytes) {
//            value = (value << 8) + (b & 0xFF);
//        }
        return ByteBuffer.wrap(bytes, offset, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
    }

    public static float fourBytesToFloat(byte[] bytes, int offset) {
        return ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
    }

    public static byte[] stringToAsciiArray(byte id, String command) {
        if (id > 127) throw new IllegalArgumentException("id= " + id);
        char[] chars = command.trim().toCharArray();
        byte[] ascii = new byte[chars.length + 2];
        ascii[0] = 1;
        ascii[1] = id;
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] > 127) throw new IllegalArgumentException("ascii val= " + (int) chars[i]);
            ascii[i + 2] = (byte) chars[i];
        }
        return ascii;
    }

    public static String getAccAsStr(DataPoint dataPoint) {
        DecimalFormat df = new DecimalFormat("0.00");
        String accStr = "X: " + df.format(dataPoint.getAccX()) + " Y: " + df.format(dataPoint.getAccY()) + " Z: " + df.format(dataPoint.getAccZ());
        return accStr;
    }

    public static String getGyroAsStr(DataPoint dataPoint) {
        DecimalFormat df = new DecimalFormat("0.00");
        String gyroStr = "X: " + df.format(dataPoint.getGyroX()) + " Y: " + df.format(dataPoint.getGyroY()) + " Z: " + df.format(dataPoint.getGyroZ());
        return gyroStr;
    }

    public static String getHrAsStr(HrPoint hrPoint) {
        DecimalFormat df = new DecimalFormat("0.0");
        String hrStr = df.format(hrPoint.getHr());
        return hrStr;
    }

    public static String getPrettyDate(long millis){
        SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
        Date date = new Date(millis);
       return formatter.format(date);
    }

}
