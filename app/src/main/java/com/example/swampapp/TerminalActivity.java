package com.example.swampapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;

public class TerminalActivity extends AppCompatActivity {
    private static final String TAG = "TerminalActivity";

    private BluetoothLeService bluetoothLeService;

    private Button btnSend;
    private EditText edtTerminal;

    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    private TerminalAdapter terminalAdapter;

//CALLBACKS
    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e(TAG, "onReceive");

            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Toast.makeText(getApplicationContext(), "Dispositivo desconectado, reiniciando...", Toast.LENGTH_SHORT).show();
                restartApp();
            } else if(BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                terminalAdapter.notifyDataSetChanged();
                if(terminalAdapter.getItemCount()-1 > 0) {
                    recyclerView.scrollToPosition(terminalAdapter.getItemCount() - 1);
                }
            } else if(BluetoothLeService.ACTION_DATA_WRITE.equals(action)) {
                terminalAdapter.notifyDataSetChanged();
                if(terminalAdapter.getItemCount()-1 > 0) {
                    recyclerView.scrollToPosition(terminalAdapter.getItemCount() - 1);
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

            //Preenche a lista com os dispositivos na UI
            terminalAdapter = new TerminalAdapter(bluetoothLeService.log, bluetoothLeService.logTimeStamp);
            recyclerView.setAdapter(terminalAdapter);
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
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_WRITE);
        return intentFilter;
    }





//ACTIVITY METHODS
    public void prepareRecyclerView() {
        //Anexa a variável recyclerView ao RecyclerView listDevices que está no layout
        recyclerView = findViewById(R.id.recyclerTerminal);
        recyclerView.setHasFixedSize(true);

        //Define o tipo de layout que vai ser usado para colocar os itens na tela
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        //Preenche a lista com os dispositivos na UI
        terminalAdapter = new TerminalAdapter(new ArrayList<String>(), new ArrayList<String>());
        recyclerView.setAdapter(terminalAdapter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terminal);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        btnSend = findViewById(R.id.btnSend);
        edtTerminal = findViewById(R.id.edtTerminal);

        prepareRecyclerView();

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, serviceConnection, BIND_AUTO_CREATE);

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bluetoothLeService.writeSerialCharacteristic(edtTerminal.getText().toString());
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

    public void restartApp() {
        Intent restartApp = new Intent(this, MainActivity.class);
        startActivity(restartApp);
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent back = new Intent(this, DeviceActivity.class);
        startActivity(back);
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
