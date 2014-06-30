package com.geoio.osmhunter.app.Fragments;

import android.accounts.AccountManager;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.geoio.osmhunter.app.Workarounds.FormField;
import com.geoio.osmhunter.app.R;
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


public class AttributeChangeFragment extends Fragment {
    List<JSONObject> attributes_list = new ArrayList<JSONObject>();
    LayoutInflater inflater;
    LinearLayout list_layout;
    private ArrayList<FormField> formFields = new ArrayList<FormField>();
    private String id;
    private String lat;
    private String lon;
    private MenuItem saveButton;
    private HunterActivity ac;
    private View view;

    public boolean onlyFragment = true;

    @Override
    public View onCreateView(LayoutInflater inf, ViewGroup container, Bundle savedInstanceState) {
        view = inf.inflate(R.layout.fragment_attribute_change, container, false);
        ac = ((HunterActivity) getActivity());

        inflater = inf;
        list_layout = (LinearLayout) view.findViewById(R.id.list);

        setHasOptionsMenu(true);

        return view;
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.attribute_change, menu);
        saveButton = (MenuItem) menu.findItem(R.id.action_save);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch(item.getItemId()) {
            case R.id.action_save:
                saveForm();
                return true;

            case R.id.action_navigate:
                StringBuilder stringBuilder = new StringBuilder("geo:0,0?q=").append(lat).append(",").append(lon);
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(stringBuilder.toString()));

                if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                    startActivity(intent);
                }

                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveForm() {
        if(!ac.accountReady) {
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

                    if(!selectedValue.equals(options.getString("value")) || options.getBoolean("prefilled"))        // value changed
                        if(!(!options.getBoolean("allow_empty") && selection == -1))                                // value is null but cannot be null
                            if(!(selection == -1 && options.isNull("value") && options.getBoolean("allow_empty")))  // value did not changed and is null
                                tags.put(options.getString("name"), selectedValue);

                } else {
                    EditText textView = (EditText) view.findViewById(R.id.input);

                    if(!textView.getText().toString().equals(options.getString("value")) || options.getBoolean("prefilled"))                // value changed
                        if(!(!options.getBoolean("allow_empty") && TextUtils.isEmpty(textView.getText())))                                  // value is null but cannot be null
                            if(!(TextUtils.isEmpty(textView.getText()) && options.isNull("value") && options.getBoolean("allow_empty")))    // value did not changed and is null
                                tags.put(options.getString("name"), textView.getText());
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if(tags.length() > 0) {
            saveButton.setEnabled(false);

            Uri.Builder b = Uri.parse(getString(R.string.geoio_api_url)).buildUpon();
            AsyncHttpClient client = new AsyncHttpClient();
            ByteArrayEntity entity;

            // build url
            b.appendPath("buildings");
            b.appendPath(id);
            b.appendQueryParameter("apikey", ac.user.getString(AccountManager.KEY_AUTHTOKEN));
            String url = b.build().toString();

            try {
                entity = new ByteArrayEntity(payload.toString().getBytes("UTF-8"));

                // fire http
                client.put(getActivity(), url, entity, "application/json", new JsonHttpResponseHandler() {
                    @Override
                    public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                        Toast toast = Toast.makeText(getActivity(), getString(R.string.error_api), Toast.LENGTH_LONG);
                        toast.show();

                        saveButton.setEnabled(true);
                    }
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        if(statusCode == 401) {
                            ac.accountInvalidate();
                            return;
                        }

                        Toast toast = Toast.makeText(getActivity(), getString(R.string.attribute_change_finished), Toast.LENGTH_LONG);
                        toast.show();

                        if(onlyFragment) {
                            getActivity().finish();
                        }
                    }
                });
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else {
            Toast toast = Toast.makeText(getActivity(), getString(R.string.attribute_change_nothing_changed), Toast.LENGTH_LONG);
            toast.show();
        }
    }

    public void setBuilding(String a1, String a2, String a3) {
        id = a1;
        lat = a2;
        lon = a3;

        updateAttributesList(id);
    }

    private void updateAttributesList(final String id) {
        Uri.Builder b = Uri.parse(getString(R.string.geoio_api_url)).buildUpon();
        AsyncHttpClient client = new AsyncHttpClient();

        getActivity().setProgressBarIndeterminateVisibility(true);

        // remove all old views
        formFields.clear();
        list_layout.removeAllViews();
        list_layout.invalidate();

        // build url
        b.appendPath("buildings");
        b.appendPath(id);
        b.appendPath("edit-form");
        String url = b.build().toString();

        // fire http
        client.get(url, null, new JsonHttpResponseHandler() {
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                Toast toast = Toast.makeText(getActivity(), getString(R.string.error_api), Toast.LENGTH_LONG);
                toast.show();
            }
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                if(!isAdded()) {
                    return;
                }

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
                            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_dropdown_item);

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
                            input.setHint(labelText);
                            if(!valueText.equals("null")) {
                                input.setText(valueText);
                            }

                            // prefilled indicator
                            if(attribute.getBoolean("prefilled")) {
                                Drawable errorIcon = ac.res.getDrawable(R.drawable.indicator_input_error);
                                errorIcon.setBounds(0, 0, errorIcon.getIntrinsicWidth(), errorIcon.getIntrinsicHeight());
                                input.setError(ac.res.getString(R.string.attribute_prefilled_indicator), errorIcon);
                            }

                            if(type.equals("url")) {
                                input.setRawInputType(InputType.TYPE_TEXT_VARIATION_URI);
                            } else if(type.equals("phone")) {
                                input.setRawInputType(InputType.TYPE_CLASS_PHONE);
                            }
                        }

                        getActivity().setProgressBarIndeterminateVisibility(false);

                        list_layout.addView(elem);
                        list_layout.invalidate();
                        formFields.add(field);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

}
