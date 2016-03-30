package com.secuchat;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.DateUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.secuchat.DBObjects.ChatRoomRecord;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;

/**
 * Created by ArnoldB on 10/17/2014.
 */
public class ChatMessage{
    // Object that handles making objects ready for firebase
    private String msg;
    private String author;
    private long timePostedInMillis;
    Context currentContext;

    private boolean seenByUser;
    private long messageId;

    public ChatMessage(String stringJObj)
    {

        try {
            /*
            * Code reference:
            * http://stackoverflow.com/questions/21544973/convert-jsonobject-to-map
            * */
            HashMap<String, Object> msgMap = new Gson().fromJson(stringJObj, HashMap.class);
            this.author = (String) msgMap.get("author");
            this.msg = (String) msgMap.get("message");
            //weird stuff here but this is the only way it works
            this.timePostedInMillis = ((Double) msgMap.get("timePosted")).longValue();
            this.messageId = ((Double) msgMap.get("messageId")).longValue();
            this.seenByUser = (Boolean) msgMap.get("seenByUser");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ChatMessage(Context currentContext, String author, String msg, long timePostedInMillis) {
        super();
        this.currentContext = currentContext;
        this.msg = msg;
        this.author = author;
        this.timePostedInMillis = timePostedInMillis;
        //generates a long message Id
        this.messageId = new Random().nextLong();
        this.seenByUser = false;
    }

    public long getMessageId() {
        return messageId;
    }

    public boolean isSeenByUser() {
        return seenByUser;
    }

    public void setSeenByUser(boolean bool){
        this.seenByUser = bool;
    }

    public long getTimePostedInMillis() {
        return timePostedInMillis;
    }

    public String getFormattedTimePosted(){
        Date time = new Date(timePostedInMillis);
        SimpleDateFormat formatter = null;
        Calendar cal = Calendar.getInstance();

        if (timePostedInMillis < (System.currentTimeMillis()-604800000))//greater than a week
            formatter = new SimpleDateFormat("MMM dd, hh:mm a");
        if (timePostedInMillis < (System.currentTimeMillis()-84600000))//greater than a day
            formatter = new SimpleDateFormat("E hh:mm a");
        if (timePostedInMillis > (System.currentTimeMillis()-84600000))//smaller than 24 hours
            formatter = new SimpleDateFormat("hh:mm a");
        return formatter.format(time);
    }

    public String getRaw() {
        return msg;
    }
    public String getAuthor() {
        return author;
    }

}
