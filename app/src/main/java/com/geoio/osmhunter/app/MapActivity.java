package com.geoio.osmhunter.app;

import android.app.ActionBar;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Toast;

import com.geoio.osmhunter.app.SyncAdapter.HunterActivity;
import com.geoio.osmhunter.app.Workarounds.HouseOverlay;
import com.geoio.osmhunter.app.Workarounds.MyMapView;
import com.geoio.osmhunter.app.Workarounds.UserLocationOverlay;
import com.geoio.osmhunter.app.Workarounds.MapBoxTileSource;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.events.DelayedMapListener;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.util.GeoPoint;


public class MapActivity extends HunterActivity {

    public MyMapView mapView;
    public UserLocationOverlay myLocationOverlay;
    public ActionBar actionBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_map);

        actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        MapBoxTileSource.retrieveMapBoxMapId(this);
        MapBoxTileSource tileSource = new MapBoxTileSource();

        mapView = (MyMapView) this.findViewById(R.id.mapview);
        mapView.setTileSource(tileSource);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(res.getInteger(R.integer.map_initial_zoom));

        showPositionOverlay();

        MyMapListener mListener = new MyMapListener();
        mapView.setMapListener(new DelayedMapListener(mListener));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.map, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch(item.getItemId()) {
            case R.id.action_location:
                if(myLocationOverlay.getMyLocation() != null) {
                    mapView.getController().setZoom(res.getInteger(R.integer.map_initial_zoom));
                    mapView.getController().animateTo(myLocationOverlay.getMyLocation());
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // show current position
    private void showPositionOverlay() {
        myLocationOverlay = new UserLocationOverlay(this, mapView);
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.setDrawAccuracyEnabled(true);
        mapView.getOverlays().add(myLocationOverlay);
    }


    // update shapes on map move/zoom
    public void updateShapesOverlay() {
        // minimum zoomlevel
        if(mapView.getZoomLevel() < res.getInteger(R.integer.geoio_api_min_zoom)) {
            return;
        }

        setProgressBarIndeterminateVisibility(true);

        BoundingBoxE6 bb = mapView.getBoundingBox();
        Uri.Builder b = Uri.parse(getString(R.string.geoio_api_url)).buildUpon();
        AsyncHttpClient client = new AsyncHttpClient();

        // fix the projection
        GeoPoint l1 = new GeoPoint(bb.getLatSouthE6(), bb.getLonWestE6());
        GeoPoint l2 = new GeoPoint(bb.getLatNorthE6(), bb.getLonEastE6());

        // build url
        b.appendPath("buildings");
        b.appendQueryParameter("south", String.valueOf(l1.getLatitude()));
        b.appendQueryParameter("west", String.valueOf(l1.getLongitude()));
        b.appendQueryParameter("north", String.valueOf(l2.getLatitude()));
        b.appendQueryParameter("east", String.valueOf(l2.getLongitude()));
        b.appendQueryParameter("limit", res.getString(R.integer.geoio_api_buildings_per_request));
        String url = b.build().toString();

        // fire http
        client.get(url, null, new JsonHttpResponseHandler() {
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.error_api), Toast.LENGTH_LONG);
                toast.show();
            }
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {

                    // draw houses
                    JSONArray results = response.getJSONArray("results");

                    for(int i = 0; i < results.length(); i++) {
                        JSONObject result = results.getJSONObject(i);
                        JSONArray nodes = result.getJSONArray("nodes");

                        HouseOverlay poly = new HouseOverlay(mapView, result);

                        for(int ii = 0; ii < nodes.length(); ii++) {
                            double lat = nodes.getJSONObject(ii).getDouble("lat");
                            double lon = nodes.getJSONObject(ii).getDouble("lon");

                            poly.addPoint(new GeoPoint(lat, lon));
                        }

                        poly.setPoints();
                    }

                    setProgressBarIndeterminateVisibility(false);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private class MyMapListener implements MapListener {
        @Override
        public boolean onZoom(ZoomEvent ev) {
            updateShapesOverlay();
            return true;
        }

        @Override
        public boolean onScroll(ScrollEvent ev) {
            updateShapesOverlay();
            return true;
        }
    }

}
