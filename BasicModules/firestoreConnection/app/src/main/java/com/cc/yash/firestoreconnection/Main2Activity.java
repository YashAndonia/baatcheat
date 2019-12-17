package com.cc.yash.firestoreconnection;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;


import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
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

public class Main2Activity extends AppCompatActivity implements SetLocationListener{

    FirebaseFirestore db = FirebaseFirestore.getInstance();

    private double currentLatitude;
    private double currentLongitude;
    private double currentRangeOfQuery;
    private TextView LatValue,LongValue,listOfPeople;
    private CollectionReference myCollection = db.collection("users");
    private GeoFire geoFire = new GeoFire(myCollection);

    public Main2Activity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        currentRangeOfQuery=10;

        LatValue=findViewById(R.id.LatValue);
        LongValue=findViewById(R.id.LongValue);
        listOfPeople=findViewById(R.id.listOfPeople);

        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);


        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    // Got last known location. In some rare situations this can be null.
                    currentLatitude=location.getLatitude();
                    currentLongitude=location.getLongitude();
                    LongValue.setText(getApplicationContext().getString(R.string.longitude,currentLongitude));
                    LatValue.setText(getApplicationContext().getString(R.string.latitude,currentLongitude));
                    Map<String,Object> user=new HashMap<>();
                    user.put("Latitude",location.getLatitude());
                    user.put("Longitude",location.getLongitude());
                    user.put("Name","Johnny");
                    db.collection("users")
                            .add(user)
                            .addOnSuccessListener(documentReference -> geoFire.setLocation(documentReference.getId(),currentLatitude,currentLongitude,null));
                    reader();


                });

    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    public void onCompleted(Exception exception) {
    }

    public void reader(){

        QueryLocation queryLocation = QueryLocation.fromDegrees(currentLatitude, currentLongitude);

        Distance searchDistance = new Distance(1000.0, DistanceUnit.KILOMETERS);
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
