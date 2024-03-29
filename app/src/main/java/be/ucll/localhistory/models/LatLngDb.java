package be.ucll.localhistory.models;

import com.google.android.gms.maps.model.LatLng;

import java.io.Serializable;

/*
Firebase serializer needs empty constructor
Does not exist with Google Maps' LatLng
 */
public class LatLngDb implements Serializable {

    private double latitude;
    private double longitude;


    public LatLngDb() {
        this.latitude = 0;
        this.longitude = 0;
    }

    public LatLngDb(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public LatLngDb(LatLng latLng) {
        this.latitude = latLng.latitude;
        this.longitude = latLng.longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public LatLng ToLatLng() {
        return new LatLng(this.latitude, this.longitude);
    }
}
