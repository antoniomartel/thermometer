package com.antoniomartel.thermometer;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Thermometer thermometer;
    private float temperature;
    private boolean existTemperatureSensor;
    private boolean existHumiditySensor;
    private boolean existPressureSensor;
    private Sensor mHumiditySensor;
    private Sensor mPressureSensor;
    private Sensor mTemperatureSensor;

    private Temperature temp = new Temperature();

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mTemperaturesDatabaseReference;

    private void getAmbientData() {
        // Get ambient temperature
        Sensor mTemperatureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        if (mTemperatureSensor != null) {
            sensorManager.registerListener(this, mTemperatureSensor, SensorManager.SENSOR_DELAY_FASTEST);
            existTemperatureSensor = true;
        } else {
            existTemperatureSensor = false;
            Toast.makeText(this, "No Temperature Sensor !", Toast.LENGTH_LONG).show();
        }

        // Get relative humidity
        Sensor mHumiditySensor = sensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);
        if (mHumiditySensor != null) {
            sensorManager.registerListener(this, mHumiditySensor, SensorManager.SENSOR_DELAY_FASTEST);
            existHumiditySensor = true;
        } else {
            existHumiditySensor = false;
            Toast.makeText(this, "No Humidity Sensor !", Toast.LENGTH_LONG).show();
        }

        // Get Millibars of pressure
        Sensor mPressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        if (mPressureSensor != null) {
            sensorManager.registerListener(this, mPressureSensor, SensorManager.SENSOR_DELAY_FASTEST);
            existPressureSensor = true;
        } else {
            existPressureSensor = false;
            Toast.makeText(this, "No Pressure Sensor !", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        thermometer = (Thermometer) findViewById(R.id.thermometer);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mTemperaturesDatabaseReference = mFirebaseDatabase.getReference().child("temperatures");

        //mTemperaturesDatabaseReference.removeValue();

        getAmbientData();
        saveAmbientData();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (existHumiditySensor) {
            sensorManager.registerListener(this, mHumiditySensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (existTemperatureSensor) {
            sensorManager.registerListener(this, mTemperatureSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (existPressureSensor) {
            sensorManager.registerListener(this, mPressureSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        // loadAmbientTemperature();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // unregisterAll();

        if (existHumiditySensor || existPressureSensor || existTemperatureSensor) {
            sensorManager.unregisterListener(this);
        }
    }

    private void loadAmbientTemperature() {
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);

        if (sensor != null) {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST);
        } else {
            Toast.makeText(this, "No Temperature Sensors !", Toast.LENGTH_LONG).show();
        }
    }

    private void unregisterAll() {
        sensorManager.unregisterListener(this);
    }

    private Location getLastKnownLocation(LocationManager lm) {
        lm = (LocationManager)getApplicationContext().getSystemService(LOCATION_SERVICE);
        List<String> providers = lm.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            Location l = lm.getLastKnownLocation(provider);
            if (l == null) {
                continue;
            }
            if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                // Found best last known location: %s", l);
                bestLocation = l;
            }
        }
        return bestLocation;
    }

    private void saveAmbientData() {

        try {
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                //temp.longitude = 0.0;
                //temp.latitude = 0.0;
                //temp.altitude = 0.0;
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 0);
                Location location = getLastKnownLocation(lm);
                temp.longitude = location.getLongitude();
                temp.latitude = location.getLatitude();
                temp.altitude = location.getAltitude();
            }
            else {
                Location location = getLastKnownLocation(lm);
                temp.longitude = location.getLongitude();
                temp.latitude = location.getLatitude();
                temp.altitude = location.getAltitude();
            }

        }
        catch (Exception e) {
            e.printStackTrace();
            temp.longitude = 0.0;
            temp.latitude = 0.0;
            temp.altitude = 0.0;
        }

        temp.date = new Date();

        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        isoFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        try {
            String d = isoFormat.format(new Date());
            temp.date = isoFormat.parse(d);
        } catch (ParseException e) {
            temp.date = null;
            e.printStackTrace();
        }

        mTemperaturesDatabaseReference.push().setValue(temp);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        if (sensorEvent.sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
            temp.degrees = sensorEvent.values[0];
            thermometer.setCurrentTemp(temperature);
            getSupportActionBar().setTitle(getString(R.string.app_name) + " : " + temperature);
        } else {
            if (sensorEvent.sensor.getType() == Sensor.TYPE_RELATIVE_HUMIDITY) {
                temp.humidity = sensorEvent.values[0];
            } else {
                if (sensorEvent.sensor.getType() == Sensor.TYPE_PRESSURE) {
                    temp.pressure = sensorEvent.values[0];
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}