package com.shopping.result;

import com.google.zxing.client.result.ParsedResult;
import android.app.Activity;

/**
 * This class handles TextParsedResult as well as unknown formats. It's the
 * fallback handler.
 */
public final class TextResultHandler extends ResultHandler {

	public TextResultHandler(Activity activity, ParsedResult result) {
		super(activity, result);
	}

}
