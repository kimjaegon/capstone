package chromecast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import com.example.chromecastv2.R;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.nostra13.universalimageloader.cache.memory.impl.WeakMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.listener.ImageLoadingProgressListener;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class PPTFragment extends Fragment {
	private static final String TAG = "PPTActivity";
	public static final String NAMESPACE = "urn:x-cast:com.ls.cast.sample";
	private static final int REQUEST_GMS_ERROR = 0;
	private boolean applicationStarted;
	private ArrayList<String> mFileNames = new ArrayList<String>();
	ListView mFlieListView;
	ImageView pptImage;
	public static int prgmImages = R.drawable.powerpoint;
	private String path = Environment.getExternalStorageDirectory().getAbsolutePath();
	private Button showBtn, takeBtn;
	public static boolean onCastFlag = false;

	private MediaRouter mediaRouter;
	private MediaRouteSelector mediaRouteSelector;
	private MediaRouteActionProvider mediaRouteActionProvider;
	private CastDevice selectedDevice;
	private GoogleApiClient apiClient;
	public static final String APP_ID = "7C2258A5";
	private String pptServerUri = null;
	private int serverResponseCode = 0;
	private int fileUploadNum = 0;
	static int checkedNum;
	private PPTAdapter pptAdapter;
	private String resetServerUri = null;
	static final int REQUEST_CODE = 1234;
	private ProgressDialog dialog;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View v = inflater.inflate(R.layout.pptfragment, container, false);


		showBtn = (Button) v.findViewById(R.id.pptShowBtn);
		takeBtn = (Button) v.findViewById(R.id.pptTakeBtn);
		mFlieListView = (ListView) v.findViewById(R.id.pptListview);
		pptServerUri = "http://203.246.112.116/UploadToServerPPT.php";
		resetServerUri = "http://203.246.112.116/ResetServer.php";

		updateFileList(path);

		if(mFileNames.isEmpty()){			//sdcard폴더 안에 ppt 파일이 존재하지 않을 때
			path += "/Download/";			//ppt파일은 보통 다운로드 받는경우가 많아 Download폴더 검색을 위해 path에 추가.
			updateFileList(path);
		}
		
		if(path.lastIndexOf("/") != path.length()){
			path += "/";
		}
		
		pptAdapter = new PPTAdapter(getActivity().getApplicationContext(), mFileNames);
		mFlieListView.setAdapter(pptAdapter);

		showBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if(ApiClientData.getObjectForKey("key") == null)
					Toast.makeText(getActivity(), "크롬캐스트를 연결해주세요!", Toast.LENGTH_SHORT).show();

				else{
					dialog = ProgressDialog.show(getActivity(), "전송중","Loading....",true);
					new Thread(new Runnable() {
						public void run() {

							ArrayList<String> sendImgList = pptAdapter.getCheckedItems();
							Log.e("EEEEEEEEEEEEERRRRROOORRR", Integer.toString(sendImgList.size()));
							resetServer();
							
							fileUploadNum = 0;
							final int want_count = sendImgList.size();
							for(int i=0;i<sendImgList.size();i++){
								uploadFile(path+sendImgList.get(i));
								Log.e("파일 명 ", path+sendImgList.get(i));
							}
							
							getActivity().runOnUiThread(new Runnable() {
								public void run() {
									if(fileUploadNum == want_count){
										dialog.dismiss();
										Intent interfaceIntent = new Intent(getActivity(), ShowActivity.class);
										startActivityForResult(interfaceIntent, REQUEST_CODE);
									}
								}
							});   
						}
					}).start();   

					ArrayList<String> sendImgList = pptAdapter.getCheckedItems();
					checkedNum = sendImgList.size();

					if(checkedNum == 0){
						Toast.makeText(getActivity(), "PPT를 선택해 주세요.", Toast.LENGTH_LONG).show();
						return;
					}
				}
			}
		});

		takeBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if(ApiClientData.getObjectForKey("key") == null)
					Toast.makeText(getActivity(), "크롬캐스트를 연결해주세요!", Toast.LENGTH_SHORT).show();

				else{
					Intent takeIntent = new Intent(getActivity(), TakeActivity.class);
					startActivity(takeIntent);
				}
			}
		});

		return v;
	}

	public void updateFileList(String str)
	{
		File files = new File(path);
		String[] pptList;

		pptList = files.list(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String filename) {
				boolean pptOk = false;
				if(filename.toLowerCase().endsWith(".pptx"))
					pptOk = true;
				if(filename.toLowerCase().endsWith(".ppt"))
					pptOk = true;
				return pptOk;
			}
		});
		for(int i=0; i<pptList.length; i++)
			mFileNames.add(pptList[i]);
	}

	public int uploadFile(String sourceFileUri) {
		String fileName = sourceFileUri;

		HttpURLConnection conn = null;
		DataOutputStream dos = null;  
		String lineEnd = "\r\n";
		String twoHyphens = "--";
		String boundary = "*****";
		int bytesRead, bytesAvailable, bufferSize;
		byte[] buffer;
		int maxBufferSize = 1000 * 1024 * 1024; 
		File sourceFile = new File(sourceFileUri); 

		if (!sourceFile.isFile()) {
			getActivity().runOnUiThread(new Runnable() {
				public void run() {
					//					Toast.makeText(PPTActivity.this, "Source File not exist ", Toast.LENGTH_LONG).show();
				}
			}); 

			return 0;

		}
		else
		{
			try { 

				// open a URL connection to the Servlet
				FileInputStream fileInputStream = new FileInputStream(sourceFile);

				URL resetUrl = new URL(pptServerUri);

				conn = (HttpURLConnection) resetUrl.openConnection(); 	// Open a HTTP  connection to  the URL
				conn.setDoInput(true); // Allow Inputs
				conn.setDoOutput(true); // Allow Outputs
				conn.setUseCaches(false); // Don't use a Cached Copy
				conn.setRequestMethod("POST");
				conn.setRequestProperty("Connection", "Keep-Alive");
				conn.setRequestProperty("ENCTYPE", "multipart/form-data");
				conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
				conn.setRequestProperty("uploaded_file", fileName);

				dos = new DataOutputStream(conn.getOutputStream());

				dos.writeBytes(twoHyphens + boundary + lineEnd); 
				dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\""
						+ fileName + "\"" + lineEnd);

				dos.writeBytes(lineEnd);

				// create a buffer of  maximum size
				bytesAvailable = fileInputStream.available(); 

				bufferSize = Math.min(bytesAvailable, maxBufferSize);
				buffer = new byte[bufferSize];

				// read file and write it into form...
				bytesRead = fileInputStream.read(buffer, 0, bufferSize);  

				while (bytesRead > 0) {

					dos.write(buffer, 0, bufferSize);
					bytesAvailable = fileInputStream.available();
					bufferSize = Math.min(bytesAvailable, maxBufferSize);
					bytesRead = fileInputStream.read(buffer, 0, bufferSize);   

				}

				// send multipart form data necesssary after file data...
				dos.writeBytes(lineEnd);
				dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

				// Responses from the server (code and message)
				serverResponseCode = conn.getResponseCode();
				String serverResponseMessage = conn.getResponseMessage();

				Log.i("uploadFile", "HTTP Response is : " 
						+ serverResponseMessage + ": " + serverResponseCode);

				if(serverResponseCode == 200){
					fileUploadNum++;
//					getActivity().runOnUiThread(new Runnable() {
//						public void run() {
//
//						}
//					});   
				}    

				//close the streams //
				fileInputStream.close();
				dos.flush();
				dos.close();

				
			} catch (MalformedURLException ex) {

				//	              dialog.dismiss();  
				ex.printStackTrace();

				getActivity().runOnUiThread(new Runnable() {
					public void run() {
						//	                	  messageText.setText("MalformedURLException Exception : check script url.");
						//						Toast.makeText(PPTActivity.this, "MalformedURLException", Toast.LENGTH_SHORT).show();
					}
				});

				Log.e("Upload file to server", "error: " + ex.getMessage(), ex);  
			} catch (Exception e) {

				//	              dialog.dismiss();  
				e.printStackTrace();

				getActivity().runOnUiThread(new Runnable() {
					public void run() {
						//	                	  messageText.setText("Got Exception : see logcat ");
						//						Toast.makeText(PPTActivity.this, "Got Exception : see logcat ", Toast.LENGTH_SHORT).show();
					}
				});
				Log.e("Upload file to server Exception", "Exception : "  + e.getMessage(), e);  
			}
			//	          dialog.dismiss();       
			return serverResponseCode; 
		} // End else block 
	}


	private void resetServer() 
	{
		HttpURLConnection conn = null;

		try { 

			// open a URL connection to the Servlet
			URL url = new URL(resetServerUri);

			conn = (HttpURLConnection) url.openConnection(); 	// Open a HTTP  connection to  the URL
			conn.setDoInput(true); // Allow Inputs
			conn.setDoOutput(true); // Allow Outputs
			conn.setUseCaches(false); // Don't use a Cached Copy
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Connection", "Keep-Alive");
			conn.setRequestProperty("ENCTYPE", "multipart/form-data");

			// Responses from the server (code and message)
			serverResponseCode = conn.getResponseCode();
			String serverResponseMessage = conn.getResponseMessage();

			Log.i("uploadFile", "HTTP Response is : " 
					+ serverResponseMessage + ": " + serverResponseCode);

			if(serverResponseCode == 200){

				getActivity().runOnUiThread(new Runnable() {
					public void run() {
						//Toast.makeText(getActivity().getApplicationContext(), "RESET Complete!", Toast.LENGTH_SHORT).show();
					}
				});   
			}    
		} catch (MalformedURLException ex) {
			ex.printStackTrace();

			getActivity().runOnUiThread(new Runnable() {
				public void run() {
					//	                	  messageText.setText("MalformedURLException Exception : check script url.");
					Toast.makeText(getActivity().getApplicationContext(), "MalformedURLException", Toast.LENGTH_SHORT).show();
				}
			});

			Log.e("Reset server", "error: " + ex.getMessage(), ex);  
		} catch (Exception e) {

			//	              dialog.dismiss();  
			e.printStackTrace();

			getActivity().runOnUiThread(new Runnable() {
				public void run() {
					Toast.makeText(getActivity().getApplicationContext(), "Got Exception : see logcat ", Toast.LENGTH_SHORT).show();
				}
			});
			Log.e("Reset server Exception", "Exception : "  + e.getMessage(), e);  
		}

	}

	public class PPTAdapter extends BaseAdapter {

		ArrayList<String> mList;
		SparseBooleanArray mSparseBooleanArray;
		ViewHolder holder;

		public PPTAdapter(Context context, ArrayList<String> imageList) {
			mSparseBooleanArray = new SparseBooleanArray();
			mList = new ArrayList<String>();
			this.mList = imageList;
		}

		public ArrayList<String> getCheckedItems() {
			ArrayList<String> mTempArry = new ArrayList<String>();

			for(int i=0;i<mList.size();i++) {
				if(mSparseBooleanArray.get(i)) {
					mTempArry.add(mList.get(i));
				}
			}

			return mTempArry;
		}

		@Override
		public int getCount() {
			return mFileNames.size();
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			View view = convertView;
			if (view == null) {
				view = getActivity().getLayoutInflater().inflate(R.layout.item_listview_text, parent, false);
				holder = new ViewHolder();
				assert view != null;
				holder.imageView = (ImageView) view.findViewById(R.id.pptImage);
				holder.textView = (TextView) view.findViewById(R.id.pptText);
				view.setTag(holder);
			} else {
				holder = (ViewHolder) view.getTag();
			}

			holder.checkbox = (CheckBox) view.findViewById(R.id.pptCheckBox);

			holder.checkbox.setTag(position);
			holder.checkbox.setChecked(mSparseBooleanArray.get(position));
			holder.checkbox.setOnCheckedChangeListener(mCheckedChangeListener);

			holder.imageView.setImageResource(R.drawable.powerpoint);
			holder.textView.setText(mList.get(position));

			return view;
		}

		OnCheckedChangeListener mCheckedChangeListener = new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				// TODO Auto-generated method stub
				mSparseBooleanArray.put((Integer) buttonView.getTag(), isChecked);
			}
		};

		class ViewHolder {
			ImageView imageView;
			TextView textView;
			CheckBox checkbox;
		}
	}	
}
