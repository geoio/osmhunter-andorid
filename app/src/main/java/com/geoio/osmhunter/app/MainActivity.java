package com.geoio.osmhunter.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;

import com.geoio.osmhunter.app.Workarounds.MyMapView;
import com.geoio.osmhunter.app.Workarounds.UserLocationOverlay;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.bonuspack.overlays.Polygon;
import org.osmdroid.events.DelayedMapListener;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;

import java.util.ArrayList;


public class MainActivity extends Activity {

    private MyMapView mapView;
    private UserLocationOverlay myLocationOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mapView = (MyMapView) this.findViewById(R.id.mapview);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(18);

        showPositionOverlay();

        MyMapListener mListener = new MyMapListener();
        mapView.setMapListener(new DelayedMapListener(mListener));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
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
            case R.id.action_location:
                if(myLocationOverlay.getMyLocation() != null) {
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
    private void updateShapesOverlay() {
        BoundingBoxE6 bb = mapView.getBoundingBox();
        Uri.Builder b = Uri.parse(getString(R.string.geoio_api_url)).buildUpon();
        AsyncHttpClient client = new AsyncHttpClient();

        // fix the projection
        GeoPoint l1 = new GeoPoint(bb.getLatSouthE6(), bb.getLonWestE6());
        GeoPoint l2 = new GeoPoint(bb.getLatNorthE6(), bb.getLonEastE6());

        // build url
        b.path("/buildings/");
        b.appendQueryParameter("south", String.valueOf(l1.getLatitude()));
        b.appendQueryParameter("west", String.valueOf(l1.getLongitude()));
        b.appendQueryParameter("north", String.valueOf(l2.getLatitude()));
        b.appendQueryParameter("east", String.valueOf(l2.getLongitude()));
        b.appendQueryParameter("limit", "6");
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

                    // draw houses
                    JSONArray results = response.getJSONArray("results");

                    for(int i = 0; i < results.length(); i++) {
                        JSONObject result = results.getJSONObject(i);
                        JSONArray nodes = result.getJSONArray("nodes");

                        HouseOverlay poly = new HouseOverlay(mapView.getContext(), result);
                        poly.setFillColor(Color.BLUE);
                        poly.setStrokeColor(Color.RED);
                        poly.setStrokeWidth(5);
                        ArrayList<GeoPoint> points = new ArrayList<GeoPoint>();

                        for(int ii = 0; ii < nodes.length(); ii++) {
                            double lat = nodes.getJSONObject(ii).getDouble("lat");
                            double lon = nodes.getJSONObject(ii).getDouble("lon");

                            points.add(new GeoPoint(lat, lon));
                        }

                        poly.setPoints(points);
                        mapView.getOverlays().add(poly);
                    }

                    mapView.invalidate();

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private class HouseOverlay extends Polygon {
        private JSONObject house;

        public HouseOverlay(final Context ctx, JSONObject h) {
            super(ctx);
            house = h;
        }

        @Override
        public boolean onSingleTapConfirmed(final MotionEvent event, final MapView mapView) {
            boolean touched = contains(event, mapView);
            if (touched){
                Projection pj = mapView.getProjection();
                GeoPoint position = (GeoPoint)pj.fromPixels((int)event.getX(), (int)event.getY());
                try {
                    Intent intent = new Intent(getApplicationContext(), AttributeChangeActivity.class);
                    intent.putExtra("id", house.getString("id"));
                    startActivity(intent);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return touched;
        }
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
