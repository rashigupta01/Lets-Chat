package com.example.letschat.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.letschat.R;
import com.example.letschat.adpaters.UsersAdapter;
import com.example.letschat.listeners.UsersListener;
import com.example.letschat.models.User;
import com.example.letschat.utilities.Constants;
import com.example.letschat.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements UsersListener {

    TextView fullName;
    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestore;
    String userId;

    private List<User> users;
    private UsersAdapter userAdapter;
    private TextView textErrorMessage;
    private SwipeRefreshLayout swipeRefreshLayout;

    private PreferenceManager preferenceManager;

    private int REQUEST_CODE_BATTERY_OPTIMIZATION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferenceManager = new PreferenceManager(getApplicationContext());

        fullName = findViewById(R.id.textTitle);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        userId = firebaseAuth.getCurrentUser().getUid();

        DocumentReference documentReference = firestore.collection("users").document(userId);
        fullName.setText(preferenceManager.getString(Constants.KEY_FULL_NAME));

        // Adding fmc token in firestore
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull Task<String> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    documentReference.update(Constants.KEY_FCM_TOKEN, task.getResult());
                }
            }
        });

        RecyclerView userRecyclerView = findViewById(R.id.usersRecyclerView);
        textErrorMessage = findViewById(R.id.textErrorMessage);

        users = new ArrayList<>();
        userAdapter = new UsersAdapter(users, this);
        userRecyclerView.setAdapter(userAdapter);

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(this::getUsers);

        getUsers();
        checkForBatteryOptimizations();
    }

    private void getUsers() {
        swipeRefreshLayout.setRefreshing(true);
        firestore.collection(Constants.KEY_COLLECTION_USERS)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {

                        swipeRefreshLayout.setRefreshing(false);
                        if (task.isSuccessful() && task.getResult() != null) {
                            // To remove previous data
                            users.clear();

                            for (QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                                if (userId.equals(documentSnapshot.getId())) {
                                    continue;
                                }
                                User user = new User();
                                user.fname = documentSnapshot.getString("fName");
                                user.email = documentSnapshot.getString("email");
                                user.token = documentSnapshot.getString("fcm_token");
                                user.userID = documentSnapshot.getId();
                                users.add(user);
                            }
                            if (users.size() > 0) {
                                userAdapter.notifyDataSetChanged();
                            } else {
                                textErrorMessage.setText(String.format("%s", "No users available"));
                                textErrorMessage.setVisibility(View.VISIBLE);
                            }
                        } else {
                            textErrorMessage.setText(String.format("%s", "No users available"));
                            textErrorMessage.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }


    public void logout(View view) {
        // Remove token from firestore
        HashMap<String, Object> updates = new HashMap<>();
        updates.put(Constants.KEY_FCM_TOKEN, FieldValue.delete());
        firestore.collection(Constants.KEY_COLLECTION_USERS)
                .document(userId)
                .update(updates);

        // logout user
        firebaseAuth.signOut();

        preferenceManager.clearPreferences();

        // redirect to register activity
        startActivity(new Intent(getApplicationContext(), RegisterActivity.class));

        finish();
    }

    public boolean isUserVerified (FirebaseUser user) {

        user.reload();
        if (user.isEmailVerified()) {
            return true;
        } else {

            AlertDialog.Builder verifyEmailDialog = new AlertDialog.Builder(fullName.getContext());
            verifyEmailDialog.setTitle("Email not verified");
            verifyEmailDialog.setMessage("You need to verify you Email for audio or video calls.");

            verifyEmailDialog.setPositiveButton("Resend link", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    user.sendEmailVerification().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(MainActivity.this, "Verification Email has been sent.", Toast.LENGTH_SHORT).show();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(MainActivity.this, "Error! Email can't be sent. " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });

            verifyEmailDialog.setNegativeButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    // close the dialog
                    // do nothing else
                }
            });
            verifyEmailDialog.create().show();

            return false;
        }
    }

    @Override
    public void initiateVideoMeeting(User user) {

        if (!isUserVerified(firebaseAuth.getCurrentUser())) {
            return;
        }

        if (user.token == null || user.token.trim().isEmpty()) {
            Toast.makeText(this, user.fname + " is not available for meeting", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Video Meeting with " + user.fname, Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getApplicationContext(), OutgoingMeetingInvitationActivity.class);
            intent.putExtra("user", user);
            intent.putExtra(Constants.REMOTE_MSG_TYPE, Constants.KEY_VIDEO);
            startActivity(intent);
        }
    }

    @Override
    public void initiateAudioMeeting(User user) {

        if (!isUserVerified(FirebaseAuth.getInstance().getCurrentUser())) {
            return;
        }

        if (user.token == null || user.token.trim().isEmpty()) {
            Toast.makeText(this, user.fname + " is not available for meeting", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Audio Meeting with " + user.fname, Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getApplicationContext(), OutgoingMeetingInvitationActivity.class);
            intent.putExtra("user", user);
            intent.putExtra(Constants.REMOTE_MSG_TYPE, Constants.KEY_AUDIO);
            startActivity(intent);
        }
    }

    private void checkForBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            if (!powerManager.isIgnoringBatteryOptimizations(getPackageName())) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Warning");
                builder.setMessage("Battery optimization is enabled. It can interrupt running background services.");
                builder.setPositiveButton("Disable", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                    startActivityForResult(intent, REQUEST_CODE_BATTERY_OPTIMIZATION);
                });
                builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
                builder.create().show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_BATTERY_OPTIMIZATION) {
            checkForBatteryOptimizations();
        }
    }

}