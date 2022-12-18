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

    public static final String MEAS_IMU6_52 = "Meas/IMU6/52"; // see documentation
    public static final String MEAS_HR = "Meas/HR"; // see documentation
    public static final String STATES_TAP = "System/States/4"; // see documentation
    public static final String STATES_DOUBLE_TAP = "System/States/3"; // see documentation
    public static final String MOVESENSE = "Movesense"; // filter for Movesense device
    public static final byte MOVESENSE_RESPONSE = 2; //op code that initiates data notifications
    public static final byte START_STREAM = 1, STOP_STREAM = 2; //op code that initiates commands
    public static final byte REQUEST_ID_IMU6 = 99, REQUEST_ID_HR = 98,
            REQUEST_ID_DOUBLE_TAP = 96, REQUEST_ID_TAP = 95;

}
