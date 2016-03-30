package com.secuchat.adapters;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.secuchat.DBObjects.ChatRoomRecord;
import com.secuchat.R;
import java.util.ArrayList;

public class ChatRoomsViewAdapter extends ArrayAdapter<ChatRoomRecord> {

    private final Context context;
    private final ArrayList<ChatRoomRecord> roomIDs;
 

    public ChatRoomsViewAdapter(Context c, ArrayList<ChatRoomRecord> array) {
        super(c, R.layout.chat_room_list_item, R.id.roomNameText, array);
        this.context = c;
        this.roomIDs = array;
    }


    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View v = convertView;

        if (v == null) {

            LayoutInflater vi;
            vi = LayoutInflater.from(getContext());
            // Create new cell
            v = vi.inflate(R.layout.chat_room_list_item, null);

            if (getItem(position) != null) { //Check if null

                TextView participantsText = (TextView) v.findViewById(R.id.participantsText);
                TextView roomNameText = (TextView) v.findViewById(R.id.roomNameText);

                if (participantsText != null && roomNameText != null) {
                    String participantsString = roomIDs.get(position).getParticipants();
                    participantsText.setText(participantsString);
                    // Set to correct label
                    roomNameText.setText(roomIDs.get(position).getLabel());
                }
            }
        }
        return v;
    }
}

