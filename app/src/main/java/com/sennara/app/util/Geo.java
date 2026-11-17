package com.sennara.app.util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.core.content.ContextCompat;

import com.sennara.app.data.Session;

import java.util.List;
import java.util.Locale;

public class Geo {
    public static final double DEFAULT_LAT = 36.8065;
    public static final double DEFAULT_LNG = 10.1815;

    public static boolean hasPermission(Context c) {
        int fine = ContextCompat.checkSelfPermission(c, Manifest.permission.ACCESS_FINE_LOCATION);
        int coarse = ContextCompat.checkSelfPermission(c, Manifest.permission.ACCESS_COARSE_LOCATION);
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean known(Context c) {
        return Session.prefs(c).contains("gps_lat");
    }

    public static double[] here(Context c) {
        SharedPreferences prefs = Session.prefs(c);
        if (!prefs.contains("gps_lat")) {
            return new double[]{DEFAULT_LAT, DEFAULT_LNG};
        }
        double lat = Double.parseDouble(prefs.getString("gps_lat", "0"));
        double lng = Double.parseDouble(prefs.getString("gps_lng", "0"));
        return new double[]{lat, lng};
    }

    private static void save(Context c, Location location) {
        SharedPreferences.Editor editor = Session.prefs(c).edit();
        editor.putString("gps_lat", String.valueOf(location.getLatitude()));
        editor.putString("gps_lng", String.valueOf(location.getLongitude()));
        editor.apply();
    }

    @SuppressLint("MissingPermission")
    public static void refresh(Context context, final Runnable onChange) {
        final Context c = context.getApplicationContext();
        if (!hasPermission(c)) {
            return;
        }
        final LocationManager manager = (LocationManager) c.getSystemService(Context.LOCATION_SERVICE);
        if (manager == null) {
            return;
        }
        try {
            Location best = null;
            List<String> providers = manager.getProviders(true);
            for (String provider : providers) {
                Location l = manager.getLastKnownLocation(provider);
                if (l != null && (best == null || l.getTime() > best.getTime())) {
                    best = l;
                }
            }
            if (best != null) {
                save(c, best);
                if (onChange != null) {
                    onChange.run();
                }
            }

            String provider = null;
            if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                provider = LocationManager.GPS_PROVIDER;
            } else if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                provider = LocationManager.NETWORK_PROVIDER;
            }
            if (provider == null) {
                return;
            }
            LocationListener listener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    manager.removeUpdates(this);
                    save(c, location);
                    if (onChange != null) {
                        onChange.run();
                    }
                }

                @Override
                public void onStatusChanged(String p, int status, Bundle extras) {
                }

                @Override
                public void onProviderEnabled(String p) {
                }

                @Override
                public void onProviderDisabled(String p) {
                }
            };
            manager.requestLocationUpdates(provider, 0, 0, listener);
        } catch (RuntimeException e) {
        }
    }

    public static double km(double lat1, double lng1, double lat2, double lng2) {
        float[] result = new float[1];
        Location.distanceBetween(lat1, lng1, lat2, lng2, result);
        return result[0] / 1000.0;
    }

    public static double kmFromHere(Context c, double lat, double lng) {
        double[] here = here(c);
        return km(here[0], here[1], lat, lng);
    }

    public static String kmText(double km) {
        if (km < 10) {
            return String.format(Locale.US, "%.1f km", km);
        }
        return String.format(Locale.US, "%.0f km", km);
    }
}
