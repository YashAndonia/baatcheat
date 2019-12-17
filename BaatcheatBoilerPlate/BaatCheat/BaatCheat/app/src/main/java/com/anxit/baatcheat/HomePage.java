package com.anxit.baatcheat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.anxit.baatcheat.adapters.RecyclerViewAdapter;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class HomePage extends AppCompatActivity {

    //Constants:
    private static final String TAG = "CustomLog";
    private static final String INTEREST_KEY = "interests";
    private static final String NAME_KEY = "name";

    ArrayList<String> usernameList;
    RecyclerViewAdapter usernameListAdapter;

    //UI Elements:
    EditText interestEditText;
    RecyclerView usernameRecyclerView;

    //FireStore:
    private DocumentReference userDocumentReference;
    private CollectionReference userCollectionReference;

    //Authentication:
    FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        //Initializing UI Elements:
        interestEditText = findViewById(R.id.interest_edittext);

        initializeRecyclerView();

        currentUser  = FirebaseAuth.getInstance().getCurrentUser();
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
        userDocumentReference = FirebaseFirestore.getInstance().document("users/"+currentUser.getEmail());
        userCollectionReference = FirebaseFirestore.getInstance().collection("users");

        Map<String, Object> userData = new HashMap<>();
        userData.put(INTEREST_KEY, interests);

        userData.put(NAME_KEY, Objects.requireNonNull(currentUser.getDisplayName()));

        userDocumentReference.set(userData).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.i(TAG, "findPeople: "+"Interest saved successfully");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG, "findPeople: "+"Could not save data + "+e.toString());
            }
        });

        for(int i=0; i<interests.size(); i++) {
            userCollectionReference.whereArrayContains("interests", interests.get(i)).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                @Override
                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                    ArrayList<DocumentSnapshot> documents;
                    documents = (ArrayList<DocumentSnapshot>) queryDocumentSnapshots.getDocuments();
                    for(int i=0; i<documents.size(); i++){
                        Log.i(TAG, "onSuccess: "+documents.get(i).get("name"));
                        if(!documents.get(i).get("name").equals(currentUser.getDisplayName())
                                && !usernameList.contains(documents.get(i).get("name")))
                            usernameList.add((String)documents.get(i).get("name"));
                    }
                }
            });
        }
        usernameList.remove(currentUser.getDisplayName());
        usernameListAdapter.notifyDataSetChanged();
    }
}

/*
class User{
    String name;
    ArrayList<String> interests;

    User(){
        name = "";
        interests = new ArrayList<>();
    }

    public boolean areInterestsSame(ArrayList<String> searchInterests){
        ArrayList<String> temp = new ArrayList<>();
        temp = interests;
        temp.retainAll(searchInterests);
        if(temp.size()==0)
            return false;
        return true;
    }
}
*/