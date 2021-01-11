package be.ucll.localhistory.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
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
import be.ucll.localhistory.helpers.PermissionUtils;
import be.ucll.localhistory.helpers.PermissionStatus;
import be.ucll.localhistory.models.LocationDb;

public class LocationMapsFragment extends Fragment
        implements
        OnMapReadyCallback,
        GoogleMap.OnCameraMoveStartedListener,
        GoogleMap.OnMapLongClickListener,
        GoogleMap.OnMarkerClickListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static boolean MAP_FOLLOW_ME = true;

    private GoogleMap mMap;
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

        addMarker(latLng, "Add new", 0f, null);
        jumpToLocation(latLng, 16.0f, true);
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
            if (locationManager == null) locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

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

            FloatingActionButton myLocationButton = getView().findViewById(R.id.my_location_button);
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
        addMarker(pos, location.getName(), 215, location);
        jumpToLocation(pos, 16.0f, true);
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
}