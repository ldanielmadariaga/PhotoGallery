package com.example.photogallery;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class PollService extends IntentService {

	private static final String TAG = "PollService";
	private static final int POLL_INTERVAL = 1000 * 60 * 5; // 5 minutes
	public static final String PREF_IS_ALARM_ON = "isAlarmOn";
	public static final String ACTION_SHOW_NOTIFICATION = "com.bignerdranch.android.photogallery.SHOW_NOTIFICATION";
	public static final String PERM_PRIVATE = "com.bignerdranch.android.photogallery.PRIVATE";

	public PollService() {
		super(TAG);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		@SuppressWarnings("deprecation")
		Boolean isNetworkAvailableBoolean = connectivityManager.getBackgroundDataSetting()
				&& (connectivityManager.getActiveNetworkInfo() != null);
		if (!isNetworkAvailableBoolean) {
			return;
		}
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		String query = sharedPreferences.getString(FlickrFetchr.PREF_SEARCH_QUERY, null);
		String lastResultId = sharedPreferences.getString(FlickrFetchr.PREF_LAST_RESULT_ID, null);

		ArrayList<GalleryItem> items;
		if (query != null) {
			items = new FlickrFetchr().search(query);
		} else {
			items = new FlickrFetchr().fetchItems();
		}
		if (items.size() == 0) {
			return;
		}
		String resultId = items.get(0).getId();

		if (!resultId.equals(lastResultId)) {
			Log.i(TAG, "Got a new result: " + resultId);
			Resources resources = getResources();
			PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
					PhotoGalleryActivity.class), 0);

			Notification notification = new NotificationCompat.Builder(this)
					.setTicker(resources.getString(R.string.new_pictures_title))
					.setSmallIcon(android.R.drawable.ic_menu_report_image)
					.setContentTitle(resources.getString(R.string.new_pictures_title))
					.setContentText(resources.getString(R.string.new_pictures_text))
					.setContentIntent(pendingIntent).setAutoCancel(true).build();

			showBackgroundNotification(0, notification);

		}

		sharedPreferences.edit().putString(FlickrFetchr.PREF_LAST_RESULT_ID, resultId).commit();
	}

	public static void setServiceAlarm(Context context, boolean isOn) {
		Intent intent = new Intent(context, PollService.class);
		PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, 0);

		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		if (isOn) {
			alarmManager.setRepeating(AlarmManager.RTC, System.currentTimeMillis(), POLL_INTERVAL, pendingIntent);
		} else {
			alarmManager.cancel(pendingIntent);
			pendingIntent.cancel();
		}

		PreferenceManager.getDefaultSharedPreferences(context).edit()
				.putBoolean(PollService.PREF_IS_ALARM_ON, isOn).commit();

	}

	public static boolean isServiceAlarmOn(Context context) {
		Intent intent = new Intent(context, PollService.class);
		PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_NO_CREATE);
		return pendingIntent != null;
	}

	void showBackgroundNotification(int requestCode, Notification notification) {
		Intent intent = new Intent(ACTION_SHOW_NOTIFICATION);
		intent.putExtra("REQUEST_CODE", requestCode);
		intent.putExtra("NOTIFICATION", notification);

		sendOrderedBroadcast(intent, PERM_PRIVATE, null, null, Activity.RESULT_OK, null, null);
	}
}
