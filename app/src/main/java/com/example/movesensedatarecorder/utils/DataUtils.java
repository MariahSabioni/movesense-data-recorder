package com.example.movesensedatarecorder.utils;

import android.util.Log;

import com.example.movesensedatarecorder.model.IMU6Point;
import com.example.movesensedatarecorder.model.HrPoint;
import com.example.movesensedatarecorder.model.TempPoint;

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
        int stdLen = hrDataSize + rrDataSize + offset;
        // parse and interpret the data, ...
        if (len == stdLen) {
            float hr = DataUtils.bytesToFloat(data, offset, hrDataSize);
            int rr = DataUtils.bytesToInt(data, offset + hrDataSize, rrDataSize);
            long sysTime = System.currentTimeMillis();
            HrPoint hrPoint = new HrPoint(sysTime, hr, rr);
            hrPointList.add(hrPoint);
            return hrPointList;
        } else {
            Log.i(TAG, "package with inconsistent length:" + len);
            return null;
        }
    }

    public static ArrayList<TempPoint> TempDataConverter(byte[] data) {
        ArrayList<TempPoint> tempPointList = new ArrayList<>();
        int len = data.length;
        int offset = 2; //byte 1 = op code; byte 2 = request_id
        int timeDataSize = 4; //32 bit (4 bytes)
        int tempDataSize = 4; //32 bit (4 bytes)
        int stdLen = timeDataSize + tempDataSize + offset;
        // parse and interpret the data, ...
        if (len == stdLen) {
            int time = DataUtils.bytesToInt(data, offset, timeDataSize);
            float temp = DataUtils.bytesToFloat(data, offset, tempDataSize);
            long sysTime = System.currentTimeMillis();
            TempPoint tempPoint = new TempPoint(sysTime, time, temp);
            tempPointList.add(tempPoint);
            return tempPointList;
        } else {
            Log.i(TAG, "package with inconsistent length:" + len);
            return null;
        }
    }

    public static ArrayList<IMU6Point> IMU6DataConverter(byte[] data) {
        ArrayList<IMU6Point> IMU6PointList = new ArrayList<>();
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
                IMU6Point datapoint = new IMU6Point(time, accX, accY, accZ, gyroX, gyroY, gyroZ, sysTime);
                IMU6PointList.add(datapoint);
            }
            return IMU6PointList;
        } else {
            Log.i(TAG, "package with inconsistent length:" + len);
            return null;
        }
    }

    public static boolean DoubleTapDataConverter(byte[] data) {
        int len = data.length;
        int offset = 2; //byte 1 = op code; byte 2 = request_id
        int timeDataSize = 4; //32 bit (4 bytes)
        int stateIdDataSize = 4; //32 bit (4 bytes)
        int stateDataSize = 4;
        int stdLen = timeDataSize + stateIdDataSize + stateDataSize + offset;
        // parse and interpret the data, ...
        if (len == stdLen) {
            int time = DataUtils.bytesToInt(data, offset, timeDataSize);
            int stateId = DataUtils.bytesToInt(data, offset + timeDataSize, stateIdDataSize);
            long sysTime = System.currentTimeMillis();
            int state = DataUtils.bytesToInt(data, offset + timeDataSize + stateIdDataSize, stateDataSize);
            return state == 1;
        } else {
            Log.i(TAG, "package with inconsistent length:" + len);
            return false;
        }
    }

    public static int bytesToInt(byte[] bytes, int offset, int length) {
        return ByteBuffer.wrap(bytes, offset, length).order(ByteOrder.LITTLE_ENDIAN).getShort();
    }

    public static float bytesToFloat(byte[] bytes, int offset, int length) {
        return ByteBuffer.wrap(bytes, offset, length).order(ByteOrder.LITTLE_ENDIAN).getFloat();
    }

    public static byte[] getCommand(byte opCode, byte requestId, String command) {
        if (requestId > 127) throw new IllegalArgumentException("id= " + requestId);
        char[] chars = command.trim().toCharArray();
        byte[] ascii = new byte[chars.length + 2];
        ascii[0] = opCode;
        ascii[1] = requestId;
        if (chars.length > 0) {
            for (int i = 0; i < chars.length; i++) {
                if (chars[i] > 127)
                    throw new IllegalArgumentException("ascii val= " + (int) chars[i]);
                ascii[i + 2] = (byte) chars[i];
            }
        }
        return ascii;
    }

    public static String getAccAsStr(IMU6Point IMU6Point) {
        DecimalFormat df = new DecimalFormat("0.00");
        String accStr = "X: " + df.format(IMU6Point.getAccX()) + " Y: " + df.format(IMU6Point.getAccY()) + " Z: " + df.format(IMU6Point.getAccZ());
        return accStr;
    }

    public static String getGyroAsStr(IMU6Point IMU6Point) {
        DecimalFormat df = new DecimalFormat("0.00");
        String gyroStr = "X: " + df.format(IMU6Point.getGyroX()) + " Y: " + df.format(IMU6Point.getGyroY()) + " Z: " + df.format(IMU6Point.getGyroZ());
        return gyroStr;
    }

    public static String getHrAsStr(HrPoint hrPoint) {
        DecimalFormat df = new DecimalFormat("0.0");
        String hrStr = df.format(hrPoint.getHr());
        return hrStr;
    }

    public static String getTempAsStr(TempPoint tempPoint) {
        DecimalFormat df = new DecimalFormat("0.0");
        String tempStr = df.format(tempPoint.getTemp());
        return tempStr;
    }

    public static String getPrettyDate(long millis) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
        Date date = new Date(millis);
        return formatter.format(date);
    }

}
