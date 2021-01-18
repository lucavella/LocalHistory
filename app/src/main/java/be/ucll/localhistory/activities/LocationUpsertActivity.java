package be.ucll.localhistory.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import be.ucll.localhistory.R;
import be.ucll.localhistory.helpers.LocationTypeAdapter;
import be.ucll.localhistory.models.LocationDb;

public class LocationUpsertActivity extends AppCompatActivity {

    private LocationDb location;
    private boolean locationIsNew;

    private final FirebaseDatabase database = FirebaseDatabase.getInstance();
    private final DatabaseReference locationRef = database.getReference("locations");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_upsert);

        createButtonClickListener();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        Spinner typeSpinner = findViewById(R.id.location_upsert_type_spinner);
        LocationTypeAdapter typeAdapter =
                new LocationTypeAdapter(this, android.R.layout.simple_spinner_item);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(typeAdapter);

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
            typeSpinner.setSelection(location.getTypeNr());
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
                        int typeNr = ((Spinner)findViewById(R.id.location_upsert_type_spinner))
                                .getSelectedItemPosition();

                        if (!name.isEmpty() && !description.isEmpty()) {
                            location.setName(name);
                            location.setDescription(description);
                            location.setTypeNr(typeNr);

                            if (locationIsNew) {
                                DatabaseReference newLocationSnap = locationRef.push();
                                newLocationSnap.setValue(location);

                                location.setKey(newLocationSnap.getKey());
                            } else {
                                locationRef.child(location.getKey()).setValue(location);
                            }

                            Intent showLocationInfoIntent = new Intent()
                                    .setAction(Intent.ACTION_GET_CONTENT)
                                    .putExtra(getString(R.string.location_txt), location);

                            setResult(RESULT_OK, showLocationInfoIntent);
                            finish();
                        } else {
                            Toast.makeText(LocationUpsertActivity.this, R.string.location_upsert_incomplete, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}