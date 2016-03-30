package com.secuchat;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.orm.SugarApp;
import com.secuchat.DBObjects.ChatRoomRecord;
import com.secuchat.Services.SecuChatNotificationService;
import com.secuchat.Utils.CryptoHelper;

import java.util.Arrays;

/**
 * Created by ArnoldB on 10/22/2014.
 */
public class SecuChatApp extends SugarApp {

    static Firebase firebaseMainRef;

    public static Firebase getFirebaseMainRef() {
        return firebaseMainRef;
    }
    public static Activity currentActivity = null;
    @Override
    public void onCreate() {
        super.onCreate();

        Firebase.setAndroidContext(this);
        firebaseMainRef = new Firebase("https://secuchat.firebaseio.com");
    }
}