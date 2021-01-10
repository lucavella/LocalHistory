package be.ucll.localhistory.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import be.ucll.localhistory.R;
import be.ucll.localhistory.models.LocationDb;

public class LocationInfoActivity extends AppCompatActivity {

    private LocationDb location;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_info);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        handleIntent(getIntent());
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

            TextView nameText = findViewById(R.id.location_info_name_val_text_view);
            TextView placeText = findViewById(R.id.location_info_place_val_text_view);
            TextView descriptionText = findViewById(R.id.location_info_description_val_text_view);
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
}