/**
 * 
 */
package com.shopping.main;

import org.kobjects.base64.Base64;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * @author Administrator
 * 
 */
public class Product_MoreInfo extends Activity implements OnClickListener {

	private ImageView productPreviewImageView;
	private LinearLayout offerLayout;

	private Button addCartButton, backButton, viewCartButton, exitButton;

	private String details = "";

	private String productDetailsToAdd = "";

	private DatabaseHelper databaseHelper;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.more_info);

		productPreviewImageView = (ImageView) findViewById(R.id.productPreview);
		offerLayout = (LinearLayout) findViewById(R.id.OfferDisplayLayout);

		addCartButton = (Button) findViewById(R.id.DisplayAddToCart);
		backButton = (Button) findViewById(R.id.DisplayBack);
		viewCartButton = (Button) findViewById(R.id.DisplayViewcart);
		exitButton = (Button) findViewById(R.id.DisplayExit);

		databaseHelper = new DatabaseHelper(this);

		addCartButton.setOnClickListener(this);
		backButton.setOnClickListener(this);
		viewCartButton.setOnClickListener(this);
		exitButton.setOnClickListener(this);

		Bundle bundle = getIntent().getExtras();

		details = CaptureActivity.getMoreInfoToPass();

		if (bundle != null) {
			productDetailsToAdd = bundle.getString("Details");
			// Log.e("Shopping", "details " + details);
			// Log.e("Shopping", "productDetailsToAdd " + productDetailsToAdd);
		}

		String[] firstSplits = details.split(":product_Preview:");

		// get product preview and set it
		byte[] productPreviewBytes = Base64.decode(firstSplits[0]);

		Bitmap bitmap = BitmapFactory.decodeByteArray(productPreviewBytes, 0,
				productPreviewBytes.length);
		productPreviewImageView.setImageBitmap(bitmap);

		// get offer details
		if (firstSplits.length > 1) {

			String offers = firstSplits[1];

			if (offers.contains(":NextOffer:")) {

				String[] secondSplits = offers.split(":NextOffer:");

				for (int i = 0; i < secondSplits.length; i++) {

					if (secondSplits[i].contains(":ImageOfferSplit:")) {

						String[] split1 = secondSplits[i]
								.split(":ImageOfferSplit:");

						TextView offerDetailsTextView = new TextView(this);
						offerDetailsTextView.setText(split1[0]);
						offerDetailsTextView.setTextColor(Color.RED);
						offerDetailsTextView.setTextSize(20);

						offerLayout.addView(offerDetailsTextView);

						byte[] offerPreviewBytes = Base64.decode(split1[1]);

						ImageView offerImageView = new ImageView(this);
						Bitmap bitmap2 = BitmapFactory.decodeByteArray(
								offerPreviewBytes, 0, offerPreviewBytes.length);
						offerImageView.setImageBitmap(bitmap2);
						offerImageView.setMaxHeight(120);
						offerImageView.setMaxWidth(120);
						offerImageView.setAdjustViewBounds(true);

						offerLayout.addView(offerImageView);
					}
				}

			}
		} else {
			TextView offerDetailsTextView = new TextView(this);
			offerDetailsTextView.setText("Sorry, no offers available!!");
			offerDetailsTextView.setTextColor(Color.RED);
			offerDetailsTextView.setTextSize(20);

			offerLayout.addView(offerDetailsTextView);
		}

	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {

		case R.id.DisplayAddToCart:
			this.finish();
			if (productDetailsToAdd.contains(",")) {

				String[] detail = productDetailsToAdd.split(",");

				databaseHelper.addToCart(detail[0], detail[1],
						Integer.parseInt(detail[2]),
						Integer.parseInt(detail[3]));

				Toast.makeText(this,
						"This product " + detail[1] + " is added to the cart!",
						Toast.LENGTH_SHORT).show();
				startActivity(new Intent(getApplicationContext(),
						Cart_Activity.class));
			} else {
				Toast.makeText(
						this,
						"This product cannot be added to the cart! Try again! ",
						Toast.LENGTH_SHORT).show();
			}
			break;

		case R.id.DisplayBack:
			this.finish();
			startActivity(new Intent(this, CaptureActivity.class));
			break;

		case R.id.DisplayViewcart:
			this.finish();
			startActivity(new Intent(this, Cart_Activity.class));
			break;

		case R.id.DisplayExit:
			this.finish();
			startActivity(new Intent(this, Welcome.class));
			break;
		}

	}

	@Override
	protected void onDestroy() {
		databaseHelper.close();
		this.finish();
		super.onDestroy();
	}
}