package com.onekosmos.blockidsample.util;

import android.Manifest;
import android.app.Activity;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.Task;

import java.text.DateFormat;
import java.util.Date;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
public class CurrentLocationHelper {
    private static final long INTERVAL = 1000 * 10;
    private static final long FASTEST_INTERVAL = 1000 * 5;
    private final static int REQUEST_CHECK_SETTINGS_GPS = 0x1;

    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;
    private LocationRequest mLocationRequest;
    private Location mCurrentLocation;
    private String mLastUpdateTime;
    private Activity activity;
    private LocationSettingsCallback settingsCallback;

    /**
     * Callback interface for location settings resolution
     */
    public interface LocationSettingsCallback {
        void onLocationSettingsResolutionRequired(ResolvableApiException exception);
    }

    public CurrentLocationHelper(Activity activity) {
        this.activity = activity;
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(activity);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    onLocationChanged(location);
                }
            }
        };
    }

    /**
     * Set callback for location settings resolution
     *
     * @param callback Callback to handle settings resolution
     */
    public void setLocationSettingsCallback(LocationSettingsCallback callback) {
        this.settingsCallback = callback;
    }

    public void createLocationRequest() {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    public boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int status = googleApiAvailability.isGooglePlayServicesAvailable(activity);
        if (status == ConnectionResult.SUCCESS) {
            return true;
        } else {
            if (googleApiAvailability.isUserResolvableError(status)) {
                googleApiAvailability.getErrorDialog(activity, status, 0).show();
            }
            return false;
        }
    }

    /**
     * Start location updates
     * Call this method after permissions are granted
     */
    public void startLocationUpdates() {
        if (mLocationRequest == null) {
            createLocationRequest();
        }

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

        SettingsClient settingsClient = LocationServices.getSettingsClient(activity);
        Task<LocationSettingsResponse> task = settingsClient.checkLocationSettings(builder.build());

        task.addOnSuccessListener(activity, locationSettingsResponse -> {
            // Location settings are satisfied, start location updates
            requestLocationUpdates();
        });

        task.addOnFailureListener(activity, e -> {
            // Location settings are not satisfied
            if (e instanceof ResolvableApiException) {
                ResolvableApiException resolvable = (ResolvableApiException) e;
                if (settingsCallback != null) {
                    // Notify the callback to handle resolution
                    settingsCallback.onLocationSettingsResolutionRequired(resolvable);
                } else {
                    // Fallback: try to show dialog directly (old behavior)
                    try {
                        resolvable.startResolutionForResult(activity, REQUEST_CHECK_SETTINGS_GPS);
                    } catch (IntentSender.SendIntentException sendEx) {
                        // Ignore the error
                    }
                }
            }
        });
    }

    /**
     * Call this method after location settings have been resolved successfully
     * This should be called from the activity's onActivityResult or ActivityResultLauncher callback
     */
    public void onLocationSettingsResolved() {
        // Retry requesting location updates after settings are resolved
        requestLocationUpdates();
    }

    /**
     * Request location updates from FusedLocationProviderClient
     * Made public so it can be called after settings resolution
     */
    public void requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback,
                Looper.getMainLooper());
    }

    private void onLocationChanged(Location location) {
        mCurrentLocation = location;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        getLocation();
    }

    public Location getLocation() {
        if (null != mCurrentLocation) {
            return mCurrentLocation;
        } else {
            return null;
        }
    }

    public void stopLocationUpdates() {
        if (mFusedLocationClient != null && mLocationCallback != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
    }
}