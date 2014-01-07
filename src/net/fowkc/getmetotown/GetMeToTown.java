package net.fowkc.getmetotown;

import java.util.List;

import net.fowkc.transportscraper.Journey;

import org.customsoft.stateless4j.StateMachine;
import org.customsoft.stateless4j.delegates.Action;

import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.service.dreams.DreamService;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

interface Callback {
	void invoke() throws Exception;
}

public class GetMeToTown extends DreamService implements OnClickListener
{
	
	private enum State
	{
		INITIALISING,
		IDLE,
		UPDATING,
	};
	
	private enum Event
	{
		GET_JOURNEYS_REQ,
		UPDATE_STARTED, 
		UPDATE_COMPLETE,
		REDRAW_DISPLAY_REQ,
	}
	
	StateMachine<State, Event> sm = new StateMachine<State, Event>(State.INITIALISING); 
	
	SharedPreferences prefs;
	UI ui;
	Updater updater;
	Handler handler;

	Callback updateCompleteCallback = new Callback() {
		public void invoke() throws Exception
		{
			smEvent(Event.UPDATE_COMPLETE);
		}
	};
	
	Action onStartUpdate = new Action() {
		@Override
		public void doIt() {
			ui.setUpdating();
			updater.startUpdate(updateCompleteCallback);
		}
	};
	
	Action onUpdateComplete = new Action() {
		@Override
		public void doIt() {
			updateDisplay();
		}
	};
	
	Action onUpdateDisplay  = new Action() {
		@Override
		public void doIt() {
			updateDisplay();
		}
	};
			
	@Override
	public void onDreamingStarted() {
		super.onDreamingStarted();
	}
	
	@Override
	public void onDreamingStopped() {
		super.onDreamingStarted();
	}
	
	@Override
	public void onClick(View v) {
		if (ui.isUpdateButton(v))
		{
			smEvent(Event.GET_JOURNEYS_REQ);
		}
	}
	
	@Override
	public void onAttachedToWindow() {
		
		super.onAttachedToWindow();
		setInteractive(true);
		setFullscreen(true);
		
		configureStateMachine();
		
		updater = new Updater();
		
		ui = new UI(this);
		ui.createLayout(0, null, this);
		
		handler = new Handler();
		
		createApplicationData();

		dumpPreferencesToLog();
		
		smEvent(Event.GET_JOURNEYS_REQ);
		
		handler.postDelayed(applicationTick, displayUpdateIntervalMs());
	}
	
	void configureStateMachine()
	{
		try {
			sm.Configure(State.INITIALISING)
				.Permit(Event.GET_JOURNEYS_REQ, State.UPDATING);
			
			sm.Configure(State.UPDATING)
				.OnEntry(onStartUpdate)
				.Permit(Event.UPDATE_COMPLETE, State.IDLE);
			
			sm.Configure(State.IDLE)
				.OnEntryFrom(Event.UPDATE_COMPLETE, onUpdateComplete)
				.Permit(Event.GET_JOURNEYS_REQ, State.UPDATING);
			
			sm.Configure(State.IDLE)
				.PermitReentry(Event.REDRAW_DISPLAY_REQ).OnEntry(onUpdateDisplay);
			
		} catch (Exception e) {
			Log.e(this.getPackageName(), e.getMessage(), e);
		}
	}
	
	void createApplicationData()
	{
		ui.addToImageMap("DefaultImage", R.drawable.ic_launcher);
		ui.addToImageMap("Skylink Nottingham", R.drawable.nottskylink_logo);
		ui.addToImageMap("Train", R.drawable.br_logo);
		ui.addToImageMap("18", R.drawable.route18_logo);
		ui.addToImageMap("20", R.drawable.route18_logo);
		ui.addToImageMap("sawley xprss", R.drawable.sawley_logo);
		ui.addToImageMap("Y5", R.drawable.y5_logo);
		
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
	}
	
	void dumpPreferencesToLog()
	{
		Log.i(this.getPackageName(), String.format("Minutes before removal: %d", minutesBeforeRemoval()));
		Log.i(this.getPackageName(), String.format("Maximum update interval: %d", maximumUpdateInterval()));
		Log.i(this.getPackageName(), String.format("Rows to display: %d", rowsToDisplay()));
		Log.i(this.getPackageName(), String.format("Switch to time remaining: %d", displayRemainingTimeWhenLessThan()));
		Log.i(this.getPackageName(), String.format("Display update interval: %d", displayUpdateIntervalMs()));
	}
	
	@Override
	public void onDetachedFromWindow() {
		super.onDetachedFromWindow();
	}
	
	private void updateDisplay()
	{
		int i = 0;
		
		List<Journey> journeys = updater.getSortedJourneys();
		
		int rowsToDisplay = rowsToDisplay();
		
		if (journeys.size() < rowsToDisplay)
		{
			ui.createLayout(journeys.size(), updater.lastUpdate(), this);
		}
		else
		{
			ui.createLayout(rowsToDisplay, updater.lastUpdate(), this);
		}
		
		for (Journey j : journeys)
		{
			int remainingTime = j.remainingTime();
			int highlightTime = getResources().getInteger(R.integer.default_hightlight_time);
			
			if (remainingTime > minutesBeforeRemoval())
			{
				highlightTime = Integer.parseInt(prefs.getString("minutes_to_highlight_preference", Integer.toString(highlightTime)));
				
				boolean highlight = remainingTime < highlightTime;
				boolean showAsTime = remainingTime > displayRemainingTimeWhenLessThan();
				
				StringBuilder displayString = new StringBuilder();
				
				if (showAsTime)
				{
					displayString.append(j.leavingTimeFormatted("HH:mm"));
				}
				else
				{
					displayString.append(Integer.toString(remainingTime));
					displayString.append(j.isDelayed() ? " m" : " minutes");
				}
				
				if (j.isDelayed())
				{
					displayString.append( " (" + Integer.toString(j.delay()) + "m late)" );
				}
				
				ui.setDisplayRow(i, j.transportName(), displayString.toString(), highlight);
			
				i++;
			}
		}
	}
	
	private int minutesBeforeRemoval()
	{
		int minutes = getResources().getInteger(R.integer.default_minutes_before_removal);
		minutes = Integer.parseInt(prefs.getString("minutes_before_removal_preference", Integer.toString(minutes)));
		return minutes;
	}
	
	private int maximumUpdateInterval()
	{	
		int minutes = getResources().getInteger(R.integer.maximum_update_interval);
		minutes = Integer.parseInt(prefs.getString("maximum_update_interval_preference", Integer.toString(minutes)));
		return minutes;
	}
	
	private int rowsToDisplay()
	{
		int rows = getResources().getInteger(R.integer.rows_to_show);
		rows = Integer.parseInt(prefs.getString("rows_to_display_preference", Integer.toString(rows)));
		return rows;
	}
	
	private int displayRemainingTimeWhenLessThan()
	{
		int minutes = getResources().getInteger(R.integer.switch_to_remaining_minutes);
		minutes = Integer.parseInt(prefs.getString("switch_to_remaining_minutes", Integer.toString(minutes)));
		return minutes;
	}
	
	private int displayUpdateIntervalMs()
	{
		return getResources().getInteger(R.integer.update_tick_seconds) * 1000;
	}
	
	private Runnable applicationTick = new Runnable() {
	   @Override
	   public void run() {

		   if (sm.getState() == State.IDLE)
		   {
			   int maximumUpdateInterval = maximumUpdateInterval();
			   
			   if (updater.isUpdateRequired(maximumUpdateInterval))
			   {
				   smEvent(Event.GET_JOURNEYS_REQ);
			   }
			   else
			   {
				   smEvent(Event.REDRAW_DISPLAY_REQ);
			   }
		   }
		   
		   handler.postDelayed(this, displayUpdateIntervalMs());
	   }
	};
	
	private void smEvent(Event event)
	{
		try
		{
			sm.Fire(event);
		} catch (Exception e) {
			String message = "State machine exception" +
				e.getMessage() +
				" in state " +
				sm.getState().toString() +
				" with event " +
				event.toString() +
				".";
			
			Log.e(this.getPackageName(), message);
		}
	}
}
