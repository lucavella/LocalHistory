package be.ucll.localhistory;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.security.Provider;

import be.ucll.localhistory.PermissionUtils.PermissionStatus;

public class MapsActivity extends AppCompatActivity
        implements
        OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static boolean MAP_FOLLOW_ME = true;

    private GoogleMap mMap;
    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // get SupportMapFragment and execute onMapReady when the map is ready to be used.
        // supportMapFragment is not for production
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_map, menu);
        return true;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        enableLocation(false);
        createMyLocationButtonListener();
        createMyLocationChangedListener();

        if (locationManager == null) {
            LatLng genk = new LatLng(50.96667, 5.5);
            mMap.addMarker(new MarkerOptions().position(genk).title("Marker in Genk"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(genk, 16.0f));
        }
    }

    @SuppressLint("MissingPermission")
    private void enableLocation(boolean overrideRationale) {
        if (PermissionUtils.getPermissionStatus(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PermissionStatus.GRANTED) {
            if (locationManager == null) locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            if (mMap != null) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMapToolbarEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
            }
        } else {
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, overrideRationale);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            switch (PermissionUtils.getPermissionStatus(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                case GRANTED:
                    Toast.makeText(this, R.string.location_permission_granted, Toast.LENGTH_SHORT).show();
                    enableLocation(false);
                    jumpToMyLocation();
                    break;
                case DENIED:
                    Toast.makeText(this, R.string.location_permission_declined, Toast.LENGTH_SHORT).show();
                    break;
                case DONT_ASK_OR_NEVER_ASKED:
                    // never asked in this case
                    Toast.makeText(this, R.string.location_permission_never, Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    }

    private void jumpToLocation(Location location, float zoom) {
        LatLng latlng = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, zoom));
    }

    @SuppressLint("MissingPermission")
    private void jumpToMyLocation() {
        if (locationManager != null) {
            String bestProvider = locationManager.getBestProvider(new Criteria(), false);
            if (bestProvider == null) return;
            Location myLocation = locationManager.getLastKnownLocation(bestProvider);
            if (myLocation == null) return;
            jumpToLocation(myLocation, 16.0f);
        } else {
            enableLocation(false);
        }
    }

    private void createMyLocationButtonListener() {
        FloatingActionButton myLocationButton = findViewById(R.id.myLocationButton);
        myLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PermissionUtils.getPermissionStatus(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PermissionStatus.GRANTED) {
                    jumpToMyLocation();
                } else {
                    enableLocation(true);
                }
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void createMyLocationChangedListener() {
        String bestProvider = locationManager.getBestProvider(new Criteria(), false);
        if (bestProvider == null) return;
        locationManager.requestLocationUpdates(bestProvider, 100, 1, new LocationListener() {
                    @Override
                    public void onLocationChanged(@NonNull Location location) {
                        if ((MAP_FOLLOW_ME) && (PermissionUtils.getPermissionStatus(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                                == PermissionStatus.GRANTED)) {
                            jumpToMyLocation();
                        }
                    }
                }

        );
    }
}