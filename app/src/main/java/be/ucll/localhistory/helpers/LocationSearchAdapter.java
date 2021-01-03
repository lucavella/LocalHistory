package be.ucll.localhistory.helpers;

import android.content.Context;
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
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference locationRef = database.getReference("locations");

    private static final String[] mVisible =
            {
                    "name",
                    "place",
                    "_id",
                    "db_id"
            };
    private static final int[] mViewIds =
            {
                    R.id.location_name,
                    R.id.location_place
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

    public void UpdateAdapterCursorByQuery(String query) {
        final MatrixCursor suggestionsCursor = new MatrixCursor(mVisible);

        locationRef.orderByChild("name").startAt(query).endAt(query + "\uf8ff")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        int id = 0;
                        for (DataSnapshot ds : dataSnapshot.getChildren()) {
                            LocationDb loc = ds.getValue(LocationDb.class);
                            String db_id = ds.getKey();
                            suggestionsCursor.newRow()
                                    .add(loc.getName())
                                    .add(String.format("%s, %s", loc.getCity(), loc.getCountry()))
                                    .add(id++)
                                    .add(db_id);
                        }

                        LocationSearchAdapter.super.changeCursor(suggestionsCursor);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e("error", databaseError.getMessage());
                    }
                });
    }
}
