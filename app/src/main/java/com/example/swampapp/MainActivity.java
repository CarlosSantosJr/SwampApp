package com.example.swampapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private BluetoothLeService bluetoothLeService;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothManager bluetoothManager;

    private String linha;
    private ArrayList<String> log = new ArrayList<>();
    private ArrayList<String> logTime = new ArrayList<>();

    private boolean isConnected = false;

    private TextView txtStatus;

//CALLBACKS
    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e(TAG, "onReceive");
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                txtStatus.setText("Procurando Serviços");
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                isConnected = false;
                Toast.makeText(getApplicationContext(), "Dispositivo desconectado, reiniciando...", Toast.LENGTH_SHORT).show();
                restartApp();
            } else if (BluetoothLeService.
                    ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                txtStatus.setText("Esperando Comunicação");
            } else if(BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                //Trata os bytes recebidos e transforma-os em strings
                next();
            } else if(BluetoothLeService.ACTION_DEVICE_FOUND.equals(action)) {
                String tmp = intent.getStringExtra("DEVICE_NAME");
                if(tmp.equals("GreenStick") || tmp.equals("HMSoft")) {
                    isConnected = true;
                    invalidateOptionsMenu();
                    txtStatus.setText("Conectando ao Dispositivo");
                    bluetoothLeService.connect(intent.getStringExtra("DEVICE_ADDRESS"));
                }
            }
        }
    };





//SERVICE CONNECTION AND BROADCAST FILTER
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.e(TAG, "onServiceConnected");
            bluetoothLeService = ((BluetoothLeService.LocalBinder) iBinder).getService();
            bluetoothLeService.initialize();
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

    
    
    

//PERMISSION OF BLUETOOTH AND LOCATION
    public void checkPermissions() {
        Log.e(TAG, "checkPermissions");
        if(bluetoothAdapter != null) {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
                Log.e(TAG, "isBluetoothOn: Ativando Bluetooth");
            }

            //Checa a permissão de localização que é necessária para o BluetoothLE
            int permissionCheck = 0;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                permissionCheck = this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
                if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                }
            }

        } else {
            Toast.makeText(getApplicationContext(), "Adaptador Bluetooth não encontrado", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 1) {
            if(resultCode == RESULT_CANCELED) {
                Toast.makeText(getApplicationContext(), "É necessário habilitar o Bluetooth para continuar", Toast.LENGTH_SHORT).show();
            }
            Intent intent = new Intent(this, MainActivity.class);
            finish();
            startActivity(intent);
        }

    }





//ACTIVITY METHODS
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.e(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();

        txtStatus = findViewById(R.id.txtStatus);

        try {
            if (intent.getStringExtra("DEVICE_LIST_CODE").equals("devices_list")) {
                txtStatus.setText("Conectando ao Dispositivo");
            }
        } catch (Exception e) {

        }

        bluetoothManager = (BluetoothManager) getSystemService(this.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        checkPermissions();

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, serviceConnection, BIND_AUTO_CREATE);

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
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mItemSearch:
                bluetoothLeService.close();
                Intent intent = new Intent(this, DevicesListActivity.class);
                startActivity(intent);
                finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if(menu != null) {
            menu.findItem(R.id.mItemSearch).setVisible(!isConnected);
        }
        return true;
    }

    private void next() {
        bluetoothLeService.initCommands();
        Intent next = new Intent(this, DeviceActivity.class);
        startActivity(next);
        finish();
    }

    private void restartApp() {
        Intent restartApp = new Intent(this, MainActivity.class);
        startActivity(restartApp);
        finish();
    }
}
