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
                float hr = DataUtils.bytesToFloat(data, sampleOffset, hrDataSize);
                int rr = DataUtils.bytesToInt(data, sampleOffset + hrDataSize, rrDataSize);
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
                int time = DataUtils.bytesToInt(data, offset, timeDataSize);
                int sampleOffset = offset + timeDataSize + (i * coordinates * imuDataSize);
                float accX = DataUtils.bytesToFloat(data, sampleOffset + 0 * imuDataSize, imuDataSize);
                float accY = DataUtils.bytesToFloat(data, sampleOffset + 1 * imuDataSize, imuDataSize);
                float accZ = DataUtils.bytesToFloat(data, sampleOffset + 2 * imuDataSize, imuDataSize);
                float gyroX = DataUtils.bytesToFloat(data, sampleOffset + 0 * imuDataSize + (numOfSamples * coordinates * imuDataSize), imuDataSize);
                float gyroY = DataUtils.bytesToFloat(data, sampleOffset + 1 * imuDataSize + (numOfSamples * coordinates * imuDataSize), imuDataSize);
                float gyroZ = DataUtils.bytesToFloat(data, sampleOffset + 2 * imuDataSize + (numOfSamples * coordinates * imuDataSize), imuDataSize);
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

    public static int bytesToInt(byte[] bytes, int offset, int length) {
        return ByteBuffer.wrap(bytes, offset, length).order(ByteOrder.LITTLE_ENDIAN).getShort();
    }

    public static float bytesToFloat(byte[] bytes, int offset, int length) {
        return ByteBuffer.wrap(bytes, offset, length).order(ByteOrder.LITTLE_ENDIAN).getFloat();
    }

    public static byte[] getCommand(String type, byte requestId, String command){
        if (requestId > 127) throw new IllegalArgumentException("id= " + requestId);
        char[] chars = command.trim().toCharArray();
        byte[] ascii = new byte[chars.length + 2];
        ascii[1] = requestId;
        if (type.equals("stop")){
            ascii[0] = 2;
        }else if (type.equals("start")) {
            ascii[0] = 1;
            for (int i = 0; i < chars.length; i++) {
                if (chars[i] > 127)
                    throw new IllegalArgumentException("ascii val= " + (int) chars[i]);
                ascii[i + 2] = (byte) chars[i];
            }
        }else {
            throw new IllegalArgumentException("command type not recognized " + type);
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
