package com.makienkovs.simpletracker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private final int REQUEST_CODE = 2;
    private float distance = 1;
    private long duration = 1;
    private String time;
    private boolean isStart = false;
    private int type = 1;
    private int dimensions = 1;
    private int period = 5;
    public static ArrayList<Position> positions;
    private static final int NOTIFY_ID = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        ActionBar actionbar = getSupportActionBar();
        if (actionbar != null)
            actionbar.setTitle("");
        readParameters();
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
        }
    }

    @Override
    protected void onDestroy() {
        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(MapsActivity.this);
        notificationManager.cancel(NOTIFY_ID);
        super.onDestroy();
    }

    private void readParameters() {
        SharedPreferences settings = getSharedPreferences("APP_PREFERENCES", Context.MODE_PRIVATE);
        if (settings.contains("APP_PREFERENCES_TYPE")) {
            type = settings.getInt("APP_PREFERENCES_TYPE", 1);
        }
        if (settings.contains("APP_PREFERENCES_DIMENSIONS")) {
            dimensions = settings.getInt("APP_PREFERENCES_DIMENSIONS", 1);
        }
        if (settings.contains("APP_PREFERENCES_PERIOD")) {
            period = settings.getInt("APP_PREFERENCES_PERIOD", 5);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("...", "PERMISSION_GRANTED");
            } else {
                permissionDialog();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.start:
                startServiceDialog();
                break;
            case R.id.stop:
                stopService();
                break;
            case R.id.info:
                showInformation();
                break;
            case R.id.settings:
                settings();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @SuppressLint("SetTextI18n")
    private void settings() {
        final View settingsLayout = getLayoutInflater().inflate(R.layout.settings, null);
        final RadioButton normal = settingsLayout.findViewById(R.id.normal);
        final RadioButton hybrid = settingsLayout.findViewById(R.id.hybrid);
        final RadioButton satellite = settingsLayout.findViewById(R.id.satellite);
        switch (type) {
            case 2:
                hybrid.setChecked(true);
                break;
            case 3:
                satellite.setChecked(true);
                break;
            default:
                normal.setChecked(true);
        }
        normal.setOnClickListener(click -> {
            hybrid.setChecked(false);
            satellite.setChecked(false);
            type = 1;
        });
        hybrid.setOnClickListener(click -> {
            normal.setChecked(false);
            satellite.setChecked(false);
            type = 2;
        });
        satellite.setOnClickListener(click -> {
            normal.setChecked(false);
            hybrid.setChecked(false);
            type = 3;
        });
        final RadioButton meter = settingsLayout.findViewById(R.id.meter);
        final RadioButton km = settingsLayout.findViewById(R.id.km);
        final RadioButton mile = settingsLayout.findViewById(R.id.mile);
        switch (dimensions) {
            case 2:
                km.setChecked(true);
                break;
            case 3:
                mile.setChecked(true);
                break;
            default:
                meter.setChecked(true);
        }
        meter.setOnClickListener(click -> {
            km.setChecked(false);
            mile.setChecked(false);
            dimensions = 1;
        });
        km.setOnClickListener(click -> {
            meter.setChecked(false);
            mile.setChecked(false);
            dimensions = 2;
        });
        mile.setOnClickListener(click -> {
            meter.setChecked(false);
            km.setChecked(false);
            dimensions = 3;
        });
        final EditText periodText = settingsLayout.findViewById(R.id.period);
        periodText.setText("" + period);
        new AlertDialog.Builder(this)
                .setTitle(R.string.settings)
                .setPositiveButton(R.string.ok, ((dialog, which) -> {
                    period = Integer.parseInt(periodText.getText().toString());
                    saveParameters();
                }))
                .setView(settingsLayout)
                .setIcon(R.drawable.settings)
                .setCancelable(false)
                .create()
                .show();
    }

    private void saveParameters() {
        switch (type) {
            case 2:
                mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                break;
            case 3:
                mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                break;
            default:
                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }

        SharedPreferences.Editor editor = getSharedPreferences("APP_PREFERENCES", Context.MODE_PRIVATE).edit();
        editor.putInt("APP_PREFERENCES_TYPE", type);
        editor.putInt("APP_PREFERENCES_PERIOD", period);
        editor.putInt("APP_PREFERENCES_DIMENSIONS", dimensions);
        editor.apply();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        switch (type) {
            case 2:
                mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                break;
            case 3:
                mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                break;
            default:
                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        if (TrackService.imHere != null) {
            LatLng loc = new LatLng(TrackService.imHere.getLatitude(), TrackService.imHere.getLongitude());
            moveCamera(loc);
        }
    }

    public void permissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.nopermission)
                .setPositiveButton(R.string.preferences, ((dialog, which) -> openSystemPreferences()))
                .setNegativeButton(R.string.cancel, null)
                .setMessage(R.string.addpermission)
                .setCancelable(false)
                .create()
                .show();
    }

    public void openSystemPreferences() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    public void startServiceDialog() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionDialog();
            return;
        }
        if (isStart) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.newroute)
                    .setPositiveButton(R.string.ok, ((dialog, which) -> startService()))
                    .setNegativeButton(R.string.cancel, null)
                    .setMessage(R.string.alreadyrunning)
                    .setCancelable(false)
                    .create()
                    .show();
        } else {
            startService();
        }
    }

    public void startService() {
        positions = new ArrayList<>();
        isStart = true;
        Toast.makeText(this, R.string.start, Toast.LENGTH_SHORT).show();
        startService(new Intent(this, TrackService.class).putExtra("period", period));
        if (TrackService.imHere != null) {
            positions.add(new Position(TrackService.imHere));
        }
        drawOnline();
        notification();
    }

    private void drawOnline() {
        new Thread(() -> {
            while (isStart) {
                try {
                    runOnUiThread(this::draw);
                    Thread.sleep(period * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void stopService() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionDialog();
            return;
        }
        if (positions == null) {
            Toast.makeText(this, R.string.noroute, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isStart) {
            Toast.makeText(this, R.string.alreadystopped, Toast.LENGTH_SHORT).show();
            return;
        }
        if (TrackService.imHere != null) {
            positions.add(new Position(TrackService.imHere));
        }
        isStart = false;
        Toast.makeText(this, R.string.stop, Toast.LENGTH_SHORT).show();
        stopService(new Intent(this, TrackService.class));
        calculate();
        showInformation();

        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(MapsActivity.this);
        notificationManager.cancel(NOTIFY_ID);
    }

    @SuppressLint("DefaultLocale")
    private void calculate() {
        float[] results = new float[1];
        if (positions == null || positions.size() < 2) return;
        distance = 1;

        for (int i = 1; i < positions.size(); i++) {
            double startLat = positions.get(i - 1).getLocation().getLatitude();
            double startLong = positions.get(i - 1).getLocation().getLongitude();
            double endLat = positions.get(i).getLocation().getLatitude();
            double endLong = positions.get(i).getLocation().getLongitude();
            Location.distanceBetween(startLat, startLong, endLat, endLong, results);
            distance += results[0];
        }

        Position startPos = positions.get(0);
        Position stopPos = positions.get(positions.size() - 1);
        duration = stopPos.getDateTime() - startPos.getDateTime();
        long millis = duration % 1000;
        long second = (duration / 1000) % 60;
        long minute = (duration / (1000 * 60)) % 60;
        long hour = (duration / (1000 * 60 * 60)) % 24;
        time = String.format("%02d:%02d:%02d.%02d", hour, minute, second, millis);
    }

    private void draw() {
        if (mMap == null) return;
        mMap.clear();
        PolylineOptions polylineOptions = new PolylineOptions().color(Color.RED).width(10);

        if (positions == null || positions.size() < 1) return;

        for (int i = 0; i < positions.size(); i++) {
            LatLng pos = new LatLng(positions.get(i).getLocation().getLatitude(), positions.get(i).getLocation().getLongitude());
            polylineOptions.add(pos);
        }

        mMap.addPolyline(polylineOptions);
        Position startPos = positions.get(0);
        Position stopPos = positions.get(positions.size() - 1);
        LatLng start = new LatLng(startPos.getLocation().getLatitude(), startPos.getLocation().getLongitude());
        LatLng stop = new LatLng(stopPos.getLocation().getLatitude(), stopPos.getLocation().getLongitude());
        addMarker(start, getString(R.string.start));
        addMarker(stop, getString(R.string.stop));
    }

    public void showInformation() {
        if (positions == null || positions.size() < 1) {
            Toast.makeText(this, R.string.noroute, Toast.LENGTH_SHORT).show();
            return;
        }
        calculate();

        String speedD;
        String distanceD;
        String distanceString;

        float speed;
        switch (dimensions) {
            case 2:
                speedD = getString(R.string.kmph);
                distanceD = getString(R.string.km);
                distanceString = "" + distance / 1000;
                speed = distance * 3600 / duration;
                break;
            case 3:
                speedD = getString(R.string.mph);
                distanceD = getString(R.string.miles);
                distanceString = "" + distance / 1609;
                speed = distance * 2237 / duration;
                break;
            default:
                speedD = getString(R.string.metersps);
                distanceD = getString(R.string.meters);
                distanceString = "" + distance;
                speed = distance * 1000 / duration;
        }

        String message =
                getString(R.string.distance) + " : " + distanceString + " " + distanceD + "\n" +
                        getString(R.string.start) + " : " + positions.get(0).getDateTimeString() + "\n" +
                        getString(R.string.stop) + " : " + positions.get(positions.size() - 1).getDateTimeString() + "\n" +
                        getString(R.string.time) + " : " + time + "\n" +
                        getString(R.string.speed) + " : " + speed + " " + speedD;

        new AlertDialog.Builder(this)
                .setTitle(R.string.info)
                .setPositiveButton(R.string.ok, null)
                .setIcon(R.drawable.info)
                .setMessage(message)
                .setCancelable(false)
                .create()
                .show();
    }

    private void notification() {
        Intent notificationIntent = new Intent(MapsActivity.this, MapsActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(MapsActivity.this,
                0, notificationIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(MapsActivity.this, "...")
                        .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.icon))
                        .setSmallIcon(android.R.drawable.ic_dialog_map)
                        .setContentTitle(getString(R.string.run))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setTicker(getString(R.string.run))
                        .setUsesChronometer(true)
                        .setOngoing(true)
                        .setContentIntent(contentIntent);
        Notification notification = builder.build();
        notification.defaults = Notification.DEFAULT_VIBRATE;

        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(MapsActivity.this);
        notificationManager.notify(NOTIFY_ID, notification);
    }

    private void addMarker(LatLng pos, String title) {
        MarkerOptions marker = new MarkerOptions();
        marker.position(pos);
        marker.title(title);
        mMap.addMarker(marker);
    }

    private void moveCamera(LatLng pos) {
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(pos, 15);
        mMap.animateCamera(update);
    }
}