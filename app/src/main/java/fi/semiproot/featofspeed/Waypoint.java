package fi.semiproot.featofspeed;


import java.io.Serializable;

public class Waypoint implements Serializable {
    private int id;
    private String name;
    private double lat, lng;

    public Waypoint(int id, String name, double lat, double lng) {
        this.id = id;
        this.name = name;
        this.lat = lat;
        this.lng = lng;
    }

    public String getName() {
        return this.name;
    }

    public double getLat() {
        return this.lat;
    }

    public double getLng() {
        return this.lng;
    }
}
