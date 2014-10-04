package com.example.photogallery;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

public class ThumbnailDownloader<Token> extends HandlerThread {

	private static final String TAG = "ThumbnailDownloader";
	private static final int MESSAGE_DOWNLOAD = 0;

	Map<Token, String> requestMap = Collections.synchronizedMap(new HashMap<Token, String>());
	Handler handler;
	Handler responseHandler;
	Listener<Token> listener;

	public interface Listener<Token> {

		void onThumbnailDownloaded(Token token, Bitmap thumbnail);
	}

	public void setListener(Listener<Token> listener) {
		this.listener = listener;
	}

	public ThumbnailDownloader(Handler responseHandler) {
		super(TAG);
		this.responseHandler = responseHandler;
	}

	@SuppressLint("HandlerLeak")
	@Override
	protected void onLooperPrepared() {
		handler = new Handler() {

			@Override
			public void handleMessage(Message msg) {
				if (msg.what == MESSAGE_DOWNLOAD) {
					@SuppressWarnings("unchecked")
					Token token = (Token) msg.obj;
					Log.i(TAG, "Got a request for url: " + requestMap.get(token));
					handleRequest(token);
				}
			}
		};
	}

	public void queueThumbnail(Token token, String url) {
		Log.i(TAG, "Got an URL: " + url);
		requestMap.put(token, url);
		handler.obtainMessage(MESSAGE_DOWNLOAD, token).sendToTarget();
	}

	private void handleRequest(final Token token) {
		try {
			final String url = requestMap.get(token);
			if (url == null) {
				return;
			}
			byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(url);
			final Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
			Log.i(TAG, "Bitmap created");

			this.responseHandler.post(new Runnable() {

				@Override
				public void run() {
					if (requestMap.get(token) != url) {
						return;
					}
					requestMap.remove(token);
					listener.onThumbnailDownloaded(token, bitmap);
				}
			});
		} catch (IOException ioException) {
			Log.e(TAG, "Error downloading image", ioException);
		}
	}

	public void clearQueue() {
		handler.removeMessages(MESSAGE_DOWNLOAD);
		requestMap.clear();

	}
}
