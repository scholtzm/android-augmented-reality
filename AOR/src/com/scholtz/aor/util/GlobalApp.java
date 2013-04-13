package com.scholtz.aor.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import android.app.Application;
import android.location.Location;
import android.location.LocationManager;

public class GlobalApp extends Application {
	private final double D = 0.01;
	private final double treshhold = 500;
	private List<Poi> stops = null;
	private List<Poi> relevant = new ArrayList<Poi>();
	private Location currentLocation = null;

	public List<Poi> getStops() {
		return stops;
	}

	public void setStops(List<Poi> stops) {
		this.stops = stops;
	}
	
	public Location getCurrentLocation() {
		return currentLocation;
	}

	public void setCurrentLocation(Location currentLocation) {
		this.currentLocation = currentLocation;
	}
	
	public double getTreshhold() {
		return treshhold;
	}
	
	/**
	 * Finds relevant bus stops around the user
	 * @param loc - Current location
	 * @param stops - List of all stops
	 * @param relevant - List of relevant stops - to be populated
	 * @param D - Distance (half of the length of the square around the user)
	 */
	public void findRelevantStops() {
		for(Poi p : stops) {
			if(p.getLat() >= (currentLocation.getLatitude() - D) &&
			   p.getLat() <= (currentLocation.getLatitude() + D) &&
			   p.getLon() >= (currentLocation.getLongitude() - D) &&
			   p.getLon() <= (currentLocation.getLongitude() + D)) {
				relevant.add(p);
			}
		}
	}
	
	/**
	 * Sorts stops by distance according to current location
	 * @param loc - Current location
	 * @param list - List of all stops
	 */
	public void sortByDistance(final Location loc, ArrayList<Poi> list) {
		Collections.sort(list, new Comparator<Poi>() {
		    public int compare(Poi a, Poi b) {
				Location locA = new Location(LocationManager.PASSIVE_PROVIDER);
				locA.setLatitude(a.getLat());
				locA.setLongitude(a.getLon());
				
				Location locB = new Location(LocationManager.PASSIVE_PROVIDER);
				locB.setLatitude(b.getLat());
				locB.setLongitude(b.getLon());
				
		    	float distA = locA.distanceTo(loc);
		    	float distB = locB.distanceTo(loc);
		    	
		    	return Float.compare(distA, distB);
		    }
		});
	}
}
