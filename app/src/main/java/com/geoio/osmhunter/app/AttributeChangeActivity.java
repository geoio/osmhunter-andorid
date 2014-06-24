package com.geoio.osmhunter.app;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class AttributeChangeActivity extends Activity {

    private ListView attributes_list_view;
    List<JSONObject> attributes_list = new ArrayList<JSONObject>();
    AttributeChangeAdapter attribute_adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attribute_change);

        Intent intent = getIntent();
        String id = intent.getStringExtra("id");

        attribute_adapter = new AttributeChangeAdapter(this, attributes_list);

        attributes_list_view = (ListView) this.findViewById(R.id.attributes_list);
        attributes_list_view.setAdapter(attribute_adapter);

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
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateAttributesList(String id) {
        Uri.Builder b = Uri.parse(getString(R.string.geoio_api_url)).buildUpon();
        AsyncHttpClient client = new AsyncHttpClient();

        // build url
        b.path("/buildings/");
        b.appendPath(id);
        b.appendPath("edit-form");
        String url = b.build().toString();

        // let's fix felix' pathes
        if(!url.endsWith("/")) {
            url += "/";
        }

        Log.v("x", url);

        // fire http
        client.get(url, null, new JsonHttpResponseHandler() {
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                // please do anything!
            }
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    List<JSONObject> attributes_list_buffer = new ArrayList<JSONObject>();
                    JSONArray result = response.getJSONArray("result");

                    for(int i = 0; i < result.length(); i++) {
                        attributes_list_buffer.add(result.getJSONObject(i));
                    }

                    attributes_list.addAll(attributes_list_buffer);
                    attribute_adapter.notifyDataSetChanged();

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

}
