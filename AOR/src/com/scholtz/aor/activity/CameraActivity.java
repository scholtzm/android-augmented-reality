package com.scholtz.aor.activity;

import java.io.InputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.NinePatchDrawable;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextPaint;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.scholtz.aor.R;
import com.scholtz.aor.util.GlobalApp;
import com.scholtz.aor.util.Poi;
import com.scholtz.aor.util.Util;
import com.scholtz.aor.view.CameraView;

/**
 * CameraActivity class
 * This class combines CameraView and DrawView and
 * implements location and sensor listeners (accelerometer, magnetic field)
 * @author Mike
 *
 */
public class CameraActivity extends Activity implements LocationListener, SensorEventListener {
	private final float OUT_OF_SCREEN = Float.MIN_VALUE;
	
	private FrameLayout frameLayout;
	private CameraView cameraView;
	private PoiView poiView;
	private LocationManager mLocationManager;
	private SensorManager mSensorManager;
	private GlobalApp gApp;
	
	// orientation, location, calculations
	private List<Poi> visible = null;
	private List<Poi> relevant = null;
	private double lat = 0, lon = 0;
	private float[] accelerometerValues = new float[3];
	private float[] magneticFieldValues = new float[3];
	private float[] orientation = new float[3];
	private String locationSource = "---";
	private float filteredOrientation = Float.NaN;
	boolean firstFix = true;
	
	// hud
	private Bitmap bitmap;
	private int hudSize = 400;
	
	/**
	 * Setup required services in onCreate
	 * Set window parameters
	 */
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        gApp = ((GlobalApp)getApplicationContext());
        
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);		
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        // check if GPS is enabled; if not, redirect user to settings
		if(mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) == false) {
			Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
			startActivity(intent);
		}
    }
	
	/**
	 * Set Layout as a combination of 2 views on top of each other
	 * Open camera and pass it as parameter to CameraView
	 */
	@SuppressWarnings("deprecation")
	private void LayoutSetup() {
		Camera camera = Camera.open();
		
	    frameLayout = new FrameLayout(this);
	    frameLayout.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
	    
	    cameraView = new CameraView(this, camera);
	    frameLayout.addView(cameraView);
	    
	    poiView = new PoiView(this);
	    frameLayout.addView(poiView);
	    
	    setContentView(frameLayout);
	}
	
	/**
	 * Take care of camera and unregister listeners
	 */
	protected void onPause() {
		super.onPause();
		
		if(cameraView != null) {
			cameraView.onPause();
			cameraView = null;
		}
		
		mLocationManager.removeUpdates(this);
		mSensorManager.unregisterListener(this);
	}
	
	/**
	 * Call layout setup
	 * Request location and sensor updates
	 */
	protected void onResume() {
		super.onResume();
		
		LayoutSetup();
		
		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, this);
		mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1, this);
		
		mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);
		mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_FASTEST);
	
        // let's use last known location; gps provider is preferred
        Location lastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if(lastKnownLocation == null) {
        	lastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        	if(lastKnownLocation != null) {
        		Toast.makeText(getApplicationContext(), "Using last known location ...", Toast.LENGTH_LONG).show();
        		updateLocation(lastKnownLocation, true);
        	}
        	Toast.makeText(getApplicationContext(), "Obtaining location ...", Toast.LENGTH_LONG).show();
        } else {
        	Toast.makeText(getApplicationContext(), "Using last known location ...", Toast.LENGTH_LONG).show();
        	updateLocation(lastKnownLocation, true);
        }
	}
	
	/**
	 * Update location and relevant stops if necessary.
	 * @param location Latest location update
	 * @param updateRelevant Update flag
	 */
	private void updateLocation(Location location, boolean updateRelevant) {
		gApp.setCurrentLocation(location);
		
		if(updateRelevant) {
			gApp.findRelevantStops();
			relevant = gApp.getRelevant();
			if(relevant.size() == 0) {
				Toast.makeText(getApplicationContext(), "No relevant stops found ...", Toast.LENGTH_LONG).show();
			}
		}
		
		locationSource = location.getProvider();
		lat = location.getLatitude();
		lon = location.getLongitude();
		
		String url = String.format("http://maps.googleapis.com/maps/api/staticmap?center=%s,%s&size=%dx%d&sensor=false&maptype=roadmap&zoom=13",
		Double.toString(lat).replace(',', '.'), Double.toString(lon).replace(',', '.'), hudSize, hudSize);

		new DownloadImageTask().execute(url);
	}
	
	/**
	 * AsyncTask to download static map from google servers
	 */
	private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
		protected Bitmap doInBackground(String... urls) {
			String url = urls[0];
			Bitmap bitmap = null;
			try {
				InputStream in = new java.net.URL(url).openStream();
				bitmap = BitmapFactory.decodeStream(in);
				in.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return bitmap;
		}
		
		protected void onPostExecute(Bitmap result) {
			bitmap = result;
		}
	}
	
	// Location Listener Methods - START
	/**
	 * This method takes care of location events
	 * Store GPS coordinates if possible, otherwise use network
	 */
	public void onLocationChanged(Location location) {
		if(gApp.getCurrentLocation() == null || firstFix == true) {
			updateLocation(location, true);	
			firstFix = false;
			Toast.makeText(getApplicationContext(), "Received updated location fix ...", Toast.LENGTH_LONG).show();
			
		} else if(location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
			if(gApp.getCurrentLocation().getProvider().equals(LocationManager.NETWORK_PROVIDER)) {
				updateLocation(location, true);
			} else {
				if(location.distanceTo(gApp.getCalcLocation()) >= gApp.getTRESHOLD()) {
					updateLocation(location, true);
				} else {
					updateLocation(location, false);
				}
			}
			
		} else if(location.getProvider().equals(LocationManager.NETWORK_PROVIDER)) {
			if(gApp.getCurrentLocation().getProvider().equals(LocationManager.NETWORK_PROVIDER)) {
				updateLocation(location, true);
			}
		}
		
		poiView.invalidate();
	}

	public void onProviderDisabled(String provider) {
	}

	public void onProviderEnabled(String provider) {
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
	}
	// Location Listener Methods - END 
	
	// Sensor Event Listener Methods - START
	/**
	 * This method takes care of location events
	 * Recalculate azimuth and visible stops
	 */
	public void onSensorChanged(SensorEvent event) {
		if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			System.arraycopy(event.values, 0, accelerometerValues, 0, event.values.length);
		}
		if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
			System.arraycopy(event.values, 0, magneticFieldValues, 0, event.values.length);
		}
		
		float[] R = new float[9];
		
		SensorManager.getRotationMatrix(R, null, accelerometerValues, magneticFieldValues);
		// needs to be remapped for landscape mode
		SensorManager.remapCoordinateSystem(R, SensorManager.AXIS_Z, SensorManager.AXIS_MINUS_X, R);
		SensorManager.getOrientation(R, orientation);

		// orientation smoothing
		float calcOrientation = orientation[0];
		float alpha = 0.02f;

		if (Float.isNaN(filteredOrientation))
			filteredOrientation = calcOrientation;

		float diffOrientation = calcOrientation - filteredOrientation;
		if (diffOrientation > Math.PI)
			diffOrientation -= 2 * Math.PI;
		filteredOrientation = (float) (alpha * diffOrientation + filteredOrientation);

		// find visible stops according to azimuth
		gApp.findVisibleStops(Util.rad2deg(filteredOrientation));
		visible = gApp.getVisible();
		
		poiView.invalidate();
	}
	
	public void onAccuracyChanged(Sensor arg0, int arg1) {
	}
	// Sensor Event Listener Methods - END
	
	/**
	 * Inner class for DrawView
	 * @author Mike
	 *
	 */
	private class PoiView extends SurfaceView {
		private	NinePatchDrawable npd = (NinePatchDrawable)getResources().getDrawable(R.drawable.ultimate);
		
		// extra info
		private boolean drawExtraInfo = false;
		private Poi poiExtraInfo = null;
		private float xTouch = OUT_OF_SCREEN;
		private float yTouch = OUT_OF_SCREEN;

		public PoiView(Context context) {
			super(context);
			setWillNotDraw(false);
		}

		/**
		 * Draw on screen.
		 */
		@Override
		protected void onDraw(Canvas canvas) {
			//drawDebug(canvas);
			if(gApp.getCurrentLocation() != null) drawHud(canvas);
			drawStops(canvas);
			if(drawExtraInfo) drawExtraInfo(canvas);
		}
		
		/**
		 * Take care of touch event.
		 */
		@Override
		public boolean onTouchEvent(MotionEvent event) {
			if(event.getAction() == MotionEvent.ACTION_DOWN) {
				if(drawExtraInfo) {
					drawExtraInfo = false;
					poiExtraInfo = null;
					xTouch = OUT_OF_SCREEN;
					yTouch = OUT_OF_SCREEN;
				} else {
					drawExtraInfo = true;
					xTouch = event.getX();
					yTouch = event.getY();
				}
			}
			
			return true;
		}
		
		/**
		 * Method to draw stops on canvas.
		 * @param canvas Canvas we draw on.
		 */
		private void drawStops(Canvas canvas) {
			if(visible == null || visible.size() == 0)
				return;
			
			// sort by distance
			Collections.sort(visible, new Comparator<Poi>() {
				public int compare(Poi a, Poi b) {
					if (a.getDistance() > b.getDistance()) return 1;
					if (a.getDistance() < b.getDistance()) return -1;
					return 0;
				}
			});

			// remove certain duplicates
			Set<String> duplicates = new HashSet<String>(visible.size());
			int i = 0;
			while (i < visible.size()) {
				Poi p = visible.get(i);
				if (duplicates.contains(p.getName()) == false) {
					duplicates.add(p.getName());
					i++;
				} else {
					if (p.getDistance() <= 250) {
						i++;
					} else {
						visible.remove(i);
					}
				}
			}
			
			// draw farthest first
			Collections.reverse(visible);
			
			// setup text paint
			float textSize = 28f;		// initial minimum // ZMENA
			float textSizeDiff = 32f;	// total max is textSize + textSizeDiff // ZMENA
			TextPaint textPaint = new TextPaint();
			textPaint.setARGB(255, 255, 255, 255);
			textPaint.setAntiAlias(true);
			
			// calculate data related to width
			int cw = canvas.getWidth();
			double widthFOV = (double) cw / (double) gApp.getFOV();
			
			// calculate variables related to height
			int ch = canvas.getHeight();
			Location currentLocation = gApp.getCurrentLocation();
			float[] distanceBetween = new float[3];
			Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(), visible.get(0).getLat(), visible.get(0).getLon(), distanceBetween);
			double heightDistance = (double) ch / (double) distanceBetween[0];

			// draw each visible stop
			for(Poi p : visible) {
				double angleDiff = p.getAngleDiff() + (double) gApp.getFOVHALF();
				
				// manually calculate dimensions
				int left = (int) (widthFOV * angleDiff);
				int top;
				if(visible.size() != 1) {
					top = ch - (int) (heightDistance * p.getDistance());
				} else {
					top = (ch / 2) - (((int)textSize + (int)textSize + 10) / 2);
				}
				textSize += textSizeDiff * (float)top / (float)ch;
				textPaint.setTextSize(textSize);
				int right = left + 20;
				int bottom = top + (int)textSize + (int)textSize + 10;
				
				// determine size; fix for very short stop names
				int initialRight;
				Rect npdBounds = new Rect();
				StringBuilder sb = new StringBuilder(String.valueOf(p.getDistance()));
				
				textPaint.getTextBounds(p.getName(), 0, p.getName().length(), npdBounds);
				initialRight = npdBounds.right;
				sb.append("m");
				textPaint.getTextBounds(sb.toString(), 0, sb.length(), npdBounds);
				
				if(initialRight > npdBounds.right)
					npdBounds.right = initialRight;
				
				// put everything together
				npdBounds.left = left;
				npdBounds.top = top;
				npdBounds.right += right;
				npdBounds.bottom = bottom;
				
				// adjust to center
				int shiftLeft = npdBounds.width() / 2;
				npdBounds.left -= shiftLeft;
				npdBounds.right -= shiftLeft;
				
				// fix drawing at the bottom of the screen
				int shiftUp = 0;
				if(npdBounds.bottom > ch) {
					shiftUp = npdBounds.bottom - ch;
					npdBounds.top -= shiftUp;
					npdBounds.bottom -= shiftUp;
				}
				
				// check if this stop is "touched"
				if(drawExtraInfo == true) {
					if(npdBounds.left <= xTouch &&
					   npdBounds.right >= xTouch &&
					   npdBounds.top <= yTouch &&
					   npdBounds.bottom >= yTouch) {
						poiExtraInfo = p;
					}
				}
				
				// draw npd
				npd.setBounds(npdBounds);
				npd.draw(canvas);
				
				// draw text
				canvas.drawText(p.getName(), left + 10 - shiftLeft, top + textSize + 5 - shiftUp, textPaint);
				canvas.drawText(String.valueOf(p.getDistance()) + "m", left + 10 - shiftLeft, top + textSize + 5 + textSize - shiftUp, textPaint);
			}
			
			// end of cycle; "remove" touch
			xTouch = OUT_OF_SCREEN;
			yTouch = OUT_OF_SCREEN;
		}
		
		/**
		 * Method to draw stops on canvas.
		 * @param canvas Canvas we draw on.
		 */
		private void drawExtraInfo(Canvas canvas) {	
			if(poiExtraInfo == null)
				return;
			
			int x = 0;
			int y = 0;
			int width = canvas.getWidth();
			int height = 140;
			int textSize = 30;
			
			Rect npdBounds = new Rect(x, y, x + width, y + height);
			npd.setBounds(npdBounds);
			npd.draw(canvas);
			
			TextPaint textPaint = new TextPaint();
			textPaint.setARGB(255, 255, 255, 255);
			textPaint.setTextSize(textSize);
			textPaint.setAntiAlias(true);
			
			canvas.drawText("Name: " + poiExtraInfo.getName() + " (" + poiExtraInfo.getDistance() +"m)", x + 10, y + 5 + 30, textPaint);
			canvas.drawText("Latitude: " + poiExtraInfo.getLat(), x + 10, y + 5 + 60, textPaint);
			canvas.drawText("Longitude: " + poiExtraInfo.getLon(), x + 10, y + 5 + 90, textPaint);
			canvas.drawText("Lines: " + poiExtraInfo.getDescription(), x + 10, y + 5 + 120, textPaint);
		}
		
		/**
		 * Draw HUD
		 * This method is only for debugging.
		 * @param canvas Canvas we draw on.
		 */
		@SuppressLint({ "FloatMath", "FloatMath" })
		private void drawHud(Canvas canvas) {
			relevant = gApp.getRelevant();
			Location cloc = gApp.getCalcLocation();
			
			Paint paintBg = new Paint();
			paintBg.setARGB(255, 0, 0, 0);
			Paint paintGreen = new Paint();
			paintGreen.setARGB(255, 0, 255, 0);
			paintGreen.setStrokeWidth(3);
			Paint paintRed = new Paint();
			paintRed.setARGB(255, 255, 0, 0);
			paintRed.setStrokeWidth(3);
			Paint paintBlue = new Paint();
			paintBlue.setARGB(255, 0, 0, 255);
			paintBlue.setStrokeWidth(2);
			
			// center - kind of
			float fromLeft = canvas.getWidth() - (hudSize / 2);
			float fromTop = hudSize / 2;
			
			// draw black border
			canvas.drawRect(canvas.getWidth() - (hudSize + 2), 0, canvas.getWidth(), (hudSize + 2), paintBg);
			
			// draw white bg or a map
			paintBg.setARGB(255, 255, 255, 255);
			if(bitmap == null) {
				canvas.drawRect(canvas.getWidth() - hudSize, 0, canvas.getWidth(), hudSize, paintBg);
			} else {
				canvas.drawBitmap(bitmap, canvas.getWidth() - hudSize, 0, paintBg);
			}
			
			// draw direction			
			float angleRad = (float) ((Math.PI/2.0) - filteredOrientation);
			float userX = (float) Math.cos(angleRad);
			float userY = (float) -Math.sin(angleRad);
			canvas.drawLine(fromLeft, fromTop, userX * (hudSize / 2) + fromLeft, userY * (hudSize / 2) + fromTop, paintBlue);
			
			// draw stops
			if(relevant != null && cloc != null) {
				for(Poi p : relevant) {
					float pX = (float) ((p.getLon() - cloc.getLongitude()) / gApp.getD() * 70);
					float pY = (float) -((p.getLat() - cloc.getLatitude()) / gApp.getD() * 85);
					
					if(p.isVisible() == true) {
						canvas.drawPoint(pX + fromLeft, pY + fromTop, paintGreen);
					} else {
						canvas.drawPoint(pX + fromLeft, pY + fromTop, paintRed);
					}
				}
			}
		}
		
		/**
		 * Method to draw debug info onto canvas
		 * @param canvas
		 */
		@SuppressWarnings("unused")
		private void drawDebug(Canvas canvas) {
			TextPaint textPaint = new TextPaint();
			textPaint.setARGB(255, 255, 43, 43);
			textPaint.setTextSize(30);
			
			canvas.drawText("Location source: " + locationSource, 10, 30, textPaint);
			canvas.drawText("Latitude: " + lat, 10, 60, textPaint);
			canvas.drawText("Longitude: " + lon, 10, 90, textPaint);
			canvas.drawText(String.format("Azimuth: %5.2f", Util.normalize(Util.rad2deg(filteredOrientation))), 10, 120, textPaint);
			canvas.drawText(String.format("Pitch: %5.2f", Util.rad2deg(orientation[1])), 10, 150, textPaint);
			canvas.drawText(String.format("Roll: %5.2f", Util.rad2deg(orientation[2])), 10, 180, textPaint);
			
			if(visible != null) {
				int startY = 210;
				int max = 7;
				
				if(max > visible.size()) {
					max = visible.size();
				}
				
				for(int i = 0; i < max; i++) {
					Poi p = visible.get(i);
					canvas.drawText(p.getName() + " - " + p.getDescription(), 10, startY, textPaint);
					startY += 30;
				}
			}
		}
	}
}