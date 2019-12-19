package com.cc.yash.firestoreconnection;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class Main3Activity extends AppCompatActivity {

    FirebaseFirestore db = FirebaseFirestore.getInstance();

    EditText rangeValue;
    Button searchButton;
    private double currentLatitude;
    private double currentLongitude;
    private double currentRangeOfQuery;
    private TextView LatValue, LongValue, listOfPeople;
    public static String docIds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        searchButton = findViewById(R.id.searchButton);
        rangeValue = findViewById(R.id.rangeValue);

        currentRangeOfQuery = 10;

        //locationStuff
        LatValue = findViewById(R.id.LatValue);
        LongValue = findViewById(R.id.LongValue);
        listOfPeople = findViewById(R.id.listOfPeople);


        searchButton.setOnClickListener(view -> {
            String rangeVal = rangeValue.getText().toString();
            currentRangeOfQuery = Double.valueOf(rangeVal);
            reader(currentRangeOfQuery);
        });

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






    }

    private void reader(double currentRangeOfQuery) {


        //get last location

        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    // Got last known location. In some rare situations this can be null.
                    if (location != null) {
                        currentLatitude = location.getLatitude();
                        currentLongitude = location.getLongitude();
                        LongValue.setText(getApplicationContext().getString(R.string.longitude, currentLongitude));
                        LatValue.setText(getApplicationContext().getString(R.string.latitude, currentLatitude));
                        Map<String, Object> user = new HashMap<>();
                        user.put("Latitude", location.getLatitude());
                        user.put("Longitude", location.getLongitude());
                        user.put("Name", "Jacob");

                        db.collection("users")
                                .document("Jacob")
                                .set(user)
                                .addOnSuccessListener(aVoid -> {

                                });

                        //query on latitude values range
                        //each latitude degree is 110km. so we get our range accordingly, in km
                        db.collection("users")
                                .whereLessThanOrEqualTo("Latitude",currentLatitude+currentRangeOfQuery*(1/110.0))
                                .whereGreaterThanOrEqualTo("Latitude",currentLatitude-currentRangeOfQuery*(1/110.0))
                                .get()
                                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                    final Set<String> listOfNames = new HashSet<>();
                                    @Override
                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {

                                        if (task.isSuccessful()) {


                                            for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
                                                Log.d("FIRESTOREMESSAGE1 LatitudeValues", document.getId() + " => " + document.get("Name"));

                                                listOfNames.add((String)document.get("Name"));
                                                //System.out.println("THE LAT IDS ARE"+latIds);


                                            }

                                            Log.d("LongitudeList",listOfNames.toString());
                                        }

                                        //inside it we are doing a query on longittude values range
                                        //each longitude degree value depens on the current latitude observed.
                                        //so we caluclate value of 1 degree at that latitude and then multiply by required range km

                                        db.collection("users")
                                                .whereLessThanOrEqualTo("Longitude",currentLongitude+currentRangeOfQuery*(1/valueOfOneLongitude(currentLatitude)))
                                                .whereGreaterThanOrEqualTo("Longitude",currentLongitude-currentRangeOfQuery*(1/valueOfOneLongitude(currentLatitude)))
                                                .get()
                                                .addOnCompleteListener(task1 -> {
                                                    Set<String> listOfNames2=new HashSet<>();

                                                    for (QueryDocumentSnapshot document : Objects.requireNonNull(task1.getResult())) {
                                                        Log.d("FIRESTOREMESSAGE1 LatitudeValues", document.getId() + " => " + document.get("Name"));

                                                        listOfNames2.add((String)document.get("Name"));
                                                    }

                                                    Log.d("LatitudeList",listOfNames2.toString());
                                                    listOfNames2.retainAll(listOfNames);

                                                    Log.d("CompiledList",listOfNames2.toString());


                                                    StringBuilder toShow= new StringBuilder();
                                                    for(String x: listOfNames2){
                                                        toShow.append(x).append("\n");
                                                    }
                                                    listOfPeople.setText(getApplicationContext().getString(R.string.displayTestMessage,currentRangeOfQuery, toShow.toString()));
                                                });
                                    }

                                });

                        Log.d("DOCREF2", "The value is " + docIds);


                    }
                });
    }


    public static double valueOfOneLongitude(double latitude){
        double value=Math.cos(latitude * Math.PI / 180);
        //reference:https://www.space.com/17638-how-big-is-earth.html
        double radiusOfEarth = 6356.0;
        value=value*Math.PI/180* radiusOfEarth;

        return value;

    }

    @Override
    protected void onResume() {
        super.onResume();

    }

}
