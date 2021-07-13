package com.example.letschat.listeners;

import com.example.letschat.models.User;

public interface UsersListener {

    void initiateVideoMeeting(User user);

    void initiateAudioMeeting(User user);
}
