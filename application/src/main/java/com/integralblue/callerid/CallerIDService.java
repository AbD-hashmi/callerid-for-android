package com.integralblue.callerid;

import java.util.List;
import java.util.Locale;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import roboguice.service.RoboService;
import roboguice.util.Ln;
import roboguice.util.RoboAsyncTask;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.location.Address;
import android.location.Geocoder;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.blundell.tut.LoaderImageView;
import com.google.inject.Inject;
import com.integralblue.callerid.contacts.ContactsHelper;

public class CallerIDService extends RoboService {
	@Inject
	ContactsHelper contactsHelper;

	@Inject
	WindowManager windowManager;
	
	@Inject
	LayoutInflater layoutInflater;

	//@InjectView(R.layout.toast)
	View toastLayout;
	
	//@InjectView(R.id.text)
	TextView text;
	
	//@InjectView(R.id.address)
	TextView address;
	
	//@InjectView(R.id.map_view)
	MapView mapView;
	
	//@InjectView(R.id.image)
	LoaderImageView image;
	
	@Inject
	CallerIDLookup callerIDLookup;
	
	//@InjectResource(R.string.lookup_no_result)
	String lookupNoResult;
	
	//@InjectResource(R.string.lookup_error)
	String lookupError;
	
	//@InjectResource(R.string.lookup_in_progress)
	String lookupInProgress;

	@Inject
	SharedPreferences sharedPreferences;
	

	//@InjectResource(R.integer.default_popup_horizontal_gravity)
	int defaultPopupHorizontalGravity;
	
	//@InjectResource(R.integer.default_popup_vertical_gravity)
	int defaultPopupVerticalGravity;

	class LookupAsyncTask extends RoboAsyncTask<CallerIDResult> {

		final String phoneNumber;

		public LookupAsyncTask(String phoneNumber) {
			this.phoneNumber = phoneNumber;
		}

		public CallerIDResult call() throws Exception {
			return callerIDLookup.lookup(phoneNumber);
		}

		@Override
		protected void onPreExecute() throws Exception {
			super.onPreExecute();
			showLookupInProgress();
		}

		@Override
		protected void onSuccess(CallerIDResult callerIDResult)
				throws Exception {
			super.onSuccess(callerIDResult);
			showCallerID(callerIDResult);
		}

		@Override
		protected void onException(Exception e) throws RuntimeException {
			if (e instanceof CallerIDLookup.NoResultException) {
				showNoResult();
			} else {
				Ln.e(e);
				showError(e);
			}
		}

		@Override
		protected void onInterrupted(Exception e) {
			super.onInterrupted(e);
			hideCallerID();
		}
	};

	LookupAsyncTask currentLookupAsyncTask = null;
	
	class GeocoderAsyncTask extends RoboAsyncTask<Address> {

		final String locationName;
		
		final Geocoder geocoder = new Geocoder(context);

		public GeocoderAsyncTask(String locationName) {
			this.locationName = locationName;
		}

		public Address call() throws Exception {
			Address address = new Address(Locale.US);
			address.setLatitude(42.3583333);
			address.setLongitude(-71.0602778);
			return address;
			/*
			List<Address> addresses = geocoder.getFromLocationName(locationName, 1);
			if(addresses.size()==1){
				return addresses.get(0);
			}else{
				return null;
			}
			*/
		}

		@Override
		protected void onSuccess(Address address)
				throws Exception {
			if(address == null){
				//mapView.setVisibility(View.GONE);
			}else{
		        mapView.getController().setZoom(16);
				mapView.getController().setCenter(new GeoPoint(address.getLatitude(),address.getLongitude()));
				mapView.setVisibility(View.VISIBLE);
			}
		}

		@Override
		protected void onException(Exception e) throws RuntimeException {
			Ln.e(e);
			showError(e);
			mapView.setVisibility(View.GONE);
		}

		@Override
		protected void onInterrupted(Exception e) {
			super.onInterrupted(e);
			mapView.setVisibility(View.GONE);
		}
	};
	
	GeocoderAsyncTask geocoderAsyncTask = null;

	
	// This is the old onStart method that will be called on the pre-2.0
	// platform.  On 2.0 or later we override onStartCommand() so this
	// method will not be called.
	@Override
	public void onStart(Intent intent, int startId) {
	    handleCommand(intent, startId);
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		handleCommand(intent, startId);
		return START_NOT_STICKY;
	}
	
	protected void handleCommand(Intent intent, int startId){
		final String phone_state = intent
				.getStringExtra(TelephonyManager.EXTRA_STATE);
		final String phoneNumber = intent
				.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);

		// since we're about to start a new lookup or the phone stopped ringing,
		// we want to cancel any lookups in progress
		if (currentLookupAsyncTask != null)
			currentLookupAsyncTask.cancel(true);
		if (geocoderAsyncTask != null)
			geocoderAsyncTask.cancel(true);

		if (phone_state.equals(TelephonyManager.EXTRA_STATE_RINGING)
				&& !contactsHelper.haveContactWithPhoneNumber(phoneNumber)) {
			currentLookupAsyncTask = new LookupAsyncTask(phoneNumber);
			currentLookupAsyncTask.execute();
		} else {
			hideCallerID();
			stopSelf(startId);
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		
		toastLayout = layoutInflater.inflate(R.layout.toast, null);
		text = (TextView) toastLayout.findViewById(R.id.text);
		address = (TextView) toastLayout.findViewById(R.id.address);
		image = (LoaderImageView) toastLayout.findViewById(R.id.image);
		mapView = (MapView) toastLayout.findViewById(R.id.map_view);
		
		lookupNoResult = getString(R.string.lookup_no_result);
		lookupError = getString(R.string.lookup_error);
		lookupInProgress = getString(R.string.lookup_in_progress);
		
		defaultPopupHorizontalGravity = getResources().getInteger(R.integer.default_popup_horizontal_gravity);
		defaultPopupVerticalGravity = getResources().getInteger(R.integer.default_popup_vertical_gravity);
		
		WindowManager.LayoutParams params = new WindowManager.LayoutParams(
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
						| WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
				PixelFormat.TRANSLUCENT);
		//params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
		params.gravity = 0;
		String popupVerticalGravity = sharedPreferences.getString("popup_vertical_gravity", null);
		if(popupVerticalGravity == null)
			params.gravity |= defaultPopupVerticalGravity;
		else
			params.gravity |= Integer.parseInt(popupVerticalGravity);
		String popupHorizontalGravity = sharedPreferences.getString("popup_horizontal_gravity", null);
		if(popupHorizontalGravity == null)
			params.gravity |= defaultPopupHorizontalGravity;
		else
			params.gravity |= Integer.parseInt(popupHorizontalGravity);
		toastLayout.setVisibility(View.GONE);
		windowManager.addView(toastLayout, params);
	}

	@Override
	public IBinder onBind(Intent intent) {
		// Does not support binding
		return null;
	}

	@Override
	public void onDestroy() {
		windowManager.removeView(toastLayout);
	}

	protected void showLookupInProgress() {
		//mapView.setVisibility(View.GONE);
		address.setVisibility(View.GONE);
		text.setText(lookupInProgress);
		image.spin();
		toastLayout.setVisibility(View.VISIBLE);
	}

	protected void showError(Throwable t) {
		address.setVisibility(View.GONE);
		mapView.setVisibility(View.GONE);
		text.setText(lookupError);
		image.setImageDrawable(null);
		toastLayout.setVisibility(View.VISIBLE);
	}

	protected void showNoResult() {
		address.setVisibility(View.GONE);
		mapView.setVisibility(View.GONE);
		text.setText(lookupNoResult);
		image.setImageDrawable(null);
		toastLayout.setVisibility(View.VISIBLE);
	}

	protected void hideCallerID() {
		toastLayout.setVisibility(View.GONE);
	}

	protected void showCallerID(CallerIDResult callerIDResult) {
		if(callerIDResult.getAddress()==null){
			//address.setVisibility(View.GONE);
		}else{
			address.setVisibility(View.VISIBLE);
			address.setText(callerIDResult.getAddress());
			// since we're about to start a new lookup,
			// we want to cancel any lookups in progress
			if (geocoderAsyncTask != null)
				geocoderAsyncTask.cancel(true);
			geocoderAsyncTask = new GeocoderAsyncTask(callerIDResult.getAddress());
			geocoderAsyncTask.execute();
		}
		image.setImageDrawable(null);
		text.setText(callerIDResult.getName());
	}
}
