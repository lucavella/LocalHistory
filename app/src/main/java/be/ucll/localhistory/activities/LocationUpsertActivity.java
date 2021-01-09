package be.ucll.localhistory.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

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
        setTitle(R.string.title_activity_location_insert);

        createButtonClickListener();

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_INSERT.equals(intent.getAction())) {
            locationIsNew = true;
            location = (LocationDb)intent.getSerializableExtra(
                    getString(R.string.location_txt)
            );

            EditText placeText = (EditText)findViewById(R.id.location_upsert_place_edit_text);
            placeText.setText(location.getPlace());
            if ((location.getCity() != null) && (location.getCountry() != null)) {
                placeText.setEnabled(false);
            }
        }
    }

    private void createButtonClickListener() {
        ((Button)findViewById(R.id.location_upsert_done_button))
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

                            DatabaseReference newLocationSnap = locationRef.push();
                            newLocationSnap.setValue(location);

                            Uri dbIdUri = new Uri.Builder()
                                    .appendPath(getString(R.string.db_location_txt))
                                    .appendPath(newLocationSnap.getKey())
                                    .build();

                            Intent showLocationIntent = new Intent()
                                    .setAction(Intent.ACTION_VIEW)
                                    .setData(dbIdUri);

                            setResult(RESULT_OK, showLocationIntent);
                            finish();
                        } else {
                            Toast.makeText(LocationUpsertActivity.this, R.string.location_upsert_incomplete, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}