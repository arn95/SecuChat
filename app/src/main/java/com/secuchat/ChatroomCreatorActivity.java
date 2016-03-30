package com.secuchat;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.Toolbar;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.secuchat.DBObjects.ChatRoomRecord;
import com.secuchat.Services.SecuChatNotificationService;
import com.secuchat.Utils.CryptoHelper;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;


public class ChatroomCreatorActivity extends Activity {

    ChatRoomRecord createdRoom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatroom_creator);
        Toolbar toolbar = (Toolbar) this.findViewById(R.id.toolbarChatroomCreator);
        setActionBar(toolbar);
        this.getActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ChatroomCreatorActivity.this, MyChatRooms.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            }
        });
        this.getActionBar().setTitle("Create Chatroom");

        final LinearLayout editTextList = (LinearLayout) findViewById(R.id.participant_edittext_linear_layout);

        Button addListItem = (Button) findViewById(R.id.add_participants_btn);
        addListItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText editText = new EditText(ChatroomCreatorActivity.this);
                editText.setHint("participant name");
                EditText participantField = new EditText(ChatroomCreatorActivity.this);
                participantField.setHint("participant name");
                participantField.setBottom(5);
                editTextList.addView(participantField);
            }
        });

        Button createChatroomButton = (Button) findViewById(R.id.create_chatroom_btn);
        createChatroomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final EditText et = (EditText) ChatroomCreatorActivity.this.findViewById(R.id.chatroom_name);
                String label = et.getText().toString();
                if (label.length() != 0) {
                    ArrayList<String> participantStringArray = new ArrayList<String>();
                    Log.d("EditText List:", editTextList.getChildCount() + "");
                    for (int i = 0; i < editTextList.getChildCount(); i++) {
                        View linearLayoutView = editTextList.getChildAt(i);
                        String participantName = ((EditText) linearLayoutView).getText().toString();
                        participantStringArray.add(participantName);
                    }
                    createChatRoom(label, participantStringArray);
                    startActivity(new Intent(ChatroomCreatorActivity.this, MyChatRooms.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                } else
                    et.setError("Chatroom name is required!");
            }
        });


    }

    @Override
    protected void onResume() {
        super.onResume();
        SecuChatApp.currentActivity = this;
    }

    @Override
    protected void onPause() {
        super.onPause();
        SecuChatApp.currentActivity = null;
    }

    public void createChatRoom(final String label, ArrayList<String> participants)
    {
        //create room in SugarDB
        ChatRoomRecord room = new ChatRoomRecord();

        room.setLabel(label);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        room.setCreator(prefs.getString("nickname", "Unknown"));
        Log.d("Participants Size:", participants.size() + "");
        if (participants.size() > 0){
            room.addParticipants(participants);
        }
        room.save();
        createdRoom = room;
        if (participants.size() > 0 ){
            for (int i = 0; i<participants.size(); i++){
                //push chatroom and invites for each participant
                pushChatRoom(room,participants.get(i));
            }
            Toast.makeText(ChatroomCreatorActivity.this, "Invites Sent", Toast.LENGTH_SHORT).show();
        }
    }

    public void pushChatRoom(final ChatRoomRecord selectedRoom, final String receiverNickname)
    {
            Log.d("Invite Push", "pushChatroom method");
            //add room in firebase
            SecuChatApp.getFirebaseMainRef().child("users/"+receiverNickname).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.getValue() != null) {
                        //add invite in users invite
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        Firebase inviteRef = SecuChatApp.getFirebaseMainRef().child("invites").child(receiverNickname).push();
                        //add invite in firebase invite
                        HashMap<String, Object> map = new HashMap<String, Object>();
                        map.put("uid", selectedRoom.getUid());
                        map.put("aesKey", Base64.encodeToString(selectedRoom.getAesKey(), Base64.CRLF));
                        map.put("label", selectedRoom.getLabel());
                        map.put("roomCreator", selectedRoom.getCreator());
                        map.put("participants", selectedRoom.getParticipants());
                        map.put("sender", prefs.getString("nickname", ""));
                        JSONObject jsonInvite = new JSONObject(map);
                        Log.d("InviteString:", Base64.encodeToString(jsonInvite.toString().getBytes(), Base64.CRLF));
                        try {
                            byte[] result = CryptoHelper.RSAEncrypt(jsonInvite.toString(), snapshot.getValue(String.class));
                            inviteRef.setValue(Base64.encodeToString(result, Base64.CRLF));
                            Log.d("InviteStringEnc:", Base64.encodeToString(result, Base64.CRLF));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onCancelled(FirebaseError firebaseError) {

                }
            });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_chatroom_creator, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
