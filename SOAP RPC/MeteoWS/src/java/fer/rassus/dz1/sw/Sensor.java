/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fer.rassus.dz1.sw;

/**
 *
 * @author nameless
 */
public class Sensor {
    
    private final String username;
    private final double latitude;
    private final double longitude;
    private final UserAddress address;

    public Sensor(String username, double latitude, double longitude, String IPaddress, int port) {
        this.username = username;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = new UserAddress(IPaddress, port);
    }

    public String getUsername() {
        return username;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public UserAddress getAddress() {
        return address;
    }

    
    
    public static double computeDistance(Sensor sensor1, Sensor sensor2) {
        double R = 6371;
        double dlat = sensor2.getLatitude() - sensor1.getLatitude();
        double dlon = sensor2.getLongitude() - sensor1.getLongitude();
        double a = Math.pow(Math.sin(dlat/2), 2) + (Math.cos(sensor1.getLatitude())*Math.cos(sensor2.getLatitude())*Math.pow(Math.sin(dlon/2), 2));
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }
}