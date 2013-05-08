package com.scholtz.aor.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import android.app.Application;
import android.location.Location;
import android.location.LocationManager;

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
	}
	
	/**
	 * Finds visible stops according to users location and azimuth
	 * @param azimuth Device azimuth
	 */
	public void findVisibleStops(double azimuth) {
		if(currentLocation == null || relevant.size() == 0)
			return;
		
		visible.clear();
		float[] distanceToVisible = new float[3];
		
		double currentX = currentLocation.getLongitude();
		double currentY = currentLocation.getLatitude();
		
		// rotate azimuth according to atan2
		azimuth = 90 - azimuth;

		for(Poi p : relevant) {
			double poiX = p.getLon();
			double poiY = p.getLat();
			
			double angle = Math.atan2(poiY - currentY, poiX - currentX);
			double degAngle = Util.rad2deg(angle);
			double poiAngle = Util.normalize(degAngle);

			double angleDiff = Util.angleDiff(azimuth, poiAngle);
			if(Math.abs(angleDiff) <= FOVHALF) {
				Location.distanceBetween(currentY, currentX, p.getLat(), p.getLon(), distanceToVisible);
				p.setVisible(true);
				p.setAngleDiff(angleDiff);
				p.setDistance((int) distanceToVisible[0]);
				visible.add(p);
			} else {
				p.setVisible(false);
			}
		}
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
