package be.ucll.localhistory.activities;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import be.ucll.localhistory.R;
import be.ucll.localhistory.helpers.LocationSearchSuggestionsAdapter;

public class LocationSearchableActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_search_results);

        ListView resultsListView = findViewById(R.id.location_search_results_listview);
        TextView emptyView = findViewById(R.id.no_result_textview);
        resultsListView.setEmptyView(emptyView);

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            final LocationSearchSuggestionsAdapter resultsAdapter = new LocationSearchSuggestionsAdapter(
                    this,
                    R.layout.location_search_suggestion_item
            );

            String query = intent.getStringExtra(SearchManager.QUERY);
            if (query != null) {
                resultsAdapter.UpdateAdapterCursorByQuery(query);

                ListView resultsListView = findViewById(R.id.location_search_results_listview);
                resultsListView.setAdapter(resultsAdapter);
            }


        }
    }
}