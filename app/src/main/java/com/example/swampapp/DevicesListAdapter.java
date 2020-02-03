package com.example.swampapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class DevicesListAdapter extends RecyclerView.Adapter<DevicesListAdapter.DevicesViewHolder>{
    private ArrayList<String> devicesName;
    private ArrayList<String> devicesAddress;
    private OnDevicesListener onDevicesListener;

    public DevicesListAdapter(ArrayList<String> devicesName, ArrayList<String> devicesAddress, OnDevicesListener onDevicesListener) {
        this.devicesName = devicesName;
        this.devicesAddress = devicesAddress;
        this.onDevicesListener = onDevicesListener;
    }

    @NonNull
    @Override
    public DevicesListAdapter.DevicesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = (View) LayoutInflater.from(parent.getContext()).inflate(R.layout.rec_item_device_list, parent, false);
        DevicesViewHolder devicesViewHolder = new DevicesViewHolder(v, onDevicesListener);
        return devicesViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull DevicesListAdapter.DevicesViewHolder holder, int position) {
        String deviceName = devicesName.get(position);
        String deviceAddress = devicesAddress.get(position);

        holder.deviceName.setText(deviceName);
        holder.deviceAddress.setText(deviceAddress);
    }

    @Override
    public int getItemCount() {
        return devicesName.size();
    }

    public static class DevicesViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView deviceName;
        TextView deviceAddress;
        OnDevicesListener onDevicesListener;

        public DevicesViewHolder(@NonNull View itemView, OnDevicesListener onDevicesListener) {
            super(itemView);
            deviceName = itemView.findViewById(R.id.txtDeviceName);
            deviceAddress = itemView.findViewById(R.id.txtDeviceAddress);
            this.onDevicesListener = onDevicesListener;

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            onDevicesListener.onDeviceClick(getAdapterPosition());
        }
    }

    public interface OnDevicesListener{
        void onDeviceClick(int position);
    }
}
