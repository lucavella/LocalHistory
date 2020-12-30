package be.ucll.localhistory.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.Context;
import android.database.Cursor;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Random;

import be.ucll.localhistory.R;
import be.ucll.localhistory.helpers.LocationSearchSuggestionsAdapter;
import be.ucll.localhistory.helpers.PermissionUtils;
import be.ucll.localhistory.helpers.PermissionUtils.PermissionStatus;
import be.ucll.localhistory.models.LocationDb;


public class MapsActivity extends AppCompatActivity
        implements
        OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static boolean MAP_FOLLOW_ME = true;

    private GoogleMap mMap;
    private LocationManager locationManager;

    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference locationRef = database.getReference("locations");


    @Override
    @SuppressWarnings("ConstantConditions")
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
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.activity_maps_menu, menu);

        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView =
                (SearchView) menu.findItem(R.id.location_search).getActionView();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        final LocationSearchSuggestionsAdapter suggestionsAdapter = new LocationSearchSuggestionsAdapter(
                this,
                R.layout.location_search_suggestion_item
        );

        final SearchView searchView =
                (SearchView) menu.findItem(R.id.location_search).getActionView();
        searchView.setSuggestionsAdapter(suggestionsAdapter);
        searchView.setIconifiedByDefault(false);

        searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionSelect(int position) {
                Cursor selectedCursor = (Cursor) suggestionsAdapter.getItem(position);
                String selectedTxt = selectedCursor.getString(selectedCursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1));
                searchView.setQuery(selectedTxt, true);
                return true;
            }

            @Override
            public boolean onSuggestionClick(int position) {
                return true;
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                suggestionsAdapter.UpdateAdapterCursorByQuery(newText);
                return false;
            }
        });

        return true;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        enableLocation(false);
        createMyLocationButtonListener();
        createMyLocationChangedListener();
        createMoveCameraStopFollowMeListener();
        createLongPressListener();

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

    private void jumpToLocation(Location location, float zoom, boolean animated) {
        LatLng latlng = new LatLng(location.getLatitude(), location.getLongitude());
        if (!animated) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, zoom));
        } else {
            CameraPosition.Builder positionBuilder = new CameraPosition.Builder();
            positionBuilder.target(latlng);
            positionBuilder.zoom(zoom);
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(positionBuilder.build()));
        }
    }

    @SuppressLint("MissingPermission")
    private void jumpToMyLocation() {
        if (locationManager != null) {
            String bestProvider = locationManager.getBestProvider(new Criteria(), false);
            if (bestProvider == null) return;
            Location myLocation = locationManager.getLastKnownLocation(bestProvider);
            if (myLocation == null) return;
            MAP_FOLLOW_ME = true;
            jumpToLocation(myLocation, 16.0f, true);
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
        locationManager.requestLocationUpdates(bestProvider, 50, 1, new LocationListener() {
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

    private void createMoveCameraStopFollowMeListener() {
        mMap.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener() {
            @Override
            public void onCameraMoveStarted(int reason) {
                if (reason == REASON_GESTURE) MAP_FOLLOW_ME = false;
            }
        });
    }

    public static String random() {
        Random generator = new Random();
        StringBuilder randomStringBuilder = new StringBuilder();
        int randomLength = 5 + generator.nextInt(10);
        char tempChar;
        for (int i = 0; i < randomLength; i++){
            tempChar = (char) (generator.nextInt(30) + 97);
            if ((int) tempChar > 97 + 26) tempChar = ' ';
            randomStringBuilder.append(tempChar);
        }
        return randomStringBuilder.toString();
    }

    private void createLongPressListener() {
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                LocationDb loc = new LocationDb(latLng, random(), random(), random(), random());
                locationRef.push().setValue(loc);
            }
        });
    }
}