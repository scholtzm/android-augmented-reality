package com.scholtz.aor.activity;

import com.scholtz.aor.R;
import com.scholtz.aor.util.GlobalApp;
import com.scholtz.aor.util.XmlParser;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;

public class MainActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		//new Loading().execute();
		
        new Thread(new Runnable() {
            public void run() {
            	GlobalApp gApp = ((GlobalApp)getApplicationContext());
            	XmlParser xml = new XmlParser(getApplicationContext());
            	
                gApp.setStops(xml.parse());
                
        		Intent cameraActivity = new Intent(getApplicationContext(), CameraActivity.class);
        		startActivity(cameraActivity);
        		
        		finish();
            }
        }).start();
	}
	
	/*private class Loading extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... arg0) {
			GlobalApp gApp = ((GlobalApp)getApplicationContext());
        	XmlParser xml = new XmlParser(getApplicationContext());
        	
            gApp.setStops(xml.parse());
            
			return null;
		}
		
		protected Void onPostExecute() {
			Intent cameraActivity = new Intent(getApplicationContext(), CameraActivity.class);
			startActivity(cameraActivity);
			
			finish();
			
			return null;
		}
		
	}*/
}
