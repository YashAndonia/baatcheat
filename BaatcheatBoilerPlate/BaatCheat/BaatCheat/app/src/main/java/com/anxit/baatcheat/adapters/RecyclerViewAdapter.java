package com.anxit.baatcheat.adapters;

// Goto https://docs.google.com/document/d/1o8M2VXJpily2orCvxVgrWESwaT6MFbgPxZjOgLR86Wk
// For understanding this class

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.anxit.baatcheat.R;

import java.util.ArrayList;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>{

    private ArrayList<String> usernameList;

    public RecyclerViewAdapter(ArrayList<String> usernameList){
        this.usernameList = usernameList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.username_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.usernameTextView.setText(usernameList.get(position));
    }

    @Override
    public int getItemCount() {
        return usernameList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder{
        TextView usernameTextView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);

            //Initializing UI elements
            usernameTextView = itemView.findViewById(R.id.username_textview);
        }
    }
}
