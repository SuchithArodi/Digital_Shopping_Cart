package com.shopping.main;

import com.shopping.result.ResultHandler;
import com.shopping.result.ResultHandlerFactory;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.google.zxing.ResultMetadataType;
import com.google.zxing.ResultPoint;
import com.shopping.camera.CameraManager;

import android.app.Activity;
import android.app.AlertDialog;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.content.res.Configuration;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;

import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;

import android.preference.PreferenceManager;

import android.util.Log;
import android.util.TypedValue;

import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

import java.text.DateFormat;

import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.xmlpull.v1.XmlPullParserException;

/**
 * The barcode reader activity itself. This is loosely based on the
 * CameraPreview example included in the Android SDK.
 */

public final class CaptureActivity extends Activity implements
		SurfaceHolder.Callback {

	private ServerDetails serverDetails;

	private static final String TAG = "Shopping Cart";// CaptureActivity.class.getSimpleName();

	// private static final long INTENT_RESULT_DURATION = 1500L;
	private static final long BULK_MODE_SCAN_DELAY_MS = 1000L;
	private static final float BEEP_VOLUME = 0.10f;
	private static final long VIBRATE_DURATION = 200L;

	private static final Set<ResultMetadataType> DISPLAYABLE_METADATA_TYPES;
	static {
		DISPLAYABLE_METADATA_TYPES = new HashSet<ResultMetadataType>(5);
		DISPLAYABLE_METADATA_TYPES.add(ResultMetadataType.ISSUE_NUMBER);
		DISPLAYABLE_METADATA_TYPES.add(ResultMetadataType.SUGGESTED_PRICE);
		DISPLAYABLE_METADATA_TYPES
				.add(ResultMetadataType.ERROR_CORRECTION_LEVEL);
		DISPLAYABLE_METADATA_TYPES.add(ResultMetadataType.POSSIBLE_COUNTRY);
	}

	private enum Source {
		NATIVE_APP_INTENT, PRODUCT_SEARCH_LINK, NONE
	}

	private CaptureActivityHandler handler;

	private ViewfinderView viewfinderView;
	private TextView statusView;
	private View resultView;
	private MediaPlayer mediaPlayer;
	private Result lastResult;
	private boolean hasSurface;
	private boolean playBeep;
	private boolean vibrate;
	private String sourceUrl;
	private Source source;
	private Vector<BarcodeFormat> decodeFormats;
	private String characterSet;
	private InactivityTimer inactivityTimer;

	private DatabaseHelper databaseHelper;

	private String productId;
	private String prod_Name = "";
	private int cost1 = 0;
	private int quantity1 = 0;
	/* This is used to store product details */
	private String productDetails = "";

	private String warning = "";

	private static String moreInfoToPass;

	/**
	 * When the beep has finished playing, rewind to queue up another one.
	 */
	private final OnCompletionListener beepListener = new OnCompletionListener() {
		public void onCompletion(MediaPlayer mediaPlayer) {
			mediaPlayer.seekTo(0);
		}
	};

	ViewfinderView getViewfinderView() {
		return viewfinderView;
	}

	public Handler getHandler() {
		return handler;
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		Window window = getWindow();
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.capture);

		CameraManager.init(getApplication());
		viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
		resultView = findViewById(R.id.result_view);
		statusView = (TextView) findViewById(R.id.status_view);
		handler = null;
		lastResult = null;
		hasSurface = false;
		inactivityTimer = new InactivityTimer(this);

		databaseHelper = new DatabaseHelper(this);

	}

	@Override
	protected void onResume() {
		super.onResume();
		resetStatusView();

		SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
		SurfaceHolder surfaceHolder = surfaceView.getHolder();
		if (hasSurface) {
			// The activity was paused but not stopped, so the surface still
			// exists. Therefore
			// surfaceCreated() won't be called, so init the camera here.
			initCamera(surfaceHolder);
		} else {
			// Install the callback and wait for surfaceCreated() to init the
			// camera.
			surfaceHolder.addCallback(this);
			surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}

		Intent intent = getIntent();
		String action = intent == null ? null : intent.getAction();
		String dataString = intent == null ? null : intent.getDataString();

		if (intent != null && action != null) {
			if (action.equals(Intents.Scan.ACTION)) {
				// Scan the formats the intent requested, and return the result
				// to the calling activity.
				source = Source.NATIVE_APP_INTENT;
				decodeFormats = DecodeFormatManager.parseDecodeFormats(intent);
			} else if (dataString != null && dataString.contains("12551")) {
				// Scan only products and send the result to mobile Product
				// Search.
				source = Source.PRODUCT_SEARCH_LINK;
				sourceUrl = dataString;
				decodeFormats = DecodeFormatManager.PRODUCT_FORMATS;

			} else {
				// Scan all formats and handle the results ourselves (launched
				// from Home).
				source = Source.NONE;
				decodeFormats = null;
			}
			characterSet = intent.getStringExtra(Intents.Scan.CHARACTER_SET);
		} else {
			source = Source.NONE;
			decodeFormats = null;
			characterSet = null;
		}

		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		playBeep = prefs.getBoolean(PreferencesActivity.KEY_PLAY_BEEP, true);
		if (playBeep) {
			// See if sound settings overrides this
			AudioManager audioService = (AudioManager) getSystemService(AUDIO_SERVICE);
			if (audioService.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
				playBeep = false;
			}
		}
		vibrate = prefs.getBoolean(PreferencesActivity.KEY_VIBRATE, false);
		initBeepSound();
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (handler != null) {
			handler.quitSynchronously();
			handler = null;
		}
		CameraManager.get().closeDriver();
	}

	@Override
	protected void onDestroy() {
		inactivityTimer.shutdown();
		super.onDestroy();
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
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			/*
			 * if (source == Source.NATIVE_APP_INTENT) { return true; } else if
			 * (source == Source.NONE) { resetStatusView(); if (handler != null)
			 * { handler.sendEmptyMessage(R.id.restart_preview); } return true;
			 * }
			 */
			this.finish();
			startActivity(new Intent(this, Welcome.class));
		} else if (keyCode == KeyEvent.KEYCODE_FOCUS
				|| keyCode == KeyEvent.KEYCODE_CAMERA) {
			// Handle these events so they don't launch the Camera app
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onConfigurationChanged(Configuration config) {
		// Do nothing, this is to prevent the activity from being restarted when
		// the keyboard opens.
		super.onConfigurationChanged(config);
	}

	public void surfaceCreated(SurfaceHolder holder) {
		if (!hasSurface) {
			hasSurface = true;
			initCamera(holder);
		}
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		hasSurface = false;
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {

	}

	/**
	 * A valid barcode has been found, so give an indication of success and show
	 * the results.
	 * 
	 * @param rawResult
	 *            The contents of the barcode.
	 * @param barcode
	 *            A grayscale bitmap of the camera data which was decoded.
	 */
	public void handleDecode(Result rawResult, Bitmap barcode) {
		inactivityTimer.onActivity();
		lastResult = rawResult;
		if (barcode == null) {
			// This is from history -- no saved barcode
			handleDecodeInternally(rawResult, null);
		} else {
			playBeepSoundAndVibrate();
			drawResultPoints(barcode, rawResult);
			switch (source) {
			case NATIVE_APP_INTENT:
			case NONE:
				SharedPreferences prefs = PreferenceManager
						.getDefaultSharedPreferences(this);
				if (prefs.getBoolean(PreferencesActivity.KEY_BULK_MODE, false)) {
					Toast.makeText(this, R.string.msg_bulk_mode_scanned,
							Toast.LENGTH_SHORT).show();
					// Wait a moment or else it will scan the same barcode
					// continuously about 3 times
					if (handler != null) {
						handler.sendEmptyMessageDelayed(R.id.restart_preview,
								BULK_MODE_SCAN_DELAY_MS);
					}
					resetStatusView();
				} else {
					handleDecodeInternally(rawResult, barcode);
				}
				break;
			}
		}
	}

	/**
	 * Superimpose a line for 1D or dots for 2D to highlight the key features of
	 * the barcode.
	 * 
	 * @param barcode
	 *            A bitmap of the captured image.
	 * @param rawResult
	 *            The decoded results which contains the points to draw.
	 */
	private void drawResultPoints(Bitmap barcode, Result rawResult) {

		ResultPoint[] points = rawResult.getResultPoints();
		if (points != null && points.length > 0) {
			Canvas canvas = new Canvas(barcode);
			Paint paint = new Paint();
			paint.setColor(getResources().getColor(R.color.result_image_border));
			paint.setStrokeWidth(3.0f);
			paint.setStyle(Paint.Style.STROKE);
			Rect border = new Rect(2, 2, barcode.getWidth() - 2,
					barcode.getHeight() - 2);
			canvas.drawRect(border, paint);

			paint.setColor(getResources().getColor(R.color.result_points));

			if (points.length == 2) {
				paint.setStrokeWidth(4.0f);
				drawLine(canvas, paint, points[0], points[1]);
			} else if (points.length == 4
					&& (rawResult.getBarcodeFormat()
							.equals(BarcodeFormat.UPC_A))
					|| (rawResult.getBarcodeFormat()
							.equals(BarcodeFormat.EAN_13))) {
				// draw two lines, for the barcode and metadata
				drawLine(canvas, paint, points[0], points[1]);
				drawLine(canvas, paint, points[2], points[3]);
			} else {
				paint.setStrokeWidth(10.0f);
				for (ResultPoint point : points) {
					canvas.drawPoint(point.getX(), point.getY(), paint);
				}
			}
		}
	}

	private static void drawLine(Canvas canvas, Paint paint, ResultPoint a,
			ResultPoint b) {
		canvas.drawLine(a.getX(), a.getY(), b.getX(), b.getY(), paint);
	}

	// Put up our own UI for how to handle the decoded contents.
	private void handleDecodeInternally(Result rawResult, Bitmap barcode) {
		statusView.setVisibility(View.GONE);
		viewfinderView.setVisibility(View.GONE);
		resultView.setVisibility(View.VISIBLE);

		ImageView barcodeImageView = (ImageView) findViewById(R.id.barcode_image_view);

		if (barcode == null) {
			barcodeImageView.setImageBitmap(BitmapFactory.decodeResource(
					getResources(), R.drawable.ic_launcher));
		} else {
			barcodeImageView.setImageBitmap(barcode);
		}

		TextView formatTextView = (TextView) findViewById(R.id.format_text_view);
		formatTextView.setText(rawResult.getBarcodeFormat().toString());

		ResultHandler resultHandler = ResultHandlerFactory.makeResultHandler(
				this, rawResult);
		TextView typeTextView = (TextView) findViewById(R.id.type_text_view);
		typeTextView.setText(resultHandler.getType().toString());

		DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.SHORT,
				DateFormat.SHORT);
		String formattedTime = formatter.format(new Date(rawResult
				.getTimestamp()));
		TextView timeTextView = (TextView) findViewById(R.id.time_text_view);
		timeTextView.setText(formattedTime);

		TextView metaTextView = (TextView) findViewById(R.id.meta_text_view);
		View metaTextViewLabel = findViewById(R.id.meta_text_view_label);
		metaTextView.setVisibility(View.GONE);
		metaTextViewLabel.setVisibility(View.GONE);
		Map<ResultMetadataType, Object> metadata = (Map<ResultMetadataType, Object>) rawResult
				.getResultMetadata();
		if (metadata != null) {
			StringBuilder metadataText = new StringBuilder(20);
			for (Map.Entry<ResultMetadataType, Object> entry : metadata
					.entrySet()) {
				if (DISPLAYABLE_METADATA_TYPES.contains(entry.getKey())) {
					metadataText.append(entry.getValue()).append('\n');
				}
			}
			if (metadataText.length() > 0) {
				metadataText.setLength(metadataText.length() - 1);
				metaTextView.setText(metadataText);
				metaTextView.setVisibility(View.VISIBLE);
				metaTextViewLabel.setVisibility(View.VISIBLE);
			}
		}

		TextView contentsTextView = (TextView) findViewById(R.id.contents_text_view);

		// BARCODE RESULT WILL BE RECIEVED HERE
		CharSequence displayContents = resultHandler.getDisplayContents();

		// ID OF THE SCANNED PRODUCT
		productId = displayContents.toString();

		// Fetch product info from the productDetails Server
		String methodName = "getProductInfo";

		serverDetails = new ServerDetails(getApplicationContext(), methodName);

		Toast.makeText(getApplicationContext(), "Product Id : " + productId,
				Toast.LENGTH_SHORT).show();

		try {
			SoapObject soapObject = new SoapObject(serverDetails.NAME_SPACE,
					methodName);

			soapObject.addProperty("productId", productId);

			SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(
					SoapEnvelope.VER11);
			envelope.setOutputSoapObject(soapObject);

			HttpTransportSE httpTransportSE = new HttpTransportSE(
					serverDetails.URL);

			httpTransportSE.call(serverDetails.SOAP_ACTION, envelope);

			soapObject = (SoapObject) envelope.bodyIn;

			productDetails = soapObject.getProperty(0).toString();

			Log.d(TAG, "Product Det : " + productDetails);

		} catch (IOException e1) {
			Toast.makeText(getApplicationContext(),
					"IO Exception found " + e1.getMessage(), Toast.LENGTH_LONG)
					.show();
			Log.e(TAG, "IO Exception found " + e1.getMessage());
		} catch (XmlPullParserException e1) {
			Toast.makeText(getApplicationContext(),
					"XmlPullParserException found " + e1.getMessage(),
					Toast.LENGTH_LONG).show();
			Log.e(TAG, "XmlPullParserException found " + e1.getMessage());
		} catch (Exception e) {
			Toast.makeText(getApplicationContext(),
					"Some Error found " + e.getMessage(), Toast.LENGTH_SHORT)
					.show();
			Log.e(TAG,
					"Some Error found while connecting to the servers! "
							+ e.getMessage());
		}

		ViewGroup buttonView = (ViewGroup) findViewById(R.id.result_button_view);
		buttonView.requestFocus();

		if (productDetails == null
				|| productDetails.equalsIgnoreCase("anyType{}")
				|| productDetails.equals("")
				|| productDetails.equalsIgnoreCase(":Offers:")) {
			warning = "Sorry, no details found!";
			Toast.makeText(this, warning, Toast.LENGTH_LONG).show();

		} else {
			// Extract the product details
			String prodPreviewImageString = "";
			String productMainDetails = "";
			String offerDetails = "";

			if (productDetails.contains(":Offers:")) {
				String[] firstSplits = productDetails.split(":Offers:");

				if (firstSplits.length > 0) {
					productMainDetails = firstSplits[0];
				}

				if (firstSplits.length > 1) {
					offerDetails = firstSplits[1];
				}

			}

			if (productMainDetails.contains(":ImageSplit:")) {
				String[] secondSplits = productMainDetails
						.split(":ImageSplit:");
				productMainDetails = secondSplits[0];
				prodPreviewImageString = secondSplits[1];
			}

			// Now get the product name, quantity and cost from the product
			// details to add to the cart

			String cost = "", quantity = "";

			if (productMainDetails.contains("Cost")) {

				String prod_Details = productMainDetails.substring(0,
						productMainDetails.lastIndexOf("~"));

				Log.d(TAG, "prod_Details : " + prod_Details);

				String[] details = prod_Details.split("~");

				String[] pro_Names = details[0].split(":");
				prod_Name = pro_Names[1];

				String[] qnty = details[2].split(":");
				quantity = qnty[1];

				String[] costs = details[3].split(":");
				cost = costs[1];

				try {
					cost1 = Integer.parseInt(cost.trim());
					quantity1 = Integer.parseInt(quantity.trim());
				} catch (NumberFormatException e) {
				}
			}

			Log.d(TAG, "Prod Details :  ID : " + productId + "  Pro Name : "
					+ prod_Name + "   cost :" + cost1 + " Qnty : " + quantity1);

			productMainDetails = productMainDetails.replaceAll("~", "");

			if (warning.length() > 0) {
				contentsTextView.setText(warning);
			} else {
				contentsTextView.setText(productMainDetails);
			}

			productDetails = prodPreviewImageString + ":product_Preview:"
					+ offerDetails;

			// Set text size..
			contentsTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);

			// Button to Search more about the product.
			TextView moreInfo = (TextView) buttonView.getChildAt(0);
			moreInfo.setVisibility(View.VISIBLE);

			// Button to Search more about the product.
			TextView button_addToCart = (TextView) buttonView.getChildAt(1);
			button_addToCart.setVisibility(View.VISIBLE);
		}

		// Button To Capture Again.
		TextView getNewProduct = (TextView) buttonView.getChildAt(2);
		getNewProduct.setVisibility(View.VISIBLE);

		// Button to cancel Capturing.
		TextView cancelCapture = (TextView) buttonView.getChildAt(3);
		cancelCapture.setVisibility(View.VISIBLE);

		// Button to view cart
		TextView View_Cart = (TextView) buttonView.getChildAt(4);
		View_Cart.setVisibility(View.VISIBLE);

	}

	// Method for handling the product details after getting the product details
	public void handleProductDetails(View view) {

		switch (view.getId()) {

		case R.id.MoreDetails:
			this.finish();
			if (warning.length() > 0) {
				Toast.makeText(getApplicationContext(),
						"Sorry, we dont have this product info!",
						Toast.LENGTH_LONG).show();
			} else {
				Intent intent = new Intent(this, Product_MoreInfo.class);
				Log.e(TAG, "Len : " + productDetails.length());
				setMoreInfoToPass(productDetails);
				// intent.putExtra("productDetails", productDetails);
				intent.putExtra("Details", productId + "," + prod_Name + ","
						+ cost1 + "," + quantity1);
				Log.e(TAG, "Clicked! 1");
				startActivity(intent);
			}
			break;
		case R.id.AddToCart:
			this.finish();
			if (productDetails.equalsIgnoreCase("Sorry, No Details Found!")
					|| productDetails.equals("") || productDetails.equals(null)) {
				Toast.makeText(getApplicationContext(),
						"Sorry! We dont have this product's details!",
						Toast.LENGTH_LONG).show();
			} else {
				try {
					databaseHelper.addToCart(productId, prod_Name, cost1,
							quantity1);
				} catch (Exception e) {
					Log.d(TAG, "Some Error : " + e.getMessage());
				}
				Toast.makeText(getApplicationContext(),
						"" + prod_Name + " is added to the cart!",
						Toast.LENGTH_SHORT).show();

				startActivity(new Intent(getApplicationContext(),
						Cart_Activity.class));
			}
			break;

		case R.id.CaptureAgain:
			this.finish();
			startActivity(new Intent(getApplicationContext(),
					CaptureActivity.class));
			break;

		case R.id.Cancel:
			this.finish();
			startActivity(new Intent(getApplicationContext(), Welcome.class));
			break;

		case R.id.ViewCart:
			this.finish();
			startActivity(new Intent(getApplicationContext(),
					Cart_Activity.class));
			break;
		}
	}

	/**
	 * Creates the beep MediaPlayer in advance so that the sound can be
	 * triggered with the least latency possible.
	 */
	private void initBeepSound() {
		if (playBeep && mediaPlayer == null) {
			// The volume on STREAM_SYSTEM is not adjustable, and users found it
			// too loud,
			// so we now play on the music stream.
			setVolumeControlStream(AudioManager.STREAM_MUSIC);
			mediaPlayer = new MediaPlayer();
			mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mediaPlayer.setOnCompletionListener(beepListener);

			AssetFileDescriptor file = getResources().openRawResourceFd(
					R.raw.beep);
			try {
				mediaPlayer.setDataSource(file.getFileDescriptor(),
						file.getStartOffset(), file.getLength());
				file.close();
				mediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME);
				mediaPlayer.prepare();
			} catch (IOException e) {
				mediaPlayer = null;
			}
		}
	}

	private void playBeepSoundAndVibrate() {
		if (playBeep && mediaPlayer != null) {
			mediaPlayer.start();
		}
		if (vibrate) {
			Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
			vibrator.vibrate(VIBRATE_DURATION);
		}
	}

	private void initCamera(SurfaceHolder surfaceHolder) {
		try {
			CameraManager.get().openDriver(surfaceHolder);
		} catch (IOException ioe) {
			Log.w(TAG, ioe);
			displayFrameworkBugMessageAndExit();
			return;
		} catch (RuntimeException e) {
			// Barcode Scanner has seen crashes in the wild of this variety:
			// java.?lang.?RuntimeException: Fail to connect to camera service
			Log.w(TAG, "Unexpected error initializating camera", e);
			displayFrameworkBugMessageAndExit();
			return;
		}
		if (handler == null) {
			handler = new CaptureActivityHandler(this, decodeFormats,
					characterSet);
		}
	}

	private void displayFrameworkBugMessageAndExit() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.app_name));
		builder.setMessage(getString(R.string.msg_camera_framework_bug));
		builder.setPositiveButton(R.string.button_ok, new FinishListener(this));
		builder.setOnCancelListener(new FinishListener(this));
		builder.show();
	}

	private void resetStatusView() {
		resultView.setVisibility(View.GONE);
		statusView.setText(R.string.msg_default_status);
		statusView.setVisibility(View.VISIBLE);
		viewfinderView.setVisibility(View.VISIBLE);
		lastResult = null;
	}

	public void drawViewfinder() {
		viewfinderView.drawViewfinder();
	}

	public static String getMoreInfoToPass() {
		return moreInfoToPass;
	}

	public static void setMoreInfoToPass(String moreInfoToPass) {
		CaptureActivity.moreInfoToPass = moreInfoToPass;
	}

}