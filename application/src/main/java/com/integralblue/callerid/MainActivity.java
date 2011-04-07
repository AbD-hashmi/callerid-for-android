package com.integralblue.callerid;

import roboguice.activity.RoboActivity;
import roboguice.inject.InjectResource;
import roboguice.inject.InjectView;
import roboguice.util.Ln;
import roboguice.util.RoboAsyncTask;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.blundell.tut.LoaderImageView;
import com.google.inject.Inject;
import com.integralblue.callerid.contacts.ContactsHelper;

public class MainActivity extends RoboActivity {
	@InjectView(R.id.phone_number) EditText phoneNumber;
	
	@InjectResource(R.string.lookup_no_result)
	String lookupNoResult;
	
	@InjectResource(R.string.lookup_error)
	String lookupError;
	
	@InjectResource(R.string.lookup_in_progress)
	String lookupInProgress;
	
	@InjectView(R.id.image) LoaderImageView image;
	
	@InjectView(R.id.text)
	TextView text;
	
	@InjectView(R.id.perform_lookup)
	Button performLookup;
	
	@InjectView(R.id.create_contact)
	Button createContact;
	
	@Inject
	CallerIDLookup callerIDLookup;
	
	@Inject
	ContactsHelper contactsHelper;
	
	LookupAsyncTask currentLookupAsyncTask = null;

	//contains the last lookup result
	private CallerIDResult callerIDResult = null;
	
	class LookupAsyncTask extends RoboAsyncTask<CallerIDResult> {

		final CharSequence phoneNumber;

		public LookupAsyncTask(CharSequence phoneNumber) {
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
		protected void onSuccess(CallerIDResult result)
				throws Exception {
			super.onSuccess(callerIDResult);
			callerIDResult = result;
			showCallerID();
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
	
    /**
     * Called when the activity is first created.
     * @param savedInstanceState If the activity is being re-initialized after
     * previously being shut down then this Bundle contains the data it most
     * recently supplied in onSaveInstanceState(Bundle). <b>Note: Otherwise it is null.</b>
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ln.d("onCreate");
        
     // Injection doesn't happen until you call setContentView()
        
        setContentView(R.layout.main);
        
        // Because we want compatibility with Android 1.5, we can't use the new xml-style android:onClick method of binding click listeners :-(
        performLookup.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	performLookupClick(v);
            }
        });
        createContact.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	createContactClick(v);
            }
        });
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main_menu, menu);
        return true;
    }
    
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.settings:
			startActivity(new Intent(this, PreferencesActivity.class));
			return true;
		case R.id.help:
			Intent viewIntent = new Intent("android.intent.action.VIEW", Uri.parse("http://www.integralblue.com/callerid-for-android"));
			startActivity(viewIntent);  
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
    
    public void performLookupClick(View button){
		// since we're about to start a new lookup,
		// we want to cancel any lookups in progress
		if (currentLookupAsyncTask != null)
			currentLookupAsyncTask.cancel(true);
    	showLookupInProgress();
		currentLookupAsyncTask = new LookupAsyncTask(phoneNumber.getText());
		currentLookupAsyncTask.execute();
    }
    
    public void createContactClick(View button){
    	contactsHelper.createContactEditor(callerIDResult);
    }

	protected void showLookupInProgress() {
		createContact.setVisibility(View.GONE);
		image.setVisibility(View.VISIBLE);
		text.setVisibility(View.VISIBLE);
		text.setText(lookupInProgress);
		image.spin();
	}

	protected void showError(Throwable t) {
		createContact.setVisibility(View.GONE);
		text.setText(lookupError);
		image.setImageDrawable(null);
	}

	protected void showNoResult() {
		createContact.setVisibility(View.GONE);
		text.setText(lookupNoResult);
		image.setImageDrawable(null);
	}

	protected void hideCallerID() {
		createContact.setVisibility(View.GONE);
		image.setVisibility(View.GONE);
		text.setVisibility(View.GONE);
	}

	protected void showCallerID() {
		if(contactsHelper.haveContactWithPhoneNumber(callerIDResult.getPhoneNumber())){
			createContact.setVisibility(View.GONE);
		}else{
			createContact.setVisibility(View.VISIBLE);
		}
		image.setImageDrawable(null);
		text.setText(callerIDResult.getName());
	}
}
