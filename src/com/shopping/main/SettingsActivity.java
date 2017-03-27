/**
 * 
 */
package com.shopping.main;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * @author Java Team!
 * 
 */
public class SettingsActivity extends Activity implements View.OnClickListener {

	private EditText serverEditText;
	private Button setButton;

	private final String SETTING_FILE = "shopServerDetails";
	private final String SERVER_KEY = "server";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);

		serverEditText = (EditText) findViewById(R.id.editTextIPAddress);
		setButton = (Button) findViewById(R.id.buttonSetServer);

		setButton.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {

		String serverAdd = serverEditText.getText().toString().trim();

		if (serverAdd.length() == 0) {
			Toast.makeText(this, "Enter server address!", Toast.LENGTH_SHORT)
					.show();
		} else {
			
			SharedPreferences settingsPreferences = this.getSharedPreferences(
					SETTING_FILE, MODE_PRIVATE);

			SharedPreferences.Editor editor = settingsPreferences.edit();
			editor.putString(SERVER_KEY, serverAdd);
			editor.commit();

			Toast.makeText(this, "Set successfully!", Toast.LENGTH_SHORT)
					.show();

			serverEditText.setText("");
			this.finish();

			startActivity(new Intent(this, Welcome.class));
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_layout, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {

		switch (item.getItemId()) {
		case R.id.settings:
			startActivity(new Intent(this, SettingsActivity.class));
			break;

		case R.id.help:
			startActivity(new Intent(this, Help.class));
			break;

		case R.id.about:
			startActivity(new Intent(this, About.class));
			break;
		}
		return super.onMenuItemSelected(featureId, item);
	}

}