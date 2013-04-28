package com.github.matt.williams.argolf;

import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

public class RoundActivity extends Activity implements LocationListener {

    public class Hole {
        public final LatLng tee;
        public final LatLng hole;
        public final int par;

        public Hole(LatLng tee, LatLng hole, int par) {
            this.tee = tee;
            this.hole = hole;
            this.par = par;
        }
    }

    private final List<Hole> HOLES = Arrays.asList(new Hole[] {
            new Hole(new LatLng(51.520377, -0.086249), // Bloomberg, Finsbury Square
                     new LatLng(51.523544, -0.088271), // Bunhill Fields Burial Grounds
                     3),
            new Hole(new LatLng(51.523765, -0.089757), // North-East Bunhill Fields Burial Grounds
                     new LatLng(51.525527, -0.091174), // South Bath Street
                     3),
            new Hole(new LatLng(51.526208, -0.093137), // Radnor Street Open Space
                     new LatLng(51.527369, -0.09863), // Kings Square Garden
                     4)
    });

    protected static final int REQUEST_CODE_SWING = 1;
    private MapView mMapView;
    private Location mNextLocation;
    private boolean mPendingNextLocation;
    private final Handler mHandler = new Handler();

    private LocationManager mLocationManager;

    private Location mMyLocation;

    private String[] mPlayers;
    private Marker[] mPlayerMarkers;
    private static final Criteria CRITERIA = new Criteria();
    static {
        CRITERIA.setAccuracy(Criteria.ACCURACY_COARSE);
    }
    private static final long MIN_UPDATE_TIME = 10000;
    private static final float MIN_UPDATE_DISTANCE = 10.0f;
    private static final float CLOSE_ENOUGH_DISTANCE = 25.0f;

    public static final String EXTRA_PLAYERS = "PLAYERS";
    private int mHoleIndex = -1;

    private int mNextPlayerIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_round);
        mMapView = (MapView)findViewById(R.id.map);
        mMapView.onCreate(savedInstanceState);

        mPlayers = getIntent().getStringArrayExtra(EXTRA_PLAYERS);
        mPlayerMarkers = new Marker[mPlayers.length];

        final GoogleMap map = mMapView.getMap();
        map.setMyLocationEnabled(true);
        try {
            MapsInitializer.initialize(this);
            final LatLngBounds.Builder bounds = new LatLngBounds.Builder();
            for (int holeIdx = 0; holeIdx < HOLES.size(); holeIdx++) {
                Hole hole = HOLES.get(holeIdx);
                map.addPolyline(new PolylineOptions().add(hole.tee, hole.hole).width(5).color(0x7f0000ff));
                //map.addMarker(new MarkerOptions().position(hole.tee).title("Tee " + (holeIdx + 1)).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                //map.addMarker(new MarkerOptions().position(hole.hole).title("Hole " + (holeIdx + 1)).snippet("Par " + hole.par).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                bounds.include(hole.tee).include(hole.hole);
            }
            for (int playerIndex = 0; playerIndex < mPlayers.length; playerIndex++) {
                mPlayerMarkers[playerIndex] = map.addMarker(new MarkerOptions().position(HOLES.get(0).tee).title(mPlayers[playerIndex]));
            }
            map.setOnCameraChangeListener(new OnCameraChangeListener() {
                @Override
                public void onCameraChange(CameraPosition position) {
                    // Move camera.
                    map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 32));
                    // Remove listener to prevent position reset on camera move.
                    map.setOnCameraChangeListener(null);
                }
            });
        } catch (GooglePlayServicesNotAvailableException e) {
            android.util.Log.e("RoundActivity", "Caught exception", e);
        }

        mLocationManager = (LocationManager)getSystemService(Activity.LOCATION_SERVICE);
        String providerName = mLocationManager.getBestProvider(CRITERIA, true);
        if (providerName == null) {
            providerName = LocationManager.NETWORK_PROVIDER;
        }
        Location location = mLocationManager.getLastKnownLocation(providerName);
        if (location != null) {
            onLocationChanged(location);
        }
        mLocationManager.requestLocationUpdates(providerName, MIN_UPDATE_TIME, MIN_UPDATE_DISTANCE, this, Looper.getMainLooper());

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                targetNextLocation();
            }
        }, 2000);
    }

    private void setPlayersAtNextTee() {
        mHoleIndex++;
        for (int playerIndex = 0; playerIndex < mPlayerMarkers.length; playerIndex++) {
            mPlayerMarkers[playerIndex].setPosition(HOLES.get(mHoleIndex).tee);
        }
    }

    private void targetNextLocation() {
        if (mMyLocation != null) {
            GoogleMap map = mMapView.getMap();
            if (mNextLocation == null) {
                setPlayersAtNextTee();
                mNextPlayerIndex = getNextPlayer();
                targetPlayer(mNextPlayerIndex);
            }
            if (mMyLocation.distanceTo(mNextLocation) < CLOSE_ENOUGH_DISTANCE) {
                targetHole();
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(RoundActivity.this, SwingActivity.class);
                        intent.putExtra(SwingActivity.EXTRA_TITLE, mPlayers[mNextPlayerIndex] + "'s Swing");
                        startActivityForResult(intent, REQUEST_CODE_SWING);
                    }
                }, 2000);
            }
            map.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder(map.getCameraPosition()).target(new LatLng(mMyLocation.getLatitude(), mMyLocation.getLongitude())).bearing(mMyLocation.bearingTo(mNextLocation)).build()));
        } else {
            mPendingNextLocation = true;
        }
    }

    private void targetHole() {
        mNextLocation = latLngToLocation(HOLES.get(mHoleIndex).hole);
    }

    private int getNextPlayer() {
        Location holeLocation = latLngToLocation(HOLES.get(mHoleIndex).hole);
        double furthestDistance = 0.0;
        int furthestPlayerIndex = -1;
        for (int playerIndex = 0; playerIndex < mPlayerMarkers.length; playerIndex++) {
            Location playerLocation = latLngToLocation(mPlayerMarkers[playerIndex].getPosition());
            double distance = playerLocation.distanceTo(holeLocation);
            if (distance > furthestDistance) {
                furthestDistance = distance;
                furthestPlayerIndex = playerIndex;
            }
        }
        if (furthestDistance < CLOSE_ENOUGH_DISTANCE) {
            furthestPlayerIndex = -1;
        }
        return furthestPlayerIndex;
    }

    private void targetPlayer(int playerIndex) {
        mNextLocation = latLngToLocation(mPlayerMarkers[playerIndex].getPosition());
    }

    private Location latLngToLocation(LatLng latLng) {
        Location location = new Location(mMyLocation);
        location.setLatitude(latLng.latitude);
        location.setLongitude(latLng.longitude);
        return location;
    }

    @Override
    protected void onDestroy() {
        mMapView.onDestroy();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        mMapView.onPause();
        super.onPause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if ((requestCode == REQUEST_CODE_SWING) &&
            (resultCode == RESULT_OK)) {
            float speed = intent.getFloatExtra(SwingActivity.EXTRA_SPEED, 0.0f);
            float distance = 260.0f / 145.0f * 2.23f * speed;
            Marker marker = mPlayerMarkers[mNextPlayerIndex];
            float bearing = mMapView.getMap().getCameraPosition().bearing;
            LatLng oldPosition = marker.getPosition();
            LatLng newPosition = new LatLng(oldPosition.latitude + distance * Math.cos(bearing / 180 * (float)Math.PI) / 111131.745, oldPosition.longitude + distance * Math.sin(bearing / 180 * (float)Math.PI) / 78846.80572069259);
            marker.setPosition(newPosition);
        }

        mNextPlayerIndex = getNextPlayer();
        if (mNextPlayerIndex == -1) {
            setPlayersAtNextTee();
            mNextPlayerIndex = getNextPlayer();
            targetPlayer(mNextPlayerIndex);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        mMyLocation = location;
        if (mPendingNextLocation) {
            mPendingNextLocation = false;
            targetNextLocation();
        } else if ((mNextLocation != null) &&
                   (mMyLocation.distanceTo(mNextLocation) < CLOSE_ENOUGH_DISTANCE)) {
            targetNextLocation();
        }

    }

    @Override
    public void onProviderDisabled(String provider) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onProviderEnabled(String provider) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onStatusChanged(String provider, int arg1, Bundle arg2) {
        // TODO Auto-generated method stub

    }
}
