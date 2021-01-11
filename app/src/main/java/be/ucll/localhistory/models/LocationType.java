package be.ucll.localhistory.models;

import java.lang.reflect.Field;

import be.ucll.localhistory.R;

public enum LocationType {
    OTHER,
    NATURE,
    MEMORIAL,
    MONUMENT,
    BUILDING;

    final private static String drawableNameTemplate = "ic_location_type_%s";


    @Override
    public String toString() {
        String first = this.name().substring(0, 1).toUpperCase();
        String next = this.name().substring(1).toLowerCase();

        return String.format("%s%s", first, next);
    }

    public int getResourceId() {
        try {
            String typeName = this.name().toLowerCase();
            String drawableName = String.format(drawableNameTemplate, typeName);
            Field resField = R.drawable.class.getDeclaredField(drawableName);
            return resField.getInt(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return -1;
        }
    }
}
