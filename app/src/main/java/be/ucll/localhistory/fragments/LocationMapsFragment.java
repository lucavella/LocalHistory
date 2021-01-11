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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

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
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
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

    private GoogleMap mMap;
    private Marker mMarker;
    private LocationManager locationManager;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_location_maps, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
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

        enableLocation(false);

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
                    enableLocation(false);
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

    @Override
    public void onCameraMoveStarted(int reason) {
        if (reason == REASON_GESTURE) MAP_FOLLOW_ME = false;
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        MAP_FOLLOW_ME = false;

        addMarker(latLng, "Add new", 0f, null, true);
        jumpToLocation(latLng, 0f, true);
    }

    @Override
    public void onMarkerDragStart(Marker marker) {

    }

    @Override
    public void onMarkerDrag(Marker marker) {

    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        LatLng position = marker.getPosition();
        mMarker.setPosition(position);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        LocationDb location = (LocationDb) marker.getTag();
        if (location == null) {
            LatLng pos = marker.getPosition();

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
            Toast.makeText(getActivity(), R.string.location_add_failed, Toast.LENGTH_SHORT).show();
        } else {
            Intent infoIntent = new Intent(getActivity(),
                    LocationInfoActivity.class)
                    .setAction(Intent.ACTION_VIEW)
                    .putExtra(getString(R.string.location_txt), location);

            startActivityForResult(infoIntent, 1);
            marker.remove();
        }

        return true;
    }

    @SuppressLint("MissingPermission")
    private void enableLocation(boolean overrideRationale) {
        if (PermissionUtils.getPermissionStatus(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PermissionStatus.GRANTED) {
            Activity activity = getActivity();
            if (activity == null) return;
            if (locationManager == null) locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);

            if (mMap != null) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMapToolbarEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
            }

            String bestProvider = locationManager.getBestProvider(new Criteria(), false);
            if (bestProvider == null) return;
            locationManager.requestLocationUpdates(bestProvider, 50, 1, new LocationListener() {
                        @Override
                        public void onLocationChanged(@NonNull Location location) {
                            if ((MAP_FOLLOW_ME) && (PermissionUtils.getPermissionStatus(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
                                    == PermissionStatus.GRANTED)) {
                                jumpToMyLocation();
                            }
                        }
                    }

            );

            View view = getView();
            if (view == null) return;
            FloatingActionButton myLocationButton = view.findViewById(R.id.my_location_button);
            myLocationButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (PermissionUtils.getPermissionStatus(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
                            == PermissionStatus.GRANTED) {
                        jumpToMyLocation();
                    } else {
                        enableLocation(true);
                    }
                }
            });

        } else {
            PermissionUtils.requestPermission(getActivity(), LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, overrideRationale);
        }
    }

    public void showLocation(LocationDb location) {
        MAP_FOLLOW_ME = false;

        LatLng pos = location.getPosition().ToLatLng();
        addMarker(pos, location.getName(), 215, location, false);
        jumpToLocation(pos, 16.0f, true);
    }

    private void addMarker(LatLng position, String title, float hue, Object data, boolean draggable) {
        mMap.clear();

        MarkerOptions markerOpt = new MarkerOptions()
                .position(position)
                .title(title)
                .icon(BitmapDescriptorFactory.defaultMarker(hue));

        mMarker = mMap.addMarker(markerOpt);
        mMarker.setTag(data);
        mMarker.showInfoWindow();
        mMarker.setDraggable(draggable);
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
            enableLocation(false);
        }
    }
}