package be.ucll.localhistory.activities;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import be.ucll.localhistory.R;
import be.ucll.localhistory.helpers.LocationSearchAdapter;
import be.ucll.localhistory.models.LocationDb;

public class LocationSearchableActivity extends AppCompatActivity
        implements
        SwipeRefreshLayout.OnRefreshListener {

    private LocationSearchAdapter resultsAdapter;
    private SearchView searchView;
    private SwipeRefreshLayout resultsSwipeRefreshLayout;
    private SwipeRefreshLayout emptySwipeRefreshLayout;

    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference locationRef = database.getReference("locations");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_search_results);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ListView resultsListView = findViewById(R.id.location_searchable_list_view);
        TextView emptyView = findViewById(R.id.location_searchable_empty_text_view);
        resultsListView.setEmptyView(emptyView);

        resultsSwipeRefreshLayout =
                findViewById(R.id.location_searchable_results_swipe_refresh);
        emptySwipeRefreshLayout =
                findViewById(R.id.location_searchable_empty_swipe_refresh);
        resultsSwipeRefreshLayout.setOnRefreshListener(this);

        handleIntent(getIntent());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_activity_location_searchable, menu);

        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView =
                (SearchView) menu.findItem(R.id.location_searchable_search).getActionView();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        final MenuItem searchMenuItem =
                menu.findItem(R.id.location_searchable_search);
        final SearchView searchView =
                (SearchView)searchMenuItem.getActionView();
        final ListView resultsListView =
                findViewById(R.id.location_searchable_list_view);
        this.searchView = searchView;

        searchView.setIconifiedByDefault(false);
        searchView.setFocusable(true);

        String query = getIntent().getStringExtra(SearchManager.QUERY);
        if (query != null) {
            searchView.setQuery(query, false);
        }

        searchMenuItem.getIcon().setTint(Color.WHITE);
        searchMenuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        searchView.requestFocus();
                        InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (inputManager != null) {
                            inputManager.showSoftInput(searchView.findFocus(), 0);
                        }
                    }
                });
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                searchView.clearFocus();
                return true;
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchMenuItem.collapseActionView();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.length() >= 2) {
                    final LocationSearchAdapter resultsAdapter = (LocationSearchAdapter) resultsListView.getAdapter();
                    resultsAdapter.updateCursorByQuery(newText);
                }

                return false;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.location_searchable_search:
                return false;

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

    @Override
    public void onRefresh() {
        String query = searchView.getQuery().toString();

        if (query.length() >= 2) {
            resultsAdapter.updateCursorByQuery(query);
        }

        resultsSwipeRefreshLayout.setRefreshing(false);
        emptySwipeRefreshLayout.setRefreshing(false);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            resultsAdapter = new LocationSearchAdapter(
                    this,
                    R.layout.location_search_suggestion_item
            );

            String query = intent.getStringExtra(SearchManager.QUERY);
            if (query != null) {
                resultsAdapter.updateCursorByQuery(query);

                final ListView resultsListView =
                        findViewById(R.id.location_searchable_list_view);
                resultsListView.setAdapter(resultsAdapter);
                createListViewOnClickListener();
            }
        }
    }

    private void createListViewOnClickListener() {
        final ListView resultsListView = findViewById(R.id.location_searchable_list_view);

        resultsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                LocationSearchAdapter resultsAdapter = (LocationSearchAdapter)resultsListView.getAdapter();
                String locationId = resultsAdapter.getLocationKeyAtPosition(position);

                locationRef.child(locationId)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                LocationDb location = dataSnapshot.getValue(LocationDb.class);
                                if (location != null) {
                                    location.setKey(dataSnapshot.getKey());

                                    Intent showLocationIntent = new Intent()
                                            .setAction(Intent.ACTION_VIEW)
                                            .putExtra(getString(R.string.location_txt), location);

                                    setResult(RESULT_OK, showLocationIntent);
                                    finish();
                                } else {
                                    Toast.makeText(LocationSearchableActivity.this, R.string.location_not_found, Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                Toast.makeText(LocationSearchableActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
                                Log.e("error", databaseError.getMessage());
                            }
                        });
            }
        });
    }
}