package com.shopping.result;

import com.google.zxing.Result;
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.ParsedResultType;
import com.google.zxing.client.result.ResultParser;

import android.app.Activity;

/**
 * Manufactures Android-specific handlers based on the barcode content's type.
 */
public final class ResultHandlerFactory {
	private ResultHandlerFactory() {
	}

	public static ResultHandler makeResultHandler(Activity activity,
			Result rawResult) {
		ParsedResult result = parseResult(rawResult);
		ParsedResultType type = result.getType();
		return new TextResultHandler(activity, result);

	}

	private static ParsedResult parseResult(Result rawResult) {
		return ResultParser.parseResult(rawResult);
	}
}
