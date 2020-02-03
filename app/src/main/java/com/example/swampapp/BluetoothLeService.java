package com.example.swampapp;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static java.lang.Thread.sleep;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public static final int CLOCK_UPDATE = 1;
    public static final int CLOCK_CALENDAR_UPDATE = 2;
    public static final int CLOCK_TIME_STAMP = 3;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String ACTION_DATA_WRITE =
            "com.example.bluetooth.le.ACTION_DATA_CONFIRM_WRITE";
    public final static String ACTION_DEVICE_FOUND =
            "com.example.bluetooth.le.ACTION_DEVICE_FOUND";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";

    private BluetoothGattCharacteristic bluetoothGattCharacteristic;
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothGatt bluetoothGatt;
    private String bluetoothDeviceAddress;
    private ScanCallback scanCallback;

    private int mConnectionState = STATE_DISCONNECTED;
    private boolean isScanning = false;

    private ArrayList<BluetoothDevice> devices = new ArrayList<>();

    private String linha;
    public ArrayList<String> log = new ArrayList<>();
    public ArrayList<String> logTimeStamp = new ArrayList<>();


//CALLBACKS
        private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.e(TAG, "onConnectionStateChange");
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        bluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.e(TAG, "onServicesDiscovered");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                bluetoothGattCharacteristic = getSerialCharacteristic();
                setCharacteristicNotification(bluetoothGattCharacteristic, true);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            Log.e(TAG, "onCharacteristicChanged");

            final byte[] data = characteristic.getValue();
            if(data != null && data.length > 0) {
                String tmp = new String(data);
                for(char c : tmp.toCharArray()) {
                    if(linha == null) linha = new String();
                    if(c != '\0') {
                        if(c != '\n') {
                            linha = linha + c;
                        } else {
                            if(!linha.trim().replace("\n", "").isEmpty()) {
                                logTimeStamp.add(getClock(CLOCK_TIME_STAMP));
                                log.add(linha.trim().replace("\n", ""));
                                broadcastUpdate(ACTION_DATA_AVAILABLE, linha);
                            }
                            linha = null;
                        }
                    }
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.e(TAG, "onCharacteristicWrite");
            if(status == BluetoothGatt.GATT_SUCCESS) {
                logTimeStamp.add(getClock(CLOCK_TIME_STAMP));
                log.add(new String(characteristic.getValue()).trim().replace("\n", ""));
                broadcastUpdate(ACTION_DATA_WRITE);
            }
        }
    };





//BROADCAST
    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final String data) {
        Log.e(TAG, "broadcastUpdate");
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_DATA, data);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothDevice device) {
        Log.e(TAG, "broadcastUpdate");
        final Intent intent = new Intent(action);


        intent.putExtra("DEVICE_NAME", device.getName());
        intent.putExtra("DEVICE_ADDRESS", device.getAddress());

        sendBroadcast(intent);
    }



//SERVICE METHODS
    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.e(TAG, "onBind");
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.e(TAG, "onUnbind");
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();





//CONNECTION
    public void startScan(ScanCallback callback) {
    Log.e(TAG, "startScan: Começando a scanear");
    if(bluetoothAdapter == null) {
        Log.e(TAG, "startScan: BluetoothAdapter é nulo");
        return;
    }
    if(bluetoothLeScanner == null) {
        Log.e(TAG, "startScan: BluetoothScanner é nulo");
        return;
    }
    isScanning = true;
    bluetoothLeScanner.startScan(callback);
}

    public void stopScan(ScanCallback callback) {
        Log.e(TAG, "stopScan: Parando");
        if(bluetoothAdapter == null) {
            Log.e(TAG, "stopScan: BluetoothAdapter é nulo");
            return;
        }
        if(bluetoothLeScanner == null) {
            Log.e(TAG, "stopScan: BluetoothScanner é nulo");
            return;
        }
        isScanning = false;
        bluetoothLeScanner.stopScan(callback);
    }

    public boolean connect(final String address) {
        Log.e(TAG, "connect");
        if (bluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (bluetoothDeviceAddress != null && address.equals(bluetoothDeviceAddress)
                && bluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (bluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "DeviceActivity not found.  Unable to connect.");
            return false;
        }

        stopScan(scanCallback);

        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        bluetoothGatt = device.connectGatt(this, true, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        bluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }


    public void disconnect() {
        Log.e(TAG, "disconnect");
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        stopScan(scanCallback);
        bluetoothGatt.disconnect();
    }


    public void close() {
        Log.e(TAG, "close");
        if (bluetoothGatt == null) {
            return;
        }
        bluetoothGatt.close();
        bluetoothGatt = null;
    }





//OTHER
    public boolean initialize() {
        Log.e(TAG, "initialize");
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (bluetoothManager == null) {
            bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                if(result.getDevice().getName() != null) {
                    Log.e(TAG, "FOUND:" + result.getDevice().getName());

                    if(!devices.contains(result.getDevice())) {
                        devices.add(result.getDevice());
                        broadcastUpdate(ACTION_DEVICE_FOUND, result.getDevice());
                    }
                }
            }
        };

        startScan(scanCallback);

        return true;
    }


    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        Log.e(TAG, "readCharacteristic");
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        bluetoothGatt.readCharacteristic(characteristic);
    }


    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        Log.e(TAG, "setCharacteristicNotification");
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        bluetoothGatt.setCharacteristicNotification(characteristic, enabled);
    }


    public List<BluetoothGattService> getSupportedGattServices() {
        Log.e(TAG, "getSupportedGattServices");
        if (bluetoothGatt == null) return null;

        return bluetoothGatt.getServices();
    }

    public BluetoothGattCharacteristic getSerialCharacteristic() {
        List<BluetoothGattService> servicos = bluetoothGatt.getServices();
        BluetoothGattCharacteristic bluetoothGattCharacteristic;
        BluetoothGattService service;

        String strUUID;
        UUID uuid;

        for(int i = 0; i < servicos.size(); i++) {
            strUUID = servicos.get(i).getUuid().toString();
            if(SampleGattAttributes.isOnSampleAttributes(strUUID)) {
                service = servicos.get(i); //------------------------------------------------|Gets the right service
                uuid = UUID.fromString(SampleGattAttributes.getCharacteristic(strUUID)); //--|Gets the UUID of the characteristic
                bluetoothGattCharacteristic = service.getCharacteristic(uuid);//-------------|Gets the characteristic
                Log.e(TAG, "getSerialCharacteristic: Service: " + strUUID);
                Log.e(TAG, "getSerialCharacteristic: Characteristic: " + bluetoothGattCharacteristic.getUuid());
                return bluetoothGattCharacteristic;
            }
        }

        Toast.makeText(this, "O dispositivo não é compatível com o APP", Toast.LENGTH_SHORT).show();
        onDestroy();

        return null;
    }

    public void writeSerialCharacteristic(String comando) {
        if(comando != null && comando.length() > 0) {
            Log.e(TAG, "writeSerialCharacteristic");
            comando = comando + '\n';
            bluetoothGattCharacteristic.setValue(comando.getBytes());
            bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic);
        }
    }





//COMMANDS
    public void initCommands() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    sleep(2000);
                    writeSerialCharacteristic(commandAlwaysAwake());
                    sleep(2000);
                    writeSerialCharacteristic(getClock(CLOCK_UPDATE));
                    sleep(2000);
                    writeSerialCharacteristic(getClock(CLOCK_CALENDAR_UPDATE));
                    sleep(2000);
                    writeSerialCharacteristic(commandInformation());
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "Não foi possivel escrever comandos iniciais", Toast.LENGTH_SHORT).show();
                }
            }
        }).start();
    }

    public String getClock(int type) {
        SimpleDateFormat sdf = new SimpleDateFormat("YY/MM/dd:u HH:mm:ss");
        Date date = Calendar.getInstance().getTime();

        if(type == CLOCK_UPDATE) {
            sdf = new SimpleDateFormat("HH:mm:ss");
        } else if(type == CLOCK_CALENDAR_UPDATE) {
            sdf = new SimpleDateFormat("YY/MM/dd:u");

        } else {
            sdf = new SimpleDateFormat("HH:mm:ss");
            return sdf.format(date);
        }

        return "T " + sdf.format(date);
    }

    public String commandInformation() {
        return "C";
    }

    public String commandAlwaysAwake() {
        return "Z 0";
    }

    public String commandSleepNow() {
        return "Z 1";
    }

    public String commandVerbose() {
        return "V";
    }
}