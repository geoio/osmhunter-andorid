package com.geoio.osmhunter.app;

import android.app.ActionBar;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Window;

import com.geoio.osmhunter.app.Fragments.AttributeChangeFragment;
import com.geoio.osmhunter.app.SyncAdapter.HunterActivity;


public class AttributeChangeActivity extends HunterActivity {
    private AttributeChangeFragment attributeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // stop the activity if the user rotates the screen of his tablet
        int screenLayout = getResources().getConfiguration().screenLayout;
        screenLayout &= Configuration.SCREENLAYOUT_SIZE_MASK;
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE && screenLayout == Configuration.SCREENLAYOUT_SIZE_LARGE) {
            finish();
            return;
        }

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_attribute_change);

        final ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();

        attributeFragment = (AttributeChangeFragment) getSupportFragmentManager().findFragmentById(R.id.editor);
        attributeFragment.setBuilding(intent.getStringExtra("id"), intent.getStringExtra("lat"), intent.getStringExtra("lon"));
        attributeFragment.onlyFragment = true;
    }

}
