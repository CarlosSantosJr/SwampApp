package com.example.swampapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Window;
import android.widget.ProgressBar;

import java.util.ArrayList;

public class DevicesListActivity extends AppCompatActivity implements DevicesListAdapter.OnDevicesListener{
    private static final String TAG = "DevicesListActivity";

    private ProgressBar progressBar;

    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    private DevicesListAdapter devicesListAdapter;

    private BluetoothLeService bluetoothLeService;

    private ArrayList<String> devicesName = new ArrayList<>();
    private ArrayList<String> devicesAddress = new ArrayList<>();

//CALLBACKS
    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if(BluetoothLeService.ACTION_DEVICE_FOUND.equals(action)) {
                String name = intent.getStringExtra("DEVICE_NAME");
                String address = intent.getStringExtra("DEVICE_ADDRESS");

                if(!devicesName.contains(name) && name != null) {
                    devicesName.add(name);
                    devicesAddress.add(address);
                    devicesListAdapter.notifyDataSetChanged();
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
        intentFilter.addAction(BluetoothLeService.ACTION_DEVICE_FOUND);
        return intentFilter;
    }

    public void prepareRecyclerView() {
        //Anexa a variável recyclerView ao RecyclerView listDevices que está no layout
        recyclerView = findViewById(R.id.recyclerDevicesList);
        recyclerView.setHasFixedSize(true);

        //Define o tipo de layout que vai ser usado para colocar os itens na tela
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        //Aloca a lista de dispositivos bluetoothLE
        devicesName = new ArrayList<>();
        devicesAddress = new ArrayList<>();

        //Preenche a lista com os dispositivos na UI
        devicesListAdapter = new DevicesListAdapter(devicesName, devicesAddress,this);
        recyclerView.setAdapter(devicesListAdapter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_devices_list);

        progressBar = findViewById(R.id.progressBar2);
        progressBar.getIndeterminateDrawable();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        prepareRecyclerView();

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
    public void onDeviceClick(int position) {
        String device = devicesAddress.get(position);

        bluetoothLeService.connect(device);

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("DEVICE_LIST_CODE", "devices_list");
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        bluetoothLeService.close();
        Intent back = new Intent(this, MainActivity.class);
        finish();
        startActivity(back);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
