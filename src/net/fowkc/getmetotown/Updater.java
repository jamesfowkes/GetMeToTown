package net.fowkc.getmetotown;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import net.fowkc.transportscraper.Journey;
import net.fowkc.transportscraper.JourneyManager;
import net.fowkc.transportscraper.NextBusesScraper;
import net.fowkc.transportscraper.TrainTimesScraper;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import android.os.AsyncTask;
import android.util.Log;

public class Updater {

	private static final JourneyManager journeyManager = new JourneyManager();
	private static final NextBusesScraper nextBusesScraper = new NextBusesScraper();
	private static final TrainTimesScraper trainTimesScraper = new TrainTimesScraper();
	
	private static final URL urls[] = {
		nextBusesScraper.getURL("ntsawmgw"),
		nextBusesScraper.getURL("ntsdatat"),
		trainTimesScraper.getURL("BEE", "NOT")
	};
	
	private DateTime lastUpdated = new DateTime(0);
	
	private Callback onUpdateCompleteCallback;
	public void startUpdate(Callback onUpdateComplete) {
		onUpdateCompleteCallback = onUpdateComplete;
		journeyManager.clearJourneys();
		new UpdateTask().execute(urls);	
	}
	
	public List<Journey> getSortedJourneys()
	{
		journeyManager.sortJourneys();
		return journeyManager.journeys();
	}
	
	public boolean isUpdateRequired(int maximumUpdateTime)
	{
		
		/* An update is required if any journey has a
		 * remaining time of <= 0 minutes
		 * or if the maximum update time is exceeded 
		 */
		
		boolean needToUpdate = false;
		
		for (Journey j : journeyManager.journeys())
		{
			needToUpdate |= (j.remainingTime() == 0);
		}
		
		Duration diff = new Duration(lastUpdated, new DateTime());
		Log.i("Updater", Long.toString(diff.getStandardMinutes()));
		
		needToUpdate |= (diff.getStandardMinutes() > maximumUpdateTime);
		
		return needToUpdate;
	}
	
	public DateTime lastUpdate()
	{
		return lastUpdated;
	}
	
	public class UpdateTask extends AsyncTask<URL, Integer, Long> {

		private List<String> urlData;
		
		protected Long doInBackground(URL... urls) {
			
			urlData = new ArrayList<String>();
			StringBuilder dataCache;

	        for (int i = 0; i < urls.length; i++) {
	        	
	        	try {
	                URLConnection conn = urls[i].openConnection();
	                BufferedReader reader = new BufferedReader(
	                        new InputStreamReader(conn.getInputStream()));
	                String line = "";
	                dataCache = new StringBuilder();
	                while ((line = reader.readLine()) != null) {
	                	dataCache.append(line);
	                }
	                
	                urlData.add(dataCache.toString());
	                
	            } catch (Exception e) {
	            	urlData.add(e.toString());
	            }
	        	
	            // Escape early if cancel() is called
	            if (isCancelled()) break;
	        }
	        return (long) 0;
		}
		
	    protected void onPostExecute(Long result) {
			onUpdateComplete(urlData);
	    }
	    
		private void onUpdateComplete(List<String> urlData)
		{
			lastUpdated = new DateTime();
			
			try {
				Journey hawthornGroveJourneys[] = nextBusesScraper.parseData("Hawthorn Grove", urlData.get(0));
				Journey padgeRoadJourneys[] = nextBusesScraper.parseData("Padge Road", urlData.get(1));
				Journey trainJourneys[] = trainTimesScraper.parseData("Beeston", urlData.get(2));
				journeyManager.addJourneys(hawthornGroveJourneys);
				journeyManager.addJourneys(padgeRoadJourneys);
				journeyManager.addJourneys(trainJourneys);

				onUpdateCompleteCallback.invoke();
				
			} catch (Exception e) {
				Log.e("Updater", e.getMessage());
			}
		}
	}
}

