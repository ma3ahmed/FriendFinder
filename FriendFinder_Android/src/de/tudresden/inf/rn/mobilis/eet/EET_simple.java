package de.tudresden.inf.rn.mobilis.eet;

import java.io.File;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

/**
 * class provides a simple gps-tracking and gpx-emulation with the same interface like the 
 * complex tracking-class EET
 */
public class EET_simple implements ILocationProxy, LocationListener {
	private static final String TAG = "EET_simpe";
	/**
	 * save the gps-positions and save them in a gpx-File
	 */
	private GPXTrack mGPX;
	/**
	 * LocationManager for access to the GPS-Unit
	 */
	private LocationManager locMan;

	private Context ctx;
	/**
	 * last received location
	 */
	private Location lastLoc;
	/**
	 * extern LocationListener, registered from the BackgroundService
	 * called at every location-updates
	 */
	private LocationListener externListener;
	/**
	 * the gps-update-interval
	 */
	private int gpsIntervall = 5000;
	/**
	 * is tracking enabled
	 */
	private boolean isTracking = false;
	/**
	 * count the gps-updates
	 */
	private int countGPSUpdates = 0;
	/**
	 * get the location-updates from the gps-unit or from the emulation-file
	 */
	private Boolean emulationMode = false;
	/**
	 * class for reading a gpx-file and emulate it
	 */
	private TrackEmulation trackEmulation = null;

	/******************* Handler *************************/
	/**
	 * called, if the emulation has stopped
	 */
	protected Handler onEmulationEndHandler = new Handler(Looper.getMainLooper()) {
		@Override
		public void handleMessage(Message msg) {	
			Log.i(TAG, "Emulation terminated");
		}
	};

	/******************** class specific functions ******************************/

	public EET_simple(Context ctx, String serviceJID, IEETProxy mock) {
		this.ctx = ctx;
		this.mGPX = new GPXTrack();
		
		File gpxFile = GPXTrack.getGPXFile("emulation");
		emulationMode = gpxFile.exists();
		
		initGPS();
	}

	/**
	 * start tracking or emulation
	 */
	public void start() {
		if(emulationMode) Toast.makeText(ctx.getApplicationContext(), "Tracking start" + " (Emulation, " + trackEmulation.countPoints() + " Points)", Toast.LENGTH_SHORT).show();
		else Toast.makeText(ctx.getApplicationContext(), "Tracking start", Toast.LENGTH_SHORT).show();
		Log.i(TAG, "start Tracking");
		
		enableGPS();		
		
		isTracking = true;
	}

	/**
	 * stop tracking or emulation
	 */
	public void stop() {
		disableGPS();
		
		if (!emulationMode)	
			mGPX.saveFile();
		else

		if(isTracking) Toast.makeText(ctx.getApplicationContext(), "Tracking stop", Toast.LENGTH_SHORT).show();
		isTracking = false;
	}

	/**
	 * returns the last received location
	 */
	public Location getLastLocation() {
		return lastLoc;
	}

	/**
	 * register a LocationListener, there can only be one registered
	 */
	public void registerLocationListener(LocationListener locationListener) {
		this.externListener = locationListener;
	}
	
	/************* gps *****************/
	
	protected void initGPS(){
		locMan = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
		lastLoc = locMan.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		if(!emulationMode){
			
		}
		else{
			lastLoc = new Location("");
			trackEmulation = new TrackEmulation(
					GPXTrack.getGPXFile("emulation"), this,
					null, onEmulationEndHandler, 10);
			Log.i(TAG, "Emulation mode");
		}
	}
	
	protected void enableGPS(){
		Log.i(TAG, "enableGPS");
				
		if(!emulationMode){
			locMan.removeUpdates(this);
			locMan.requestLocationUpdates(LocationManager.GPS_PROVIDER, gpsIntervall,
					0, this);
		} else {
			trackEmulation.setHandlers(this, null, onEmulationEndHandler);
			trackEmulation.start();
			
			// energytest
			locMan.requestLocationUpdates(LocationManager.GPS_PROVIDER, gpsIntervall,
					0, testLocListener);
		}
	}
	
	protected void disableGPS(){
		
		if(!emulationMode){
			locMan.removeUpdates(this);
		} else {
			if(trackEmulation != null) trackEmulation.stop();
			
			// energytest
			locMan.removeUpdates(testLocListener);
		}
	}
	
	/**************** Listener **********************/
	
	private LocationListener testLocListener = new LocationListener(){
		@Override
		public void onLocationChanged(Location arg0) {
			String bat = BatteryManager.EXTRA_LEVEL + " von " + BatteryManager.EXTRA_SCALE;
			Log.d(TAG, "testLocListener:onLocationChanged (" + bat + ")");
			Toast.makeText(ctx.getApplicationContext(), "tLL (" + bat + ")", Toast.LENGTH_SHORT).show();
			countGPSUpdates++;
		}
		@Override
		public void onProviderDisabled(String arg0) {}
		@Override
		public void onProviderEnabled(String arg0) {}
		@Override
		public void onStatusChanged(String arg0, int arg1, Bundle arg2) {}
	};

	
	@Override
	public void onLocationChanged(Location arg0) {
		if (externListener != null)
			externListener.onLocationChanged(arg0);

		Log.v(TAG,
				"onLocationChanged: N" + arg0.getLatitude() + " E"
						+ arg0.getLongitude());

	}

	@Override
	public void onProviderDisabled(String arg0) {
		if (externListener != null)
			externListener.onProviderDisabled(arg0);

	}

	@Override
	public void onProviderEnabled(String arg0) {
		if (externListener != null)
			externListener.onProviderEnabled(arg0);

	}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		if (externListener != null)
			externListener.onStatusChanged(arg0, arg1, arg2);

	}

	/******************* Getter/Setter *********************/

	public Boolean isTracking() {
		return isTracking;
	}

	public void setServiceJID(String serviceJID) {
	}
	
	public int getPredStatus(){
		return 0;
	}
	
	public GPXTrack getPredTempTrack(){
		return new GPXTrack();
	}
	
	public String getEETStatus(){
		return "simple GPS tracking (" + countGPSUpdates + " updates)";
	}


}
