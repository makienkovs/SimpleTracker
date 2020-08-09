package com.makienkovs.simpletracker;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

public class TrackService extends Service implements LocationListener {

    private boolean run;
    private LocationManager locationManager;
    static Location imHere;

    @Override
    public void onCreate() {
        Log.d("...", "Service onCreate");
        initLocationManager();
        run = true;
        super.onCreate();
    }

    private void initLocationManager() {
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager == null) return;
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, this);
            imHere = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        } else {
            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, 1000, 1, this);
            imHere = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int period = intent.getIntExtra("period", 5) * 1000;
        new Thread(() -> {
            while (run) {
                try {
                    Thread.sleep(period);
                    if (imHere != null && MapsActivity.positions != null) {
                        MapsActivity.positions.add(new Position(imHere));
                        Log.d("Service add a point:", "Lat " + imHere.getLatitude() + " , Long " + imHere.getLongitude());
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Log.d("...", "Service stopSelf");
            stopSelf();
        }).start();
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        run = false;
        Log.d("...", "Service onDestroy");
        super.onDestroy();
    }

    @Override
    public void onLocationChanged(Location location) {
        imHere = location;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {
        initLocationManager();
    }

    @Override
    public void onProviderDisabled(String provider) {
        locationManager = null;
        imHere = null;
    }
}