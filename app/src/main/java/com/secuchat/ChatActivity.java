package com.secuchat;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.secuchat.DBObjects.ChatRoomRecord;
import com.secuchat.Services.SecuChatNotificationService;
import com.secuchat.Utils.CryptoHelper;
import com.secuchat.Utils.ResizeListener;
import com.secuchat.Utils.ResizeView;
import com.secuchat.adapters.ChatViewAdapter;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;


public class ChatActivity extends Activity implements AbsListView.OnScrollListener {

    ArrayList<ChatMessage> messageArray = new ArrayList<ChatMessage>();
    ChatRoomRecord selectedRoom;
    public static Boolean paused = false;
    static ChatViewAdapter chatViewListAdapter;
    ListView messageListView;
    SecuChatNotificationService notificationService;
    boolean isServiceBound = false;
    String nickname;
    Button unreadButton;
    HashMap<String, Object> messageListeners = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbarChatroomCreator);
        setActionBar(toolbar);
        this.getActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ChatActivity.this, MyChatRooms.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            }
        });

        Window window = this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(this.getResources().getColor(R.color.statusBarColor));

        paused=false;

        Intent i = getIntent();
        String roomId = i.getStringExtra("ID"); // Our room ID to open

        boolean fromService = i.getBooleanExtra("fromService", false);
        if (fromService){
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.cancel(1);
        }

        //Find the ROOM
        List<ChatRoomRecord> id = ChatRoomRecord.find(ChatRoomRecord.class, "uid = ?",roomId);//do a query on the db for the item using the id
        selectedRoom = id.get(0); //get the only item in the List (since all Uid will be unique)

        this.getActionBar().setTitle(selectedRoom.getLabel());
        this.getActionBar().setSubtitle(selectedRoom.getParticipants());

        this.getWindow().setStatusBarColor(getResources().getColor(R.color.statusBarColor));
        FrameLayout frame = new FrameLayout(this);

        unreadButton = (Button) ChatActivity.this.findViewById(R.id.unreadBtn);

        ResizeView resizeView = (ResizeView) findViewById(R.id.rootView);

        final TextView msgBox = (TextView) findViewById(R.id.editText);

        messageListView = (ListView) findViewById(R.id.listView);
        messageListView.setOnScrollListener(this);
        if(chatViewListAdapter == null||i.getBooleanExtra("new",false))
            chatViewListAdapter = new ChatViewAdapter(this, selectedRoom, messageListView, R.layout.chat_bubble_left, messageArray);
        chatViewListAdapter.setListView(messageListView);
        messageListView.setAdapter(chatViewListAdapter);

        resizeView.setSizeListener(new ResizeListener() {
            @Override
            public void viewSizeChanged(int w, int h, int oldw, int oldh) {
                Log.d("ScrollListener", "Scroll!");
                messageListView.clearFocus();
                messageListView.post(new Runnable() {
                    @Override
                    public void run() {
                        //Scroll if keyboard opened
                        unreadButton.setVisibility(View.GONE);
                        messageListView.setSelection(chatViewListAdapter.getCount());
                    }
                });
            }
        });
        //send button event
        Button btnSend = (Button) findViewById(R.id.button);
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //get time for when message was sent
                Calendar calendar = Calendar.getInstance();
                long currentTimeInMillis = calendar.getTimeInMillis();
                //Send message!
                SharedPreferences dataStore = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                ChatMessage msg = new ChatMessage(ChatActivity.this, dataStore.getString("nickname", ""), msgBox.getText().toString(), currentTimeInMillis);
                selectedRoom.sendMessage(msg);
                msgBox.setText("");
            }
        });

        if (!isServiceRunning(SecuChatNotificationService.class)){
            startService(new Intent(getApplicationContext(),SecuChatNotificationService.class));
        }
        Log.d("Activity State: ", "Created");

        //update the screen when new message comes in while its visible
        startMessageListener();
    }

    private void startMessageListener(){
        if (!messageListeners.containsKey(selectedRoom.getUid())) {
            ChildEventListener messageListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    String decJSONObj = null;
                    try {
                        decJSONObj = selectedRoom.stringDecrypt(dataSnapshot.getValue().toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    ChatMessage messageObj = new ChatMessage(decJSONObj);
                    chatViewListAdapter.add(messageObj);
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {

                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onCancelled(FirebaseError firebaseError) {

                }
            };
            selectedRoom.getRef().limitToLast(200).addChildEventListener(messageListener);
            addMessageListener(messageListener);
        }
        else{
            restartMessageListener();
        }
    }

    private void restartMessageListener(){
        stopMessageListener();
        removeMessageListener();
        startMessageListener();
    }

    private void stopMessageListener(){
        selectedRoom.getRef().removeEventListener((ChildEventListener) messageListeners.get(selectedRoom.getUid()));
    }

    private void removeMessageListener(){
        messageListeners.remove(selectedRoom.getUid());
    }

    private void addMessageListener(ChildEventListener eventListener){
        messageListeners.put(selectedRoom.getUid(), eventListener);
    }


    @Override
    protected void onStart() {
        super.onStart();
        Log.d("Activity State: ", "Started");
    }

    @Override
    protected void onPause() {
        paused = true;
        Log.d("Activity State: ", "Paused");
        SecuChatApp.currentActivity = null;
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.d("Activity State: ", "Stopped");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("Activity State:", "Destroyed");
    }

    @Override
    protected void onResume() {
        super.onResume();
        paused = false;
        Log.d("Activity State: ", "Resumed");
        SecuChatApp.currentActivity = this;
    }

    public boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.add_new_participant) {
            Dialog dialog = createDialog();
            dialog.show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        int messageNum = messageListView.getChildCount();
        LayoutInflater inflater = this.getLayoutInflater();
        LinearLayout messageFieldLayout = (LinearLayout) findViewById(R.id.messageFieldLayout);

        if (messageNum != view.getLastVisiblePosition() && (scrollState == SCROLL_STATE_FLING || scrollState == SCROLL_STATE_TOUCH_SCROLL))
            messageFieldLayout.setElevation(30);
        else
            messageFieldLayout.setElevation(0);

    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

    }

    public Dialog createDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final LayoutInflater inflater = this.getLayoutInflater();
        final View v = inflater.inflate(R.layout.invite_participant_popup,null);

        builder.setView(v)
                .setPositiveButton(R.string.add_participant_popup_ok_btn, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        final EditText participantName = (EditText) v.findViewById(R.id.participant_name);
                        String participantNameString = participantName.getText().toString();
                        boolean isKeyIn;
                        if (participantNameString.length() != 0) {
                            isKeyIn = true;
                            //update the chatroom participants
                            SharedPreferences dataStore = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                            selectedRoom.addParticipant(dataStore.getString("nickname",""));
                            selectedRoom.save();
                            pushChatRoom(participantNameString, isKeyIn);
                            dialog.dismiss();
                        }
                    }
                })
                .setNegativeButton(R.string.popup_cancel_btn, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        builder.setTitle(R.string.title_popup_chat_room_invite);
        return builder.create();
    }

    public void pushChatRoom(final String receiverNickname, boolean isKeyIn)
    {
        if (isKeyIn)
        {
            //add room in firebase
            SecuChatApp.getFirebaseMainRef().child("users/"+receiverNickname).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if(snapshot.getValue() != null) {
                        //add invite in users invite
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        Firebase inviteRef = SecuChatApp.getFirebaseMainRef().child("invites").child(receiverNickname).push();
                        //add invite in firebase invite
                        HashMap<String, Object> map = new HashMap<String, Object>();
                        map.put("uid", selectedRoom.getUid());
                        map.put("aesKey",  Base64.encodeToString(selectedRoom.getAesKey(), Base64.CRLF));
                        map.put("label", selectedRoom.getLabel());
                        map.put("roomCreator", selectedRoom.getCreator());
                        map.put("participants", selectedRoom.getParticipants());
                        map.put("sender",prefs.getString("nickname", ""));
                        JSONObject jsonInvite = new JSONObject(map);
                        Log.d("InviteString:", Base64.encodeToString(jsonInvite.toString().getBytes(), Base64.CRLF));
                        try {
                            byte[] result = CryptoHelper.RSAEncrypt(jsonInvite.toString(), snapshot.getValue(String.class));
                            inviteRef.setValue(Base64.encodeToString(result, Base64.CRLF));
                            Log.d("InviteStringEnc:", Base64.encodeToString(result, Base64.CRLF));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        Toast.makeText(ChatActivity.this, "Invite Sent", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(FirebaseError firebaseError) {

                }
            });

        }
    }


}




