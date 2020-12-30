package be.ucll.localhistory.models;

import com.google.android.gms.maps.model.LatLng;

public class LocationDb {
    private LatLng position;
    private String name, description, city, country;

    public LocationDb() {
    }

    public LocationDb(LatLng position, String name, String description, String city, String country) {
        this.position = position;
        this.name = name;
        this.description = description;
        this.city = city;
        this.country = country;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public LatLng getPosition() {
        return this.position;
    }

    public void setPosition(LatLng position) {
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
}
