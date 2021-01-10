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
import android.view.inputmethod.InputMethodManager;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import be.ucll.localhistory.R;
import be.ucll.localhistory.fragments.LocationMapsFragment;
import be.ucll.localhistory.helpers.LocationSearchAdapter;
import be.ucll.localhistory.models.LocationDb;


public class MapsActivity extends AppCompatActivity  {

    private Menu menu;

    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference locationRef = database.getReference("locations");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.activity_maps, new LocationMapsFragment())
                    .commit();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (menu != null) {
            MenuItem searchMenuItem =
                    menu.findItem(R.id.location_search);

            searchMenuItem.collapseActionView();
        }
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
        this.menu = menu;

        final LocationSearchAdapter suggestionsAdapter = new LocationSearchAdapter(
                this,
                R.layout.location_search_suggestion_item
        );

        final MenuItem searchMenuItem =
                menu.findItem(R.id.location_search);
        final SearchView searchView =
                (SearchView) searchMenuItem.getActionView();
        searchView.setSuggestionsAdapter(suggestionsAdapter);
        searchView.setIconifiedByDefault(false);
        searchView.setFocusable(true);

        searchMenuItem.getIcon().setTint(Color.WHITE);
        searchMenuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        searchView.requestFocus();
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (imm != null) { // it's never null. I've added this line just to make the compiler happy
                            imm.showSoftInput(searchView.findFocus(), 0);
                        }

                        String query = searchView.getQuery().toString();
                        if (query.length() >= 2) {
                            suggestionsAdapter.updateAdapterCursorByQuery(query);
                        }
                    }
                });
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                return true;
            }
        });

        searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionSelect(int position) {
                return true;
            }

            @Override
            public boolean onSuggestionClick(int position) {
                String locationId = suggestionsAdapter.getLocationKeyAtPosition(position);

                locationRef.child(locationId)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                LocationDb location = dataSnapshot.getValue(LocationDb.class);
                                if (location != null) {
                                    location.setKey(dataSnapshot.getKey());

                                    LocationMapsFragment locationMapsFragment = (LocationMapsFragment)getSupportFragmentManager()
                                            .findFragmentById(R.id.activity_maps);
                                    locationMapsFragment.showLocation(location);

                                } else {
                                    Toast.makeText(MapsActivity.this, R.string.location_not_found, Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                Toast.makeText(MapsActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
                                Log.e("error", databaseError.getMessage());
                            }
                        });
                return false;
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchMenuItem.collapseActionView();

                final Intent searchIntent = new Intent(getApplicationContext(),
                        LocationSearchableActivity.class)
                        .setAction(Intent.ACTION_SEARCH)
                        .putExtra(SearchManager.QUERY, query);
                startActivityForResult(searchIntent, 1);

                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                suggestionsAdapter.updateAdapterCursorByQuery(newText);
                return false;
            }
        });

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (data != null) {
                if (Intent.ACTION_VIEW.equals(data.getAction())) {
                    LocationDb location = (LocationDb) data.getSerializableExtra(
                            getString(R.string.location_txt)
                    );

                    LocationMapsFragment locationMapsFragment = (LocationMapsFragment)getSupportFragmentManager()
                            .findFragmentById(R.id.activity_maps);
                    locationMapsFragment.showLocation(location);
                }
            }
        }
    }
}