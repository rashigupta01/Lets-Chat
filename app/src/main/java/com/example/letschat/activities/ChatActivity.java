package com.example.letschat.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.letschat.R;
import com.example.letschat.adpaters.MessageAdapter;
import com.example.letschat.models.Message;
import com.example.letschat.utilities.Constants;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class ChatActivity extends AppCompatActivity {

    MessageAdapter adapter;
    ArrayList<Message> messages;

    String senderRoom, receiverRoom;

    FirebaseDatabase database;

    String senderUid;
    String receiverUid;

    TextView textFirstChar, textUsername;
    ImageView sendBtn;
    EditText messageBox;
    RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Declaring Views in activity file
        textFirstChar = findViewById(R.id.textFirstChar);
        textUsername = findViewById(R.id.name);
        sendBtn = findViewById(R.id.sendBtn);
        messageBox = findViewById(R.id.messageBox);
        recyclerView = findViewById(R.id.recyclerView);

        // getting instance of Firebase Database
        database = FirebaseDatabase.getInstance();

        messages = new ArrayList<>();

        String name = getIntent().getStringExtra(Constants.KEY_FULL_NAME);
        receiverUid = getIntent().getStringExtra(Constants.KEY_USER_ID);
        senderUid = FirebaseAuth.getInstance().getUid();

        // Setting up values in actionbar
        textUsername.setText(name);
        textFirstChar.setText(name.substring(0, 1));

        // Creating rooms to keep the messages private
        senderRoom = senderUid + receiverUid;
        receiverRoom = receiverUid + senderUid;

        // Setting up recycler view and adapter
        adapter = new MessageAdapter(this, messages);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        getMessages();

        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String messageTxt = messageBox.getText().toString();

                Message message = new Message(messageTxt, senderUid);
                messageBox.setText("");

                // storing in sender room
                database.getReference().child(Constants.KEY_COLLECTION_CHATS)
                        .child(senderRoom)
                        .child(Constants.KEY_MESSAGES)
                        .push()
                        .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                        // storing message in receiver room
                        database.getReference().child(Constants.KEY_COLLECTION_CHATS)
                                .child(receiverRoom)
                                .child(Constants.KEY_MESSAGES)
                                .push()
                                .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {

                            }
                        });
                    }
                });
            }
        });
    }

    public void getMessages () {
        database.getReference().child(Constants.KEY_COLLECTION_CHATS)
                .child(senderRoom)
                .child(Constants.KEY_MESSAGES)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        messages.clear();
                        for(DataSnapshot snapshot1 : snapshot.getChildren()) {
                            Message message = snapshot1.getValue(Message.class);
                            message.setMessageId(snapshot1.getKey());
                            messages.add(message);
                        }
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }
}