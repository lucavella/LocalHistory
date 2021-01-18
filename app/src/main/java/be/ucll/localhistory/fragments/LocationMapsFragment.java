package be.ucll.localhistory.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQueryBounds;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import be.ucll.localhistory.R;
import be.ucll.localhistory.activities.LocationInfoActivity;
import be.ucll.localhistory.activities.LocationUpsertActivity;
import be.ucll.localhistory.helpers.PermissionStatus;
import be.ucll.localhistory.helpers.PermissionUtils;
import be.ucll.localhistory.models.LocationDb;

public class LocationMapsFragment extends Fragment
        implements
        OnMapReadyCallback,
        GoogleMap.OnCameraMoveStartedListener,
        GoogleMap.OnMapLongClickListener,
        GoogleMap.OnMarkerDragListener,
        GoogleMap.OnMarkerClickListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static boolean MAP_FOLLOW_ME = true;

    private final FirebaseDatabase database = FirebaseDatabase.getInstance();
    private final DatabaseReference locationRef = database.getReference("locations");

    private final List<Marker> nearbyMarkers = new ArrayList<>();
    private final double nearbyRadiusMeters = 200;

    private GoogleMap mMap;
    private Marker targetMarker;
    private LocationManager locationManager;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_location_maps, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();

        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.fragment_location_maps);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        enableMyLocation(false);
        enableMyLocationButton();

        mMap.setOnCameraMoveStartedListener(this);
        mMap.setOnMapLongClickListener(this);
        mMap.setOnMarkerDragListener(this);
        mMap.setOnMarkerClickListener(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            switch (PermissionUtils.getPermissionStatus(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)) {
                case GRANTED:
                    Toast.makeText(getActivity(), R.string.location_permission_granted, Toast.LENGTH_SHORT).show();
                    enableMyLocation(false);
                    jumpToMyLocation();
                    break;
                case DENIED:
                    Toast.makeText(getActivity(), R.string.location_permission_declined, Toast.LENGTH_SHORT).show();
                    break;
                case DONT_ASK_OR_NEVER_ASKED:
                    // don't ask in this case
                    Toast.makeText(getActivity(), R.string.location_permission_never, Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    }

    private LocationListener locationUpdatesHandler = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            if (MAP_FOLLOW_ME) {
                jumpToMyLocation();
            }

            final GeoLocation myGeoLocation = new GeoLocation(location.getLatitude(), location.getLongitude());
            List<GeoQueryBounds> bounds = GeoFireUtils.getGeoHashQueryBounds(myGeoLocation, nearbyRadiusMeters);

            final List<Task<DataSnapshot>> tasks = new ArrayList<>();
            for (GeoQueryBounds b : bounds) {
                Query query = locationRef
                        .orderByChild("geoHash")
                        .startAt(b.startHash)
                        .endAt(b.endHash);
                tasks.add(query.get());
            }

            Tasks.whenAllComplete(tasks)
                    .addOnCompleteListener(new OnCompleteListener<List<Task<?>>>() {
                        @Override
                        public void onComplete(@NonNull Task<List<Task<?>>> t) {
                            boolean isSuccessful = true;

                            for (Task<DataSnapshot> task : tasks) {
                                isSuccessful &= task.isSuccessful();
                            }

                            if (isSuccessful) {
                                for (Marker marker : nearbyMarkers) {
                                    marker.remove();
                                }
                                nearbyMarkers.clear();

                                for (Task<DataSnapshot> task : tasks) {
                                    DataSnapshot snapshot = task.getResult();
                                    if (snapshot != null) {
                                        for (DataSnapshot ds : snapshot.getChildren()) {
                                            LocationDb location = ds.getValue(LocationDb.class);
                                            if (location == null) continue;
                                            location.setKey(ds.getKey());

                                            // filter false positives
                                            GeoLocation geoLocation = new GeoLocation(location.getPosition().getLatitude(),
                                                    location.getPosition().getLongitude());
                                            double distanceInM = GeoFireUtils.getDistanceBetween(geoLocation, myGeoLocation);

                                            if (distanceInM <= nearbyRadiusMeters) {
                                                Marker marker = addMarker(location.getPosition().ToLatLng(),
                                                        "", 215f, location, false);

                                                nearbyMarkers.add(marker);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    });
        }
    };

    @Override
    public void onCameraMoveStarted(int reason) {
        if (reason == REASON_GESTURE) MAP_FOLLOW_ME = false;
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        MAP_FOLLOW_ME = false;

        removeLocation();
        targetMarker = addMarker(latLng, "Add new", 40f, null, true);

        jumpToLocation(latLng, 0.0f, true);
    }

    @Override
    public void onMarkerDragStart(Marker marker) { }

    @Override
    public void onMarkerDrag(Marker marker) { }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        LatLng position = marker.getPosition();
        targetMarker.setPosition(position);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if ((targetMarker != null) && !targetMarker.equals(marker)) {
            targetMarker.remove();
        }

        LocationDb location = (LocationDb) marker.getTag();
        if (location == null) {
            LatLng pos = marker.getPosition();
            targetMarker.showInfoWindow();

            try {
                Geocoder geocoder = new Geocoder(getActivity(), Locale.ENGLISH);
                List<Address> addresses = geocoder.getFromLocation(pos.latitude, pos.longitude, 1); //null or empty on no match
                if (addresses.size() > 0) {
                    Address address = addresses.get(0);

                    if (address != null) {
                        location = new LocationDb(pos, address.getLocality(), address.getCountryName());
                        Intent addIntent = new Intent(getActivity(),
                                LocationUpsertActivity.class)
                                .setAction(Intent.ACTION_INSERT)
                                .putExtra(getString(R.string.location_txt), location);

                        startActivityForResult(addIntent, 1);

                        jumpToLocation(marker.getPosition(), 0f, true);
                        return false;
                    }
                }
            }
            catch (IOException ex)
            {
                Log.d("geocoder error", ex.getMessage());
            }

            Toast.makeText(getActivity(), R.string.location_add_failed, Toast.LENGTH_SHORT).show();
        } else {
            if ((targetMarker != null) && targetMarker.equals(marker)) {
                targetMarker.showInfoWindow();
            }

            Intent infoIntent = new Intent(getActivity(),
                    LocationInfoActivity.class)
                    .setAction(Intent.ACTION_GET_CONTENT)
                    .putExtra(getString(R.string.location_txt), location);

            startActivityForResult(infoIntent, 1);
        }
        if ((targetMarker != null) && targetMarker.equals(marker)) {
            jumpToLocation(marker.getPosition(), 0f, true);
        }
        return false;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onStop() {
        super.onStop();

        if (locationManager != null) {
            locationManager.removeUpdates(locationUpdatesHandler);
        }

        if (mMap != null) {
            mMap.setMyLocationEnabled(false);

            mMap.setOnCameraMoveStartedListener(null);
            mMap.setOnMapLongClickListener(null);
            mMap.setOnMarkerDragListener(null);
            mMap.setOnMarkerClickListener(null);
        }
    }

    @SuppressLint("MissingPermission")
    private void enableMyLocation(boolean overrideRationale) {
        final Activity activity = getActivity();
        if (activity == null) return;

        if (PermissionUtils.getPermissionStatus(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                == PermissionStatus.GRANTED) {
            if (locationManager == null) locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);

            if (mMap != null) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMapToolbarEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
            }

            String bestProvider = locationManager.getBestProvider(new Criteria(), false);
            if (bestProvider == null) return;
            locationManager.requestLocationUpdates(2_000, 1, new Criteria(), locationUpdatesHandler, Looper.getMainLooper());

        } else {
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, overrideRationale);
        }
    }

    private void enableMyLocationButton() {
        final Activity activity = getActivity();
        if (activity == null) return;

        View view = getView();
        if (view == null) return;

        FloatingActionButton myLocationButton = view.findViewById(R.id.my_location_button);
        myLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PermissionUtils.getPermissionStatus(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PermissionStatus.GRANTED) {
                    jumpToMyLocation();
                } else {
                    enableMyLocation(true);
                }
            }
        });
    }

    public void showLocation(LocationDb location) {
        MAP_FOLLOW_ME = false;

        LatLng pos = location.getPosition().ToLatLng();

        removeLocation();
        targetMarker = addMarker(pos, location.getName(), 0f, location, false);

        jumpToLocation(pos, 16.0f, true);
    }

    public void removeLocation() {
        if (targetMarker != null) {
            targetMarker.remove();
        }
    }

    private Marker addMarker(LatLng position, String title, float hue, Object data, boolean draggable) {
        MarkerOptions markerOpt = new MarkerOptions()
                .position(position)
                .title(title)
                .icon(BitmapDescriptorFactory.defaultMarker(hue));

        Marker marker = mMap.addMarker(markerOpt);
        marker.setTag(data);
        marker.showInfoWindow();
        marker.setDraggable(draggable);

        return marker;
    }

    private void jumpToLocation(LatLng position, float zoom, boolean animated) {
        CameraPosition.Builder positionBuilder = new CameraPosition.Builder();
        positionBuilder.target(position);
        if (zoom == 0) {
            zoom = mMap.getCameraPosition().zoom;
        }
        positionBuilder.zoom(zoom);

        CameraUpdate cameraPos = CameraUpdateFactory.newCameraPosition(positionBuilder.build());
        if (animated) {
            mMap.animateCamera(cameraPos);
        } else {
            mMap.moveCamera(cameraPos);
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
            enableMyLocation(false);
        }
    }
}