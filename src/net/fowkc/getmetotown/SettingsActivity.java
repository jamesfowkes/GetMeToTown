package net.fowkc.getmetotown;

import android.app.Activity;
import android.os.Bundle;

public class SettingsActivity extends Activity {

	 @Override
	 protected void onCreate(Bundle savedInstanceState) {
	  // TODO Auto-generated method stub
	  super.onCreate(savedInstanceState);
	  
	  getFragmentManager().beginTransaction().replace(android.R.id.content,
	                new Settings()).commit();
	 }
}