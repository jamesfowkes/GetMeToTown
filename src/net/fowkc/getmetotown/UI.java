package net.fowkc.getmetotown;

import java.util.HashMap;
import java.util.Map;

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
	
	public void createLayout(int rows, OnClickListener onClickListener)
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
		TableRow.LayoutParams textParams = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT, 1);
		
		textParams.gravity = Gravity.CENTER;
		
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
			textViews[i].setPadding(0, 10, 10, 10);
			textViews[i].setLayoutParams(textParams);
			tableRow.addView(textViews[i]);
			
			tableLayout.addView(tableRow);
		}
		
		// The bottom row has the update button
		updateBtn = new Button(service);
		updateBtn.setText(R.string.button_idle);
		updateBtn.setBackgroundColor(Color.TRANSPARENT);
		updateBtn.setTextColor(Color.GRAY);
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
	
	public void setButtonText(int resid) { updateBtn.setText(resid); }
	
	public void setButtonText(String text) { updateBtn.setText(text); }
	
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
