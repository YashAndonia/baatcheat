package com.anxit.baatcheat.geolocationSearch;

import android.location.Location;
import android.os.Bundle;

import com.anxit.baatcheat.R;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import java.util.Set;

public class geolocationSearch extends AppCompatActivity {

    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private double currentLatitude;
    private double currentLongitude;
    private TextView LatValue,LongValue,listOfPeople;

    private FirebaseFirestore fireStoreDatabase;

    public static Set<String> latIds;
    public static Set<String> longIds;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_geolocation_search);

        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        Log.d("Latitude"," "+location.getLatitude());
                        Log.d("Longitude"," "+location.getLongitude());

                    }
                });
    }


}
