package com.geoio.osmhunter.app;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.EditText;

import com.geoio.osmhunter.app.SyncAdapter.HunterActivity;
import com.geoio.osmhunter.app.Workarounds.HouseOverlay;
import com.geoio.osmhunter.app.Workarounds.MapBoxTileSource;
import com.geoio.osmhunter.app.Workarounds.MyMapView;
import com.geoio.osmhunter.app.Workarounds.UserLocationOverlay;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;


public class AddressActivity extends HunterActivity implements SensorEventListener {

    public static final int MODE_FIXED = 0;
    public static final int MODE_NODE  = 1;
    public static final int MODE_FREE  = 2;

    private MyMapView mapView;
    public UserLocationOverlay myLocationOverlay;
    private View view;
    private SensorManager mSensorManager;
    private Sensor mAccelerometerSensor;
    private Sensor mMagneticSensor;
    private float[] mGravity;
    private float[] mGeomagnetic;
    private Float azimut;


    private GeoPoint location = new GeoPoint(53.56487535,9.97735779693333);
    private int mode = MODE_FREE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address);
        view = this.getCurrentFocus();

        showMap();
        showPositionOverlay();

        switch(mode) {
            case MODE_FREE:
                initCompass();
                initFreeMode();
                return;

            case MODE_NODE:
                initCompass();
                initNodeMode();
                return;
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.address, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    // http://www.codingforandroid.com/2011/01/using-orientation-sensors-simple.html
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            mGravity = sensorEvent.values.clone();
        if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            mGeomagnetic = sensorEvent.values.clone();

        if (mGravity != null && mGeomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
            if (success) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);
                azimut = orientation[0]; // orientation contains: azimut, pitch and roll
                Log.v("foo", String.valueOf(azimut));
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private void showMap() {
        MapBoxTileSource.retrieveMapBoxMapId(this);
        MapBoxTileSource tileSource = new MapBoxTileSource();

        mapView = (MyMapView) this.findViewById(R.id.mapview);
        mapView.setTileSource(tileSource);
        mapView.getController().setZoom(19);

        // show the house
        HouseOverlay poly = new HouseOverlay(mapView, null);
        poly.addPoint(new GeoPoint(53.5649016, 9.9775104));
        poly.addPoint(new GeoPoint(53.5647895, 9.9774303));
        poly.addPoint(new GeoPoint(53.5648535, 9.9772077));
        poly.addPoint(new GeoPoint(53.5649612, 9.9772878));
        poly.setPoints();

        // set the center correctly
        ViewTreeObserver vto = view.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mapView.scrolling = true;
                mapView.getController().setCenter(location);
                mapView.scrolling = false;
            }
        });
    }

    private void showPositionOverlay() {
        myLocationOverlay = new UserLocationOverlay(this, mapView);
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.setDrawAccuracyEnabled(true);
        mapView.getOverlays().add(myLocationOverlay);
    }

    private void initCompass() {
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagneticSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    private void initFreeMode() {
        // todo: http://www.movable-type.co.uk/scripts/latlong.html#destPoint
    }

    private void initNodeMode() {
        // todo: http://www.ina-de-brabandt.de/analysis/lin/gerade2d-orthogonal.html
    }
}
