package com.geoio.osmhunter.app.Workarounds;

import android.content.Context;
import android.location.Location;

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
    }

    @Override
    public void onLocationChanged(Location location, IMyLocationProvider source) {
        super.onLocationChanged(location, source);

        if(location != null && noFix) {
            mapView.getController().setCenter(new GeoPoint(location));
            noFix = false;
        }
    }
}