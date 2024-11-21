// MyReceiver.java
package com.example.gamemonitoringapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MyReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("com.example.gamemonitoringapp.RESTART_SERVICE".equals(intent.getAction())) {
            context.startService(new Intent(context, MyService.class));
        }
    }
}
