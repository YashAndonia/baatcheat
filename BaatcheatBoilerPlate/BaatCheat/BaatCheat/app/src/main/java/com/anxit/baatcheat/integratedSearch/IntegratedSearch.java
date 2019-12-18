package com.anxit.baatcheat.integratedSearch;

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
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import uk.co.mgbramwell.geofire.android.GeoFire;
import uk.co.mgbramwell.geofire.android.listeners.SetLocationListener;
import uk.co.mgbramwell.geofire.android.model.Distance;
import uk.co.mgbramwell.geofire.android.model.DistanceUnit;
import uk.co.mgbramwell.geofire.android.model.QueryLocation;

public class IntegratedSearch extends AppCompatActivity implements SetLocationListener {


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

    //private GeoFire geoFire = new GeoFire(myCollection);
    DocumentReference userDocumentReference;
    CollectionReference userCollectionReference;
    ArrayList interests;
    List<String> listOfGeoNames;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_integrated_search);
        //Initializing UI Elements:
        interestEditText = findViewById(R.id.interest_edittext);

        listOfGeoNames= new ArrayList<>();

        initializeRecyclerView();

        currentUser  = FirebaseAuth.getInstance().getCurrentUser();
        currentRangeOfQuery=1.0;
    }

    private void initializeRecyclerView(){
        usernameList = new ArrayList<>();

        usernameRecyclerView = findViewById(R.id.username_recyclerview);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        usernameRecyclerView.setLayoutManager(layoutManager);

        usernameListAdapter = new RecyclerViewAdapter(usernameList);
        usernameRecyclerView.setAdapter(usernameListAdapter);


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
                           /* LongValue.setText(getApplicationContext().getString(R.string.longitude, currentLongitude));
                            LatValue.setText(getApplicationContext().getString(R.string.latitude, currentLongitude));
                            Map<String, Object> user = new HashMap<>();
                            user.put("Latitude", location.getLatitude());
                            user.put("Longitude", location.getLongitude());
                            user.put("Name", "Johnny");
                            db.collection("users")
                                    .add(user)
                                    .addOnSuccessListener(documentReference -> geoFire.setLocation(documentReference.getId(), currentLatitude, currentLongitude, null));*/
                        }})
                    .addOnFailureListener(e -> Log.d("FAILUREMESSAGE", "Message is ::" + e));
        }

//search people nearby using this
        /*searchButton.setOnClickListener(view -> {
            String rangeVal=rangeValue.getText().toString();
            currentRangeOfQuery=Double.valueOf(rangeVal);
            reader(currentRangeOfQuery);
        });*/

    }

    public void findPeople(View v){
        usernameList.clear();
        String[] interestString = interestEditText.getText().toString().split(",");
        if(interestString.length==0){
            Toast.makeText(this, "Nope", Toast.LENGTH_SHORT).show();
            return;
        }
        interests = new ArrayList<>(Arrays.asList(interestEditText.getText().toString().split(",")));
        if(interests.isEmpty()) {
            Toast.makeText(this, "Nope", Toast.LENGTH_SHORT).show();
            return;
        }
        //FireStore:
        userDocumentReference = FirebaseFirestore.getInstance().document("users/" + currentUser.getEmail());
        userCollectionReference = FirebaseFirestore.getInstance().collection("users");

        Map<String, Object> userData = new HashMap<>();
        userData.put(INTEREST_KEY, interests);

        userData.put(NAME_KEY, Objects.requireNonNull(currentUser.getDisplayName()));
        userData.put("Latitude",currentLatitude);
        userData.put("Longitude",currentLongitude);

        //geoFire stuff:
        GeoFire geoFire=new GeoFire(FirebaseFirestore.getInstance().collection("users"));
/*

        //saving current search data of the user along with geofire data
        FirebaseFirestore.getInstance().document(Objects.requireNonNull(currentUser.getEmail())).set(userData).addOnSuccessListener(
                aVoid -> geoFire.setLocation(currentUser.getEmail(), currentLatitude, currentLongitude, null))
                .addOnFailureListener(e -> Log.e(TAG, "findPeople: "+"Could not save data + "+e.toString()));
*/
        FirebaseFirestore.getInstance().collection("users")
                .add(userData)
                .addOnSuccessListener(
                        documentReference -> geoFire.setLocation(documentReference.getId(), currentLatitude, currentLongitude, null))
                .addOnFailureListener(e -> Log.d("FAILUREMESSAGE", "Message is ::" + e));

        ///
        // as per suggestion, we should now read this document with this id, then we should write all the data to the document with
        ///our email id
        ///then we delete this new one!
        //https://stackoverflow.com/a/52117929/7406257

/*
        for(int i=0; i<interests.size(); i++) {
            userCollectionReference.whereArrayContains("interests", interests.get(i)).get().addOnSuccessListener(queryDocumentSnapshots -> {
                ArrayList<DocumentSnapshot> documents;
                documents = (ArrayList<DocumentSnapshot>) queryDocumentSnapshots.getDocuments();
                for(int i1 = 0; i1 <documents.size(); i1++){
                    Log.i(TAG, "onSuccess: "+documents.get(i1).get("name"));
                    //noinspection SuspiciousMethodCalls
                    if(!Objects.equals(documents.get(i1).get("name"), currentUser.getDisplayName())
                            && !usernameList.contains(documents.get(i1).get("name"))) {
                        usernameList.add((String)documents.get(i1).get("name"));
                    }
                }
            });
        }
        usernameList.remove(currentUser.getDisplayName());
        usernameListAdapter.notifyDataSetChanged();*/

        //geofireQuery:

        QueryLocation queryLocation = QueryLocation.fromDegrees(currentLatitude, currentLongitude);

        Distance searchDistance = new Distance(currentRangeOfQuery, DistanceUnit.KILOMETERS);


        geoFire.query()
                .whereNearTo(queryLocation,searchDistance)
                .limit(10)
                .build()
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {

                        for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
                            Log.d("QueryMessage1", document.getId() + " => " + document.get("Name"));

                            listOfGeoNames.add((String) document.get("Name"));
                        }


                            StringBuilder toShow= new StringBuilder("\n");
                            for(String x: listOfGeoNames){
                                toShow.append(x).append("\n");
                            }
                            Log.d("GeoList: ",getApplicationContext().getString(R.string.displayTestMessage,currentRangeOfQuery, toShow.toString()));


                    }
                });




        //now comparing wiht the interest list

        for(int i=0; i<interests.size(); i++) {
            userCollectionReference.whereArrayContains("interests", interests.get(i)).get().addOnSuccessListener(queryDocumentSnapshots -> {
                ArrayList<DocumentSnapshot> documents;
                documents = (ArrayList<DocumentSnapshot>) queryDocumentSnapshots.getDocuments();
                for(int i1 = 0; i1 <documents.size(); i1++){
                    Log.i(TAG, "onSuccess: "+documents.get(i1).get("name"));
                    //noinspection SuspiciousMethodCalls
                    if(!Objects.equals(documents.get(i1).get("name"), currentUser.getDisplayName())
                            && !usernameList.contains(documents.get(i1).get("name"))) {
                        usernameList.add((String)documents.get(i1).get("name"));
                    }
                }

                //for debugging
                StringBuilder toShow= new StringBuilder("\n");
                for(String x: usernameList){
                    toShow.append(x).append("\n");
                }
                Log.d("userNameList: ",getApplicationContext().getString(R.string.displayTestMessage2, toShow.toString()));
                //

            });
        }


        usernameList= (ArrayList<String>) intersection(usernameList,listOfGeoNames);

        usernameListAdapter.notifyDataSetChanged();

        //for debugging
        StringBuilder toShow= new StringBuilder("\n");
        for(String x: usernameList){
            toShow.append(x).append("\n");
        }
        Log.d("integratedList: ",getApplicationContext().getString(R.string.displayTestMessage3, toShow.toString()));
        //
    }

    @Override
    public void onCompleted(Exception exception) {


    }

    public <T> List<T> intersection(List<T> list1, List<T> list2) {
        List<T> list = new ArrayList<T>();

        for (T t : list1) {
            if(list2.contains(t)) {
                list.add(t);
            }
        }

        return list;
    }
}
