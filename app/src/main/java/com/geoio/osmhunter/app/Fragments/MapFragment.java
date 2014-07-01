package com.geoio.osmhunter.app.Fragments;

import android.app.Activity;
import android.app.Fragment;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.geoio.osmhunter.app.R;
import com.geoio.osmhunter.app.SyncAdapter.HunterActivity;
import com.geoio.osmhunter.app.Workarounds.HouseOverlay;
import com.geoio.osmhunter.app.Workarounds.MapBoxTileSource;
import com.geoio.osmhunter.app.Workarounds.MyMapView;
import com.geoio.osmhunter.app.Workarounds.UserLocationOverlay;
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

import java.util.ArrayList;


public class MapFragment extends Fragment {

    public MyMapView mapView;
    public UserLocationOverlay myLocationOverlay;
    public HunterActivity ac;
    public OnHouseSelectedListener listener;
    private ArrayList<String> drawnBuildings = new ArrayList<String>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        ac = ((HunterActivity) getActivity());

        setHasOptionsMenu(true);

        MapBoxTileSource.retrieveMapBoxMapId(getActivity());
        MapBoxTileSource tileSource = new MapBoxTileSource();

        mapView = (MyMapView) view.findViewById(R.id.mapview);
        mapView.setTileSource(tileSource);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(ac.res.getInteger(R.integer.map_initial_zoom));

        showPositionOverlay();

        MyMapListener mListener = new MyMapListener();
        mapView.setMapListener(new DelayedMapListener(mListener));

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.map, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch(item.getItemId()) {
            case R.id.action_location:
                if(myLocationOverlay.getMyLocation() != null) {
                    mapView.getController().setZoom(ac.res.getInteger(R.integer.map_initial_zoom));
                    mapView.getController().animateTo(myLocationOverlay.getMyLocation());
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // show current position
    private void showPositionOverlay() {
        myLocationOverlay = new UserLocationOverlay(getActivity(), mapView);
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.setDrawAccuracyEnabled(true);
        mapView.getOverlays().add(myLocationOverlay);
    }

    public interface OnHouseSelectedListener {
        public void onHouseSelected(String id, String lat, String lon);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if(activity instanceof OnHouseSelectedListener) {
            listener = (OnHouseSelectedListener) activity;
        }
    }
    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }


    // update shapes on map move/zoom
    public void updateShapesOverlay() {
        // minimum zoomlevel
        if(mapView.getZoomLevel() < ac.res.getInteger(R.integer.geoio_api_min_zoom)) {
            Toast toast = Toast.makeText(getActivity(), getString(R.string.error_zoom), Toast.LENGTH_LONG);
            toast.show();

            return;
        }

        if(!isAdded()) {
            return;
        }

        getActivity().setProgressBarIndeterminateVisibility(true);

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
                    // draw houses
                    JSONArray results = response.getJSONArray("results");

                    for(int i = 0; i < results.length(); i++) {
                        JSONObject result = results.getJSONObject(i);
                        JSONArray nodes = result.getJSONArray("nodes");

                        String id = result.getString("id");
                        if(drawnBuildings.contains(id)) {
                            return;
                        }
                        drawnBuildings.add(id);

                        HouseOverlay poly = new HouseOverlay(mapView, result, listener);

                        for(int ii = 0; ii < nodes.length(); ii++) {
                            double lat = nodes.getJSONObject(ii).getDouble("lat");
                            double lon = nodes.getJSONObject(ii).getDouble("lon");

                            poly.addPoint(new GeoPoint(lat, lon));
                        }

                        poly.setPoints();
                    }

                    getActivity().setProgressBarIndeterminateVisibility(false);

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
