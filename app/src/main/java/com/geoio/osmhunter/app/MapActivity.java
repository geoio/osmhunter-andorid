package com.geoio.osmhunter.app;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;

import com.geoio.osmhunter.app.Fragments.AttributeChangeFragment;
import com.geoio.osmhunter.app.Fragments.MapFragment;
import com.geoio.osmhunter.app.SyncAdapter.HunterActivity;


public class MapActivity extends HunterActivity implements MapFragment.OnHouseSelectedListener {
    private AttributeChangeFragment attributeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_map);

        final ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        attributeFragment = (AttributeChangeFragment) getSupportFragmentManager().findFragmentById(R.id.editor);
    }

    @Override
    public void onHouseSelected(String id, String lat, String lon) {
        Log.v("foo", "yaa");
        if(attributeFragment != null && attributeFragment.isInLayout()) {
            Log.v("foo", "this");
            attributeFragment.setBuilding(id, lat, lon);
        } else {
            Log.v("foo", "that");
            Intent intent = new Intent(this, AttributeChangeActivity.class);
            intent.putExtra("id", id);
            intent.putExtra("lat", lat);
            intent.putExtra("lon", lon);
            startActivity(intent);
        }
    }
}
