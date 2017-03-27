/**
 * 
 */
package com.shopping.main;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * @author Java Team!
 * 
 */
public class ServerDetails {

	private String SERVER_ADDRESS;
	private String METHOD_NAME;
	public String SOAP_ACTION;

	public String URL;

	public final String NAME_SPACE = "http://service.shopping.com";

	private final String SETTING_FILE = "shopServerDetails";
	private final String SERVER_KEY = "server";

	private Context context;

	private static final String TAG = "Shopping Cart";

	public ServerDetails(Context cntext, String methodName) {

		try {
			this.context = cntext;

			SharedPreferences settings = context.getSharedPreferences(
					SETTING_FILE, Context.MODE_PRIVATE);
			SERVER_ADDRESS = settings.getString(SERVER_KEY,
					"192.168.1.170:8080");

			METHOD_NAME = methodName;

			SOAP_ACTION = NAME_SPACE + "/" + METHOD_NAME;

			URL = "http://" + SERVER_ADDRESS
					+ "/Digital_Shopping/services/Service?wsdl";

		} catch (Exception e) {
			Log.d(TAG,
					"Some Exception while preparing for getting server Details: "
							+ e.getMessage());
		}
	}

}