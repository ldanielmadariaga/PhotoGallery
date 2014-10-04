package com.example.photogallery;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.net.Uri;
import android.util.Log;

public class FlickrFetchr {

	public static final String TAG = "FlickrFetchr";
	public static final String PREF_SEARCH_QUERY = "searchQuery";
	public static final String PREF_LAST_RESULT_ID = "lastResultId";

	private static final String ENDPOINT = "http://api.flickr.com/services/rest/";
	private static final String API_KEY = "5d576f64cb225d654c279439253a0d78";
	private static final String METHOD_GET_RECENT = "flickr.photos.getRecent";
	private static final String METHOD_SEARCH = "flickr.photos.search";
	private static final String PARAM_EXTRAS = "extras";
	private static final String PARAM_TEXT = "text";
	private static final String EXTRA_SMALL_URL = "url_s";
	private static final String XML_PHOTO = "photo";

	public ArrayList<GalleryItem> fetchItems() {

		String url = Uri.parse(ENDPOINT).buildUpon().appendQueryParameter("method", METHOD_GET_RECENT)
				.appendQueryParameter("api_key", API_KEY).appendQueryParameter(PARAM_EXTRAS, EXTRA_SMALL_URL)
				.build().toString();

		return downloadGalleryItems(url);
	}

	public ArrayList<GalleryItem> search(String query) {

		String url = Uri.parse(ENDPOINT).buildUpon().appendQueryParameter("method", METHOD_SEARCH)
				.appendQueryParameter("api_key", API_KEY).appendQueryParameter(PARAM_EXTRAS, EXTRA_SMALL_URL)
				.appendQueryParameter(PARAM_TEXT, query).build().toString();

		return downloadGalleryItems(url);
	}

	public ArrayList<GalleryItem> downloadGalleryItems(String url) {
		ArrayList<GalleryItem> galleryItems = new ArrayList<GalleryItem>();

		try {
			String xmlString = getUrl(url);
			Log.i(TAG, "Received xml: " + xmlString);
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			XmlPullParser parser = factory.newPullParser();
			parser.setInput(new StringReader(xmlString));
			parseItems(galleryItems, parser);

		} catch (IOException ioException) {
			Log.e(TAG, "Failed to fetch items", ioException);
		} catch (XmlPullParserException xmlPullParserException) {
			Log.e(TAG, "Failed to parse items", xmlPullParserException);
		}

		return galleryItems;
	}

	public void parseItems(ArrayList<GalleryItem> items, XmlPullParser parser) throws XmlPullParserException,
			IOException {
		int eventType = parser.next();
		while (eventType != XmlPullParser.END_DOCUMENT) {
			if (eventType == XmlPullParser.START_TAG && XML_PHOTO.equals(parser.getName())) {
				String id = parser.getAttributeValue(null, "id");
				String caption = parser.getAttributeValue(null, "title");
				String owner = parser.getAttributeValue(null, "owner");
				String smallUrl = parser.getAttributeValue(null, EXTRA_SMALL_URL);

				GalleryItem galleryItem = new GalleryItem();
				galleryItem.setId(id);
				galleryItem.setCaption(caption);
				galleryItem.setOwner(owner);
				galleryItem.setUrl(smallUrl);
				items.add(galleryItem);

			}
			eventType = parser.next();
		}
	}

	public String getUrl(String urlSpec) throws IOException {
		return new String(getUrlBytes(urlSpec));
	}

	byte[] getUrlBytes(String urlSpec) throws IOException {
		URL url = new URL(urlSpec);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			InputStream inputStream = connection.getInputStream();

			if (connection.getResponseCode() != HttpsURLConnection.HTTP_OK) {
				return null;
			}

			int bytesRead = 0;
			byte[] buffer = new byte[1024];
			while ((bytesRead = inputStream.read(buffer)) > 0) {
				outputStream.write(buffer, 0, bytesRead);
			}

			outputStream.close();
			return outputStream.toByteArray();

		} finally {
			connection.disconnect();
		}
	}
}
