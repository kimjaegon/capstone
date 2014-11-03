package chromecast;

import java.io.IOException;

import com.example.chromecastv2.R;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends ActionBarActivity implements
ActionBar.TabListener, OnClickListener {
 
    final static String TAG = "MainActivity";
	private MediaRouter mediaRouter;
	private MediaRouteSelector mediaRouteSelector;
	private CastDevice selectedDevice;
	private GoogleApiClient apiClient;
	boolean applicationStarted;
	MediaRouteActionProvider mediaRouteActionProvider;
    int mCurrentFragmentIndex;
	public static final String APP_ID = "7C2258A5";
	public static final String NAMESPACE = "urn:x-cast:com.ls.cast.sample";
    public final static int FRAGMENT_ONE = 0;
    public final static int FRAGMENT_TWO = 1;
    Button bt_oneFragment;
    Button bt_twoFragment;
 
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        startActivity(new Intent(this,LoadingActivity.class));
        
    	mediaRouter = MediaRouter.getInstance(getApplicationContext());
		mediaRouteSelector = new MediaRouteSelector.Builder().addControlCategory(CastMediaControlIntent.categoryForCast(APP_ID)).build();
        
        bt_oneFragment = (Button) findViewById(R.id.bt_oneFragment);
        bt_oneFragment.setOnClickListener(this);
        bt_oneFragment.setSelected(true);
        bt_twoFragment = (Button) findViewById(R.id.bt_twoFragment);
        bt_twoFragment.setOnClickListener(this);
        bt_twoFragment.setSelected(false);
        
        mCurrentFragmentIndex = FRAGMENT_ONE;
 
        fragmentReplace(mCurrentFragmentIndex);
    }
 
    public void fragmentReplace(int reqNewFragmentIndex) {
 
        Fragment newFragment = null;
 
        Log.d(TAG, "fragmentReplace " + reqNewFragmentIndex);
 
        newFragment = getFragment(reqNewFragmentIndex);
 
        // replace fragment
        final FragmentTransaction transaction = getSupportFragmentManager()
                .beginTransaction();
 
        transaction.replace(R.id.ll_fragment, newFragment);
 
        // Commit the transaction
        transaction.commit();
 
    }
 
    public static Fragment getFragment(int idx) {
        Fragment newFragment = null;
 
        switch (idx) {
        case FRAGMENT_ONE:
            newFragment = new GalleryFragment();
            break;
        case FRAGMENT_TWO:
            newFragment = new PPTFragment();
            break;
        default:
            Log.d(TAG, "Unhandle case");
            break;
        }
 
        return newFragment;
    }
 
    @Override
    public void onClick(View v) {
 
        switch (v.getId()) {
 
        case R.id.bt_oneFragment:
            mCurrentFragmentIndex = FRAGMENT_ONE;
            fragmentReplace(mCurrentFragmentIndex);
            bt_oneFragment.setSelected(true);
            bt_twoFragment.setSelected(false);
            break;
        case R.id.bt_twoFragment:
            mCurrentFragmentIndex = FRAGMENT_TWO;
            fragmentReplace(mCurrentFragmentIndex);
            bt_twoFragment.setSelected(true);
            bt_oneFragment.setSelected(false);
            break;
 
        }
 
    }
    
    private final Cast.Listener castClientListener = new Cast.Listener()
	{
		@Override
		public void onApplicationDisconnected(int statusCode)
		{
			try
			{
				Cast.CastApi.removeMessageReceivedCallbacks(apiClient, NAMESPACE);
			}
			catch (IOException e)
			{
				Log.w(TAG, "Exception while launching application", e);
			}
			setSelectedDevice(null);
		}

		@Override
		public void onVolumeChanged()
		{
			if (apiClient != null)
			{
				Log.d(TAG, "onVolumeChanged: " + Cast.CastApi.getVolume(apiClient));
			}
		}
	};

	private final ResultCallback<Cast.ApplicationConnectionResult> connectionResultCallback = new ResultCallback<Cast.ApplicationConnectionResult>()
			{
		@Override
		public void onResult(Cast.ApplicationConnectionResult result)
		{
			Status status = result.getStatus();
			if (status.isSuccess())
			{
				applicationStarted = true;

				try
				{
					Cast.CastApi.setMessageReceivedCallbacks(apiClient, NAMESPACE, incomingMsgHandler);
				}
				catch (IOException e)
				{
					Log.e(TAG, "Exception while creating channel", e);
				}
			}
		}
			};

			private final GoogleApiClient.ConnectionCallbacks connectionCallback = new GoogleApiClient.ConnectionCallbacks()
			{
				@Override
				public void onConnected(Bundle bundle)
				{
					try
					{
						Cast.CastApi.launchApplication(apiClient, APP_ID, false).setResultCallback(connectionResultCallback);
					}
					catch (Exception e)
					{
						Log.e(TAG, "Failed to launch application", e);
					}
				}

				@Override
				public void onConnectionSuspended(int i)
				{
				}
			};

			private final GoogleApiClient.OnConnectionFailedListener connectionFailedListener = new GoogleApiClient.OnConnectionFailedListener()
			{
				@Override
				public void onConnectionFailed(ConnectionResult connectionResult)
				{
					setSelectedDevice(null);
				}
			};

			public final Cast.MessageReceivedCallback incomingMsgHandler = new Cast.MessageReceivedCallback()
			{
				@Override
				public void onMessageReceived(CastDevice castDevice, String namespace, String message)
				{
					Log.d(TAG, String.format("message namespace: %s message: %s", namespace, message));
				}
			};

			private final MediaRouter.Callback mediaRouterCallback = new MediaRouter.Callback()
			{
				@Override
				public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo route)
				{
					Log.d(TAG, "onRouteSelected: " + route.getName());
					CastDevice device = CastDevice.getFromBundle(route.getExtras());
					setSelectedDevice(device);
				}

				@Override
				public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo route)
				{
					Log.d(TAG, "onRouteUnselected: " + route.getName());
					stopApplication();
					setSelectedDevice(null);
				}
			};

			
			@Override
			public boolean onCreateOptionsMenu(Menu menu)
			{
				super.onCreateOptionsMenu(menu);
				getMenuInflater().inflate(R.menu.sample_media_router_menu, menu);
				
				MenuItem mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item);
				mediaRouteActionProvider = (MediaRouteActionProvider) MenuItemCompat.getActionProvider(mediaRouteMenuItem);
				mediaRouteActionProvider.setRouteSelector(mediaRouteSelector);

				return true;
			}
			
			@Override
			protected void onStart()
			{
				super.onStart();
				mediaRouter.addCallback(mediaRouteSelector, mediaRouterCallback, MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
				
			}
			
			private void setSelectedDevice(CastDevice device)
			{
				Log.d(TAG, "setSelectedDevice: " + device);

				selectedDevice = device;

				if (selectedDevice != null)
				{
					try
					{
						stopApplication();
						disconnectApiClient();
						connectApiClient();
						
					}
					catch (IllegalStateException e)
					{
						Log.w(TAG, "Exception while connecting API client", e);
						disconnectApiClient();
					}
				}
				else
				{
					disconnectApiClient();
					mediaRouter.selectRoute(mediaRouter.getDefaultRoute());
				}
			}

			private void connectApiClient()
			{
				Cast.CastOptions apiOptions = Cast.CastOptions.builder(selectedDevice, castClientListener).build();
				apiClient = new GoogleApiClient.Builder(this)
				.addApi(Cast.API, apiOptions)
				.addConnectionCallbacks(connectionCallback)
//				.addOnConnectionFailedListener(connectionFailedListener)
				.build();
				apiClient.connect();
				ApiClientData.addObjectForKey("key", apiClient);
			}

			private void disconnectApiClient()
			{
				if (apiClient != null)
				{
					apiClient.disconnect();
					apiClient = null;
				}
			}

			private void stopApplication()
			{
				if (apiClient == null) return;

				if (applicationStarted)
				{
					Cast.CastApi.stopApplication(apiClient);
					applicationStarted = false;
				}
			}

			@Override
			public void onTabReselected(Tab arg0, FragmentTransaction arg1) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onTabSelected(Tab arg0, FragmentTransaction arg1) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onTabUnselected(Tab arg0, FragmentTransaction arg1) {
				// TODO Auto-generated method stub
				
			}
}