package be.ucll.localhistory.helpers;

import android.content.Context;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.List;

import be.ucll.localhistory.models.LocationType;

public class LocationTypeAdapter extends ArrayAdapter {


    public LocationTypeAdapter(Context context, int resource) {
        super(context, resource, new ArrayList());

        List<String> data =  new ArrayList<String>();

        for (LocationType type : LocationType.values()) {
            data.add(type.toString());
        }

        super.addAll(data);
    }
}
