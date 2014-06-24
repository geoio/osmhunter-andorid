package com.geoio.osmhunter.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class AttributeChangeAdapter extends BaseAdapter {
    private LayoutInflater mInflater;
    List<JSONObject> attributes_list;

    public AttributeChangeAdapter(Context context, List<JSONObject> content) {
        mInflater = LayoutInflater.from(context);
        attributes_list = content;
    }

    @Override
    public int getCount() {
        return attributes_list.size();
    }

    @Override
    public Object getItem(int arg0) {
        return arg0;
    }

    @Override
    public long getItemId(int arg0) {
        return arg0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        JSONObject attribute = attributes_list.get(position);

        if(convertView == null) {
            convertView = mInflater.inflate(R.layout.attribute_change_item, null);
        }

        try {
            String type = attribute.getString("type");

            if(type.equals("text")) {
                TextView label = (TextView) convertView.findViewById(R.id.label);
                EditText input = (EditText) convertView.findViewById(R.id.input);

                label.setText(attribute.getString("label"));
                input.setText(attribute.getString("value"));
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return convertView;
    }

}
