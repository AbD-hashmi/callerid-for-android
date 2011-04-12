package com.integralblue.callerid;

import java.util.List;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import roboguice.inject.InjectorProvider;
import roboguice.util.Ln;
import roboguice.util.RoboAsyncTask;
import android.location.Address;
import android.location.Geocoder;
import android.view.View;

public class GeocoderAsyncTask extends RoboAsyncTask<Address> {

	final String locationName;
	final MapView mapView;
	
	final Geocoder geocoder = new Geocoder(context);

	public GeocoderAsyncTask(String locationName, View layout) {
		((InjectorProvider)context).getInjector().injectMembers(this); //work around RoboGuice bug
		this.locationName = locationName;
		mapView = (MapView) layout.findViewById(R.id.map_view);
	}

	public Address call() throws Exception {
		List<Address> addresses = geocoder.getFromLocationName(locationName, 1);
		if(addresses.size()==1){
			return addresses.get(0);
		}else{
			return null;
		}
	}

	@Override
	protected void onSuccess(Address address)
			throws Exception {
		if(address == null){
			mapView.setVisibility(View.GONE);
		}else{
	        mapView.getController().setZoom(16);
			mapView.getController().setCenter(new GeoPoint(address.getLatitude(),address.getLongitude()));
			mapView.setVisibility(View.VISIBLE);
		}
	}

	@Override
	protected void onException(Exception e) throws RuntimeException {
		Ln.e(e);
		mapView.setVisibility(View.GONE);
	}

	@Override
	protected void onInterrupted(Exception e) {
		super.onInterrupted(e);
		mapView.setVisibility(View.GONE);
	}
}
