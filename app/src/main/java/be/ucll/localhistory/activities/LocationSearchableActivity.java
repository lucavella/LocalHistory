package be.ucll.localhistory.activities;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import be.ucll.localhistory.R;
import be.ucll.localhistory.helpers.LocationSearchAdapter;

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.activity_maps_menu, menu);

        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView =
                (SearchView) menu.findItem(R.id.location_search).getActionView();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        final SearchView searchView =
                (SearchView) menu.findItem(R.id.location_search).getActionView();
        final ListView resultsListView =
                findViewById(R.id.location_search_results_listview);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchView.clearFocus();

                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.length() > 2) {
                    final LocationSearchAdapter resultsAdapter = (LocationSearchAdapter) resultsListView.getAdapter();
                    resultsAdapter.UpdateAdapterCursorByQuery(newText);
                }

                return false;
            }
        });

        return true;
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            final LocationSearchAdapter resultsAdapter = new LocationSearchAdapter(
                    this,
                    R.layout.location_search_suggestion_item
            );

            String query = intent.getStringExtra(SearchManager.QUERY);
            if (query != null) {
                resultsAdapter.UpdateAdapterCursorByQuery(query);

                final ListView resultsListView =
                        findViewById(R.id.location_search_results_listview);
                resultsListView.setAdapter(resultsAdapter);
                createListViewOnClickListener();
            }
        }
    }

    private void createListViewOnClickListener() {
        final ListView resultsListView = findViewById(R.id.location_search_results_listview);

        resultsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ListAdapter adapter = resultsListView.getAdapter();
                Cursor selectedCursor = (Cursor)adapter.getItem(position);
                int dbIdColPos = selectedCursor.getColumnIndex("db_id");

                String dbId = selectedCursor.getString(dbIdColPos);
                Uri dbIdUri = new Uri.Builder()
                        .appendPath(getString(R.string.db_location_txt))
                        .appendPath(dbId)
                        .build();

                Intent showLocationIntent = new Intent()
                        .setAction(Intent.ACTION_VIEW)
                        .setData(dbIdUri);

                setResult(RESULT_OK, showLocationIntent);
                finish();
            }
        });
    }
}