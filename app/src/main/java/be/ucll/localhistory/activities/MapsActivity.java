package be.ucll.localhistory.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import be.ucll.localhistory.R;
import be.ucll.localhistory.helpers.LocationSearchAdapter;
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

        final LocationSearchAdapter suggestionsAdapter = new LocationSearchAdapter(
                this,
                R.layout.location_search_suggestion_item
        );

        final MenuItem searchMenuItem =
                menu.findItem(R.id.location_search);
        final SearchView searchView =
                (SearchView) searchMenuItem.getActionView();
        searchView.setSuggestionsAdapter(suggestionsAdapter);
        searchView.setIconifiedByDefault(false);
        searchView.setFocusable(true);

        searchMenuItem.getIcon().setTint(Color.WHITE);
        searchMenuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        searchView.requestFocus();
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (imm != null) { // it's never null. I've added this line just to make the compiler happy
                            imm.showSoftInput(searchView.findFocus(), 0);
                        }
                    }
                });
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                return true;
            }
        });

        searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionSelect(int position) {
                return true;
            }

            @Override
            public boolean onSuggestionClick(int position) {
                searchMenuItem.collapseActionView();

                String locationId = suggestionsAdapter.getLocationKeyAtPosition(position);

                locationRef.child(locationId)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                LocationDb location = dataSnapshot.getValue(LocationDb.class);
                                if (location != null) {
                                    location.setKey(dataSnapshot.getKey());
                                    showLocation(location);
                                } else {
                                    Toast.makeText(MapsActivity.this, R.string.location_not_found, Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                Toast.makeText(MapsActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
                                Log.e("error", databaseError.getMessage());
                            }
                        });
                return false;
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchMenuItem.collapseActionView();

                final Intent searchIntent = new Intent(getApplicationContext(),
                        LocationSearchableActivity.class)
                        .setAction(Intent.ACTION_SEARCH)
                        .putExtra(SearchManager.QUERY, query);
                startActivityForResult(searchIntent, 1);

                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                suggestionsAdapter.updateAdapterCursorByQuery(newText);
                return false;
            }
        });

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (Intent.ACTION_VIEW.equals(data.getAction())) {
                LocationDb location = (LocationDb) data.getSerializableExtra(
                        getString(R.string.location_txt)
                );

                showLocation(location);
            }
        }
    }

    private void showLocation(LocationDb location) {
        MAP_FOLLOW_ME = false;

        LatLng pos = location.getPosition().ToLatLng();
        addMarker(pos, location.getName(), 240f, location);
        jumpToLocation(pos, 16.0f, true);
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
        createMarkerPressListener();
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

    private void addMarker(LatLng location, String title, float hue, Object data) {
        mMap.clear();

        MarkerOptions markerOpt = new MarkerOptions()
                .position(location)
                .title(title)
                .icon(BitmapDescriptorFactory.defaultMarker(hue));

        Marker marker = mMap.addMarker(markerOpt);
        marker.setTag(data);
        marker.showInfoWindow();
    }

    private void jumpToLocation(LatLng location, float zoom, boolean animated) {
        if (!animated) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, zoom));
        } else {
            CameraPosition.Builder positionBuilder = new CameraPosition.Builder();
            positionBuilder.target(location);
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
            LatLng latlng = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
            jumpToLocation(latlng, 16.0f, true);
        } else {
            enableLocation(false);
        }
    }

    private void createMyLocationButtonListener() {
        FloatingActionButton myLocationButton = findViewById(R.id.my_location_button);
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

    private void createLongPressListener() {
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                addMarker(latLng, "Add new", 50f, null);
            }
        });
    }

    private void createMarkerPressListener() {
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                LocationDb location = (LocationDb) marker.getTag();
                if (location == null) {
                    LatLng pos = marker.getPosition();

                    try {
                        Geocoder geocoder = new Geocoder(MapsActivity.this, Locale.ENGLISH);
                        List<Address> addresses = geocoder.getFromLocation(pos.latitude, pos.longitude, 1); //null or empty on no match
                        if (addresses.size() > 0) {
                            Address address = addresses.get(0);

                            if (address != null) {
                                location = new LocationDb(pos, address.getLocality(), address.getCountryName());
                                Intent addIntent = new Intent(getApplicationContext(),
                                        LocationUpsertActivity.class)
                                        .setAction(Intent.ACTION_INSERT)
                                        .putExtra(getString(R.string.location_txt), location);

                                startActivityForResult(addIntent, 1);
                                marker.remove();

                                return true;
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        Log.d("geocoder error", ex.getMessage());
                    }

                    marker.remove();
                    Toast.makeText(MapsActivity.this, R.string.location_add_failed, Toast.LENGTH_SHORT).show();
                } else {
                    Intent infoIntent = new Intent(getApplicationContext(),
                            LocationInfoActivity.class)
                            .setAction(Intent.ACTION_VIEW)
                            .putExtra(getString(R.string.location_txt), location);

                    startActivityForResult(infoIntent, 1);
                    marker.remove();
                }

                return true;
            }
        });
    }
}