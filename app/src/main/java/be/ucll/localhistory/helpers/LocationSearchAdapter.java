package be.ucll.localhistory.helpers;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.util.Log;
import android.widget.SimpleCursorAdapter;

import androidx.annotation.NonNull;
import androidx.cursoradapter.widget.CursorAdapter;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import be.ucll.localhistory.R;
import be.ucll.localhistory.models.LocationDb;

public class LocationSearchAdapter extends SimpleCursorAdapter {

    private final FirebaseDatabase database = FirebaseDatabase.getInstance();
    private final DatabaseReference locationRef = database.getReference("locations");

    private static final String[] mVisible =
            {
                    "name",
                    "place",
                    "drawable",
                    "_id",
                    "db_key"
            };
    private static final int[] mViewIds =
            {
                    R.id.location_search_name,
                    R.id.location_search_place,
                    R.id.location_search_type_image
            };


    public LocationSearchAdapter(Context context, int layout) {
        super(
                context,
                layout,
                null,
                mVisible,
                mViewIds,
                CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER
        );
    }

    public void updateCursorByQuery(String query) {
        final MatrixCursor suggestionsCursor = new MatrixCursor(mVisible);

        locationRef.orderByChild("name").startAt(query).endAt(query + "\uf8ff")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        int id = 0;
                        for (DataSnapshot ds : dataSnapshot.getChildren()) {
                            LocationDb location = ds.getValue(LocationDb.class);
                            if (location != null) {
                                suggestionsCursor.newRow()
                                        .add(location.getName())
                                        .add(String.format("%s, %s", location.getCity(), location.getCountry()))
                                        .add(location.getType().getResourceId())
                                        .add(id++)
                                        .add(ds.getKey());
                            }
                        }

                        LocationSearchAdapter.super.changeCursor(suggestionsCursor);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e("error", databaseError.getMessage());
                    }
                });
    }

    public String getLocationKeyAtPosition(int position) {
        Cursor selectedCursor = (Cursor) getItem(position);
        int dbKeyColPos = selectedCursor.getColumnIndex("db_key");
        return selectedCursor.getString(dbKeyColPos);
    }
}
