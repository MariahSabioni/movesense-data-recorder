package com.example.movesensedatarecorder.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import com.example.movesensedatarecorder.model.DataPoint;
import com.example.movesensedatarecorder.model.HrPoint;
import com.example.movesensedatarecorder.utils.DataUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.example.movesensedatarecorder.service.GattActions.*;
import static com.example.movesensedatarecorder.service.UUIDs.CLIENT_CHARACTERISTIC_CONFIG;
import static com.example.movesensedatarecorder.service.UUIDs.IMU_COMMAND_HR;
import static com.example.movesensedatarecorder.service.UUIDs.MOVESENSE_2_0_COMMAND_CHARACTERISTIC;
import static com.example.movesensedatarecorder.service.UUIDs.MOVESENSE_2_0_DATA_CHARACTERISTIC;
import static com.example.movesensedatarecorder.service.UUIDs.MOVESENSE_2_0_SERVICE;
import static com.example.movesensedatarecorder.service.UUIDs.IMU_COMMAND;
import static com.example.movesensedatarecorder.service.UUIDs.MOVESENSE_RESPONSE;
import static com.example.movesensedatarecorder.service.UUIDs.REQUEST_ID;
import static com.example.movesensedatarecorder.service.UUIDs.REQUEST_ID_HR;


public class BleIMUService extends Service {

    private final static String TAG = BleIMUService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;

    private BluetoothGattService movesenseService = null;

    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device - try to reconnect
        if (address.equals(mBluetoothDeviceAddress) && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            boolean result = mBluetoothGatt.connect();
            return result;
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // Directly connect to the device, set the autoConnect to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        return true;
    }

    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager =
                    (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(
                BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected to GATT server.");
                broadcastUpdate(Event.GATT_CONNECTED);
                // attempt to discover services
                mBluetoothGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(Event.GATT_DISCONNECTED);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {

            if (status == BluetoothGatt.GATT_SUCCESS) {

                broadcastUpdate(Event.GATT_SERVICES_DISCOVERED);
                logServices(gatt); // debug

                movesenseService = gatt.getService(MOVESENSE_2_0_SERVICE);

                if (movesenseService != null) {
                    broadcastUpdate(Event.MOVESENSE_SERVICE_DISCOVERED);
                    logCharacteristics(movesenseService); // debug

                    BluetoothGattCharacteristic commandChar =
                            movesenseService.getCharacteristic(
                                    MOVESENSE_2_0_COMMAND_CHARACTERISTIC);
                    // command example: 1, 99, "/Meas/Acc/13"
                    BluetoothGattCharacteristic commandChar_HR =
                            movesenseService.getCharacteristic(
                                    MOVESENSE_2_0_COMMAND_CHARACTERISTIC);
                    // command example: 1, 99, "/Meas/HR"

                    byte[] command =
                            DataUtils.stringToAsciiArray(REQUEST_ID, IMU_COMMAND);
                    commandChar.setValue(command);
                    boolean wasSuccess = mBluetoothGatt.writeCharacteristic(commandChar);
                    Log.i(TAG, "commandChar Subscribe: " + Arrays.toString(command) + " | success = " + wasSuccess);

                    byte[] command_HR =
                            DataUtils.stringToAsciiArray(REQUEST_ID_HR, IMU_COMMAND_HR);
                    commandChar_HR.setValue(command_HR);
                    boolean wasSuccess_HR = mBluetoothGatt.writeCharacteristic(commandChar_HR);
                    Log.i(TAG, "commandChar_HR Subscribe: " + Arrays.toString(command_HR) + " | success = " + wasSuccess_HR);

                } else {
                    broadcastUpdate(Event.MOVESENSE_SERVICE_NOT_AVAILABLE);
                    Log.i(TAG, "movesense service not available");
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic
                characteristic, int status) {
            Log.i(TAG, "onCharacteristicWrite " + characteristic.getUuid().toString());

            // First: Enable receiving notifications on the client side, i.e. on this Android.
            BluetoothGattService movesenseService = gatt.getService(MOVESENSE_2_0_SERVICE);
            BluetoothGattCharacteristic dataCharacteristic =
                    movesenseService.getCharacteristic(MOVESENSE_2_0_DATA_CHARACTERISTIC);
            boolean success = gatt.setCharacteristicNotification(dataCharacteristic, true);
            if (success) {
                broadcastUpdate(Event.MOVESENSE_SERVICE_DISCOVERED);
                Log.i(TAG, "setCharactNotification success");
                // Second: set enable notification server side (sensor).
                BluetoothGattDescriptor descriptor =
                        dataCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(descriptor); // callback: onDescriptorWrite
            } else {
                broadcastUpdate(Event.MOVESENSE_SERVICE_NOT_AVAILABLE);
                Log.i(TAG, "setCharacteristicNotification failed");
            }
        }

        @Override
        public void onDescriptorWrite(final BluetoothGatt gatt, BluetoothGattDescriptor
                descriptor, int status) {
            Log.i(TAG, "onDescriptorWrite, status " + status);

            if (CLIENT_CHARACTERISTIC_CONFIG.equals(descriptor.getUuid()))
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    // if success, we should receive data in onCharacteristicChanged
                    Log.i(TAG, "notifications enabled" + status);
                    broadcastUpdate(Event.MOVESENSE_NOTIFICATIONS_ENABLED);
                }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            if (MOVESENSE_2_0_DATA_CHARACTERISTIC.equals(characteristic.getUuid())) {
                byte[] data = characteristic.getValue();
                if (data[0] == MOVESENSE_RESPONSE && data[1] == REQUEST_ID) {

                    ArrayList<DataPoint> dataPointList = DataUtils.IMU6DataConverter(data);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        if (Objects.isNull(dataPointList)) {
                            return;
                        }
                    }
                    //broadcast data update
                    broadcastMovesenseDataUpdate(dataPointList);
                } else if (data[0] == MOVESENSE_RESPONSE && data[1] == REQUEST_ID_HR) {

                    ArrayList<HrPoint> hrPointList = DataUtils.HrDataConverter(data);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        if (Objects.isNull(hrPointList)) {
                            return;
                        }
                    }
                    //broadcast data update
                    broadcastMovesenseHrDataUpdate(hrPointList);
                }
            }
        }
    };

    //Broadcast methods for events
    private void broadcastUpdate(final Event event) {
        final Intent intent = new Intent(ACTION_GATT_MOVESENSE_EVENTS);
        intent.putExtra(EVENT, event);
        sendBroadcast(intent);
    }

    //Broadcast methods for data
    private void broadcastMovesenseDataUpdate(final ArrayList<DataPoint> dataPointList) {
        final Intent intent = new Intent(ACTION_GATT_MOVESENSE_EVENTS);
        intent.putExtra(EVENT, Event.DATA_AVAILABLE);
        intent.putParcelableArrayListExtra(MOVESENSE_DATA, dataPointList);
        sendBroadcast(intent);
    }
    //Broadcast methods for hr data
    private void broadcastMovesenseHrDataUpdate(final ArrayList<HrPoint> hrPointList) {
        final Intent intent = new Intent(ACTION_GATT_MOVESENSE_EVENTS);
        intent.putExtra(EVENT, Event.HR_DATA_AVAILABLE);
        intent.putParcelableArrayListExtra(MOVESENSE_HR_DATA, hrPointList);
        sendBroadcast(intent);
    }

    //Android Service specific code for binding and unbinding to this Android service
    public class LocalBinder extends Binder {
        public BleIMUService getService() {

            return BleIMUService.this;
        }
    }

    private final IBinder mBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        //close() is invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }


    //logging and debugging
    private void logServices(BluetoothGatt gatt) {
        List<BluetoothGattService> services = gatt.getServices();
        for (BluetoothGattService service : services) {
            String uuid = service.getUuid().toString();
            Log.i(TAG, "> service: " + uuid);
        }
    }

    //logging and debugging
    private void logCharacteristics(BluetoothGattService gattService) {
        List<BluetoothGattCharacteristic> characteristics =
                gattService.getCharacteristics();
        for (BluetoothGattCharacteristic chara : characteristics) {
            String uuid = chara.getUuid().toString();
            Log.i(TAG, ">> characteristic: " + uuid);
            logCharacteristicProperties(chara);
        }
    }

    private void logCharacteristicProperties(BluetoothGattCharacteristic pChar) {
        boolean isCharacteristicWritable = (pChar.getProperties() & (BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0;
        boolean isCharacteristicReadable = ((pChar.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) != 0);
        boolean isCharacteristicNotifiable = (pChar.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0;
        Log.i(TAG, ">>> write: " + isCharacteristicWritable);
        Log.i(TAG, ">>> read: " + isCharacteristicReadable);
        Log.i(TAG, ">>> notify: " + isCharacteristicNotifiable);
    }
}

