package com.secuchat;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toolbar;

import com.secuchat.DBObjects.ChatRoomRecord;
import com.secuchat.Services.SecuChatNotificationService;
import com.secuchat.adapters.ChatRoomsViewAdapter;

import java.util.ArrayList;
import java.util.Iterator;


public class MyChatRooms extends Activity {

    SecuChatNotificationService myService;
    Context c = this;
    ArrayList<ChatRoomRecord> chatRoomArray = new ArrayList<ChatRoomRecord>();
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        SecuChatApp.currentActivity = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_chat_rooms);

        Window window = this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(this.getResources().getColor(R.color.statusBarColor));

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbarChatRooms);
        setActionBar(toolbar);
        toolbar.setTitle("SecuChat");

        refresh();

        Button btnInvite = (Button) findViewById(R.id.btnInvite);
        btnInvite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Start chatroom creator
                startActivity(new Intent(MyChatRooms.this, ChatroomCreatorActivity.class));
            }
        });

        //start the notification service if its not running
        if (!isServiceRunning(SecuChatNotificationService.class)){
            startService(new Intent(getApplicationContext(),SecuChatNotificationService.class));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        SecuChatApp.currentActivity = this;
        refresh();
    }

    @Override
    protected void onPause() {
        super.onPause();
        SecuChatApp.currentActivity = null;
    }

    @Override
    public void onBackPressed() {
        //dont do anything
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_my_chat_rooms, menu);
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
        if (id == R.id.action_invites){
            startActivity(new Intent(this, Invites.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void displayRooms(final ArrayList<ChatRoomRecord> roomIDs)
    {
        // Uses ChatRoomView adapter to populate listview with the chatrooms
        final ListView roomList = (ListView) findViewById(R.id.myChatRoomsList);
        ChatRoomsViewAdapter adapter = new ChatRoomsViewAdapter(c, roomIDs);
        roomList.setAdapter(adapter);
        //set the listener

       roomList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
           @Override
           public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
               //Chatroom clicked we must enter that ChatAcivity
               Intent i = new Intent(getApplicationContext(), ChatActivity.class);
               i.putExtra("ID",roomIDs.get(position).getUid());
               i.putExtra("new",true);
               startActivity(i);
           }
       });
       roomList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
           @Override
           public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
               // Long held. Ask if they want to delete chatroom
               AlertDialog.Builder dialog = new AlertDialog.Builder(MyChatRooms.this);
               dialog.setTitle("Delete this chatroom ?");
               dialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialog, int which) {
                       ChatRoomRecord.deleteAll(ChatRoomRecord.class, "uid = ?", roomIDs.get(position).getUid());
                       //Delete this chatroom.. refresh to show changes to local database.
                       refresh();
                   }
               });

               dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialog, int which) {
                       dialog.dismiss();
                   }
               });
               dialog.create();
               dialog.show();
               return true;
           }
       });
    }

    public void refresh(){
        Iterator<ChatRoomRecord> iterator = ChatRoomRecord.findAll(ChatRoomRecord.class);
        //Get all chatrooms. Package in array pass to the displayer
        chatRoomArray = new ArrayList<ChatRoomRecord>();
        while (iterator.hasNext())
        {
            chatRoomArray.add(iterator.next());
        }

        displayRooms(chatRoomArray);
    }

    public void createChatRoom(final String label)
    {
        //create room in SugarDB
        ChatRoomRecord room = new ChatRoomRecord();
        if (label.length() != 0) {
            room.setLabel(label);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            room.setCreator(prefs.getString("nickname", "Unknown"));
        }
        room.save();
        //add room in display
        refresh();
    }
    /*
    * Code reference:
    * http://stackoverflow.com/questions/2115758/how-to-display-alert-dialog-in-android
    *
    * */
    public Dialog createDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final LayoutInflater inflater = this.getLayoutInflater();
        final View v = inflater.inflate(R.layout.create_chatroom_popup,null);

        builder.setView(v)
                .setPositiveButton(R.string.create_chatroom_popup_ok_btn, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        final EditText et = (EditText) v.findViewById(R.id.room_name);
                        String label = et.getText().toString();
                        if (label.length() != 0) {
                            createChatRoom(label);
                            dialog.dismiss();
                        }
                    }
                })
                .setNegativeButton(R.string.popup_cancel_btn, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        builder.setTitle(R.string.title_create_chatroom_popup);
        return builder.create();
    }
}
