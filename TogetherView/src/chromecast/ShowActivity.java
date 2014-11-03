package chromecast;


import com.example.chromecastv2.R;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.Toast;

public class ShowActivity extends ActionBarActivity {

	ImageButton prevBtn, nextBtn, mainScreenBtn, secondScreenBtn;
	Object object;
	boolean identifyIntent = false;
	public static final String NAMESPACE = "urn:x-cast:com.ls.cast.sample";

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_interface);

		prevBtn = (ImageButton)findViewById(R.id.prevbtn);
		nextBtn = (ImageButton)findViewById(R.id.nextbtn);
		mainScreenBtn = (ImageButton)findViewById(R.id.mainscreenbtn);
		secondScreenBtn = (ImageButton)findViewById(R.id.secondscreenbtn);

		object = (Object)ApiClientData.getObjectForKey("key");

		prevBtn.setOnClickListener(btnClickListener);
		nextBtn.setOnClickListener(btnClickListener);
		mainScreenBtn.setOnClickListener(btnClickListener);
		secondScreenBtn.setOnClickListener(btnClickListener);
		
	}

	@Override
	protected void onResume() {
		sendMessage("s");
		SystemClock.sleep(3000);
		sendMessage("i");
		super.onResume();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK)
			finish();
		return super.onKeyDown(keyCode, event);
	}
	

	
	private final View.OnClickListener btnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.prevbtn:
				sendMessage("-");
				break;

			case R.id.nextbtn:
				sendMessage("+");
				break;
			
			case R.id.mainscreenbtn:
				Intent mainScreenIntent = new Intent(getApplicationContext(), MainScreenActivity.class);
				startActivity(mainScreenIntent);
				break;
				
			case R.id.secondscreenbtn:
				finish();
				break;
			}

		}
	};

	
	
	public void sendMessage(String message)
	{
		if (object != null)
		{
			try
			{
				Cast.CastApi.sendMessage((GoogleApiClient) object, NAMESPACE, message)
				.setResultCallback(new ResultCallback<Status>()
						{
					@Override
					public void onResult(Status result)
					{
						if (!result.isSuccess())
						{
							//Log.e(TAG, "Sending message failed");
						}
					}
						});
			}
			catch (Exception e)
			{
				//Log.e(TAG, "Exception while sending message", e);
			}
		}
	}
	
	
}
