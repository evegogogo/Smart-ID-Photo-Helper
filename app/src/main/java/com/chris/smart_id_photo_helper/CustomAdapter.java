package com.chris.smart_id_photo_helper;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class CustomAdapter extends ArrayAdapter<PhotoData> {
    public CustomAdapter(Context context, ArrayList<PhotoData> arrayList) {
        super(context, 0, arrayList);
        //this.setNotifyOnChange(true);
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        PhotoData photoData = getItem(position);;
        View currentItemView = convertView;

        if (currentItemView == null) {
            currentItemView = LayoutInflater.from(getContext()).inflate(R.layout.activity_listview, parent, false);
        }

        TextView title = currentItemView.findViewById(R.id.label);
        ImageView image = currentItemView.findViewById(R.id.list_image);
        title.setText(photoData.name);
        Picasso.get().load(photoData.image).into(image);

        return currentItemView;
    }
}
