package com.secuchat.adapters;


import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.ArrayAdapter;
import java.util.ArrayList;
import java.util.Date;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import com.secuchat.ChatActivity;
import com.secuchat.DBObjects.ChatRoomRecord;
import com.secuchat.R;
import com.secuchat.ChatMessage;

/**
 * Created by ArnoldB on 10/17/2014.
 */
public class ChatViewAdapter extends ArrayAdapter<ChatMessage> {

    private final Context context;
    private final ArrayList<ChatMessage> messageArray;
    private ListView listView;
    ChatRoomRecord chatRoom;
    String nickname = "";
    Button unreadButton;

    public ChatViewAdapter(Context context, ChatRoomRecord chatRoom, final ListView listView, int layoutResourceId, final ArrayList<ChatMessage> messageArray) {

        super(context, layoutResourceId, messageArray);
        this.context = context;
        this.messageArray = messageArray;
        this.chatRoom = chatRoom;
        this.listView = listView;

        SharedPreferences dataStore = PreferenceManager.getDefaultSharedPreferences(context);
        nickname = dataStore.getString("nickname", "");
        unreadButton = (Button)((ChatActivity) context).findViewById(R.id.unreadBtn);
        unreadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listView.smoothScrollToPosition(messageArray.size());
                unreadButton.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public long getItemId(int position) {
        return messageArray.get(position).getMessageId();
    }

    @Override
    public void add(ChatMessage object) {
        super.add(object);
        if (!object.getAuthor().equals(nickname)) {
            if (listView.getLastVisiblePosition() < messageArray.size() - 2) {
                unreadButton.setVisibility(View.VISIBLE);
            }
            if (listView.getLastVisiblePosition() > messageArray.size()-1){
                unreadButton.setVisibility(View.GONE);
                object.setSeenByUser(true);
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return messageArray.size();
    }

    public void setListView(ListView lv){
        this.listView = lv;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // 1. Create inflater
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // 2. Get rowView from inflater
        View rowView = null;
        if (messageArray.size() > 0) {
            if (nickname.equals(messageArray.get(position).getAuthor())) {
                rowView = inflater.inflate(R.layout.chat_bubble_right, parent, false);

                TextView message = (TextView) rowView.findViewById(R.id.roomNameText);
                TextView messageTime = (TextView) rowView.findViewById(R.id.messageTimeText);

                message.setText(messageArray.get(position).getRaw());
                //set time sent
                messageTime.setText(messageArray.get(position).getFormattedTimePosted());
            } else {
                rowView = inflater.inflate(R.layout.chat_bubble_left, parent, false);
                TextView user = (TextView) rowView.findViewById(R.id.messageAuthor);
                TextView message = (TextView) rowView.findViewById(R.id.roomNameText);
                TextView messageTime = (TextView) rowView.findViewById(R.id.messageTimeText);

                user.setText(messageArray.get(position).getAuthor());
                message.setText(messageArray.get(position).getRaw());
                //set time sent
                messageTime.setText(messageArray.get(position).getFormattedTimePosted());
            }
        }
        return rowView;
    }

}
