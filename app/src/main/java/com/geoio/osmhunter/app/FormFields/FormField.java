package com.geoio.osmhunter.app.FormFields;

import android.view.View;

import org.json.JSONObject;

public class FormField {
    public JSONObject options;
    public View view;

    public FormField(JSONObject opts, View v) {
        options = opts;
        view = v;
    }


}
