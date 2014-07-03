package com.geoio.osmhunter.app;

import android.app.ActionBar;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;

import com.geoio.osmhunter.app.Fragments.AttributeChangeFragment;
import com.geoio.osmhunter.app.Fragments.MapFragment;
import com.geoio.osmhunter.app.SyncAdapter.HunterActivity;
import com.geoio.osmhunter.app.Workarounds.HouseOverlay;
import com.geoio.osmhunter.app.Workarounds.MyMapView;

import org.osmdroid.util.GeoPoint;


public class MapActivity extends HunterActivity implements MapFragment.OnHouseSelectedListener, AttributeChangeFragment.OnAttributesSavedListener {
    private View attributeFragmentLayout;
    private AttributeChangeFragment attributeFragment;
    private MyMapView mapView;
    private HouseOverlay currentHouseOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_map);

        final ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        attributeFragmentLayout = (View) findViewById(R.id.editor_layout);
        attributeFragment = (AttributeChangeFragment) getSupportFragmentManager().findFragmentById(R.id.editor);
        mapView = (MyMapView) findViewById(R.id.mapview);

        // editor touch-hide-logic …
        if(attributeFragment != null && attributeFragment.isInLayout()) {
            mapView.setOnTouchListener(new View.OnTouchListener() {
                float tx = 0;
                float dx = 0;
                float offset = 0;

                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    if (currentHouseOverlay != null) {
                        switch(motionEvent.getAction()) {
                            case MotionEvent.ACTION_UP:
                                if(offset > tx/2) {
                                    mapView.locked = false;
                                    currentHouseOverlay.resetColors();
                                    currentHouseOverlay = null;
                                    attributeFragmentLayout.animate().translationX(res.getDimension(R.dimen.activity_map_editor_width)).start();
                                } else {
                                    attributeFragmentLayout.animate().translationX(0).start();
                                }
                                break;

                            case MotionEvent.ACTION_DOWN:
                                mapView.locked = true;
                                tx = attributeFragmentLayout.getTranslationX();
                                dx = motionEvent.getX();
                                break;

                            case MotionEvent.ACTION_MOVE:
                                offset = (motionEvent.getX() - dx) - tx;
                                if(offset >= tx) {
                                    attributeFragmentLayout.setTranslationX(offset);
                                }
                                break;
                        }
                    }
                    return false;
                }
            });
        }
    }

    @Override
    public void onHouseSelected(String id, String lat, String lon, HouseOverlay overlay) {
        currentHouseOverlay = overlay;

        if(attributeFragment != null && attributeFragment.isInLayout()) {
            currentHouseOverlay.highlight();

            // move the map to prevent overlaying the building
            GeoPoint pos = new GeoPoint(Double.parseDouble(lat), Double.parseDouble(lon));
            Point posPixels = mapView.getProjection().toPixels(pos, null);
            float mapWidth = mapView.getWidth() - attributeFragmentLayout.getWidth();
            float newX = mapWidth/4 + posPixels.x;
            mapView.getController().animateTo(mapView.getProjection().fromPixels(Math.round(newX), posPixels.y));

            attributeFragment.setBuilding(id, lat, lon);
            attributeFragmentLayout.animate().translationX(0).start();
        } else {
            Intent intent = new Intent(this, AttributeChangeActivity.class);
            intent.putExtra("id", id);
            intent.putExtra("lat", lat);
            intent.putExtra("lon", lon);
            startActivity(intent);
        }
    }

    @Override
    public void onAttributesSaved() {
        currentHouseOverlay.resetColors();
        attributeFragmentLayout.animate().translationX(res.getDimension(R.dimen.activity_map_editor_width)).start();
    }
}
