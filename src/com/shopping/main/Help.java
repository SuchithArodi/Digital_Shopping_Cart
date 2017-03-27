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
 * 
 */
public class Help extends Activity {

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.help);

		TextView helpTextView = (TextView) findViewById(R.id.textViewHelp);

		helpTextView.setMovementMethod(ScrollingMovementMethod.getInstance());

		String help = "\nDigital Shopping Cart! \n \n"
				+ "1.Click on 'Click here to start' to start digital shopping.\n"
				+ "2.Click on 'Exit' to exit from the application"
				+ "\n3.After capturing the product's barcode :\n"
				+ "	i. Click on 'More Details' button to know more details about the product.\n"
				+ "   ii. Click on 'Add to cart' button to add to the cart.\n"
				+ "   iii. Click on 'View Cart' button to view the cart.\n"
				+ "   iv. Click on 'Get New' button to capture a new product.\n"
				+ "    v.  Click on 'Cancel' button to go to main option.\n"
				+ "\n4.Click on 'Remove' button to remove the product from the cart.\n"
				+ "\n5.Click on 'Order' button to order for products of the cart."
				+ "\n\n6.Click on 'Finish' button to come to the main screen!";

		helpTextView.setText(help);
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
