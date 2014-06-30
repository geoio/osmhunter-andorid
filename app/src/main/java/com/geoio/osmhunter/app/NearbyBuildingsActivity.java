package com.geoio.osmhunter.app;

import android.app.ActionBar;
import android.os.Bundle;
import android.view.Window;

import com.geoio.osmhunter.app.Fragments.AttributeChangeFragment;
import com.geoio.osmhunter.app.Fragments.MapFragment;
import com.geoio.osmhunter.app.SyncAdapter.HunterActivity;


public class NearbyBuildingsActivity extends HunterActivity implements MapFragment.OnHouseSelectedListener {
    private AttributeChangeFragment attributeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_nearby_buildings);

        final ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        attributeFragment = (AttributeChangeFragment) getSupportFragmentManager().findFragmentById(R.id.editor);
    }

    @Override
    public void onHouseSelected(String id, String lat, String lon) {
        if(attributeFragment != null && attributeFragment.isInLayout()) {
            attributeFragment.setBuilding(id, lat, lon);
        }
    }

}
