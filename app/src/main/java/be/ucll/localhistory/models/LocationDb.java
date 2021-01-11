package be.ucll.localhistory.models;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.Exclude;

import java.io.Serializable;

public class LocationDb implements Serializable {
    private LatLngDb position;
    private String key, name, description, city, country;
    private LocationType type;

    public LocationDb() {
    }

    public LocationDb(LatLng position, String city, String country) {
        this.position = new LatLngDb();
        this.position.fromLatLng(position);
        this.city = city;
        this.country = country;
    }

    public LocationDb(LatLngDb position, String name, String description, String city, String country, LocationType type) {
        this.position = position;
        this.name = name;
        this.description = description;
        this.city = city;
        this.country = country;
        this.type = type;
    }

    public LocationDb(LatLng position, String name, String description, String city, String country, LocationType type) {
        this.position = new LatLngDb();
        this.position.fromLatLng(position);
        this.name = name;
        this.description = description;
        this.city = city;
        this.country = country;
        this.type = type;
    }

    @Exclude
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getCity() {
        return (city != null) ? city : "Unknown";
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return (country != null) ? country : "Unknown";
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public LatLngDb getPosition() {
        return this.position;
    }

    public void setPosition(LatLngDb position) {
        this.position = position;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocationType getType() {
        return type;
    }

    public void setType(LocationType type) {
        this.type = type;
    }

    public String getPlace() {
        String cityNonNull = (city != null) ? city : "Unknown";
        String countryNonNull = (country != null) ? country : "Unknown";

        return String.format("%s, %s", cityNonNull, countryNonNull);
    }
}
