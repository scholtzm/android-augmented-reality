package com.scholtz.aor.helper;

import java.util.List;
import android.app.Application;

public class GlobalVariables extends Application {
	public List<Poi> stops;

	public List<Poi> getStops() {
		return stops;
	}

	public void setStops(List<Poi> stops) {
		this.stops = stops;
	}
}
