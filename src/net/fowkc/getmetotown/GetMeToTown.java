package net.fowkc.getmetotown;

import java.util.Date;
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
			sm.Fire(Event.UPDATE_COMPLETE);
		}
	};
	
	Action onStartUpdate = new Action() {
		@Override
		public void doIt() {
			ui.setButtonText(R.string.button_updating);
			updater.startUpdate(updateCompleteCallback);
		}
	};
	
	Action onUpdateComplete = new Action() {
		@Override
		public void doIt() {
			onUpdateComplete();
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
		ui.createLayout(getResources().getInteger(R.integer.rows_to_show), this);
		
		handler = new Handler();
		
		createApplicationData();

		smEvent(Event.GET_JOURNEYS_REQ);
		
		int seconds_to_next_update = getResources().getInteger(R.integer.update_tick_seconds);
		handler.postDelayed(applicationTick, seconds_to_next_update * 1000);
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
	
	@Override
	public void onDetachedFromWindow() {
		super.onDetachedFromWindow();
	}
	
	private void updateDisplay()
	{
		int i = 0;
		
		List<Journey> journeys = updater.getSortedJourneys();
		
		if (journeys.size() < getResources().getInteger(R.integer.rows_to_show))
		{
			ui.createLayout(journeys.size(), this);
		}
		else
		{
			ui.createLayout(getResources().getInteger(R.integer.rows_to_show), this);
		}
		
		for (Journey j : updater.getSortedJourneys())
		{
			int remainingTime = j.remainingTime();
			int highlightTime = getResources().getInteger(R.integer.default_hightlight_time);
			
			if (remainingTime > minutesBeforeRemoval())
			{
				highlightTime = Integer.parseInt(prefs.getString("minutes_to_highlight_preference", Integer.toString(highlightTime)));
				
				boolean highlight = remainingTime < highlightTime;
				String displayString = Integer.toString(remainingTime) + " minutes";
				
				ui.setDisplayRow(i, j.transportName(), displayString, highlight);
			
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
	
	private void onUpdateComplete()
	{
		updateDisplay();
		ui.setButtonText(getString(R.string.button_idle) + String.format(" (Last Update at %1$tH:%1$tM)", new Date()));
	}
		
	private Runnable applicationTick = new Runnable() {
	   @Override
	   public void run() {

		   if (updater.isUpdateRequired(getResources().getInteger(R.integer.maximum_update_interval)))
		   {
			   smEvent(Event.GET_JOURNEYS_REQ);
		   }
		   else
		   {
			   smEvent(Event.REDRAW_DISPLAY_REQ);
		   }
		   
		   int seconds_to_next_update = getResources().getInteger(R.integer.update_tick_seconds);
		   handler.postDelayed(this, seconds_to_next_update * 1000);
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
