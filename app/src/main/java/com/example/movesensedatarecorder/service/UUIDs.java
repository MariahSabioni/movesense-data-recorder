package com.example.movesensedatarecorder.service;

import java.util.UUID;

public class UUIDs {

    public static final UUID MOVESENSE_2_0_SERVICE =
            UUID.fromString("34802252-7185-4d5d-b431-630e7050e8f0");
    public static final UUID MOVESENSE_2_0_COMMAND_CHARACTERISTIC =
            UUID.fromString("34800001-7185-4d5d-b431-630e7050e8f0");
    public static final UUID MOVESENSE_2_0_DATA_CHARACTERISTIC =
            UUID.fromString("34800002-7185-4d5d-b431-630e7050e8f0");
    // UUID for the client characteristic, which is necessary for notifications
    public static final UUID CLIENT_CHARACTERISTIC_CONFIG =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public static final String IMU_COMMAND = "Meas/IMU6/52"; // see documentation
    public static final String IMU_COMMAND_HR = "Meas/HR"; // see documentation
    public static final String MOVESENSE = "Movesense"; // filter for Movesense device
    public static final byte MOVESENSE_REQUEST = 1, MOVESENSE_RESPONSE = 2, REQUEST_ID = 99, REQUEST_ID_HR = 98;

    public static UUID convertFromInteger(int i) {
        final long MSB = 0x0000000000001000L;
        final long LSB = 0x800000805f9b34fbL;
        long value = i & 0xFFFFFFFF;
        return new UUID(MSB | (value << 32), LSB);
    }

}
