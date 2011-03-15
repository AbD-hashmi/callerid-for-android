package com.integralblue.callerid;

import java.util.List;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;

import roboguice.application.RoboApplication;
import roboguice.config.AbstractAndroidModule;
import android.os.Build;

import com.google.inject.Module;
import com.google.inject.Scopes;
import com.integralblue.callerid.contacts.ContactsHelper;
import com.integralblue.callerid.inject.ContactsHelperProvider;
import com.integralblue.callerid.inject.FroyoHttpClientProvider;

public class CallerIDApplication extends RoboApplication {
    protected void addApplicationModules(List<Module> modules) {
    	super.addApplicationModules(modules);
    	modules.add(new AbstractAndroidModule() {
			@Override
			protected void configure() {
				bind(ContactsHelper.class).toProvider(ContactsHelperProvider.class).in(Scopes.SINGLETON);
				bind(CallerIDLookup.class).to(HttpCallerIDLookup.class).in(Scopes.SINGLETON);
				if(Integer.parseInt(Build.VERSION.SDK) < Build.VERSION_CODES.FROYO){
					bind(HttpClient.class).to(DefaultHttpClient.class).in(Scopes.SINGLETON);
				}else{
					bind(HttpClient.class).toProvider(FroyoHttpClientProvider.class).in(Scopes.SINGLETON);
				}
			}
    	});
    }
}