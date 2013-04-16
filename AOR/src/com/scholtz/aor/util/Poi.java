package com.scholtz.aor.util;

/**
 * POI class that represents stop from database 
 * @author Mike
 *
 */
public class Poi {
	private int poiId;
	private String type;
	private String name;
	private double lat;
	private double lon;
	private String description;
	
	public Poi() {
	}

	public Poi(int poiId, String type, String name, double lat, double lon, String description) {
		this.setPoiId(poiId);
		this.setType(type);
		this.setName(name);
		this.setLat(lat);
		this.setLon(lon);
		this.setDescription(description);
	}

	public int getPoiId() {
		return poiId;
	}

	public void setPoiId(int poiId) {
		this.poiId = poiId;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public double getLat() {
		return lat;
	}

	public void setLat(double lat) {
		this.lat = lat;
	}

	public double getLon() {
		return lon;
	}

	public void setLon(double lon) {
		this.lon = lon;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
	
	
}
