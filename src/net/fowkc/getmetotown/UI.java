package net.fowkc.getmetotown;

import java.util.HashMap;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import android.graphics.Color;
import android.service.dreams.DreamService;

import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class UI {
	
	private Button updateBtn;
	private TextView[] textViews;
	private ImageView[] imageViews;
	
	Map<String, Integer> imageMap = new HashMap<String, Integer>();
	
	DreamService service;
	int numberOfRows = 0;
	
	public UI(DreamService parentService)
	{
		service = parentService;
	}
	
	public void createLayout(int rows, DateTime lastUpdated, OnClickListener onClickListener)
	{
		numberOfRows = rows;
		
		LayoutParams linearParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		
		// Layout parameters for the table layout (inside linear layout)
		LinearLayout.LayoutParams tableParams = new LinearLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT, 10);
		LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT, 1);
		
		// Layout parameters for the table rows (inside table layout)
		TableLayout.LayoutParams rowParams = new TableLayout.LayoutParams(TableLayout.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT, 1);
		
		// Layout parameters for the image and text views (inside table row)
		TableRow.LayoutParams imageParams = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.MATCH_PARENT);
		TableRow.LayoutParams textParams = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT);
		
		textParams.gravity = Gravity.LEFT + Gravity.CENTER_VERTICAL;
		
		LinearLayout linearLayout = new LinearLayout(service);
		linearLayout.setLayoutParams(linearParams);
		linearLayout.setOrientation(LinearLayout.VERTICAL);
		
		TableLayout tableLayout = new TableLayout(service);
		tableLayout.setLayoutParams(tableParams);
		
		textViews = new TextView[rows];
		imageViews = new ImageView[rows];
		
		// Each row has one image view and one text view
		for(int i = 0; i < rows; ++i)
		{
			TableRow tableRow = new TableRow(service);
			tableRow.setLayoutParams(rowParams);
			
			imageViews[i] = new ImageView(service);
			imageViews[i].setPadding(0, 10, 10, 10);
			imageViews[i].setLayoutParams(imageParams);
			
			tableRow.addView(imageViews[i]);
			
			textViews[i] = getNewTextView(i);
			textViews[i].setLayoutParams(textParams);
			tableRow.addView(textViews[i]);
			
			tableLayout.addView(tableRow);
		}
		
		// The bottom row has the update button and time
		
		updateBtn = new Button(service);
		
		String buttonText;
		
		if (lastUpdated != null)
		{
			buttonText = new DateTime().toString(DateTimeFormat.forPattern("HH:mm")) +
					" (Last update " + lastUpdated.toString(DateTimeFormat.forPattern("HH:mm")) + ")";
		}
		else
		{
			buttonText = new DateTime().toString(DateTimeFormat.forPattern("HH:mm"));
		}
		
		updateBtn.setText(buttonText);
		updateBtn.setBackgroundColor(Color.TRANSPARENT);
		updateBtn.setTextColor(Color.GRAY);
		updateBtn.setTextSize(service.getResources().getInteger(R.integer.max_text_size));
		updateBtn.setOnClickListener(onClickListener);
		updateBtn.setLayoutParams(buttonParams);
		
		linearLayout.addView(tableLayout);
		linearLayout.addView(updateBtn);
		
		service.setContentView(linearLayout);
	}
	
	public void addToImageMap(String key, int value)
	{
		imageMap.put(key, value);
	}
	
	public boolean isUpdateButton(View v)
	{
		return (v instanceof Button) && ((Button)v == updateBtn);
	}
	
	public void setUpdating()
	{
		updateBtn.setText(R.string.button_updating);
	}
	
	public void setDisplayRow(int i, String imageKey, String displayString, boolean highlight)
	{
		if (i < numberOfRows)
		{
			imageViews[i].setImageResource(imageMap.get(imageKey));
			textViews[i].setText(displayString);
			textViews[i].setTextColor(highlight ? Color.RED : Color.GRAY);
		}
	}
	
	public int rowCount()
	{
		return numberOfRows;
	}
	
	private TextView getNewTextView(int i)
	{
		int textSize = service.getResources().getInteger(R.integer.max_text_size) - (3 * i);
		
		TextView textView = new TextView(service);
		
		textView.setGravity(Gravity.LEFT);
		textView.setTextSize(textSize);
		textView.setTextColor(Color.GRAY);
		
		return textView; 
	}
}
