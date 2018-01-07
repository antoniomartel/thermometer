package com.antoniomartel.thermometer;

import java.util.Date;
import java.sql.Timestamp;

/**
 * Created by Antonio Martel on 07/12/2017.
 */

public class Temperature {

    public double degrees;
    public boolean ambient;
    public boolean outdoor;
    public double nearestStationTemp;
    public String IP;
    public double longitude;
    public double latitude;
    public double altitude;
    public String locality;
    public Date date;
    public String email;
    public float humidity;
    public float pressure;

    public Temperature() {

        degrees = 0.0;
        ambient = false;
        outdoor = false;
        nearestStationTemp = 0.0;
        IP = "0.0.0.0";
        longitude = 0.0;
        latitude = 0.0;
        locality = "";
        date = null;
        email = "";
    }
}
