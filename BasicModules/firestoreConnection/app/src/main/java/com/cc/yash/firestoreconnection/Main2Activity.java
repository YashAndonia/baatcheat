package com.cc.yash.firestoreconnection;

import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Main2Activity extends AppCompatActivity {

    FirebaseFirestore db = FirebaseFirestore.getInstance();

    //Define a request code to send to Google Play services
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
        setContentView(R.layout.activity_main2);


        LatValue=findViewById(R.id.LatValue);
        LongValue=findViewById(R.id.LongValue);
        listOfPeople=findViewById(R.id.listOfPeople);

        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fireStoreDatabase = FirebaseFirestore.getInstance();


        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        currentLatitude=location.getLatitude();
                        currentLongitude=location.getLongitude();
                        LongValue.setText("Longitude: "+currentLongitude);
                        LatValue.setText("Latitude: "+currentLatitude);
                        Map<String,Object> users=new HashMap<>();
                        users.put("Latitude",location.getLatitude());
                        users.put("Longitude",location.getLongitude());
                        users.put("NAME","HAHAHAHA");
                        db.collection("users")
                                .add(users);
                        displayList();


                    }
                });

    }

    @Override
    protected void onResume() {
        super.onResume();
        displayList();

    }

    public void displayList(){
        longIds=new HashSet<>();
        latIds=new HashSet<>();
        //matching latitudes

        fireStoreDatabase.collection("users")
                .whereLessThanOrEqualTo("Latitude",currentLatitude+10)
                .whereGreaterThanOrEqualTo("Latitude",currentLatitude-10)
                .whereEqualTo("Longitude",currentLongitude)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {

                        if (task.isSuccessful()) {

                            Set<String> listOfNames=new HashSet<String>();

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d("FIRESTOREMESSAGE5 LatitudeValues", document.getId() + " => " + document.get("Name"));

                                listOfNames.add((String)document.get("Name"));
                                //System.out.println("THE LAT IDS ARE"+latIds);


                            }

                            String toShow="";
                            for(String x: listOfNames){
                                toShow+=x+"\n";
                            }
                            listOfPeople.setText("The list of all people within 69km are:\n"+toShow);
                        }else{

                            Log.d("FIRESTOREMESSAGE5", "Error getting documents: ", task.getException());
                        }


                    }});
    }
}
