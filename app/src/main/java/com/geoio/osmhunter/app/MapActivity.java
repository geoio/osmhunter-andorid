package com.geoio.osmhunter.app;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;

import com.geoio.osmhunter.app.Fragments.AttributeChangeFragment;
import com.geoio.osmhunter.app.Fragments.MapFragment;
import com.geoio.osmhunter.app.SyncAdapter.HunterActivity;
import com.geoio.osmhunter.app.Workarounds.MyMapView;


public class MapActivity extends HunterActivity implements MapFragment.OnHouseSelectedListener, AttributeChangeFragment.OnAttributesSavedListener {
    private View attributeFragmentLayout;
    private AttributeChangeFragment attributeFragment;
    private MyMapView mapView;

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

        // hide the editor if the tablet user taps on the map
        if(attributeFragment != null && attributeFragment.isInLayout()) {
            mapView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    attributeFragmentLayout.animate().translationX(res.getDimension(R.dimen.activity_map_editor_width)).start();
                    return false;
                }
            });
        }
    }

    @Override
    public void onHouseSelected(String id, String lat, String lon) {
        if(attributeFragment != null && attributeFragment.isInLayout()) {
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
        attributeFragmentLayout.animate().translationX(res.getDimension(R.dimen.activity_map_editor_width)).start();
    }
}
