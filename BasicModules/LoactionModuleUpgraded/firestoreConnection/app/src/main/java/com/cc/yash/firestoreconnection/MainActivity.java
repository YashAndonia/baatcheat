package com.cc.yash.firestoreconnection;

//THE FIREBASE STRUCTURE:
//https://console.firebase.google.com/u/0/project/locationfirebaseconnection/database/locationfirebaseconnection/data


//the firestore structure:
//https://console.firebase.google.com/u/0/project/locationfirebaseconnection/database/firestore/data~2Fusers~2FhtwYeSHEJjpsPPjZSQRH

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {
    //refer this answer for details on how to find the ocation. I have essentially just done what this guy is saying
    //https://stackoverflow.com/a/35057648/7406257



    //default values to be used throughout

    //Define a request code to send to Google Play services
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private double currentLatitude,currentLatitudeRounded;
    private double currentLongitude;
    private TextView LatValue,LongValue,listOfPeople;

    private FirebaseFirestore fireStoreDatabase;

    public static Set<String> latIds;
    public static Set<String> longIds;

    public static String currentUserName="Johnny";
    //firebase stuff
    //https://firebase.google.com/docs/database/android/read-and-write
    private DatabaseReference mDatabase;

    //reference:https://www.space.com/17638-how-big-is-earth.html
    private static double radiusOfEarth=6356.0;

    private static double currentRequiredLongitudeRange=1.0;//we want a 1km range.



    public static double valueOfOneLongitude(double latitude){
        double value=Math.cos(latitude * Math.PI / 180);
        value=value*Math.PI/180*radiusOfEarth;

        return value;

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseApp.initializeApp(this);



        mGoogleApiClient = new GoogleApiClient.Builder(this)
                // The next two lines tell the new client that “this” current class will handle connection stuff
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                //fourth line adds the LocationServices API endpoint from GooglePlayServices
                .addApi(LocationServices.API)
                .build();

        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 1000)        // 10 seconds, in milliseconds
                .setFastestInterval(1 * 1000); // 1 second, in milliseconds
        LatValue=findViewById(R.id.LatValue);
        LongValue=findViewById(R.id.LongValue);

        listOfPeople=findViewById(R.id.listOfPeople);



//        mDatabase= FirebaseDatabase.getInstance().getReference();



        /*PART 2
         * attempt at getting the location  data of other guys
         * reference:
         * https://firebase.google.com/docs/database/android/lists-of-data#sort_data
         * https://github.com/firebase/snippets-android/blob/90e205c8b1a72c862095ad04f028afd3bfe00d15/database/app/src/main/java/com/google/firebase/referencecode/database/QueryActivity.java#L99-L112
         *
         * */

        //ALso see this:
        //https://firebase.google.com/docs/database/android/lists-of-data#filtering_data
        //how to handle complex queries
        // APPARENTLY CLUD FIRESTORE IS THE BEST
        //https://github.com/davideast/Querybase see the eradme
        //

        /*
        Query valueProviderQuery=mDatabase.child("Users").orderByChild("");

        valueProviderQuery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {



            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
*/


// Access a Cloud Firestore instance from your Activity


        fireStoreDatabase = FirebaseFirestore.getInstance();



    }

    @Override
    protected void onResume() {
        super.onResume();

        //Now lets connect to the API
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();

        Log.v(this.getClass().getSimpleName(), "Currently pausing!");

        //Disconnect from API onPause()
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }

    }

    @Override
    public void onConnected( Bundle bundle) {

        //why i have done this:
        //https://stackoverflow.com/questions/33327984/call-requires-permissions-that-may-be-rejected-by-user
        //https://developer.android.com/training/permissions/requesting.html


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {// Permission is not granted so request it


            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    1);

        }
        else if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {


            // Permission is not granted so request it
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1);
        }else {


            //now continuing with our earlier code
            //now using the location

            Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

            if (location == null) {
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

            } else {
                //If everything went fine lets get latitude and longitude
                currentLatitude = location.getLatitude();
                currentLatitudeRounded=Math.round((currentLatitude*10e2))/10e2;//3 precision rounding due to limitation
                currentLongitude = location.getLongitude();

                //currentLongitude=Math.round(currentLongitude);
                Log.d("longitude stored rn",""+currentLongitude);

                LongValue.setText("Longitude: "+currentLongitude);
                LatValue.setText("Latitude: "+currentLatitudeRounded);

                Map<String,Object> users=new HashMap<>();
                users.put("Name",currentUserName);
                users.put("Latitude",currentLatitudeRounded);
                users.put("Longitude",currentLongitude);



                //PART 1
                //adding a new user on firestore:

/*
//i HAVE COMMENTED THIS FOR NOW BECAUSE EVERY TIME I START THE APP, IT ADDS A NEW PEROSN WITH THE SAME DETAILS/
////FOR UPDATION OF CURRENT USER VALUES, GO DOWN A BIT

                fireStoreDatabase.collection("users")
                        .add(users)
                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                            @Override
                            public void onSuccess(DocumentReference documentReference) {

                                Log.d("MESSAGE Firestore", "DocumentSnapshot added with ID: " + documentReference.getId());

                            }
                        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                        Log.w("MESSAGE Firestore", "Error adding document", e);

                    }
                })

                ;



*/
//updating a user on firestore


                fireStoreDatabase.collection("users")
                        .document().update(users);







/*

//THIS IS JUST A TEST OF HOW TO READ A BASIC FILE, NOT IN USE
                //READING THE SAME VALUES OF DOCUMENTS:
                fireStoreDatabase.collection("users")
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {

                                if (task.isSuccessful()) {
                                    for (QueryDocumentSnapshot document : task.getResult()) {
                                        Log.d("FIRESTOREMESSAGE3", document.getId() + " => " + document.getData());
                                    }

                                }else{

                                    Log.w("FIRESTOREMESSAGE4", "Error getting documents.", task.getException());

                                }
                        }});
*/






                // PART 2
                // to display the people NEAR you
                //https://gis.stackexchange.com/questions/142326/calculating-longitude-length-in-miles

//the limitation
// i cannot do where queries on multiple fields at teh same time!
//https://firebase.google.com/docs/firestore/query-data/queries#compound_queries
//SO ONLY ONE FIELD CAN BE EQUAL AND THE OTHER CAN BE >< OR SOMETHING


                //SO HERE IS WHAT WE WILL DO
                //ONE LATITUDE DEGREE IS 110.574  TO 111.694 KM
                //REFERENCE:
                //https://en.wikipedia.org/wiki/Longitude#Length_of_a_degree_of_longitude
                //SO  WE WILL TAKE AVERAGE.
                //1LATDEGREE=111.134km
                //THEREFORE, 0.01 LAT DGREES= 1.11134KM
                //so if i give a 10^3 precision to latitude, we will get to match all people
                // within 1.1km to the right and 1.1km to the left of the guy's position by
                //simply using the equal to query
                //so, that is why we are rounfing the latitude before sending it to firebase


                Log.d("current Latutiduee value",currentLatitude+"");


                //NOW what to do wuith longitude:
                //value of longitude varies by this 1st formula:
                //https://en.wikipedia.org/wiki/Longitude#Length_of_a_degree_of_longitude
                double valueOfOneDegreeLongitude=valueOfOneLongitude(currentLatitude);

                //how many degrees range in the longitude will we look over.
                //eg. i want 2 km range.
                //and 1 degree means 111.1km
                //therefore i will do 2*(1/111.1) degrees
                double degreeRangeToLookWithin=currentRequiredLongitudeRange/valueOfOneDegreeLongitude;

                Log.d("Range of longitude is:", degreeRangeToLookWithin+"");


                longIds=new HashSet<>();
                latIds=new HashSet<>();
                //matching latitudes and searching within a range of the longitude

                fireStoreDatabase.collection("users")
                        .whereLessThanOrEqualTo("Longitude",currentLongitude+degreeRangeToLookWithin)
                        .whereGreaterThanOrEqualTo("Longitude",currentLongitude-degreeRangeToLookWithin)
                        .whereEqualTo("Latitude",currentLatitudeRounded)
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {

                                if (task.isSuccessful()) {

                                    Set<String> listOfNames=new HashSet<String>();

                                    for (QueryDocumentSnapshot document : task.getResult()) {
                                        Log.d("FIRESTOREMESSAGE5 LatitudeValues", document.getId() + " => " + document.get("Name"));

                                        //ensuring thst the name displayed is not that of the user themsef
                                        if(!currentUserName.equals(document.get("Name"))) {
                                            listOfNames.add((String)document.get("Name"));
                                        }
                                        //System.out.println("THE LAT IDS ARE"+latIds);


                                    }

                                    String toShow="";
                                    for(String x: listOfNames){
                                        toShow+=x+"\n";
                                    }
                                    listOfPeople.setText("The list of all people within 1km radius are:\n"+toShow);
                                }else{

                                    Log.d("FIRESTOREMESSAGE5", "Error getting documents: ", task.getException());
                                }


                            }});


                //System.out.println("THE LAT IDS ARE1"+latIds);


                /*
                FOR NOW, WE IGNORE LONGITUDES DUE TO 2 REASONS:
                1) LONGTUDES VARY FROM 111.1 KM TO 0KM AS PER DIST FROM EQUATOR
                2) FIREBASE IS ANNOYING ME SINCE IT IS UNABLE TO DO MULTIPLE LEVEL >< QUERIES.
                WHY THIS IS IMPLEMENTED IS:
                https://stackoverflow.com/questions/54969057/firestore-why-do-all-where-filters-have-to-be-on-the-same-field


                //matching longitudes
                fireStoreDatabase.collection("users")
                        .whereLessThan("Longitude",currentLongitude+10)
                        .whereGreaterThan("Longitude",currentLongitude-10)
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {

                                if (task.isSuccessful()) {
                                    for (QueryDocumentSnapshot document : task.getResult()) {
                                        Log.d("FIRESTOREMESSAGE6 LongitudeValues", document.getId() + " => " + document.getData());
                                        longIds.add(document.getId());
                                        System.out.println("THE LONG IDS ARE"+longIds);

                                    }
                                }else{

                                    Log.d("FIRESTOREMESSAGE6", "Error getting documents: ", task.getException());
                                }

                            }});


*/

                ///

                //System.out.println("THE LONG IDS ARE "+latIds);




/*
                mDatabase.child("Location").child("Latitude").setValue(currentLatitude);
                mDatabase.child("Location").child("Longitude").setValue(currentLongitude);
*/
                Toast.makeText(this, currentLatitude + " WORKS " + currentLongitude + "", Toast.LENGTH_LONG).show();
            }


        }}

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
                /*
                 * Thrown if Google Play services canceled the original
                 * PendingIntent
                 */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.
             */
            Log.e("Error", "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }

    @Override
    public void onLocationChanged(Location location) {

        currentLatitude = location.getLatitude();
        currentLongitude = location.getLongitude();


        currentLatitudeRounded=Math.round((currentLatitude*10e2))/10e2;//3 precision rounding due to limitation

        LongValue.setText("Longitude: "+currentLongitude);
        LatValue.setText("Latitude: "+currentLatitudeRounded);

        Map<String,Object> users=new HashMap<>();
        users.put("Name",currentUserName);
        users.put("Latitude",currentLatitudeRounded);
        users.put("Longitude",currentLongitude);


//updating the user on firestore


        fireStoreDatabase.collection("users")
                .document().update(users);




/*
        mDatabase.child("Location").child("Latitude").setValue(currentLatitude);
        mDatabase.child("Location").child("Longitude").setValue(currentLongitude);
        Toast.makeText(this, currentLatitude + " WORKS " + currentLongitude + "", Toast.LENGTH_LONG).show();
*/
    }





    //
    // PART 2
    // references used to look at the userlist generation part:
    //https://www.captechconsulting.com/blogs/firebase-realtime-database-android-tutorial
    //https://stackoverflow.com/questions/51611680/query-firebase-to-return-if-value-more-than-number


    /*
     * NOW ATTEMPTING TO GET THE DATABASE TO SHOW THE VALUES ON THE PAGE OF PEOPLE NEARBY
     * */

}


