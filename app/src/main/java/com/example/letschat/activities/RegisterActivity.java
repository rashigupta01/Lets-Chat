package com.example.letschat.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.letschat.R;
import com.example.letschat.utilities.Constants;
import com.example.letschat.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    EditText fullName, email, password, password2;
    Button registerBtn;
    TextView loginTxt;
    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestore;
    String userID;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        preferenceManager = new PreferenceManager(getApplicationContext());

        fullName = findViewById(R.id.full_name);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        password2 = findViewById(R.id.password2);
        registerBtn = findViewById(R.id.register_btn);
        loginTxt = findViewById(R.id.txt_for_login);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        if (firebaseAuth.getCurrentUser() != null) {
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
            finish();
        }

        registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String emailStr = email.getText().toString().trim();
                String passwordStr = password.getText().toString().trim();
                String password2Str = password2.getText().toString().trim();
                String fullNameStr = fullName.getText().toString().trim();

                // clearing any previous error
                fullName.setError(null);
                email.setError(null);
                password.setError(null);
                password2.setError(null);

                // checking for invalid data
                if (TextUtils.isEmpty(fullNameStr)) {
                    fullName.setError("Name is required");
                    return;
                }
                if (TextUtils.isEmpty(emailStr)) {
                    email.setError("Email is required");
                    return;
                }
                if (TextUtils.isEmpty(passwordStr)) {
                    password.setError("Password can't be empty");
                    return;
                }
                if (TextUtils.isEmpty(password2Str)) {
                    password2.setError("Please re-write the password");
                    return;
                }
                if (!passwordStr.equals(password2Str)) {
                    password.setError("Passwords do not match");
                    password2.setError("Passwords do not match");
                    return;
                }
                if (password.length()<6) {
                    password.setError("Password should be at least 6 characters");
                }

                // register the user in firebase
                firebaseAuth.createUserWithEmailAndPassword(emailStr, passwordStr).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            
                            // send verification link
                            FirebaseUser fuser = firebaseAuth.getCurrentUser();
                            fuser.sendEmailVerification().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Toast.makeText(RegisterActivity.this, "Verification Email has been sent.", Toast.LENGTH_SHORT).show();
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(RegisterActivity.this, "Error! Email can't be sent. " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });

                            Toast.makeText(RegisterActivity.this, "User Created!", Toast.LENGTH_SHORT).show();

                            // storing user's data in firebase store
                            userID = fuser.getUid();
                            DocumentReference documentReference = firestore.collection("users").document(userID);
                            Map<String, Object> user = new HashMap<>();
                            user.put("fName", fullNameStr);
                            user.put("email", emailStr);
                            documentReference.set(user);

                            preferenceManager.putString(Constants.KEY_FULL_NAME, fullNameStr);
                            preferenceManager.putString(Constants.KEY_EMAIL, emailStr);
                            preferenceManager.putString(Constants.KEY_USER_ID, userID);

                            startActivity(new Intent(getApplicationContext(), MainActivity.class));
                        } else {
                            Toast.makeText(RegisterActivity.this, "Error" + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        loginTxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                finish();
            }
        });
    }
}