package be.ucll.localhistory.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import be.ucll.localhistory.R;
import be.ucll.localhistory.models.LocationDb;

public class LocationUpsertActivity extends AppCompatActivity {

    private LocationDb location;
    private boolean locationIsNew;

    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference locationRef = database.getReference("locations");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_upsert);

        createButtonClickListener();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_INSERT.equals(intent.getAction())) {
            setTitle(R.string.title_activity_location_insert);
            locationIsNew = true;

            location = (LocationDb)intent.getSerializableExtra(
                    getString(R.string.location_txt)
            );

            EditText placeText = findViewById(R.id.location_upsert_place_edit_text);
            placeText.setText(location.getPlace());
        }
        else if (Intent.ACTION_EDIT.equals(intent.getAction())) {
            setTitle(R.string.title_activity_location_edit);
            locationIsNew = false;

            location = (LocationDb)intent.getSerializableExtra(
                    getString(R.string.location_txt)
            );

            EditText nameText = findViewById(R.id.location_upsert_name_edit_text);
            EditText placeText = findViewById(R.id.location_upsert_place_edit_text);
            EditText descriptionText = findViewById(R.id.location_upsert_description_edit_text);
            nameText.setText(location.getName());
            placeText.setText(location.getPlace());
            descriptionText.setText(location.getDescription());
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        setResult(RESULT_CANCELED);
        finish();

        return true;
    }

    private void createButtonClickListener() {
        findViewById(R.id.location_upsert_done_button)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String name = ((EditText)findViewById(R.id.location_upsert_name_edit_text))
                                .getText().toString();
                        String description = ((EditText)findViewById(R.id.location_upsert_description_edit_text))
                                .getText().toString();

                        if (!name.isEmpty() && !description.isEmpty()) {
                            location.setName(name);
                            location.setDescription(description);

                            if (locationIsNew) {
                                DatabaseReference newLocationSnap = locationRef.push();
                                newLocationSnap.setValue(location);

                                location.setKey(newLocationSnap.getKey());
                            } else {
                                locationRef.child(location.getKey()).setValue(location);
                            }

                            Intent showLocationIntent = new Intent()
                                    .setAction(Intent.ACTION_VIEW)
                                    .putExtra(getString(R.string.location_txt), location);

                            setResult(RESULT_OK, showLocationIntent);
                            finish();
                        } else {
                            Toast.makeText(LocationUpsertActivity.this, R.string.location_upsert_incomplete, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}