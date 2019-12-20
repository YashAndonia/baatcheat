package com.anxit.baatcheat.integratedSearch;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.anxit.baatcheat.R;
import com.anxit.baatcheat.adapters.RecyclerViewAdapter;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


public class IntegratedSearch extends AppCompatActivity {


    //Constants:
    private static final String TAG = "CustomLog";
    private static final String INTEREST_KEY = "interests";
    private static final String NAME_KEY = "name";

    ArrayList<String> usernameList;
    RecyclerViewAdapter usernameListAdapter;

    //UI Elements:
    EditText interestEditText;
    RecyclerView usernameRecyclerView;

    //Authentication:
    FirebaseUser currentUser;

    //location:

    private double currentLatitude;
    private double currentLongitude;
    private double currentRangeOfQuery;

    FirebaseFirestore db = FirebaseFirestore.getInstance();







    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_integrated_search);
        //Initializing UI Elements:
        interestEditText = findViewById(R.id.interest_edittext);


        initializeRecyclerView();

        currentUser  = FirebaseAuth.getInstance().getCurrentUser();
        currentRangeOfQuery=200.0;



        //ocationstuff

        //current Location finding:


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
                Log.d("LOCATIONERROR","GPS probably off");
        else {

            Log.d("getLastKnownLocation","We are here");
            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        // Got last known location. In some rare situations this can be null.
                        if(location!=null) {
                            currentLatitude = location.getLatitude();
                            currentLongitude = location.getLongitude();
                        }})
                    .addOnFailureListener(e -> Log.d("FAILUREMESSAGE", "Message is ::" + e));

        }


    }

    private void initializeRecyclerView(){
        usernameList = new ArrayList<>();

        usernameRecyclerView = findViewById(R.id.username_recyclerview);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        usernameRecyclerView.setLayoutManager(layoutManager);

        usernameListAdapter = new RecyclerViewAdapter(usernameList);
        usernameRecyclerView.setAdapter(usernameListAdapter);



    }

    public void findPeople(View v){
        //getting interest based search and inserting that data

        //arraylist for recyvleView
        usernameList.clear();
        String[] interestString = interestEditText.getText().toString().split(",");
        if(interestString.length==0){
            Toast.makeText(this, "Nope", Toast.LENGTH_SHORT).show();
            return;
        }
        ArrayList interests = new ArrayList<>(Arrays.asList(interestEditText.getText().toString().split(",")));
        if(interests.isEmpty()) {
            Toast.makeText(this, "Nope", Toast.LENGTH_SHORT).show();
            return;
        }

        //merging
        DocumentReference userDocumentReference = FirebaseFirestore.getInstance().document("users/" + currentUser.getEmail());

//        CollectionReference userCollectionReference = FirebaseFirestore.getInstance().collection("users");


        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {

                            if (location != null) {
                                currentLatitude = location.getLatitude();
                                currentLongitude = location.getLongitude();
                                Map<String, Object> user = new HashMap<>();
                                user.put("Latitude", location.getLatitude());
                                user.put("Longitude", location.getLongitude());
                                user.put(INTEREST_KEY, interests);
                                user.put(NAME_KEY, Objects.requireNonNull(currentUser.getDisplayName()));
                                user.put("email", Objects.requireNonNull(currentUser.getEmail()));

                                //adding current interests to the database
                                userDocumentReference
                                        .set(user)
                                        .addOnSuccessListener(aVoid -> Log.i(TAG, "findPeople: " + "Interest saved successfully"))
                                        .addOnFailureListener(e -> Log.e(TAG, "findPeople: " + "Could not save data + " + e.toString()));
                                //query on latitude values range
                                //each latitude degree is 110km. so we get our range accordingly, in km
                                db.collection("users")
                                        .whereLessThanOrEqualTo("Latitude", currentLatitude + currentRangeOfQuery * (1 / 110.0))
                                        .whereGreaterThanOrEqualTo("Latitude", currentLatitude - currentRangeOfQuery * (1 / 110.0))
                                        .get()
                                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                            final Set<User> setOfUsers = new HashSet<>();
                                            final Set<String> setOfEmails = new HashSet<>();

                                            @Override
                                            public void onComplete(@NonNull Task<QuerySnapshot> task) {

                                                if (task.isSuccessful()) {


                                                    for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
                                                        Log.d("FIRESTOREMESSAGE1", document.getId() + " => " + document.get("Name"));

                                                        if(document.get("email")!=currentUser.getEmail()){
                                                            setOfUsers.add(new User(document.getData()));
                                                            setOfEmails.add(new User(document.getData()).getEmail());
                                                            //System.out.println("THE LAT IDS ARE"+latIds);


                                                        }

                                                    }

                                                    Log.d("LongitudeList", setOfEmails.toString());
                                                }

                                                //inside it we are doing a query on longittude values range
                                                //each longitude degree value depens on the current latitude observed.
                                                //so we caluclate value of 1 degree at that latitude and then multiply by required range km

                                                db.collection("users")
                                                        .whereLessThanOrEqualTo("Longitude", currentLongitude + currentRangeOfQuery * (1 / valueOfOneLongitude(currentLatitude)))
                                                        .whereGreaterThanOrEqualTo("Longitude", currentLongitude - currentRangeOfQuery * (1 / valueOfOneLongitude(currentLatitude)))
                                                        .get()
                                                        .addOnCompleteListener(task1 -> {
                                                            //Set<User> setOfUsers2 = new HashSet<>();
                                                            Set<String> setOfEmails2=new HashSet<>();

                                                            for (QueryDocumentSnapshot document : Objects.requireNonNull(task1.getResult())) {
                                                                Log.d("FIRESTOREMESSAGE1", document.getId() + " => " + document.get("Name"));
                                                                if(document.get("email")!=currentUser.getEmail()){
                                                                    //setOfUsers2.add(new User(document.getData()));
                                                                    setOfEmails2.add(new User(document.getData()).getEmail());

                                                                }

                                                            }

                                                            Log.d("LatitudeList", setOfEmails2.toString());
                                                            //only for now
                                                            setOfEmails2.retainAll(setOfEmails);
                                                            setOfEmails2.remove(null);
                                                            Log.d("CompiledList", setOfEmails2.toString());


                                                            StringBuilder toShow = new StringBuilder();
                                                            for (String x : setOfEmails2) {
                                                                toShow.append(x).append("\n");
                                                            }
                                                            Log.d("GeoFiltered", getApplicationContext().getString(R.string.displayTestMessage, currentRangeOfQuery, setOfEmails2.toString()));


                                                            Set<User> compiledSet=new HashSet<>();

                                                            String[] interestString = interestEditText.getText().toString().split(",");

                                                            //this is the final set of users, in case we want any info from it.
//                                                            Set<User>finalSetOfUsers=new HashSet<>();

                                                            for(User x:setOfUsers){
                                                                if (setOfEmails2.contains(x.getEmail())){
                                                                    compiledSet.add(x);
                                                                }
                                                            }
                                                            for (User x:compiledSet){
                                                                ArrayList<String> interests=x.getInterests();
                                                                //check if this guy's interest present in interest array
                                                                int count=0;
                                                                for(String interestValue: interestString){
                                                                    if(interests.contains(interestValue)){
                                                                        count++;
                                                                    }
                                                                }
                                                                if(count>0){
                                                                   // finalSetOfUsers.add(x);
                                                                    usernameList.add(x.getName());
                                                                }
                                                            }

                                                            Log.d("userNameLiat",usernameList.toString());

                                                            usernameListAdapter.notifyDataSetChanged();






                                                        });
                                            }

                                        });

                                //Log.d("DOCREF2", "The value is " + docIds);


                            }
                        });

                        /*
                        //checking for people with same interests+latitude
                        for(int i=0; i<interests.size(); i++) {
                            userCollectionReference
                                    .whereArrayContains("interests", interests.get(i))
                                    .whereLessThanOrEqualTo("Latitude",currentLatitude+currentRangeOfQuery*(1/110.0))
                                    .whereGreaterThanOrEqualTo("Latitude",currentLatitude-currentRangeOfQuery*(1/110.0))
                                    .get()
                                    .addOnSuccessListener(queryDocumentSnapshots -> {


                                        final Set<String> setOfNames = new HashSet<>();
                                ArrayList<DocumentSnapshot> documents;
                                documents = (ArrayList<DocumentSnapshot>) queryDocumentSnapshots.getDocuments();//get each document of interest i
                                for(int i1 = 0; i1 <documents.size(); i1++){
                                    Log.i(TAG, "onSuccess: "+documents.get(i1).get("name"));//get the name of guys with shared interest i
                                    //noinspection SuspiciousMethodCalls
                                    if(!Objects.equals(documents.get(i1).get("name"), currentUser.getDisplayName())
                                            && !usernameList.contains(documents.get(i1).get("name"))) {
                                        //usernameList.add((String)documents.get(i1).get("name"));

                                        setOfNames.add((String)documents.get(i1).get("name"));
                                        Log.d("CurrentSet",setOfNames.toString());
                                    }
                                }



                                //cross refereing with longitude


                                        db.collection("users")
                                                .whereLessThanOrEqualTo("Longitude",currentLongitude+currentRangeOfQuery*(1/valueOfOneLongitude(currentLatitude)))
                                                .whereGreaterThanOrEqualTo("Longitude",currentLongitude-currentRangeOfQuery*(1/valueOfOneLongitude(currentLatitude)))
                                                .get()
                                                .addOnCompleteListener(task1 -> {
                                                    Set<String> setOfNames2=new HashSet<>();

                                                    for (QueryDocumentSnapshot document : Objects.requireNonNull(task1.getResult())) {
                                                        Log.d("FIRESTOREMESSAGE1", document.getId() + " => " + document.get("Name"));

                                                        setOfNames2.add((String)document.get("Name"));
                                                    }

                                                    Log.d("LatitudeList",setOfNames2.toString());
                                                    setOfNames2.retainAll(setOfNames);

                                                    Log.d("CompiledList",setOfNames2.toString());


                                                    StringBuilder toShow= new StringBuilder();
                                                    for(String x: setOfNames2){
                                                        toShow.append(x).append("\n");
                                                    }
                                                    Log.d("GeoFiltered",getApplicationContext().getString(R.string.displayTestMessage,currentRangeOfQuery, toShow.toString()));


                                                    //converting the merged set into an arraylist
                                                    usernameList.addAll(setOfNames2);
                                                    usernameList.remove(currentUser.getDisplayName());
                                                    usernameListAdapter.notifyDataSetChanged();
                                                });

                            });
                        }

                    }
                });*/


/*

        //FireStore:
        DocumentReference userDocumentReference = FirebaseFirestore.getInstance().document("users/" + currentUser.getEmail());
        CollectionReference userCollectionReference = FirebaseFirestore.getInstance().collection("users");

        Map<String, Object> userData = new HashMap<>();

        userDocumentReference.set(userData).addOnSuccessListener(aVoid -> Log.i(TAG, "findPeople: "+"Interest saved successfully")).addOnFailureListener(e -> Log.e(TAG, "findPeople: "+"Could not save data + "+e.toString()));

        for(int i=0; i<interests.size(); i++) {
            userCollectionReference
                    .whereArrayContains("interests", interests.get(i))
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {

                        final Set<String> setOfNames = new HashSet<>();
                ArrayList<DocumentSnapshot> documents;
                documents = (ArrayList<DocumentSnapshot>) queryDocumentSnapshots.getDocuments();//get each document of interest i
                for(int i1 = 0; i1 <documents.size(); i1++){
                    Log.i(TAG, "onSuccess: "+documents.get(i1).get("name"));//get the name of guys with shared interest i
                    //noinspection SuspiciousMethodCalls
                    if(!Objects.equals(documents.get(i1).get("name"), currentUser.getDisplayName())
                            && !usernameList.contains(documents.get(i1).get("name"))) {

                        setOfNames.add((String)documents.get(i1).get("name"));
                        //usernameList.add((String)documents.get(i1).get("name"));
                    }
                }


                        db.collection("users")
                                .whereLessThanOrEqualTo("Longitude",currentLongitude+currentRangeOfQuery*(1/valueOfOneLongitude(currentLatitude)))
                                .whereGreaterThanOrEqualTo("Longitude",currentLongitude-currentRangeOfQuery*(1/valueOfOneLongitude(currentLatitude)))
                                .get()
                                .addOnCompleteListener(task1 -> {
                                    Set<String> setOfNames2=new HashSet<>();

                                    for (QueryDocumentSnapshot document : Objects.requireNonNull(task1.getResult())) {
                                        Log.d("FIRESTOREMESSAGE1", document.getId() + " => " + document.get("Name"));

                                        setOfNames2.add((String)document.get("Name"));
                                    }

                                    Log.d("LatitudeList",setOfNames2.toString());
                                    setOfNames2.retainAll(setOfNames);

                                    Log.d("CompiledList",setOfNames2.toString());


                                    StringBuilder toShow= new StringBuilder();
                                    for(String x: setOfNames2){
                                        toShow.append(x).append("\n");
                                    }
                                    Log.d("GeoFiltered",getApplicationContext().getString(R.string.displayTestMessage,currentRangeOfQuery, toShow.toString()));


                                    //converting the merged set into an arraylist
                                    usernameList.addAll(setOfNames2);
                                });

            });
        }
        usernameList.remove(currentUser.getDisplayName());
        usernameListAdapter.notifyDataSetChanged();



        //get last location

        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    // Got last known location. In some rare situations this can be null.
                    if (location != null) {
                        currentLatitude = location.getLatitude();
                        currentLongitude = location.getLongitude();
                        Map<String, Object> user = new HashMap<>();
                        user.put("Latitude", location.getLatitude());
                        user.put("Longitude", location.getLongitude());
                        user.put(INTEREST_KEY, interests);
                        user.put(NAME_KEY, Objects.requireNonNull(currentUser.getDisplayName()));


                        db.collection("users")
                                .document("Jacob")
                                .set(user)
                                .addOnSuccessListener(aVoid -> Log.i(TAG, "findPeople: "+"Interest saved successfully"))
                                .addOnFailureListener(e -> Log.e(TAG, "findPeople: "+"Could not save data + "+e.toString()));

                        //query on latitude values range
                        //each latitude degree is 110km. so we get our range accordingly, in km
                        db.collection("users")
                                .whereLessThanOrEqualTo("Latitude",currentLatitude+currentRangeOfQuery*(1/110.0))
                                .whereGreaterThanOrEqualTo("Latitude",currentLatitude-currentRangeOfQuery*(1/110.0))
                                .get()
                                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                    final Set<String> setOfNames = new HashSet<>();
                                    @Override
                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {

                                        if (task.isSuccessful()) {


                                            for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
                                                Log.d("FIRESTOREMESSAGE1", document.getId() + " => " + document.get("Name"));

                                                setOfNames.add((String)document.get("Name"));
                                                //System.out.println("THE LAT IDS ARE"+latIds);


                                            }

                                            Log.d("LongitudeList",setOfNames.toString());
                                        }

                                        //inside it we are doing a query on longittude values range
                                        //each longitude degree value depens on the current latitude observed.
                                        //so we caluclate value of 1 degree at that latitude and then multiply by required range km

                                        db.collection("users")
                                                .whereLessThanOrEqualTo("Longitude",currentLongitude+currentRangeOfQuery*(1/valueOfOneLongitude(currentLatitude)))
                                                .whereGreaterThanOrEqualTo("Longitude",currentLongitude-currentRangeOfQuery*(1/valueOfOneLongitude(currentLatitude)))
                                                .get()
                                                .addOnCompleteListener(task1 -> {
                                                    Set<String> setOfNames2=new HashSet<>();

                                                    for (QueryDocumentSnapshot document : Objects.requireNonNull(task1.getResult())) {
                                                        Log.d("FIRESTOREMESSAGE1", document.getId() + " => " + document.get("Name"));

                                                        setOfNames2.add((String)document.get("Name"));
                                                    }

                                                    Log.d("LatitudeList",setOfNames2.toString());
                                                    setOfNames2.retainAll(setOfNames);

                                                    Log.d("CompiledList",setOfNames2.toString());


                                                    StringBuilder toShow= new StringBuilder();
                                                    for(String x: setOfNames2){
                                                        toShow.append(x).append("\n");
                                                    }
                                                    Log.d("GeoFiltered",getApplicationContext().getString(R.string.displayTestMessage,currentRangeOfQuery, toShow.toString()));
                                                });
                                    }

                                });

                        Log.d("DOCREF2", "The value is " + docIds);


                    }
                });*/
    }


    public static double valueOfOneLongitude(double latitude){
        double value=Math.cos(latitude * Math.PI / 180);
        //reference:https://www.space.com/17638-how-big-is-earth.html
        double radiusOfEarth = 6356.0;
        value=value*Math.PI/180* radiusOfEarth;

        return value;

    }

}
