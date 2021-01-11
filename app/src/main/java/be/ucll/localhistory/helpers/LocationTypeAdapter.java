package be.ucll.localhistory.helpers;

import android.content.Context;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.List;

import be.ucll.localhistory.models.LocationType;

public class LocationTypeAdapter extends ArrayAdapter<String> {


    public LocationTypeAdapter(Context context, int resource) {
        super(context, resource, new ArrayList<String>());

        List<String> data = new ArrayList<>();

        for (LocationType type : LocationType.values()) {
            data.add(type.toString());
        }

        super.addAll(data);
    }
}
