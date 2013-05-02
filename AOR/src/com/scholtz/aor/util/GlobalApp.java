package com.scholtz.aor.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import android.app.Application;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

/**
 * Globally accessible variables and methods
 * This class holds data such as stops or current location
 * @author Mike
 *
 */
public class GlobalApp extends Application {
	private final double D = 0.01;
	private final double TRESHOLD = 500.0;
	private final int FOV = 60;
	private final int FOVHALF = FOV / 2;

	private List<Poi> stops = null;
	private List<Poi> relevant = new ArrayList<Poi>();
	private List<Poi> visible = new ArrayList<Poi>();
	private Location currentLocation = null;
	private Location calcLocation = null;
	
	public int getFOV() {
		return FOV;
	}

	public int getFOVHALF() {
		return FOVHALF;
	}

	public double getTRESHOLD() {
		return TRESHOLD;
	}
	
	public double getD() {
		return D;
	}
	
	public List<Poi> getStops() {
		return stops;
	}

	public void setStops(List<Poi> stops) {
		this.stops = stops;
	}
	
	public List<Poi> getRelevant() {
		return relevant;
	}
	
	public List<Poi> getVisible() {
		return visible;
	}

	public Location getCurrentLocation() {
		return currentLocation;
	}

	public void setCurrentLocation(Location currentLocation) {
		this.currentLocation = currentLocation;
	}
	
	public Location getCalcLocation() {
		return calcLocation;
	}

	public void setCalcLocation(Location calcLocation) {
		this.calcLocation = calcLocation;
	}

	/**
	 * Finds relevant bus stops around the user according to current location
	 */
	public void findRelevantStops() {
		// TBD-s
		long start = System.nanoTime();
		// TBD-d

		relevant.clear();
		
		if(currentLocation != null) {
			for (Poi p : stops) {
				if (p.getLat() >= (currentLocation.getLatitude() - D) &&
					p.getLat() <= (currentLocation.getLatitude() + D) &&
					p.getLon() >= (currentLocation.getLongitude() - D) &&
					p.getLon() <= (currentLocation.getLongitude() + D)) {
					relevant.add(p);
				}
			}
			
			calcLocation = currentLocation;
		}

		// TBD-s
		double time = (System.nanoTime() - start) / 1000000.0;
		Log.d("AOR", "Found " + relevant.size() + " stops in " + time + "ms");
		// TBD-d
	}
	
	private long counter = 0;

	/**
	 * Finds visible stops according to users location and azimuth
	 * @param azimuth Device azimuth
	 */
	public void findVisibleStops(double azimuth) {
		if(currentLocation == null || relevant.size() == 0) {
			Log.d("AOR.noloc", "No location or relevant is empty ...");
			return;
		}
		
		// TBD-s
		counter++;
		long start = System.nanoTime();

		for (Poi p : relevant)
			p.setVisible(false);
		// TBD-e
		
		visible.clear();
		float[] distanceToVisible = new float[3];
		
		double currentX = currentLocation.getLongitude();
		double currentY = currentLocation.getLatitude();

		
		// TBD-s
		if (counter % 100 == 0)
			Log.d("AOR.azimuth", "oldAzimuth: " + azimuth);
		azimuth = 90 - azimuth;
		if (counter % 100 == 0)
			Log.d("AOR.azimuth", "newAzimuth: " + azimuth);
		// TBD-d

		for(Poi p : relevant) {
			double poiX = p.getLon();
			double poiY = p.getLat();
			
			double angle = Math.atan2(poiY - currentY, poiX - currentX);
			double degAngle = Util.rad2deg(angle);
			double poiAngle = Util.normalize(degAngle);
			
			// TBD-s
			if (counter % 100 == 0) {
				Log.d("AOR", p.getName() + " lat:" + p.getLat() + " lon:" + p.getLon() + " angle:" + poiAngle);
			}
			// TBD-d

			double angleDiff = Util.angleDiff(azimuth, poiAngle);
			if(Math.abs(angleDiff) <= FOVHALF) {
				p.setAngleDiff(angleDiff);
				Location.distanceBetween(currentY, currentX, p.getLat(), p.getLon(), distanceToVisible);
				p.setDistance((int) distanceToVisible[0]);
				visible.add(p);
				p.setVisible(true);
			}
		}

		// TBD-s
		double time = (System.nanoTime() - start) / 1000000.0;
		if (counter % 100 == 0)
			Log.d("AOR", "Added " + visible.size() + " stops in " + time + "ms");
		// TBD-d
	}

	/**
	 * Sorts stops by distance according to current location
	 * This method is currently not used.
	 */
	public void sortByDistance() {
		Collections.sort(stops, new Comparator<Poi>() {
			public int compare(Poi a, Poi b) {
				Location locA = new Location(LocationManager.PASSIVE_PROVIDER);
				locA.setLatitude(a.getLat());
				locA.setLongitude(a.getLon());

				Location locB = new Location(LocationManager.PASSIVE_PROVIDER);
				locB.setLatitude(b.getLat());
				locB.setLongitude(b.getLon());

				float distA = locA.distanceTo(currentLocation);
				float distB = locB.distanceTo(currentLocation);

				return Float.compare(distA, distB);
			}
		});
	}
}
