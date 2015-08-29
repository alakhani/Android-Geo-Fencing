package com.pervasive.geofence;

import java.util.List;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.content.*;


public class GeofenceReceiver extends BroadcastReceiver {
    Context context;

    Intent broadcastIntent = new Intent();

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;

        broadcastIntent.addCategory(GeofenceUtils.CATEGORY_LOCATION_SERVICES);

        if (LocationClient.hasError(intent)) {
            handleError(intent);
        } else {
            handleEnterExit(intent,context);
        }
    }

    private void handleError(Intent intent){
        // Get the error code
        int errorCode = LocationClient.getErrorCode(intent);

        // Get the error message
        String errorMessage = LocationServiceErrorMessages.getErrorString(
                context, errorCode);

        // Log the error
        Log.e(GeofenceUtils.APPTAG,
                context.getString(R.string.geofence_transition_error_detail,
                        errorMessage));

        // Set the action and error message for the broadcast intent
        broadcastIntent
                .setAction(GeofenceUtils.ACTION_GEOFENCE_ERROR)
                .putExtra(GeofenceUtils.EXTRA_GEOFENCE_STATUS, errorMessage);

        // Broadcast the error *locally* to other components in this app
        LocalBroadcastManager.getInstance(context).sendBroadcast(
                broadcastIntent);
    }
    
    private String getTransitionString(int transitionType,Context context) {
        switch (transitionType) {

            case Geofence.GEOFENCE_TRANSITION_ENTER:
                return context.getResources().getString(R.string.geofence_transition_entered);

            case Geofence.GEOFENCE_TRANSITION_EXIT:
                return context.getResources().getString(R.string.geofence_transition_exited);

            default:
                return context.getResources().getString(R.string.geofence_transition_unknown);
        }
    }


    private void handleEnterExit(Intent intent,Context context) {
        // Get the type of transition (entry or exit)
        int transition = LocationClient.getGeofenceTransition(intent);

        // Test that a valid transition was reported
        if ((transition == Geofence.GEOFENCE_TRANSITION_ENTER)
                || (transition == Geofence.GEOFENCE_TRANSITION_EXIT)) {

            // Post a notification
            List<Geofence> geofences = LocationClient
                    .getTriggeringGeofences(intent);
            String[] geofenceIds = new String[geofences.size()];
            String ids = TextUtils.join(GeofenceUtils.GEOFENCE_ID_DELIMITER,
                    geofenceIds);
            String transitionType =
                    getTransitionString(transition,context);

            for (int index = 0; index < geofences.size(); index++) {
                Geofence geofence = geofences.get(index);
                geofenceIds[index] = geofences.get(index).getRequestId();
            }
            // Create an Intent to broadcast to the app
            broadcastIntent
                    .setAction(GeofenceUtils.ACTION_GEOFENCE_TRANSITION)
                    .addCategory(GeofenceUtils.CATEGORY_LOCATION_SERVICES)
                    .putExtra("uniqieId", geofenceIds)
                    .putExtra("ttype",
                            transitionType);

            LocalBroadcastManager.getInstance(context)
                    .sendBroadcast(broadcastIntent);

            // Log the transition type and a message
            Log.d(GeofenceUtils.APPTAG, transitionType + ": " + ids);
            Log.d(GeofenceUtils.APPTAG,
                    context.getString(R.string.geofence_transition_notification_text));

            // In debug mode, log the result
            Log.d(GeofenceUtils.APPTAG, "transition");

            // An invalid transition was reported
        } else {
            // Always log as an error
            Log.e(GeofenceUtils.APPTAG,
                    context.getString(R.string.geofence_transition_invalid_type,
                            transition));
        }
    }

    /**
     * Posts a notification in the notification bar when a transition is
     * detected. If the user clicks the notification, control goes to the main
     * Activity.
     * 
     * @param transitionType
     *            The type of transition that occurred.
     * 
     */
    private void sendNotification(String transitionType, String locationName) {

        // Create an explicit content Intent that starts the main Activity
        Intent notificationIntent = new Intent(context, MainActivity.class);

        // Construct a task stack
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);

        // Adds the main Activity to the task stack as the parent
        stackBuilder.addParentStack(MainActivity.class);

        // Push the content Intent onto the stack
        stackBuilder.addNextIntent(notificationIntent);

        // Get a PendingIntent containing the entire back stack
        PendingIntent notificationPendingIntent = stackBuilder
                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        // Get a notification builder that's compatible with platform versions
        // >= 4
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context);

        // Set the notification contents
        builder.setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(transitionType + ": " + locationName)
                .setContentText(
                        context.getString(R.string.geofence_transition_notification_text))
                .setContentIntent(notificationPendingIntent);

        // Get an instance of the Notification manager
        NotificationManager mNotificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);

        // Issue the notification
        mNotificationManager.notify(0, builder.build());
    }
}
