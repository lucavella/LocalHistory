package be.ucll.localhistory.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import be.ucll.localhistory.R;
import be.ucll.localhistory.models.LocationDb;

public class LocationInfoActivity extends AppCompatActivity {

    private LocationDb location;;

    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference locationRef = database.getReference("locations");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_info);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        handleIntent(getIntent());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_details_menu, menu);

        return true;
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
        TextView placeText = findViewById(R.id.location_info_place_val_text_view);
        TextView descriptionText = findViewById(R.id.location_info_description_val_text_view);
        nameText.setText(location.getName());
        placeText.setText(location.getPlace());
        descriptionText.setText(location.getDescription());
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
                locationRef.child(location.getKey()).removeValue();

                setResult(RESULT_OK);
                finish();

            default:
                setResult(RESULT_CANCELED);
                finish();
                return true;
        }
    }
}