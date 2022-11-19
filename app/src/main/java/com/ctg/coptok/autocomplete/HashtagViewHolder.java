package com.ctg.coptok.autocomplete;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ctg.coptok.R;

public class HashtagViewHolder extends RecyclerView.ViewHolder {

    public TextView name;
    public TextView clips;

    public HashtagViewHolder(@NonNull View root) {
        super(root);
        name = root.findViewById(R.id.name);
        clips = root.findViewById(R.id.clips);
    }
}
