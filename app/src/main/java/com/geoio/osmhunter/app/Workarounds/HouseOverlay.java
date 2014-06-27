package com.geoio.osmhunter.app.Workarounds;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.view.MotionEvent;

import com.geoio.osmhunter.app.AttributeChangeActivity;
import com.geoio.osmhunter.app.R;

import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.bonuspack.overlays.Polygon;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;

import java.util.ArrayList;

public class HouseOverlay extends Polygon {
    private JSONObject house;
    private Context context;
    private Resources res;
    private MapView mapView;

    public ArrayList<GeoPoint> points = new ArrayList<GeoPoint>();

    public HouseOverlay(MapView mv, JSONObject h) {
        super(mv.getContext());

        house = h;
        context = mv.getContext();
        res = context.getResources();
        mapView = mv;

        this.setFillColor(res.getColor(R.color.map_building_background));
        this.setStrokeColor(res.getColor(R.color.map_building_border));
        this.setStrokeWidth(res.getInteger(R.integer.map_building_border_size));
        this.mOutlinePaint.setAntiAlias(true);
    }

    public void addPoint(GeoPoint point) {
        points.add(point);
    }

    public void setPoints() {
        super.setPoints(points);
        mapView.getOverlays().add(this);
        mapView.invalidate();
    }

    @Override
    public boolean onSingleTapConfirmed(final MotionEvent event, final MapView mapView) {
        boolean touched = contains(event, mapView);
        if (touched){
            Projection pj = mapView.getProjection();
            GeoPoint position = (GeoPoint)pj.fromPixels((int)event.getX(), (int)event.getY());
            try {
                JSONObject centroid = house.getJSONObject("centroid");
                Intent intent = new Intent(context, AttributeChangeActivity.class);

                intent.putExtra("id", house.getString("id"));
                intent.putExtra("lat", centroid.getString("lat"));
                intent.putExtra("lon", centroid.getString("lon"));

                context.startActivity(intent);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return touched;
    }
}