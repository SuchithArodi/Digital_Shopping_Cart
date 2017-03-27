/**
 * Database Activity
 */
package com.shopping.main;

/**
 *
 */
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {

	private static String databaseName = "db_purchaseCart";
	private static String table = "table_cart";

	private String cartId = "cart_id";
	private String productId = "product_id";
	private String prodName = "prod_name";
	private String prodCost = "prod_cost";
	private String prodQty = "prod_qty";
	private String date = "date";

	public DatabaseHelper(Context context) {
		super(context, databaseName, null, 1);
	}

	@Override
	public void onCreate(SQLiteDatabase database) {
		String query = "CREATE TABLE IF NOT EXISTS " + table + "(" + cartId
				+ " INTEGER PRIMARY KEY AUTOINCREMENT," + productId
				+ "  VARCHAR, " + prodName + " VARCHAR," + prodCost
				+ "  int(10), " + prodQty + " int(10)," + date + " VARCHAR)";
		database.execSQL(query);
		// database.close();
	}

	@Override
	public void onUpgrade(SQLiteDatabase database, int oldVersion,
			int newVersion) {
		database.execSQL("DROP TABLE IF EXISTS " + table);
		onCreate(database);
		database.close();
	}

	// Delete the database after the completion of the shopping
	public void deleteDb(Context context) {
		context.deleteDatabase(databaseName);
	}

	// ADD NEW PRODUCT TO THE CART
	public void addToCart(String product_ID, String product_Name, int cost,
			int quantity) {
		SQLiteDatabase database = this.getWritableDatabase();

		// Check whether this product already exist in cart
		String query = "SELECT * FROM " + table + " WHERE " + this.productId
				+ " = " + product_ID;

		Cursor cursor = database.rawQuery(query, null);

		if (cursor != null && cursor.moveToNext()) {
			int cartid = cursor.getInt(cursor.getColumnIndex(cartId));
			int qty = cursor.getInt(cursor.getColumnIndex(prodQty));
			int price = cursor.getInt(cursor.getColumnIndex(prodCost));

			cost += price;
			qty += quantity;

			query = "UPDATE " + table + " SET " + this.prodCost + " = " + cost
					+ "," + this.prodQty + "  = " + qty + " WHERE "
					+ this.cartId + " = " + cartid;
		} else {
			query = "INSERT INTO "
					+ table
					+ "("
					+ productId
					+ ","
					+ prodName
					+ ","
					+ prodCost
					+ ","
					+ prodQty
					+ ","
					+ date
					+ ") VALUES('"
					+ product_ID
					+ "','"
					+ product_Name
					+ "', "
					+ cost
					+ ", "
					+ quantity
					+ ",'"
					+ new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
							.format(new Date()) + "' )";
		}

		cursor.close();

		database.execSQL(query);
		database.close();
	}

	// RETRIEVE CART
	public Cursor getCart() {
		SQLiteDatabase db = this.getReadableDatabase();
		return db.rawQuery("SELECT * FROM " + table, null);
	}

	// REMOVE A PRODUCT FROM THE CART
	public void removeFromCart(int cartId) {
		SQLiteDatabase database = this.getWritableDatabase();
		database.execSQL("DELETE FROM " + table + " WHERE " + this.cartId
				+ " = " + cartId + " ");
		database.close();
	}

	// METHOD TO GET THE TOTAL COST
	public Cursor getTotalCost() {
		SQLiteDatabase database = this.getReadableDatabase();
		return database.rawQuery("SELECT SUM(" + prodCost + ") FROM " + table,
				null);
	}
}