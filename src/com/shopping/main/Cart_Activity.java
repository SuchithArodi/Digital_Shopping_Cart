/**
 * 
 */
package com.shopping.main;

import java.util.ArrayList;
import java.util.HashMap;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

/**
 * @author Java Team!
 * 
 */
public class Cart_Activity extends Activity {

	private ServerDetails serverDetails;

	private DatabaseHelper databaseHelper;

	private Button orderButton, addnewButton;

	private TextView totalTextView, orderIdTextView;

	private int cartId;

	private String IMEI;

	private AlertDialog.Builder confirmDialogue;

	private final static String TAG = "Shopping Cart";

	private ArrayList<HashMap<String, String>> cartList;

	private ListView listView;

	private int total;

	private String products, quantities;

	// Take a confirmation before removing
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.cart_view);

		orderButton = (Button) findViewById(R.id.buttonOrder);
		addnewButton = (Button) findViewById(R.id.buttonAddNew);

		listView = (ListView) findViewById(R.id.listViewCart);

		totalTextView = (TextView) findViewById(R.id.textviewTotalAmt);

		orderIdTextView = (TextView) findViewById(R.id.textviewOrderId);

		databaseHelper = new DatabaseHelper(this);

		cartList = new ArrayList<HashMap<String, String>>();

		IMEI = ((TelephonyManager) getSystemService(TELEPHONY_SERVICE))
				.getDeviceId();

		confirmDialogue = new AlertDialog.Builder(this);
	}

	// Button actions method
	public void handleButtonActions(View view) {

		switch (view.getId()) {

		case R.id.buttonOrder:

			confirmDialogue.setTitle("Confirm order!");
			confirmDialogue.setMessage("Want to add more products?");

			confirmDialogue.setPositiveButton("Yes",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int arg1) {
							dialog.dismiss();
						}
					});

			confirmDialogue.setNegativeButton("No",
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							order();// Method to perform order operations
						}
					});

			confirmDialogue.show();

			break;

		case R.id.buttonAddNew:
			startActivity(new Intent(this, CaptureActivity.class));
			break;

		case R.id.buttonFinish:
			databaseHelper.deleteDb(this);
			finish();
			startActivity(new Intent(this, Welcome.class));
			break;
		}

	}

	// Method which performs orders
	private void order() {

		int orderId = 0;

		// Call the web service to place the order
		if (total > 0) {

			String methodName = "getOrder";

			serverDetails = new ServerDetails(this, methodName);

			try {
				SoapObject soapObject = new SoapObject(
						serverDetails.NAME_SPACE, methodName);

				soapObject.addProperty("IMEI", IMEI);
				soapObject.addProperty("productID", products);
				soapObject.addProperty("quantity", quantities);
				soapObject.addProperty("total", total);

				SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(
						SoapEnvelope.VER11);
				envelope.setOutputSoapObject(soapObject);

				HttpTransportSE httpTransportSE = new HttpTransportSE(
						serverDetails.URL);

				httpTransportSE.call(serverDetails.SOAP_ACTION, envelope);

				soapObject = (SoapObject) envelope.bodyIn;

				orderId = Integer
						.parseInt(soapObject.getProperty(0).toString());

			} catch (Exception e) {
				Toast.makeText(this, "Some Error found " + e.toString(),
						Toast.LENGTH_LONG).show();
				Log.e(TAG, "Some Error found while connecting to the servers! "
						+ e.getMessage());
			}

			if (orderId > 0) {
				orderIdTextView.setText("Your order id is : " + orderId);
				orderButton.setEnabled(false);
				addnewButton.setEnabled(false);
				listView.setEnabled(false);
			} else {
				orderIdTextView
						.setText("Some error found in placing order. Please try again!");
			}
		} else {
			Toast.makeText(this, "No products to order!", Toast.LENGTH_SHORT)
					.show();
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

	@Override
	protected void onResume() {
		setCartList();
		super.onResume();
	}

	private void setCartList() {
		Cursor cursor = databaseHelper.getCart();

		products = "";
		quantities = "";

		if (cursor != null && cursor.moveToNext()) {
			int i = 1;
			do {

				HashMap<String, String> hashMap = new HashMap<String, String>();

				hashMap.put("No", String.valueOf(i));

				hashMap.put("cartId", String.valueOf(cursor.getInt(cursor
						.getColumnIndex("cart_id"))));

				products += cursor.getString(cursor
						.getColumnIndex("product_id")) + ",";

				hashMap.put("prodName",
						cursor.getString(cursor.getColumnIndex("prod_name")));

				int qty = cursor.getInt(cursor.getColumnIndex("prod_qty"));

				hashMap.put("prodQty", "Qty : " + qty);

				quantities += qty + ",";

				hashMap.put(
						"prodPrice",
						"Rs. "
								+ cursor.getInt(cursor
										.getColumnIndex("prod_cost")));

				cartList.add(hashMap);
				i++;
			} while (cursor.moveToNext());

			cursor.close();

			ListAdapter listAdapter = new SimpleAdapter(this, cartList,
					R.layout.cart_entry_view, new String[] { "No", "cartId",
							"prodName", "prodQty", "prodPrice" }, new int[] {
							R.id.textViewCartSerNo, R.id.textViewCartId,
							R.id.textViewCartProdName, R.id.textViewCartQty,
							R.id.textViewCartPrice });

			listView.setAdapter(listAdapter);

			listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view,
						int position, long id) {

					try {
						TextView cartIdTextView = (TextView) view
								.findViewById(R.id.textViewCartId);

						int cartId = Integer.parseInt(cartIdTextView.getText()
								.toString().trim());

						remove(cartId);

					} catch (Exception e) {
						Toast.makeText(
								getApplicationContext(),
								"Some error while removing product from cart "
										+ e.toString(), Toast.LENGTH_SHORT)
								.show();
					}

				}
			});

			Cursor cursor2 = databaseHelper.getTotalCost();
			if (cursor2.moveToNext()) {
				total = cursor2.getInt(0);
				totalTextView.setText("Total : " + total + " Rs.");

			} else {
				totalTextView.setVisibility(View.GONE);
			}
			cursor2.close();

			orderButton.setEnabled(true);

		} else {
			orderButton.setEnabled(false);
			Toast.makeText(this, "No items in the cart!", Toast.LENGTH_SHORT)
					.show();
			finish();
			return;

		}

	}

	// Product remove from cart method
	private void remove(int crtId) {

		cartId = crtId;

		confirmDialogue.setTitle("Confirm Remove!");
		confirmDialogue
				.setMessage("Are you sure to remove this from the cart?");

		confirmDialogue.setPositiveButton("Yes",
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						databaseHelper.removeFromCart(cartId);
						Toast.makeText(getApplicationContext(),
								"Removed successfully!", Toast.LENGTH_SHORT)
								.show();
						// Refresh the screen
						startActivity(getIntent());
						finish();

					}
				});

		confirmDialogue.setNegativeButton("No",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});

		confirmDialogue.show();
	}

	@Override
	protected void onPause() {
		databaseHelper.close();
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
}