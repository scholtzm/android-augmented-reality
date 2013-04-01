package com.scholtz.aor.helper;

import com.scholtz.aor.R;
import com.scholtz.aor.activity.MainActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class LocationActivity extends Activity implements LocationListener {
	private TextView latitudeField;
	private TextView longitudeField;
	private LocationManager locationManager;
	private Button btn;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_location);
		latitudeField = (TextView) findViewById(R.id.TextView02);
		longitudeField = (TextView) findViewById(R.id.TextView04);
		btn = (Button) findViewById(R.id.button1);

		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, this);
		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1, this);

		btn.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				Intent main = new Intent(getApplicationContext(), MainActivity.class);
				
				main.putExtra("lat", latitudeField.getText().toString());
				main.putExtra("lon", longitudeField.getText().toString());
				setResult(100, main);
				
				finish();
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, this);
		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1, this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		locationManager.removeUpdates(this);
	}

	public void onLocationChanged(Location location) {
		double lat = location.getLatitude();
		double lon = location.getLongitude();
		latitudeField.setText(Double.toString(lat));
		longitudeField.setText(Double.toString(lon));
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	public void onProviderEnabled(String provider) {
	}

	public void onProviderDisabled(String provider) {
	}
}