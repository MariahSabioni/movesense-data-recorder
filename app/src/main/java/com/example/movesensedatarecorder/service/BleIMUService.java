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
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.example.movesensedatarecorder.model.IMU6Point;
import com.example.movesensedatarecorder.model.HrPoint;
import com.example.movesensedatarecorder.model.TempPoint;
import com.example.movesensedatarecorder.utils.DataUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.example.movesensedatarecorder.service.GattActions.*;
import static com.example.movesensedatarecorder.service.UUIDs.CLIENT_CHARACTERISTIC_CONFIG;
import static com.example.movesensedatarecorder.service.UUIDs.MEAS_HR;
import static com.example.movesensedatarecorder.service.UUIDs.MEAS_TEMP;
import static com.example.movesensedatarecorder.service.UUIDs.MOVESENSE_2_0_COMMAND_CHARACTERISTIC;
import static com.example.movesensedatarecorder.service.UUIDs.MOVESENSE_2_0_DATA_CHARACTERISTIC;
import static com.example.movesensedatarecorder.service.UUIDs.MOVESENSE_2_0_SERVICE;
import static com.example.movesensedatarecorder.service.UUIDs.MEAS_IMU6_52;
import static com.example.movesensedatarecorder.service.UUIDs.MOVESENSE_RESPONSE;
import static com.example.movesensedatarecorder.service.UUIDs.REQUEST_ID_DOUBLE_TAP;
import static com.example.movesensedatarecorder.service.UUIDs.REQUEST_ID_IMU6;
import static com.example.movesensedatarecorder.service.UUIDs.REQUEST_ID_HR;
import static com.example.movesensedatarecorder.service.UUIDs.REQUEST_ID_TEMP;
import static com.example.movesensedatarecorder.service.UUIDs.START_STREAM;
import static com.example.movesensedatarecorder.service.UUIDs.STATES_DOUBLE_TAP;
import static com.example.movesensedatarecorder.service.UUIDs.STOP_STREAM;

public class BleIMUService extends Service {

    private final static String TAG = BleIMUService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattService movesenseService = null;

    private final Handler mHandler = new Handler();

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

    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device - try to reconnect
        if (address.equals(mBluetoothDeviceAddress) && mBluetoothGatt != null) {
            Log.i(TAG, "Trying to use an existing mBluetoothGatt for connection.");
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
        Log.i(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
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
                mBluetoothGatt.discoverServices(); //calback: onServicesDiscovered
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

                    //1st: unsubscribe from all streams to avoid getting old unwanted data
                    unsubscribeRequests(gatt);
//                    //enable notifications for data characteristic on android side
//                    BluetoothGattCharacteristic dataChar =
//                            movesenseService.getCharacteristic(MOVESENSE_2_0_DATA_CHARACTERISTIC);
//                    boolean success = gatt.setCharacteristicNotification(dataChar, true);
//                    if (success) {
//                        Log.i(TAG, "setCharactNotification success");
//                        //enable notifications for data characteristic on sensor side
//                        BluetoothGattDescriptor descriptor =
//                                dataChar.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
//                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//                        gatt.writeDescriptor(descriptor); // callback: onDescriptorWrite
//                    } else {
//                        broadcastUpdate(Event.MOVESENSE_SERVICE_NOT_AVAILABLE);
//                        Log.i(TAG, "setCharacteristicNotification failed");
//                    }

                } else {
                    broadcastUpdate(Event.MOVESENSE_SERVICE_NOT_AVAILABLE);
                    Log.i(TAG, "movesense service not available");
                }
            }
        }

        public void unsubscribeRequests(BluetoothGatt gatt){
            BluetoothGattCharacteristic commandChar =
                    movesenseService.getCharacteristic(MOVESENSE_2_0_COMMAND_CHARACTERISTIC);

            ArrayList<byte[]> commandList = new ArrayList<>();
            byte[] resetCommand = DataUtils.getCommand(STOP_STREAM, REQUEST_ID_IMU6,"");
            byte[] resetCommandHR = DataUtils.getCommand(STOP_STREAM, REQUEST_ID_HR,"");
            byte[] resetCommandTemp = DataUtils.getCommand(STOP_STREAM, REQUEST_ID_TEMP, "");
            byte[] resetCommandDoubleTap = DataUtils.getCommand(STOP_STREAM, REQUEST_ID_DOUBLE_TAP, "");
            commandList.add(resetCommand);
            commandList.add(resetCommandHR);
            commandList.add(resetCommandTemp);
            commandList.add(resetCommandDoubleTap);

            mHandler.postDelayed(() -> writeToStream(gatt, commandList, commandChar, STOP_STREAM), 500);
        }

        public void writeToStream(BluetoothGatt gatt, ArrayList<byte[]> commandList,
                                  BluetoothGattCharacteristic commandChar, byte type) {
            commandChar.setValue(commandList.get(0));
            boolean wasSuccess = mBluetoothGatt.writeCharacteristic(commandChar);
            Log.i(TAG, "commandChar Subscribe: " + Arrays.toString(commandList.get(0)) + " | success = " + wasSuccess);
            commandList.remove(commandList.get(0));
            if (commandList.size() > 0) {
                mHandler.postDelayed(() -> writeToStream(gatt, commandList, commandChar, type), 500);
            } else if (type == STOP_STREAM){
                //2nd: after unsubscribing all, enable notifications from data characteristic
                mHandler.postDelayed(() -> enableNotifications(gatt), 500);
            }
        }

        public void enableNotifications(BluetoothGatt gatt){
            //enable notifications for data characteristic on android side
            BluetoothGattCharacteristic dataChar =
                    movesenseService.getCharacteristic(MOVESENSE_2_0_DATA_CHARACTERISTIC);
            boolean success = gatt.setCharacteristicNotification(dataChar, true);
            if (success) {
                Log.i(TAG, "setCharactNotification success");
                //enable notifications for data characteristic on sensor side
                BluetoothGattDescriptor descriptor =
                        dataChar.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
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
            if (CLIENT_CHARACTERISTIC_CONFIG.equals(descriptor.getUuid()))
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    // if success, we should receive data in onCharacteristicChanged
                    Log.i(TAG, "notifications enabled");
                    broadcastUpdate(Event.MOVESENSE_NOTIFICATIONS_ENABLED);
                    //3rd: subscribe to data streams
                    subscribeRequests(gatt);
                }
        }

        public void subscribeRequests(BluetoothGatt gatt){
            BluetoothGattCharacteristic commandChar =
                    movesenseService.getCharacteristic(MOVESENSE_2_0_COMMAND_CHARACTERISTIC);

            ArrayList<byte[]> commandList = new ArrayList<>();
            byte[] command = DataUtils.getCommand(START_STREAM, REQUEST_ID_IMU6, MEAS_IMU6_52);
            byte[] commandHR = DataUtils.getCommand(START_STREAM, REQUEST_ID_HR, MEAS_HR);
            byte[] commandTemp = DataUtils.getCommand(START_STREAM, REQUEST_ID_TEMP, MEAS_TEMP);
            byte[] commandDoubleTap = DataUtils.getCommand(START_STREAM, REQUEST_ID_DOUBLE_TAP, STATES_DOUBLE_TAP);
            commandList.add(command);
            commandList.add(commandHR);
            commandList.add(commandTemp);
            commandList.add(commandDoubleTap);

            mHandler.postDelayed(() -> writeToStream(gatt, commandList, commandChar, START_STREAM), 500);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            if (MOVESENSE_2_0_DATA_CHARACTERISTIC.equals(characteristic.getUuid())) {
                byte[] data = characteristic.getValue();
                if (data[0] == MOVESENSE_RESPONSE && data[1] == REQUEST_ID_IMU6) {
                    ArrayList<IMU6Point> IMU6PointList = DataUtils.IMU6DataConverter(data);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        if (Objects.isNull(IMU6PointList)) {
                            return;
                        }
                    }
                    //broadcast data update
                    broadcastMovesenseDataUpdate(data[1], IMU6PointList, null, null);
                } else if (data[0] == MOVESENSE_RESPONSE && data[1] == REQUEST_ID_HR) {
                    ArrayList<HrPoint> hrPointList = DataUtils.HrDataConverter(data);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        if (Objects.isNull(hrPointList)) {
                            return;
                        }
                    }
                    //broadcast data update
                    broadcastMovesenseDataUpdate(data[1],null, hrPointList, null);
                } else if (data[0] == MOVESENSE_RESPONSE && data[1] == REQUEST_ID_TEMP) {
                    ArrayList<TempPoint> tempPointList = DataUtils.TempDataConverter(data);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        if (Objects.isNull(tempPointList)) {
                            return;
                        }
                    }
                    //broadcast data update
                    broadcastMovesenseDataUpdate(data[1],null, null, tempPointList);
                } else if (data[0] == MOVESENSE_RESPONSE && data[1] == REQUEST_ID_DOUBLE_TAP) {
                    boolean wasDoubleTapped = DataUtils.DoubleTapDataConverter(data);
                    //broadcast data update
                    if(wasDoubleTapped){broadcastMovesenseDataUpdate(data[1],null, null, null);}
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
    private void broadcastMovesenseDataUpdate(byte requestId,final ArrayList<IMU6Point> IMU6PointList,
                                              final ArrayList<HrPoint> hrPointList,
                                              final ArrayList<TempPoint> tempPointList) {
        final Intent intent = new Intent(ACTION_GATT_MOVESENSE_EVENTS);
        if(requestId == REQUEST_ID_IMU6){
            intent.putExtra(EVENT, Event.IMU6_DATA_AVAILABLE);
            intent.putParcelableArrayListExtra(MOVESENSE_DATA, IMU6PointList);
        }else if (requestId == REQUEST_ID_HR){
            intent.putExtra(EVENT, Event.HR_DATA_AVAILABLE);
            intent.putParcelableArrayListExtra(MOVESENSE_HR_DATA, hrPointList);
        }else if (requestId == REQUEST_ID_TEMP){
            intent.putExtra(EVENT, Event.TEMP_DATA_AVAILABLE);
            intent.putParcelableArrayListExtra(MOVESENSE_TEMP_DATA, tempPointList);
        } else if (requestId == REQUEST_ID_DOUBLE_TAP){
            intent.putExtra(EVENT, Event.DOUBLE_TAP_DETECTED);
        }
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

    //logging and debugging
    private void logCharacteristicProperties(BluetoothGattCharacteristic pChar) {
        boolean isCharacteristicWritable = (pChar.getProperties() & (BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0;
        boolean isCharacteristicReadable = ((pChar.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) != 0);
        boolean isCharacteristicNotifiable = (pChar.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0;
        Log.i(TAG, ">>> write: " + isCharacteristicWritable);
        Log.i(TAG, ">>> read: " + isCharacteristicReadable);
        Log.i(TAG, ">>> notify: " + isCharacteristicNotifiable);
    }
}

