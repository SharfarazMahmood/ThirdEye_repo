package com.example.thirdeye;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;

import org.opencv.android.OpenCVLoader;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static String TAG = "MainActivity";

    static {
        if (OpenCVLoader.initDebug()) {
            Log.e(TAG , "opencv ok");
        }else {
            Log.e(TAG , "opencv NOT ok");
        }
    }

    /////-----------Main activity also acts as the SIGN I activity
    private EditText  signInEmailEditText, signInPasswordEditText;
    private TextView haveNotSignUpTextView;
    private Button signInButton;
    private ProgressBar signInProgressBar;

    private FirebaseAuth mAuth;
    private FirebaseUser mUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();

        if(mUser != null){
            Intent intent_current_user_homeActivity = new Intent(getApplicationContext() , HomePageActivity.class);
            intent_current_user_homeActivity.addFlags(FLAG_ACTIVITY_NEW_TASK);

            startActivity(intent_current_user_homeActivity);
            finish();
        }

        signInEmailEditText =  findViewById(R.id.signInEmailEditTextId);
        signInPasswordEditText =  findViewById(R.id.signInPasswordEditTextId);
        signInButton =  findViewById(R.id.signInButtonId);
        haveNotSignUpTextView =  findViewById(R.id.haveNotSignedUpTextId);

        signInProgressBar = findViewById(R.id.signInProgressBarId);

        signInButton.setOnClickListener(this);
        haveNotSignUpTextView.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.signInButtonId:
                existingUserSignIn();
                break;
            case R.id.haveNotSignedUpTextId:
                Intent  intent_haveNotSignedUp  = new Intent(getApplicationContext(), SignUpActivity.class);
                startActivity(intent_haveNotSignedUp);
                break;
        }
    }

    private void existingUserSignIn() {
        String existingUserEmail = signInEmailEditText.getText().toString().trim();
        String existingUserPassword = signInPasswordEditText.getText().toString().trim();

        /// validity checking for email and pass
        if (existingUserEmail.isEmpty()){
            signInEmailEditText.setError("Enter an email address");
            signInEmailEditText.requestFocus();
            return;
        }
        if (  !android.util.Patterns.EMAIL_ADDRESS.matcher(existingUserEmail).matches()  ){
            signInEmailEditText.setError("Enter an VALID email address");
            signInEmailEditText.requestFocus();
            return;
        }

        if (existingUserPassword.isEmpty()){
            signInPasswordEditText.setError("Enter a password");
            signInPasswordEditText.requestFocus();
            return;
        }
        if (existingUserPassword.length()<6 ){
            signInEmailEditText.setError("Password must be at least 6 characters long");
            signInEmailEditText.requestFocus();
            return;
        }

        signInProgressBar.setVisibility(View.VISIBLE);

        mAuth.signInWithEmailAndPassword(existingUserEmail, existingUserPassword).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                signInProgressBar.setVisibility(View.GONE);
                if (task.isSuccessful()) {
                    // Sign in success, update UI with the signed-in user's information
                    Toast.makeText(getApplicationContext(), "Log in Successfull", Toast.LENGTH_LONG  ).show();
                    Intent intent_log_in_success  = new Intent(getApplicationContext(), HomePageActivity.class);
                    intent_log_in_success.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent_log_in_success);
                    //////to destroy the main activity use finish()
                    finish();
                } else {
                    Toast.makeText(getApplicationContext(), "Log in failed : Check Email/Password  " , Toast.LENGTH_LONG  ).show();
                    // If sign in fails, display a message to the user.
                }
            }
        });
    }
}