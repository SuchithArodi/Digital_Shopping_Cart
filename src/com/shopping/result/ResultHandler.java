package com.shopping.result;

import com.shopping.main.PreferencesActivity;
import com.shopping.main.R;
import com.google.zxing.Result;
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.ParsedResultType;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;

import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;


/**
 * A base class for the Android-specific barcode handlers. These allow the app
 * to polymorphically suggest the appropriate actions for each data type.
 * 
 */
public abstract class ResultHandler {
	private static final DateFormat DATE_FORMAT = new SimpleDateFormat(
			"dd-MM-yyyy");
	private static final DateFormat DATE_TIME_FORMAT = new SimpleDateFormat(
			"dd-MM-yyyy'T'HH:mm:ss");

	private final ParsedResult result;
	private final Activity activity;
	private final Result rawResult;

	ResultHandler(Activity activity, ParsedResult result) {
		this(activity, result, null);
	}

	ResultHandler(Activity activity, ParsedResult result, Result rawResult) {
		this.result = result;
		this.activity = activity;
		this.rawResult = rawResult;

		// Make sure the continue button is hidden by default. Without this,
		// scanning a product followed
		// by a QR Code would leave the button on screen among the QR Code
		// actions.
		View SearchResultButton = activity.findViewById(R.id.AddToCart);
		SearchResultButton.setVisibility(View.GONE);
	}

	ParsedResult getResult() {
		return result;
	}

	/**
	 * 
	 * @param listener
	 *            The on click listener to install for this button.
	 */
	protected void showSearchResultButton(View.OnClickListener listener) {
		View SearchResultButton = activity.findViewById(R.id.AddToCart);
		SearchResultButton.setVisibility(View.VISIBLE);
		SearchResultButton.setOnClickListener(listener);
	}

	/**
	 * Create a possibly styled string for the contents of the current barcode.
	 * 
	 * @return The text to be displayed.
	 */
	public CharSequence getDisplayContents() {
		String contents = result.getDisplayResult();
		return contents.replace("\r", "");
	}

	/**
	 * A convenience method to get the parsed type. Should not be overridden.
	 * 
	 * @return The parsed type, e.g. URI or ISBN
	 */
	public final ParsedResultType getType() {
		return result.getType();
	}

	/**
	 * Sends an intent to create a new calendar event by prepopulating the Add
	 * Event UI. Older versions of the system have a bug where the event title
	 * will not be filled out.
	 * 
	 * @param summary
	 *            A description of the event
	 * @param start
	 *            The start time as dd-MM-yyyy or dd-MM-yyyy 'T' HH:mm:ss or
	 *            dd-MM-yyyy 'T' HH:mm:ss 'Z'
	 * @param end
	 *            The end time as dd-MM-yyyy or dd-MM-yyyy 'T' HH:mm:ss or
	 *            dd-MM-yyyy 'T' HH:mm:ss 'Z''
	 * @param location
	 *            a text description of the event location
	 * @param description
	 *            a text description of the event itself
	 */
	final void addCalendarEvent(String summary, String start, String end,
			String location, String description) {
		Intent intent = new Intent(Intent.ACTION_EDIT);
		intent.setType("vnd.android.cursor.item/event");
		intent.putExtra("beginTime", calculateMilliseconds(start));
		if (start.length() == 8) {
			intent.putExtra("allDay", true);
		}
		if (end == null) {
			end = start;
		}
		intent.putExtra("endTime", calculateMilliseconds(end));
		intent.putExtra("title", summary);
		intent.putExtra("eventLocation", location);
		intent.putExtra("description", description);
		launchIntent(intent);
	}

	private static long calculateMilliseconds(String when) {
		if (when.length() == 8) {
			// Only contains day/month/year
			Date date;
			synchronized (DATE_FORMAT) {
				date = DATE_FORMAT.parse(when, new ParsePosition(0));
			}
			return date.getTime();
		} else {
			// The when string can be local time, or UTC if it ends with a Z
			Date date;
			synchronized (DATE_TIME_FORMAT) {
				date = DATE_TIME_FORMAT.parse(when.substring(0, 15),
						new ParsePosition(0));
			}
			long milliseconds = date.getTime();
			if (when.length() == 16 && when.charAt(15) == 'Z') {
				Calendar calendar = new GregorianCalendar();
				int offset = calendar.get(Calendar.ZONE_OFFSET)
						+ calendar.get(Calendar.DST_OFFSET);
				milliseconds += offset;
			}
			return milliseconds;
		}
	}

	void launchIntent(Intent intent) {
		if (intent != null) {
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
			try {
				activity.startActivity(intent);
			} catch (ActivityNotFoundException e) {
				AlertDialog.Builder builder = new AlertDialog.Builder(activity);
				builder.setTitle(R.string.app_name);
				builder.setMessage(R.string.msg_intent_failed);
				builder.setPositiveButton(R.string.button_ok, null);
				builder.show();
			}
		}
	}

	private static void putExtra(Intent intent, String key, String value) {
		if (value != null && value.length() > 0) {
			intent.putExtra(key, value);
		}
	}

	protected void showNotOurResults(int index,
			AlertDialog.OnClickListener proceedListener) {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(activity);
		if (prefs.getBoolean(PreferencesActivity.KEY_NOT_OUR_RESULTS_SHOWN,
				false)) {
			// already seen it, just proceed
			proceedListener.onClick(null, index);
		} else {
			// note the user has seen it
			prefs.edit()
					.putBoolean(PreferencesActivity.KEY_NOT_OUR_RESULTS_SHOWN,
							true).commit();
			AlertDialog.Builder builder = new AlertDialog.Builder(activity);
			builder.setMessage(R.string.msg_not_our_results);
			builder.setPositiveButton(R.string.button_ok, proceedListener);
			builder.show();
		}
	}

}