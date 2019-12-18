package com.anxit.baatcheat.interestSearch;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.anxit.baatcheat.R;
import com.anxit.baatcheat.adapters.RecyclerViewAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class InterestSearch extends AppCompatActivity {

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
        //FireStore:
        DocumentReference userDocumentReference = FirebaseFirestore.getInstance().document("users/" + currentUser.getEmail());
        CollectionReference userCollectionReference = FirebaseFirestore.getInstance().collection("users");

        Map<String, Object> userData = new HashMap<>();
        userData.put(INTEREST_KEY, interests);

        userData.put(NAME_KEY, Objects.requireNonNull(currentUser.getDisplayName()));

        userDocumentReference.set(userData).addOnSuccessListener(aVoid -> Log.i(TAG, "findPeople: "+"Interest saved successfully")).addOnFailureListener(e -> Log.e(TAG, "findPeople: "+"Could not save data + "+e.toString()));

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
        usernameListAdapter.notifyDataSetChanged();
    }
}


