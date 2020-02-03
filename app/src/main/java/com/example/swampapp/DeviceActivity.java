package com.example.swampapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.regex.Pattern;

public class DeviceActivity extends AppCompatActivity {
    private static final String TAG = "DeviceActivity";

    private TextView txtId;
    private TextView txtFirmware;
    private TextView txtTemperatura;
    private TextView txtBateria;
    private TextView txtLatitude;
    private TextView txtLongitude;

    private TextView txtLast;
    private TextView txtBalance;
    private TextView txtChannels;
    private TextView txtGain;
    private TextView txtPeriod;
    private TextView txtRadio;
    private TextView txtAlarm;

    private TextView txtH1;
    private TextView txtH2;
    private TextView txtH3;
    private TextView txtT1;

    private BluetoothLeService bluetoothLeService;

    private FusedLocationProviderClient fusedLocationClient;


//CALLBACKS
    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Toast.makeText(getApplicationContext(), "Dispositivo desconectado, reiniciando...", Toast.LENGTH_SHORT).show();
                restartApp();
            } else if (BluetoothLeService.
                    ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {

            } else if(BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                getInformationFromLog();
            }
        }
    };





//SERVICE CONNECTION AND BROADCAST FILTER
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.e(TAG, "onServiceConnected");
            bluetoothLeService = ((BluetoothLeService.LocalBinder) iBinder).getService();
            getInformationFromLog();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.e(TAG, "onServiceDisconnected");
            bluetoothLeService = null;
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        Log.e(TAG, "makeGattUpdateIntentFilter");
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_DEVICE_FOUND);
        return intentFilter;
    }





//OTHER METHODS
    private void getInformationFromLog() {
        if(bluetoothLeService.log.size() > 0) {
            for (String s : bluetoothLeService.log) {
                String array[];
                if (s.contains("H1")) {
                    array = s.split(Pattern.quote("|"));
                    if(array.length > 12) {
                        txtId.setText(array[4]);
                        txtH1.setText(array[6]);
                        txtH2.setText(array[8]);
                        txtH3.setText(array[10]);
                        txtT1.setText(addChar(array[12],'.', array[12].length() - 1) + " °C");
                    }
                } else if (s.contains("B|")) {
                    array = s.split(Pattern.quote("|"));
                    if(array.length > 8) {
                        txtTemperatura.setText(addChar(array[6],'.', array[6].length() - 1) + " °C");
                        txtBateria.setText(array[8] + "%");
                    }
                } else if (s.contains("Embrapa")) {
                    array = s.split("v");
                    if(array.length > 1) {
                        txtFirmware.setText(array[1].trim());
                    }
                } else if (s.contains("Start time")) {
                    array = s.split(Pattern.quote(" "));
                    if(array.length > 5) {
                        String tmp = array[5];
                        if(tmp.length() > 11) {
                            tmp = tmp.substring(8, 12);
                            txtLast.setText(addChar(tmp, ':', 2));
                        }
                    }
                } else if (s.contains("Running")) {
                    array = s.split(Pattern.quote(" "));
                    if(array.length > 7) {
                        txtBalance.setText(array[3]);
                        txtChannels.setText(array[5]);
                        txtGain.setText(array[7]);
                    }
                } else if (s.contains("Period")) {
                    array = s.split(Pattern.quote(" "));
                    if(array.length > 6) {
                        txtPeriod.setText(array[1] + " " + array[2]);
                        txtRadio.setText(array[5] + " " + array[6]);
                    }
                } else if (s.contains("Alarm")) {
                    array = s.split(Pattern.quote(" "));
                    if(array.length > 3) {
                        txtAlarm.setText(array[3]);
                    }
                }
            }
        }
    }

    public String addChar(String str, char ch, int position) {
        return str.substring(0, position) + ch + str.substring(position);
    }




//ACTIVITY METHODS
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        txtId = findViewById(R.id.txtId);
        txtFirmware = findViewById(R.id.txtFirmware);
        txtBateria = findViewById(R.id.txtBateria);
        txtTemperatura = findViewById(R.id.txtTemperatura);
        txtLatitude = findViewById(R.id.txtLatitude);
        txtLongitude = findViewById(R.id.txtLongitude);

        txtLast = findViewById(R.id.txtLast);
        txtBalance = findViewById(R.id.txtBalance);
        txtChannels = findViewById(R.id.txtChannels);
        txtGain = findViewById(R.id.txtGain);
        txtPeriod = findViewById(R.id.txtPeriod);
        txtRadio = findViewById(R.id.txtRadio);
        txtAlarm = findViewById(R.id.txtAlarm);

        txtH1 = findViewById(R.id.txtH1);
        txtH2 = findViewById(R.id.txtH2);
        txtH3 = findViewById(R.id.txtH3);
        txtT1 = findViewById(R.id.txtT1);

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, serviceConnection, BIND_AUTO_CREATE);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        int permissionCheck = this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        Log.e(TAG, "onSuccess: NULL");
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            // Logic to handle location object
                            Log.e(TAG, "onSuccess: PEGOU A LOCALIZACAO");
                            txtLatitude.setText(String.valueOf(location.getLatitude()));
                            txtLongitude.setText(String.valueOf(location.getLongitude()));

                            if(bluetoothLeService != null) {
                                bluetoothLeService.writeSerialCharacteristic("L " + String.valueOf(location.getLatitude()) +  " " + String.valueOf(location.getLongitude()));
                            }
                        }
                    }
                });
    }

    @Override
    protected void onResume() {
        Log.e(TAG, "onResume");
        super.onResume();
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    protected void onPause() {
        Log.e(TAG, "onPause");
        super.onPause();
        unregisterReceiver(gattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy");
        super.onDestroy();
        unbindService(serviceConnection);
        bluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_device, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mItemTerminal:
                Intent intent = new Intent(this, TerminalActivity.class);
                startActivity(intent);
                finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void restartApp() {
        Intent restartApp = new Intent(this, MainActivity.class);
        startActivity(restartApp);
        finish();
    }
}
