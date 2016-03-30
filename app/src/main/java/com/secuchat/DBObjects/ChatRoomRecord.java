package com.secuchat.DBObjects;

import android.util.Base64;
import android.util.Log;
import com.firebase.client.Firebase;
import com.orm.SugarRecord;
import com.secuchat.ChatMessage;
import com.secuchat.Utils.CryptoHelper;
import com.secuchat.SecuChatApp;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by michael on 10/19/14.
 */
public class ChatRoomRecord extends SugarRecord<ChatRoomRecord> {
    String uid;
    String aesKey;
    String participants;
    String label = "Untitled";
    String creator = "Unknown";

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public ChatRoomRecord(String uid, String aesKey, String label, String creator, String participants)
    {
        this.uid = uid;
        try {
            this.aesKey = aesKey;
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.label = label;
        this.participants = participants;
        this.creator = creator;
    }

    public ChatRoomRecord(){
        //Generate UID.
        this.uid = SecuChatApp.getFirebaseMainRef().child("messages").push().getKey();
            try {
                //Get new AES key.
                this.aesKey = Base64.encodeToString(CryptoHelper.getNewAESKey(), Base64.CRLF);
                Log.d("AES-Key", aesKey);
            } catch (Exception e) {
                e.printStackTrace();
            }
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getParticipants() {
        return participants;
    }

    public void setParticipants(String participants) {
        this.participants = participants;
    }

    public void addParticipants(ArrayList<String> participants){
        for (int i = 0; i<participants.size();i++){
            if (this.participants != null)
                this.participants += ", " + participants.get(i);
            else
                this.participants = "" + participants.get(i);
        }
    }

    public void addParticipant(String participant){
        if (this.participants != null)
            this.participants += ", " + participant;
        else
            this.participants = "" + participant;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public byte[] getAesKey() {
        return Base64.decode(aesKey,Base64.CRLF);
    }

    public Firebase getRef(){
        return  SecuChatApp.getFirebaseMainRef().child("messages/" + this.uid);
    }
    public void sendMessage(ChatMessage messageObj){
        //send the message to firebase
        HashMap<String,Object> msgMap = new HashMap<String, Object>();
        String salt = CryptoHelper.generateSalt(); // for security and repeat attacks.
        try {
            msgMap.put("author", messageObj.getAuthor());
            msgMap.put("message", messageObj.getRaw());
            msgMap.put("timePosted", messageObj.getTimePostedInMillis());
            msgMap.put("messageId", messageObj.getMessageId());
            msgMap.put("seenByUser", messageObj.isSeenByUser());
            msgMap.put("salt", salt);
            //Add random bytes so that messages always look different even if content is the same.
            getRef().push().setValue(stringEncrypt(new JSONObject(msgMap).toString())); // Push to db
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public String stringEncrypt(String cleartext) throws Exception {
        byte[] result = CryptoHelper.encrypt(getAesKey(), cleartext.getBytes());
        return Base64.encodeToString(result, Base64.CRLF);
    }
    public String stringDecrypt(String encrypted) throws Exception {
        byte[] enc = Base64.decode(encrypted, Base64.CRLF);
        byte[] result = CryptoHelper.decrypt(getAesKey(), enc);
        return new String(result);
    }


    public void setAesKey(String aesKey) {
        this.aesKey = aesKey;
    }
}
