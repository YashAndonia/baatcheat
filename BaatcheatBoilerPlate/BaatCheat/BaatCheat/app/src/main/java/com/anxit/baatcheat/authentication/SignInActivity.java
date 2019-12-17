package com.anxit.baatcheat.authentication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.anxit.baatcheat.R;
import com.anxit.baatcheat.interestSearch.HomePage;
import com.anxit.baatcheat.geolocationSearch.geolocationSearch;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class SignInActivity extends AppCompatActivity
        implements View.OnClickListener {

    public static final String TAG = "CustomLog";

    //Request code to identify the request when it is returned:
    public static final int RC_SIGN_IN = 6969;

    //UI Elements:
    CoordinatorLayout wrapperCoordinatorLayout;
    Button interestSearch;
    Button locationQueries;

    //Authentication:
    FirebaseAuth auth;
    long lastResendTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        //Initializing UI elements:
        wrapperCoordinatorLayout = findViewById(R.id.wrapper_coordinator_layout);
        interestSearch = findViewById(R.id.interestSearch);
        interestSearch.setOnClickListener(this);
        locationQueries = findViewById(R.id.locationQueries);
        locationQueries.setOnClickListener(this);

        lastResendTime = 0;

        auth = FirebaseAuth.getInstance();
        if(auth.getCurrentUser() == null) //Check if user is not signed in
            signIn();
        else
            userSignedIn();
    }

    private void signIn(){

        // Choosing authentication providers (Email and Google)
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build());

        // Create and launch sign-in intent
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setTheme(R.style.SignInTheme)
                        .setAvailableProviders(providers)
                        .build(),
                RC_SIGN_IN);
    }

    //Called after the intent returns:
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (resultCode == RESULT_OK) {
                //Sign in successful
                Log.i(TAG, "onActivityResult: " + "Signed in as: "+ Objects.requireNonNull(auth.getCurrentUser()).getEmail());
                userSignedIn();
            } else {
                //Sign In Failed:
                Log.e(TAG, "onActivityResult: " + "Sign In Failed, error code: " + resultCode);
                if (response == null)
                    showSnackBar(R.string.error_try_again);
                else if (response.getError() != null && response.getError().getErrorCode() == ErrorCodes.NO_NETWORK)
                    showSnackBar(R.string.error_no_internet);
                else
                    showSnackBar(R.string.error_try_again);

                signIn(); //try again
            }
        }
    }

    private void userSignedIn(){
        Log.i(TAG, "userSignedIn: "+"Start");
        if(auth.getCurrentUser()==null) {
            signIn();
            return;
        }
        Toast.makeText(this, "Signed in as: "+auth.getCurrentUser().getEmail(), Toast.LENGTH_LONG).show();
        if(!isEmailVerified())
            sendVerificationEmail();
        else
            proceed();
    }

    public boolean isEmailVerified(){
        auth = FirebaseAuth.getInstance(); //Refresh

        //Check if user isn't signed in
        if(auth.getCurrentUser()==null){
            Log.d(TAG, "verifyEmail: "+"This should never happen");
            signIn();
            return false;
        }

        //Check if the user uses email authentication:
        String providerId = auth.getCurrentUser().getProviderData().get(0).getProviderId();
        if(!"firebase".equals(providerId)) {
            Log.d(TAG, "verifyEmail: "+"User doesn't use email auth. ProviderID: "+providerId);
            return true;
        }

        //Check if email is already verified:
        if(auth.getCurrentUser().isEmailVerified()) {
            Log.d(TAG, "verifyEmail: "+"Email already verified");
            return true;
        }
        else  //Almost gets here if using email and password
            Log.d(TAG, "isEmailVerified: "+"Email not verified");

        return false;
    }

    private void sendVerificationEmail(){

        if(auth.getCurrentUser()==null)
            return;
        if(isEmailVerified())
            return;

        //Check if there's a sufficient time between resends:
        if(System.currentTimeMillis()-lastResendTime<10000) {
            Toast.makeText(this, "Wait for a few seconds", Toast.LENGTH_SHORT).show();
            return;
        }

        //Log.d(TAG, "verifyEmail: "+"Email should be sent now");
        //Send a verification mail:
        auth.getCurrentUser().sendEmailVerification()
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        //Check if email was sent successfully
                        if(task.isSuccessful()){
                            Log.i(TAG, "verifyEmail: "+"Verification mail sent to "+auth.getCurrentUser().getEmail());
                            Toast.makeText(SignInActivity.this, "Verification mail sent to "+auth.getCurrentUser().getEmail(), Toast.LENGTH_SHORT).show();
                            lastResendTime = System.currentTimeMillis();
                            showSnackBar(R.string.verification_email_sent);
                        }
                        else{
                            Log.e(TAG, "verifyEmail: "+"Failed to send verification email. "+task.getException());
                            showSnackBar(R.string.error_verification_email);
                        }
                    }
                });
    }




    //to interest Search
    public void proceed() {
        if (auth.getCurrentUser() == null)
            return;

        /*
        if (!isEmailVerified()) {  //Does not work!
            showSnackBar(R.string.verify_email);
            return;
        }
         */

        startActivity(new Intent(this, HomePage.class));//interestSearchPage
    }


    //to geolocationSearch
    public void proceedToLocationSearch() {
        if (auth.getCurrentUser() == null)
            return;

        /*
        if (!isEmailVerified()) {  //Does not work!
            showSnackBar(R.string.verify_email);
            return;
        }
         */

        startActivity(new Intent(this, geolocationSearch.class));//geolocationSearch
    }



    
    public void signOut(View view){
        AuthUI.getInstance()
                .signOut(this)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    public void onComplete(@NonNull Task<Void> task) {
                        Log.i(TAG, "signOut: "+"User Signed Out");
                        signIn();
                    }
                });
    }

    //Utility method to better handle SnackBars(popup messages):
    private void showSnackBar(int stringID){
        Snackbar signInSnackBar = Snackbar.make(wrapperCoordinatorLayout, stringID, BaseTransientBottomBar.LENGTH_LONG);
        if(stringID == R.string.verify_email)
            signInSnackBar.setAction(R.string.resend, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendVerificationEmail();
                }
            });
        signInSnackBar.show();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.interestSearch) {
            proceed();
        }
        if (v.getId() == R.id.locationQueries) {
            proceedToLocationSearch();
        }
    }
}