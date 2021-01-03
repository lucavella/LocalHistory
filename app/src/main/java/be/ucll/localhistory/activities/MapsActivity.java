package be.ucll.localhistory.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
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

import java.util.List;
import java.util.Random;

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

        final SearchView searchView =
                (SearchView) menu.findItem(R.id.location_search).getActionView();
        searchView.setSuggestionsAdapter(suggestionsAdapter);
        searchView.setIconifiedByDefault(false);

        searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionSelect(int position) {
                return true;
            }

            @Override
            public boolean onSuggestionClick(int position) {
                searchView.clearFocus();
                Cursor selectedCursor = (Cursor) suggestionsAdapter.getItem(position);
                int dbIdColPos = selectedCursor.getColumnIndex("db_id");
                String dbId = selectedCursor.getString(dbIdColPos);
                showLocationById(dbId);
                return true;
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchView.clearFocus();

                final Intent searchIntent = new Intent(getApplicationContext(),
                        LocationSearchableActivity.class)
                        .setAction(Intent.ACTION_SEARCH)
                        .putExtra(SearchManager.QUERY, query);
                startActivityForResult(searchIntent, 1);
                return true;
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
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (Intent.ACTION_VIEW.equals(data.getAction())) {
                Uri intentData = data.getData();
                List<String> intentDataPath = intentData.getPathSegments();
                if ((intentDataPath.size() == 2) &&
                        (getString(R.string.db_location_txt).equals(intentDataPath.get(0)))) {
                    String dbId = intentDataPath.get(1);
                    showLocationById(dbId);
                }
            }
        }
    }

    private void showLocationById(final String dbId) {
        locationRef.child(dbId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        int id = 0;
                        LocationDb loc = dataSnapshot.getValue(LocationDb.class);
                        if (loc != null) {
                            MAP_FOLLOW_ME = false;

                            LatLng pos = loc.getPosition().ToLatLng();
                            addMarker(pos, loc.getName(), 240f, loc);
                            jumpToLocation(pos, 16.0f, true);
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
                addMarker(latLng, "Add new", 50f, null);
            }
        });
    }

    private void createMarkerPressListener() {
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                LocationDb loc = (LocationDb) marker.getTag();
                if (loc == null) {
                    LatLng pos = marker.getPosition();
                    loc = new LocationDb(pos, random(), random(), random(), random());
                    locationRef.push().setValue(loc);

                    marker.remove();
                    addMarker(pos, loc.getName(), 240f, loc);
                }

                return false;
            }
        });
    }
}