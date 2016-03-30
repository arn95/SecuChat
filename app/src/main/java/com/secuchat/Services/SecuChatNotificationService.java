package com.secuchat.Services;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Base64DataException;
import android.util.Log;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.secuchat.ChatActivity;
import com.secuchat.ChatMessage;
import com.secuchat.ChatroomCreatorActivity;
import com.secuchat.DBObjects.ChatRoomRecord;
import com.secuchat.Invite;
import com.secuchat.Invites;
import com.secuchat.R;
import com.secuchat.SecuChatApp;
import com.secuchat.Utils.CryptoHelper;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Created by ArnoldB on 5/12/2015.
 */
public class SecuChatNotificationService extends Service {

    final static int MESSAGE_NOTIFICATION_ID = 1;
    final static int INVITE_NOTIFICATION_ID = 2;

    byte[] currentDecJsonInvite;
    Firebase currentDataSnapshotRef;
    String senderNickname;
    String roomName;
    String userNickname;
    SharedPreferences dataStore = null;
    Firebase firebaseMainRef = new Firebase("https://secuchat.firebaseio.com");
    Firebase inviteRef;
    IBinder serviceBinder = new SecuChatBinder();
    HashMap<String, Object> messageListenersForNotificationsStarted = new HashMap<>();
    HashMap<String, Object> inviteListenerForNotificationsStarted = new HashMap<>();
    HashMap<Long, Boolean> seenLastMessage = new HashMap<>();

    @Override
    public IBinder onBind(Intent intent) {
        return serviceBinder;
    }

    public class SecuChatBinder extends Binder {
        public SecuChatNotificationService getService(){
            return SecuChatNotificationService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startInviteListenerForNotifications();
        return Service.START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        dataStore = PreferenceManager.getDefaultSharedPreferences(SecuChatNotificationService.this);
        userNickname = dataStore.getString("nickname", "");
        inviteRef = firebaseMainRef.child("invites").child(userNickname);
        serviceBinder = new SecuChatBinder();
        //start invite listener
        startInviteListenerForNotifications();
        //start listener for all rooms
        Iterator<ChatRoomRecord> chatRoomRecordIterator = ChatRoomRecord.findAll(ChatRoomRecord.class);
        while (chatRoomRecordIterator.hasNext()){
            startMessageListenerForNotifications(chatRoomRecordIterator.next());
        }
    }

    private void addMessageId(){

    }

    public void startInviteListenerForNotifications(){
        if (!inviteListenerForNotificationsStarted.containsKey(inviteRef.toString())) {
            Log.d("Invite D:", "At the start of invite listener method");
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
                        if (!(SecuChatApp.currentActivity instanceof Invites) || SecuChatApp.currentActivity == null) {
                            //setting these for use within class
                            currentDecJsonInvite = decJsonInvite;
                            currentDataSnapshotRef = dataSnapshot.getRef();
                            Log.d("Invite D:", "Got the invite");
                            Invite invite = new Invite(new String(decJsonInvite));
                            senderNickname = invite.getSender();
                            roomName = invite.getLabel();
                            Log.d("ListenerStateService: ", "Online, invite push activated");
                            showInviteNotification();
                        }
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
            addInviteListenerForNotifications(inviteEventListener);
        }
        else{
            restartInviteListenerForNotifications();
        }
    }

    private void stopInviteListenerForNotifications(){
        inviteRef.removeEventListener((ChildEventListener) inviteListenerForNotificationsStarted.get(inviteRef.toString()));
    }

    private void removeInviteListenerForNotifications(){
        inviteListenerForNotificationsStarted.remove(inviteRef.toString());
    }

    private void addInviteListenerForNotifications(ChildEventListener eventListener){
        inviteListenerForNotificationsStarted.put(inviteRef.toString(), eventListener);
    }

    private void restartInviteListenerForNotifications(){
        stopInviteListenerForNotifications();
        removeInviteListenerForNotifications();
        startInviteListenerForNotifications();
    }

    private void startMessageListenerForNotifications(final ChatRoomRecord currentRoom){
            if (!messageListenersForNotificationsStarted.containsKey(currentRoom.getUid())) {
                ChildEventListener messageListener = new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                        String decJSONObj = null;
                        try {
                            decJSONObj = currentRoom.stringDecrypt(dataSnapshot.getValue().toString());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        ChatMessage messageObj = new ChatMessage(decJSONObj);
                        //new additions after this line
                        if (!messageObj.isSeenByUser()) {
                            if (!(SecuChatApp.currentActivity instanceof ChatActivity) || SecuChatApp.currentActivity == null) {
                                showMessageNotification(currentRoom.getUid(), currentRoom.getLabel(), messageObj.getAuthor(), messageObj.getRaw());
                            }
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
                currentRoom.getRef().limitToLast(200).addChildEventListener(messageListener);
                Log.d("ListenerStateService: ", "Online, message push activated for current room");
                addMessageListenerForNotification(currentRoom, messageListener);
            } else {
                restartMessageListenerForNotificatins(currentRoom);
            }
    }

    private void restartMessageListenerForNotificatins(ChatRoomRecord currentRoom){
        stopMessageListenerForNotifications(currentRoom);
        removeMessageListenerForNotifications(currentRoom);
        startMessageListenerForNotifications(currentRoom);
    }

    private void addMessageListenerForNotification(ChatRoomRecord currentRoom, ChildEventListener eventListener){
        messageListenersForNotificationsStarted.put(currentRoom.getUid(), eventListener);
    }

    public void removeMessageListenerForNotifications(final ChatRoomRecord currentRoom){
        messageListenersForNotificationsStarted.remove(currentRoom.getUid());
    }

    private void stopMessageListenerForNotifications(final ChatRoomRecord currentRoom){
        currentRoom.getRef().removeEventListener((ChildEventListener) messageListenersForNotificationsStarted.get(currentRoom.getUid()));
    }

    private void showMessageNotification(String roomId, String roomName, String user, String msg){

        Intent targetIntent = new Intent(SecuChatNotificationService.this, ChatActivity.class);
        targetIntent.putExtra("ID", roomId);
        targetIntent.putExtra("new", false);
        targetIntent.putExtra("fromService", true);
        PendingIntent contentIntent = PendingIntent.getActivity(SecuChatNotificationService.this, 0, targetIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        Uri notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        Notification.Builder notificationBuilder = new Notification.Builder(SecuChatNotificationService.this)
                .setSmallIcon(R.drawable.secuchat_logo)
                .setContentTitle(roomName)
                .setPriority(Notification.PRIORITY_HIGH)
                .setSound(notificationSound)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.secuchat_logo))
                .setContentText(user + ": " + "\"" + msg + "\"")
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setContentIntent(contentIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(MESSAGE_NOTIFICATION_ID, notificationBuilder.build());
    }

    private boolean checkIfAppRunning(){
        boolean val = false;
        ActivityManager activityManager = (ActivityManager) this.getSystemService( ACTIVITY_SERVICE );
        List<ActivityManager.RunningAppProcessInfo> procInfos = activityManager.getRunningAppProcesses();
        for(int i = 0; i < procInfos.size(); i++)
        {
            if(procInfos.get(i).processName.equals("com.secuchat"))
                val = true;
        }
        return val;
    }

    private void showInviteNotification(){
        Log.d("Invite D:", "Show notification method");
        Intent targetIntent = new Intent(SecuChatNotificationService.this, Invites.class);
        targetIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        targetIntent.putExtra("fromService", true);
        PendingIntent contentIntent = PendingIntent.getActivity(SecuChatNotificationService.this, 0, targetIntent, PendingIntent.FLAG_ONE_SHOT);

        Uri notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        Notification.Builder notificationBuilder =
                new Notification.Builder(SecuChatNotificationService.this)
                        .setSmallIcon(R.drawable.secuchat_logo)
                        .setContentTitle("SecuChat")
                        .setPriority(Notification.PRIORITY_MAX)
                        .setSound(notificationSound)
                        .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.secuchat_logo))
                        .setContentText(senderNickname + " invited you in a secure chatroom: " + roomName)
                        .setCategory(Notification.CATEGORY_MESSAGE)
                        .setContentIntent(contentIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(INVITE_NOTIFICATION_ID, notificationBuilder.build());
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
