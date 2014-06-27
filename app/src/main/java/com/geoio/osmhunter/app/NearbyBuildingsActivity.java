package com.geoio.osmhunter.app;

import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.geoio.osmhunter.app.Workarounds.HouseOverlay;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.List;


public class NearbyBuildingsActivity extends MapActivity {
    private List<JSONObject> nearbyBuildings = new ArrayList<JSONObject>();
    private Integer nearbyBuildingsNeedle = 0;
    private Boolean firstLoad = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.nearby_buildings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch(id) {
            case R.id.action_settings:
                return true;

            case R.id.action_previous:
                if(nearbyBuildingsNeedle > 0) {
                    nearbyBuildingsNeedle--;
                    scrollToNearby();
                }
                return true;

            case R.id.action_next:
                if(nearbyBuildingsNeedle < nearbyBuildings.size() - 1) {
                    nearbyBuildingsNeedle++;
                    scrollToNearby();
                }
                return true;
        }
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void scrollToNearby() {
        try {
            JSONObject building = nearbyBuildings.get(nearbyBuildingsNeedle);
            JSONObject buildingCentroid = building.getJSONObject("centroid");
            GeoPoint centroid;

            centroid = new GeoPoint(buildingCentroid.getDouble("lat"), buildingCentroid.getDouble("lon"));

            mapView.getController().animateTo(centroid);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateShapesOverlay() {
        if(!firstLoad) {
            return;
        }

        Uri.Builder b = Uri.parse(getString(R.string.geoio_api_url)).buildUpon();
        AsyncHttpClient client = new AsyncHttpClient();
        GeoPoint myLocation = myLocationOverlay.getMyLocation();

        if(myLocation == null) {
            return;
        }

        b.appendPath("buildings");
        b.appendPath("nearby");
        b.appendQueryParameter("lat", String.valueOf(myLocation.getLatitude()));
        b.appendQueryParameter("lon", String.valueOf(myLocation.getLongitude()));
        b.appendQueryParameter("limit", res.getString(R.integer.geoio_api_buildings_per_request));
        b.appendQueryParameter("radius", res.getString(R.integer.geoio_api_buildings_nearby_radius));
        String url = b.toString();

        client.get(url, null, new JsonHttpResponseHandler() {
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.error_api), Toast.LENGTH_LONG);
                toast.show();
            }
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {

                    // read results into our ArrayList
                    JSONArray results = response.getJSONArray("results");
                    for(int i = 0; i < results.length(); i++) {
                        nearbyBuildings.add(results.getJSONObject(i));
                    }

                    // draw them
                    for(int i = 0; i < nearbyBuildings.size(); i++) {
                        JSONObject result = results.getJSONObject(i);
                        JSONArray nodes = nearbyBuildings.get(i).getJSONArray("nodes");

                        HouseOverlay poly = new HouseOverlay(mapView, result);

                        for(int ii = 0; ii < nodes.length(); ii++) {
                            double lat = nodes.getJSONObject(ii).getDouble("lat");
                            double lon = nodes.getJSONObject(ii).getDouble("lon");

                            poly.addPoint(new GeoPoint(lat, lon));
                        }

                        poly.setPoints();
                        firstLoad = false;
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

}
