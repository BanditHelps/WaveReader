package com.github.b4ndithelps.wave

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class CustomTabsCloser : BroadcastReceiver() {
    companion object {
        const val ACTION_CLOSE_TABS = "com.github.b4ndithelps.wave.CLOSE_CUSTOM_TABS"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_CLOSE_TABS) {
            Log.d("CustomTabsCloser", "Received close tabs broadcast")
            
            // This will specifically close the Custom Tab without closing your app
            val activity = context as? Activity ?: return
            
            // Using the standard Chrome Custom Tabs close action
            val closeIntent = Intent("android.support.customtabs.action.ACTION_CLOSE")
            context.sendBroadcast(closeIntent)
            
            // Force activity to finish if it's the Custom Tab host
            activity.finishAndRemoveTask()
        }
    }
}