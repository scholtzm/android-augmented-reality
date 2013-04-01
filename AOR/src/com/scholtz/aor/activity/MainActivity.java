package com.scholtz.aor.activity;

import com.scholtz.aor.R;
import com.scholtz.aor.helper.GlobalVariables;
import com.scholtz.aor.helper.XmlParser;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;

public class MainActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
        new Thread(new Runnable() {
            public void run() {
            	GlobalVariables gVars = ((GlobalVariables)getApplicationContext());
            	XmlParser xml = new XmlParser(getApplicationContext());
            	
                gVars.setStops(xml.parse());
                
        		Intent cameraActivity = new Intent(getApplicationContext(), CameraActivity.class);
        		startActivity(cameraActivity);
            }
        }).start();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}
}
