package com.secuchat.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.Image;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.secuchat.DBObjects.ChatRoomRecord;
import com.secuchat.Invite;
import com.secuchat.MyChatRooms;
import com.secuchat.R;

import java.util.ArrayList;

/**
 * Created by ArnoldB on 5/11/2015.
 */
public class InvitesListAdapter extends ArrayAdapter<Invite> {

    private final Context currentContext;
    private final ArrayList<Invite> invites;

    public InvitesListAdapter(Context context, ArrayList<Invite> invites) {
        super(context, R.layout.invites_list_item, invites);
        currentContext = context;
        this.invites = invites;
    }



    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        // 1. Create inflater
        final LayoutInflater inflater = (LayoutInflater) currentContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        // 2. Get rowView from inflater
        convertView = inflater.inflate(R.layout.invites_list_item, null);

        TextView roomHost = (TextView) convertView.findViewById(R.id.roomHost);
        roomHost.setText(invites.get(position).getSender());

        ImageButton acceptBtn = (ImageButton) convertView.findViewById(R.id.inviteAcceptBtn);
        acceptBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Invite currentInvite = invites.get(position);
                //Add the chatroom we were invited to. To the local DB.
                ChatRoomRecord room = new ChatRoomRecord(currentInvite.getChatId(), currentInvite.getAesKey(), currentInvite.getLabel(), currentInvite.getCreator(), currentInvite.getParticipants());
                room.save();
                invites.remove(position);
                notifyDataSetChanged();
            }
        });

        ImageButton declineBtn = (ImageButton) convertView.findViewById(R.id.inviteCancelBtn);
        declineBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                invites.remove(position);
                notifyDataSetChanged();
            }
        });
        return convertView;
    }


}
