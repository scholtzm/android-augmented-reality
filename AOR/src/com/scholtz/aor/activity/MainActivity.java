package com.scholtz.aor.activity;

import com.scholtz.aor.R;
import com.scholtz.aor.util.GlobalApp;
import com.scholtz.aor.util.XmlParser;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;

/**
 * MainActivity - application starts here
 * Call LoadingTask to parse XML database
 * @author Mike
 *
 */
public class MainActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		new LoadingTask().execute();
	}

	/**
	 * Parse XML in another thread to unblock UI thread.
	 * @author Mike
	 *
	 */
	private class LoadingTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... arg0) {
			GlobalApp gApp = ((GlobalApp) getApplicationContext());
			XmlParser xml = new XmlParser(getApplicationContext());

			gApp.setStops(xml.parse());

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			Intent cameraActivity = new Intent(getApplicationContext(), CameraActivity.class);
			startActivity(cameraActivity);

			finish();
		}
	}

}
