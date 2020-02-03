package com.example.swampapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class TerminalAdapter extends RecyclerView.Adapter<TerminalAdapter.TerminalViewHolder>{
    private ArrayList<String> log;
    private ArrayList<String> logTimeStamp;

    public static class TerminalViewHolder extends RecyclerView.ViewHolder {
        public TextView txtLog;
        public TextView txtLogTimeStamp;

        public TerminalViewHolder(@NonNull View itemView) {
            super(itemView);
            txtLogTimeStamp = itemView.findViewById(R.id.txtTimeStamp);
            txtLog = itemView.findViewById(R.id.txtLog);
        }
    }

    public TerminalAdapter(ArrayList<String> log, ArrayList<String> logTimeStamp) {
        this.log = log;
        this.logTimeStamp = logTimeStamp;
    }

    @NonNull
    @Override
    public TerminalAdapter.TerminalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = (View) LayoutInflater.from(parent.getContext()).inflate(R.layout.rec_item_terminal, parent, false);
        TerminalViewHolder terminalViewHolder = new TerminalViewHolder(v);
        return terminalViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull TerminalAdapter.TerminalViewHolder holder, int position) {
        holder.txtLog.setText(log.get(position));
        holder.txtLogTimeStamp.setText(logTimeStamp.get(position));
    }

    @Override
    public int getItemCount() {
        return log.size();
    }
}
