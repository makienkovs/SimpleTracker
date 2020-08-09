package com.makienkovs.simpletracker;

import android.icu.util.Calendar;
import android.location.Location;
import android.text.format.DateFormat;

public class Position {
    private Location location;
    private long dateTime;

    public Position(Location location) {
        this.location = location;
        this.dateTime = Calendar.getInstance().getTimeInMillis();
    }

    public Location getLocation() {
        return location;
    }

    public long getDateTime() {
        return dateTime;
    }

    public String getDateTimeString() {
        return (String) DateFormat.format("dd.MM.yyyy, HH.mm.ss", dateTime);
    }
}