package com.example.movesensedatarecorder.service;

public class GattActions {

    //flag for events
    public final static String ACTION_GATT_MOVESENSE_EVENTS =
            "com.example.movesensedatarecorder.service.ACTION_GATT_MOVESENSE_EVENTS";

    //flag for event info in intents (via intent.putExtra)
    public final static String EVENT =
            "com.example.movesensedatarecorder.service.EVENT";

    //flag for data
    public final static String MOVESENSE_DATA =
            "com.example.movesensedatarecorder.service.MOVESENSE_DATA";

    //flag for hr data
    public final static String MOVESENSE_HR_DATA =
            "com.example.movesensedatarecorder.service.MOVESENSE_HR_DATA";

    //gatt status and events
    public enum Event {
        GATT_CONNECTED("Connected"),
        GATT_DISCONNECTED("Disconnected"),
        GATT_SERVICES_DISCOVERED("Services discovered"),
        MOVESENSE_SERVICE_DISCOVERED("Movesense service"),
        MOVESENSE_SERVICE_NOT_AVAILABLE("Movesense service unavailable"),
        MOVESENSE_NOTIFICATIONS_ENABLED("Notifications enabled"),
        DATA_AVAILABLE("Data available"),
        HR_DATA_AVAILABLE("HR Data available");

        @Override
        public String toString() {
            return text;
        }

        private final String text;

        private Event(String text) {
            this.text = text;
        }
    }
}
