package com.example.letschat.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

public class  LoginActivity extends AppCompatActivity {

    EditText email, password;
    Button loginBtn;
    TextView registerTxt, forgotPasswordTxt;
    FirebaseAuth firebaseAuth;
    private PreferenceManager preferenceManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        loginBtn = findViewById(R.id.login_btn);
        registerTxt = findViewById(R.id.txt_for_register);
        forgotPasswordTxt = findViewById(R.id.fogot_password);

        preferenceManager = new PreferenceManager(getApplicationContext());

        firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() != null) {
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
            finish();
        }

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String emailStr = email.getText().toString().trim();
                String passwordStr = password.getText().toString().trim();

                // clearing any previous error
                email.setError(null);
                password.setError(null);

                // checking for invalid data
                if (TextUtils.isEmpty(emailStr)) {
                    email.setError("Email is required");
                    return;
                }
                if (TextUtils.isEmpty(passwordStr)) {
                    password.setError("Password can't be empty");
                    return;
                }
                if (password.length()<6) {
                    password.setError("Password should be at least 6 characters");
                }

                // authenticate the user
                firebaseAuth.signInWithEmailAndPassword(emailStr, passwordStr).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();


                            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
                            String userId = firebaseAuth.getCurrentUser().getUid();
                            DocumentReference documentReference = firestore.collection(Constants.KEY_COLLECTION_USERS).document(userId);
                            documentReference.addSnapshotListener(LoginActivity.this, new EventListener<DocumentSnapshot>() {
                                @Override
                                public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                                    preferenceManager.putString(Constants.KEY_FULL_NAME, value.getString(Constants.KEY_FULL_NAME));
                                    preferenceManager.putString(Constants.KEY_USER_ID, value.getId());
                                    preferenceManager.putString(Constants.KEY_EMAIL, value.getString(Constants.KEY_EMAIL));
                                }
                            });

                            startActivity(new Intent(getApplicationContext(), MainActivity.class));
                            finishAffinity();
                        } else {
                            Toast.makeText(LoginActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        registerTxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), RegisterActivity.class));
                finish();
            }
        });

        forgotPasswordTxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText resetMail = new EditText((view.getContext()));
                AlertDialog.Builder passwordResetDialog = new AlertDialog.Builder(view.getContext());
                passwordResetDialog.setTitle("Reset Password");
                passwordResetDialog.setMessage("Enter your registered Email-ID to receive reset link.");
                passwordResetDialog.setView(resetMail);

                passwordResetDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // extract the email and send reset link
                        String mail = resetMail.getText().toString();

                        firebaseAuth.sendPasswordResetEmail(mail).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Toast.makeText(LoginActivity.this, "Reset link sent to your Email", Toast.LENGTH_SHORT).show();

                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(LoginActivity.this, "Error! Reset Link is not sent: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });

                passwordResetDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // close the dialog
                        // do nothing else
                    }
                });

                passwordResetDialog.create().show();
            }
        });
    }
}