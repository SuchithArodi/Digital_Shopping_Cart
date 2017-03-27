package com.shopping.main;

import java.io.IOException;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebSettings.TextSize;
import android.widget.TextView;
import android.widget.Toast;

public class Welcome extends Activity implements OnClickListener {

	private TextView startTextView, exitTextView;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.welcome);

		startTextView = (TextView) findViewById(R.id.textViewStart);
		exitTextView = (TextView) findViewById(R.id.textViewExit);

		CharSequence styledText = getResources().getText(R.string.start);
		startTextView.setText(styledText, TextView.BufferType.SPANNABLE);

		exitTextView.setText("Exit", TextView.BufferType.SPANNABLE);

		startTextView.setOnClickListener(this);
		exitTextView.setOnClickListener(this);
	}

	@Override
	public void onClick(View view) {

		switch (view.getId()) {
		case R.id.textViewStart:

			try {
				String methodName = "authenticate";

				TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
				String IMEI = telephonyManager.getDeviceId();

				ServerDetails serverDetails = new ServerDetails(this,
						methodName);

				SoapObject soapObject = new SoapObject(
						serverDetails.NAME_SPACE, methodName);

				soapObject.addProperty("IMEI", IMEI);

				SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(
						SoapEnvelope.VER11);
				envelope.setOutputSoapObject(soapObject);

				HttpTransportSE httpTransportSE = new HttpTransportSE(
						serverDetails.URL);
				httpTransportSE.call(serverDetails.SOAP_ACTION, envelope);

				soapObject = (SoapObject) envelope.bodyIn;

				String result = soapObject.getProperty(0).toString().trim();

				if (result.equals("Yes")) {
					Toast.makeText(this, "Authenticated successfully!",
							Toast.LENGTH_SHORT).show();

					startActivity(new Intent(this, CaptureActivity.class));
				} else {
					Toast.makeText(
							this,
							"Sorry, You are not an authenticated user! Kindly register!",
							Toast.LENGTH_LONG).show();
					finish();
					return;
				}
			} catch (IOException e) {
				Toast.makeText(
						this,
						"Some IOException while authenticating  : "
								+ e.toString(), Toast.LENGTH_LONG).show();
			} catch (XmlPullParserException e) {
				Toast.makeText(
						this,
						"Some XmlPullParserException while authenticating  : "
								+ e.toString(), Toast.LENGTH_LONG).show();
			} catch (Exception e) {
				Toast.makeText(
						this,
						"Some Exception while authenticating  : "
								+ e.toString(), Toast.LENGTH_LONG).show();
			}

			break;

		case R.id.textViewExit:
			finish();
			Intent intent = new Intent(Intent.ACTION_MAIN);
			intent.addCategory(Intent.CATEGORY_HOME);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
			break;
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_layout, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case R.id.settings:
			finish();
			startActivity(new Intent(this, SettingsActivity.class));
			break;

		case R.id.help:
			finish();
			startActivity(new Intent(this, Help.class));
			break;

		case R.id.about:
			finish();
			startActivity(new Intent(this, About.class));
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		this.finish();
	}
}