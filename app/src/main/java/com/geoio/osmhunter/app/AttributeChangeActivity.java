package com.geoio.osmhunter.app;

import android.accounts.AccountManager;
import android.app.ActionBar;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.geoio.osmhunter.app.FormFields.FormField;
import com.geoio.osmhunter.app.SyncAdapter.HunterActivity;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.apache.http.Header;
import org.apache.http.entity.ByteArrayEntity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;


public class AttributeChangeActivity extends HunterActivity {
    List<JSONObject> attributes_list = new ArrayList<JSONObject>();
    LayoutInflater inflater;
    LinearLayout list_layout;
    private ArrayList<FormField> formFields = new ArrayList<FormField>();
    private String id;
    private String lat;
    private String lon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attribute_change);

        inflater = LayoutInflater.from(this);
        list_layout = (LinearLayout) this.findViewById(R.id.list);

        Intent intent = getIntent();
        id = intent.getStringExtra("id");
        lat = intent.getStringExtra("lat");
        lon = intent.getStringExtra("lon");

        updateAttributesList(id);

        final ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.attribute_change, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch(item.getItemId()) {
            case R.id.action_settings:
                return true;

            case R.id.action_save:
                saveForm();
                return true;

            case R.id.action_navigate:
                StringBuilder stringBuilder = new StringBuilder("geo:0,0?q=").append(lat).append(",").append(lon);
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(stringBuilder.toString()));

                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                }

                return true;

            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveForm() {
        if(!accountReady) {
            return;
        }

        JSONObject payload = new JSONObject();
        JSONObject tags = null;
        try {
            payload.put("tags", new JSONObject());
            tags = payload.getJSONObject("tags");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // get all forms
        for(int i = 0; i < formFields.size(); i++) {
            JSONObject options = formFields.get(i).options;
            View view = formFields.get(i).view;

            try {
                if(options.getString("type").equals("select")) {
                    Spinner spinnerView = (Spinner) view.findViewById(R.id.spinner);
                    Integer selection = spinnerView.getSelectedItemPosition();
                    if(options.getBoolean("allow_empty")) {
                        selection -= 1;
                    }
                    String selectedValue = options.getJSONArray("options").getJSONObject(selection).getString("value");

                    if(!selectedValue.equals(options.getString("value")))                                           // value changed
                        if(!(!options.getBoolean("allow_empty") && selection == -1))                                // value is null but cannot be null
                            if(!(selection == -1 && options.isNull("value") && options.getBoolean("allow_empty")))  // value did not changed and is null
                                tags.put(options.getString("name"), selectedValue);

                } else {
                    EditText textView = (EditText) view.findViewById(R.id.input);

                    if(!textView.getText().toString().equals(options.getString("value")))                                                   // value changed
                        if(!(!options.getBoolean("allow_empty") && TextUtils.isEmpty(textView.getText())))                                  // value is null but cannot be null
                            if(!(TextUtils.isEmpty(textView.getText()) && options.isNull("value") && options.getBoolean("allow_empty")))    // value did not changed and is null
                                tags.put(options.getString("name"), textView.getText());
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if(tags.length() > 0) {
            Uri.Builder b = Uri.parse(getString(R.string.geoio_api_url)).buildUpon();
            AsyncHttpClient client = new AsyncHttpClient();
            ByteArrayEntity entity;

            // build url
            b.appendPath("buildings");
            b.appendPath(id);
            b.appendQueryParameter("apikey", user.getString(AccountManager.KEY_AUTHTOKEN));
            String url = b.build().toString();

            try {
                entity = new ByteArrayEntity(payload.toString().getBytes("UTF-8"));

                // fire http
                client.put(this, url, entity, "application/json", new JsonHttpResponseHandler() {
                    @Override
                    public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                        // please do anything!
                    }
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        if(statusCode == 400) {
                            accountInvalidate();
                            return;
                        }

                        finish();
                    }
                });
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateAttributesList(final String id) {
        Uri.Builder b = Uri.parse(getString(R.string.geoio_api_url)).buildUpon();
        AsyncHttpClient client = new AsyncHttpClient();

        // build url
        b.appendPath("buildings");
        b.appendPath(id);
        b.appendPath("edit-form");
        String url = b.build().toString();

        // fire http
        client.get(url, null, new JsonHttpResponseHandler() {
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                // please do anything!
            }
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    JSONArray result = response.getJSONArray("result");

                    for(int i = 0; i < result.length(); i++) {
                        JSONObject attribute = result.getJSONObject(i);
                        String type = attribute.getString("type");
                        View elem = null;
                        FormField field = null;

                        if(type.equals("select")) {
                            elem = inflater.inflate(R.layout.activity_attribute_change_item_spinner, null);
                            field = new FormField(attribute, elem);

                            TextView label = (TextView) elem.findViewById(R.id.label);
                            Spinner spinner = (Spinner) elem.findViewById(R.id.spinner);
                            JSONArray options = attribute.getJSONArray("options");

                            label.setText(attribute.getString("label"));

                            ArrayList<String> items = new ArrayList<String>();
                            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_spinner_dropdown_item);

                            if(attribute.getBoolean("allow_empty")) {
                                items.add("");  // empty
                            }

                            Integer selected = 0;
                            for(int a = 0; a < options.length(); a++) {
                                JSONObject option = options.getJSONObject(a);
                                if(option.getString("value").equals(attribute.getString("value"))) {
                                    selected = a;
                                    if(attribute.getBoolean("allow_empty")) {
                                        selected = a + 1;
                                    }
                                }
                                items.add(option.getString("label"));
                            }

                            adapter.addAll(items);
                            spinner.setAdapter(adapter);
                            spinner.setSelection(selected);
                        } else {
                            elem = inflater.inflate(R.layout.activity_attribute_change_item_text, null);
                            field = new FormField(attribute, elem);

                            TextView label = (TextView) elem.findViewById(R.id.label);
                            EditText input = (EditText) elem.findViewById(R.id.input);

                            String labelText = attribute.getString("label");
                            String valueText = attribute.getString("value");

                            label.setText(labelText);
                            if(!valueText.equals("null")) {
                                input.setText(valueText);
                            }

                            if(type.equals("url")) {
                                input.setRawInputType(InputType.TYPE_TEXT_VARIATION_URI);
                            } else if(type.equals("phone")) {
                                input.setRawInputType(InputType.TYPE_CLASS_PHONE);
                            }
                        }

                        list_layout.addView(elem);
                        formFields.add(field);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

}
