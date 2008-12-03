// Created by plusminus on 00:47:05 - 02.10.2008
package org.andnav.osm.adt.util;


import java.util.GregorianCalendar;

import org.andnav.osm.adt.GPSGeoLocation;
import org.andnav.osm.adt.GeoPoint;
import org.andnav.osm.util.constants.OSMConstants;

import android.location.Address;
import android.location.Location;

/**
 * Converts some usual types from one to another.
 * @author Nicolas Gramlich
 *
 */
public class TypeConverter implements OSMConstants {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	// ===========================================================
	// Constructors
	// ===========================================================

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods from SuperClass/Interfaces
	// ===========================================================

	// ===========================================================
	// Methods
	// ===========================================================
	
	public static GeoPoint locationToGeoPoint(final Location aLoc){
		return new GeoPoint((int)(aLoc.getLatitude() * 1E6), (int)(aLoc.getLongitude() * 1E6));
	}
	
	public static GeoPoint addressToGeoPoint(final Address pAddress){
		return new GeoPoint((int)(pAddress.getLatitude() * 1E6),  (int)(pAddress.getLongitude() * 1E6));
	}
	
	public static GPSGeoLocation locationToGPSGeoLocation(final Location aLoc){
		final int altitude = (aLoc.hasAltitude()) ? (int)aLoc.getAltitude() : NOT_SET;
		final int speed = (aLoc.hasSpeed()) ? (int)aLoc.getSpeed() : NOT_SET;
		final int latitudeE6 = (int)(aLoc.getLatitude() * 1E6);
		final int longitudeE6 = (int)(aLoc.getLongitude() * 1E6);
		final int bearing = (aLoc.hasBearing()) ? (int)aLoc.getBearing() : NOT_SET;
		final int numSatellites = aLoc.getExtras().getInt("satellites", NOT_SET);
		final long timeStamp = (aLoc.getTime() != 0) ? aLoc.getTime() : new GregorianCalendar().getTimeInMillis(); 
		
		return new GPSGeoLocation(latitudeE6, longitudeE6, timeStamp, altitude, bearing, speed, numSatellites);
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
