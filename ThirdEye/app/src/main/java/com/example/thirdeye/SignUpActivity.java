package com.example.thirdeye;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
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

public class SignUpActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText signUpEmailEditText, signUpPasswordEditText;
    private TextView alreadySignedUpTextView;
    private Button signUpButton;
    private ProgressBar signUpProgressBar;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sign_up_activity);

        mAuth = FirebaseAuth.getInstance();

        signUpEmailEditText =  findViewById(R.id.signUpEmailEditTextId);
        signUpPasswordEditText =  findViewById(R.id.signUpPasswordEditTextId);
        signUpButton =  findViewById(R.id.signUpButtonId);
        alreadySignedUpTextView  = findViewById(R.id.alreadySignedUpTextId);
        signUpProgressBar = findViewById(R.id.signUpProgressBarId);

        signUpButton.setOnClickListener(this);
        alreadySignedUpTextView.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.signUpButtonId:

                newUserRegister();

                break;
            case R.id.alreadySignedUpTextId:
                Intent intent_haveNotSignedUp  = new Intent(getApplicationContext(), MainActivity.class);
                //to resume the main activity use the following line -----------
                intent_haveNotSignedUp.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent_haveNotSignedUp);
                finish();
                break;
        }
    }

    private void newUserRegister() {
        String newUserEmail = signUpEmailEditText.getText().toString().trim();
        String newUserPassword = signUpPasswordEditText.getText().toString().trim();

        /// validity checking for email and pass
        if (newUserEmail.isEmpty()){
            signUpEmailEditText.setError("Enter an email address");
            signUpEmailEditText.requestFocus();
            return;
        }
        if (  !android.util.Patterns.EMAIL_ADDRESS.matcher(newUserEmail).matches()  ){
            signUpEmailEditText.setError("Enter an VALID email address");
            signUpEmailEditText.requestFocus();
            return;
        }

        if (newUserPassword.isEmpty()){
            signUpPasswordEditText.setError("Enter a password");
            signUpPasswordEditText.requestFocus();
            return;
        }
        if (newUserPassword.length()<6 ){
            signUpEmailEditText.setError("Password must be at least 6 characters long");
            signUpEmailEditText.requestFocus();
            return;
        }

        signUpProgressBar.setVisibility(View.VISIBLE);

        mAuth.createUserWithEmailAndPassword(newUserEmail, newUserPassword).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override

            public void onComplete(@NonNull Task<AuthResult> task) {
                signUpProgressBar.setVisibility(View.GONE);
                if (task.isSuccessful()) {
                    // Sign in success, update UI with the signed-in user's information
                    Toast.makeText(getApplicationContext(), "Registration Successfull", Toast.LENGTH_LONG  ).show();
                    Intent intent_registration_success  = new Intent(getApplicationContext(), HomePageActivity.class);
                    intent_registration_success.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent_registration_success);
                    finish();

                } else {
                    if(task.getException() instanceof FirebaseAuthUserCollisionException){
                        Toast.makeText(getApplicationContext(), "User already Exists" , Toast.LENGTH_LONG  ).show();
                    }else{
                        Toast.makeText(getApplicationContext(), "Error : "+task.getException().getMessage() , Toast.LENGTH_LONG  ).show();
                    }
                    // If sign in fails, display a message to the user.
                }
            }
        });



    }
}