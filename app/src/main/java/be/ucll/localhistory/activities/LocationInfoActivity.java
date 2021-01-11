package be.ucll.localhistory.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import be.ucll.localhistory.R;
import be.ucll.localhistory.models.LocationDb;

public class LocationInfoActivity extends AppCompatActivity
        implements
        SwipeRefreshLayout.OnRefreshListener{

    private LocationDb location;;

    private SwipeRefreshLayout swipeRefreshLayout;

    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference locationRef = database.getReference("locations");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_info);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

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
                location.setKey(snapshot.getKey());

                swipeRefreshLayout.setRefreshing(false);

                updateLocationTextInfo();
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
            if (Intent.ACTION_VIEW.equals(data.getAction())) {
                location = (LocationDb) data.getSerializableExtra(
                        getString(R.string.location_txt)
                );

                updateLocationTextInfo();
            }
        }
    }

    private void updateLocationTextInfo() {
        TextView nameText = findViewById(R.id.location_info_name_val_text_view);
        TextView typeText = findViewById(R.id.location_info_type_val_text_view);
        TextView placeText = findViewById(R.id.location_info_place_val_text_view);
        TextView descriptionText = findViewById(R.id.location_info_description_val_text_view);
        nameText.setText(location.getName());
        typeText.setText(location.getType().toString());
        placeText.setText(location.getPlace());
        descriptionText.setText(location.getDescription());
    }
}