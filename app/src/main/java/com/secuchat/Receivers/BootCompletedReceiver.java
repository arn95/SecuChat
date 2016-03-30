package com.secuchat.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.secuchat.Services.SecuChatNotificationService;

/**
 * Created by aballiu_admin on 6/18/15.
 */
public class BootCompletedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            Intent pushIntent = new Intent(context, SecuChatNotificationService.class);
            context.startService(pushIntent);
        }
    }
}
