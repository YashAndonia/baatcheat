package com.anxit.baatcheat.geolocationSearch;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.anxit.baatcheat.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import uk.co.mgbramwell.geofire.android.GeoFire;
import uk.co.mgbramwell.geofire.android.listeners.SetLocationListener;
import uk.co.mgbramwell.geofire.android.model.Distance;
import uk.co.mgbramwell.geofire.android.model.DistanceUnit;
import uk.co.mgbramwell.geofire.android.model.QueryLocation;

public class geolocationSearch extends AppCompatActivity implements SetLocationListener {

    FirebaseFirestore db = FirebaseFirestore.getInstance();

    EditText rangeValue;
    Button searchButton;
    private double currentLatitude;
    private double currentLongitude;
    private double currentRangeOfQuery;
    private TextView LatValue,LongValue,listOfPeople;
    private CollectionReference myCollection = db.collection("users");
    private GeoFire geoFire = new GeoFire(myCollection);



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_geolocation_search);

        searchButton=findViewById(R.id.searchButton);
        rangeValue=findViewById(R.id.rangeValue);

        currentRangeOfQuery=10;

        //locationStuff
        LatValue=findViewById(R.id.LatValue);
        LongValue=findViewById(R.id.LongValue);
        listOfPeople=findViewById(R.id.listOfPeople);


        //request location first
        //https://stackoverflow.com/a/50448772/7406257
        LocationRequest mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(60000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationCallback mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
            }
        };
        LocationServices.getFusedLocationProviderClient(getApplicationContext()).requestLocationUpdates(mLocationRequest, mLocationCallback, null);


        //update of location and send to database

        Log.d("getLastKnownLocation","We are here");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            return;
        else {

            Log.d("getLastKnownLocation","We are here");
            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        // Got last known location. In some rare situations this can be null.
                        if(location!=null) {
                            currentLatitude = location.getLatitude();
                            currentLongitude = location.getLongitude();
                            LongValue.setText(getApplicationContext().getString(R.string.longitude, currentLongitude));
                            LatValue.setText(getApplicationContext().getString(R.string.latitude, currentLongitude));
                            Map<String, Object> user = new HashMap<>();
                            user.put("Latitude", location.getLatitude());
                            user.put("Longitude", location.getLongitude());
                            user.put("Name", "Johnny");
                            db.collection("users")
                                    .add(user)
                                    .addOnSuccessListener(documentReference -> geoFire.setLocation(documentReference.getId(), currentLatitude, currentLongitude, null));
                        }})
                    .addOnFailureListener(e -> Log.d("FAILUREMESSAGE", "Message is ::" + e));
        }

//search people nearby using this
        searchButton.setOnClickListener(view -> {
            String rangeVal=rangeValue.getText().toString();
            currentRangeOfQuery=Double.valueOf(rangeVal);
            reader(currentRangeOfQuery);
        });


    }
    @Override
    public void onCompleted(Exception exception) {

    }

    public void reader(double currentRangeOfQuery){

        Log.d("LONG1",getApplicationContext().getString(R.string.longitude,currentLongitude));
        Log.d("LAT1",getApplicationContext().getString(R.string.latitude,currentLatitude));

        QueryLocation queryLocation = QueryLocation.fromDegrees(currentLatitude, currentLongitude);

        Distance searchDistance = new Distance(currentRangeOfQuery, DistanceUnit.KILOMETERS);
        geoFire.query()
                .whereNearTo(queryLocation,searchDistance)
                .limit(10)
                .build()
                .get()
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {


                        Set<String> listOfNames=new HashSet<>();

                        for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
                            Log.d("QueryMessage1", document.getId() + " => " + document.get("Name"));

                            listOfNames.add((String)document.get("Name"));


                        }

                        StringBuilder toShow= new StringBuilder("\n");
                        for(String x: listOfNames){
                            toShow.append(x).append("\n");
                        }
                        listOfPeople.setText(getApplicationContext().getString(R.string.displayTestMessage,currentRangeOfQuery, toShow.toString()));
                    }else{

                        Log.d("QueryMessaage2", "Error getting documents: ", task.getException());
                    }
                });
    }





}
