/**
 * 
 */
package com.shopping.main;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

/**
 */
public class About extends Activity {

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about);

		TextView aboutTextView = (TextView) findViewById(R.id.textViewAbout);

		aboutTextView.setMovementMethod(ScrollingMovementMethod.getInstance());

		String about = "\n Digital Shopping Cart! \n \n"
				+ "'Digital Shopping' is an innovative android application to make the shopping easier and simpler! "
				+ "User has to capture the barcode which on the product to know about the product details."
				+ " "
				+ "This application will maintain the cart digitally and no need to carry the products manually through out the shopping mall! The application manages the cart in user's mobile and user can know the total bill amount of the products which the user has added to his cart."
				+ "User is allowed to place the order for the products he has added in his cart! "
				+ "This is completely an innovative digital shopping cart!";

		aboutTextView.setText(about);

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
			startActivity(new Intent(this, SettingsActivity.class));
			break;

		case R.id.help:
			startActivity(new Intent(this, Help.class));
			break;

		case R.id.about:
			startActivity(new Intent(this, About.class));
			break;
		}
		return super.onOptionsItemSelected(item);
	}

}
