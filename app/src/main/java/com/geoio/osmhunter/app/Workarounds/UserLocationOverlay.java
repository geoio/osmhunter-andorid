package com.geoio.osmhunter.app.Workarounds;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

// compiling library projects is horrible, so here's a simple fix for runOnFirstFix (Bug #1)
public class UserLocationOverlay extends MyLocationNewOverlay {

    private boolean noFix = true;
    private MapView mapView;

    public UserLocationOverlay(Context context, MapView mv) {
        super(context, mv);
        mapView = mv;

        // fast fix: get the last known location
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        Location lastKnownLocation = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if(lastKnownLocation != null) {
            mapView.getController().setCenter(new GeoPoint(lastKnownLocation));
        }
    }

    @Override
    public void onLocationChanged(Location location, IMyLocationProvider source) {
        super.onLocationChanged(location, source);

        if(location != null && noFix) {
            mapView.getController().animateTo(new GeoPoint(location));
            noFix = false;
        }
    }
}