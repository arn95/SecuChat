package com.secuchat;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.HashMap;

/**
 * Created by ArnoldB on 11/11/2014.
 */
//Makes invite object ready for firebase.
public class Invite {

    String chatId;
    String aesKey;
    String label;
    String sender;
    String creator;
    String participants;

    public Invite(String jsonObj)
    {
        HashMap<String, Object> roomInfo = new Gson().fromJson(jsonObj, HashMap.class);
        this.chatId = (String)roomInfo.get("uid");
        this.aesKey = (String)roomInfo.get("aesKey");
        this.label = (String)roomInfo.get("label");
        this.sender = (String)roomInfo.get("sender");
        this.creator = (String) roomInfo.get("roomCreator");
        this.participants = (String) roomInfo.get("participants");
    }

    public String getChatId() {
        return chatId;
    }

    public String getAesKey() {
        return aesKey;
    }

    public String getParticipants() {
        return participants;
    }

    public String getLabel() {
        return label;
    }

    public String getSender() {
        return sender;
    }

    public String getCreator() {
        return creator;
    }
}
