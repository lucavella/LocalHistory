package be.ucll.localhistory.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;

import be.ucll.localhistory.R;
import be.ucll.localhistory.helpers.PermissionStatus;
import be.ucll.localhistory.helpers.PermissionUtils;
import be.ucll.localhistory.models.LocationDb;

public class LocationInfoActivity extends AppCompatActivity
        implements
        SwipeRefreshLayout.OnRefreshListener{

    private LocationDb location;

    private LocationManager locationManager;

    private SwipeRefreshLayout swipeRefreshLayout;

    private final FirebaseDatabase database = FirebaseDatabase.getInstance();
    private final DatabaseReference locationRef = database.getReference("locations");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_info);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        swipeRefreshLayout = findViewById(R.id.location_info_swipe_refresh);
        swipeRefreshLayout.setOnRefreshListener(this);

        handleIntent(getIntent());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_acticity_location_info, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_location_edit:
                Intent editIntent = new Intent(getApplicationContext(),
                        LocationUpsertActivity.class)
                        .setAction(Intent.ACTION_EDIT)
                        .putExtra(getString(R.string.location_txt), location);

                startActivityForResult(editIntent, 1);
                return true;

            case R.id.menu_location_delete:
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.location_delete_dialog_title))
                        .setMessage(R.string.location_delete_dialog_msg)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                locationRef.child(location.getKey()).removeValue();

                                setResult(RESULT_OK);
                                finish();
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();

                return true;

            default:
                setResult(RESULT_CANCELED);
                finish();
                return true;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            location = (LocationDb)intent.getSerializableExtra(
                    getString(R.string.location_txt)
            );

            updateLocationTextInfo();
        }
    }

    @Override
    public void onRefresh() {
        locationRef.child(location.getKey())
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                location = snapshot.getValue(LocationDb.class);

                if (location!= null) {
                    location.setKey(snapshot.getKey());
                    updateLocationTextInfo();
                }

                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                swipeRefreshLayout.setRefreshing(false);

                Log.e("error", error.getMessage());
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (data != null) {
                if (Intent.ACTION_VIEW.equals(data.getAction())) {
                    location = (LocationDb) data.getSerializableExtra(
                            getString(R.string.location_txt)
                    );

                    updateLocationTextInfo();
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void updateLocationTextInfo() {
        String distanceText = "Unknown";

        if ((PermissionUtils.getPermissionStatus(
                this, Manifest.permission.ACCESS_FINE_LOCATION) == PermissionStatus.GRANTED)) {
            if (locationManager == null)
                locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            String bestProvider = locationManager.getBestProvider(new Criteria(), false);
            if (bestProvider != null) {
                Location lastLocation = locationManager.getLastKnownLocation(bestProvider);

                if (lastLocation != null) {
                    GeoLocation geoLocation = new GeoLocation(location.getPosition().getLatitude(),
                            location.getPosition().getLongitude());
                    GeoLocation myGeoLocation = new GeoLocation(lastLocation.getLatitude(),
                            lastLocation.getLongitude());

                    double distanceInM = GeoFireUtils.getDistanceBetween(geoLocation, myGeoLocation);

                    if (distanceInM < 10_000) {
                        distanceText = String.format("%d m", Math.round(distanceInM));
                    } else {
                        double distanceInKm = distanceInM / 1000;

                        if (distanceInKm < 100) {
                            DecimalFormat round = new DecimalFormat("#.#");

                            distanceText = String.format("%s km", round.format(distanceInKm));
                        } else {
                            distanceText = String.format("%d km", Math.round(distanceInKm));
                        }

                    }
                }

            }
        }

        TextView nameText = findViewById(R.id.location_info_name_val_text_view);
        TextView typeText = findViewById(R.id.location_info_type_val_text_view);
        TextView placeText = findViewById(R.id.location_info_place_val_text_view);
        TextView distanceView = findViewById(R.id.location_info_distance_val_text_view);
        TextView descriptionText = findViewById(R.id.location_info_description_val_text_view);

        nameText.setText(location.getName());
        typeText.setText(location.getType().toString());
        placeText.setText(location.getPlace());
        distanceView.setText(distanceText);
        descriptionText.setText(location.getDescription());

        int imageResId = location.getType().getResourceId();
        if (imageResId > 0) {
            ImageView typeImage = findViewById((R.id.location_info_type_image_view));
            typeImage.setImageResource(imageResId);
        }
    }
}