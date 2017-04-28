package fi.semiproot.featofspeed;


import android.util.Log;

import org.json.JSONObject;

import java.io.Serializable;
import java.util.Map;

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

    public int getId() {
        return id;
    }

    public static Waypoint fromMap(Map<String, Object> map) {
        int id = ((Long)map.get("waypoint_id")).intValue();
        String name = (String)map.get("nickname");

        Map<String, Object> location = (Map<String, Object>)map.get("location");
        double lat = (double)location.get("lat");
        double lng = (double)location.get("lng");

        return new Waypoint(id, name, lat, lng);
    }

    public static Waypoint fromJSONObject(JSONObject obj) {
        try {
            int id = obj.getInt("waypoint_id");
            String name = obj.getString("name");

            JSONObject location = obj.getJSONObject("location");

            double lat = location.getDouble("lat");
            double lng = location.getDouble("lng");

            return new Waypoint(id, name, lat, lng);
        }
        catch (Exception e) {
            Log.d("Waypoint", "Could not parse JSONOBject to Waypoint");
            return null;
        }
    }
}
