package com.secuchat;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Base64DataException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ListView;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.secuchat.Services.SecuChatNotificationService;
import com.secuchat.Utils.CryptoHelper;
import com.secuchat.adapters.InvitesListAdapter;

import java.util.ArrayList;
import java.util.HashMap;


public class Invites extends Activity {

    public ArrayList<Invite> invites = new ArrayList<>();
    ListView invitesListView;
    Invite currentInvite;
    private SecuChatNotificationService myService;
    Firebase currentDataSnapshotRef = null;
    byte[] currentDecJsonInvite = null;
    boolean isBound;
    InvitesListAdapter invitesListAdapter;
    HashMap<String, Object> inviteListenerStarted = new HashMap<>();
    SharedPreferences dataStore = null;
    Firebase firebaseMainRef = new Firebase("https://secuchat.firebaseio.com");
    Firebase inviteRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invites);

        Window window = this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(this.getResources().getColor(R.color.statusBarColor));

        if (getIntent().getBooleanExtra("fromService", false)){
            NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            notificationManager.cancel(2);
        }

        invitesListView = (ListView) findViewById(R.id.invitesList);
        invitesListAdapter = new InvitesListAdapter(Invites.this,invites);
        invitesListView.setAdapter(invitesListAdapter);

        dataStore  = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        inviteRef = firebaseMainRef.child("invites").child(dataStore.getString("nickname", ""));

        //add invites when the screen is visible
        startInviteListener();
    }

    public void startInviteListener(){
        if (!inviteListenerStarted.containsKey(inviteRef.toString())) {
            ChildEventListener inviteEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    String encJsonInviteString = dataSnapshot.getValue().toString();
                    byte[] decJsonInvite = null;
                    try {
                        decJsonInvite = CryptoHelper.RSADecryptPrivate(Base64.decode(encJsonInviteString, Base64.CRLF), dataStore.getString("privKey", ""));
                    } catch (IllegalArgumentException iae) {
                        Log.d("Private Key Encryption:", "Failed, BAD BASE64");
                        iae.printStackTrace();
                    } catch (Base64DataException bde) {
                        Log.d("Base64State:", "Bad Base64 data in notification service");
                        bde.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (decJsonInvite != null) {
                        //setting these for use within class
                        Invite invite = new Invite(new String(decJsonInvite));
                        invitesListAdapter.add(invite);
                        dataSnapshot.getRef().setValue(null);
                    }
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
            inviteRef.addChildEventListener(inviteEventListener);
            addInviteListener(inviteEventListener);
        }
        else{
            restartInviteListener();
        }
    }

    private void stopInviteListener(){
        inviteRef.removeEventListener((ChildEventListener) inviteListenerStarted.get(inviteRef.toString()));
    }

    private void removeInviteListener(){
        inviteListenerStarted.remove(inviteRef.toString());
    }

    private void addInviteListener(ChildEventListener eventListener){
        inviteListenerStarted.put(inviteRef.toString(), eventListener);
    }

    private void restartInviteListener(){
        stopInviteListener();
        removeInviteListener();
        startInviteListener();
    }



    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_invites, menu);
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

    @Override
    public void onBackPressed() {
        startActivity(new Intent(this,MyChatRooms.class));
    }

}
