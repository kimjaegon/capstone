package chromecast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import chromecast.GalleryFragment.ImageAdapter.ViewHolder;

import com.example.chromecastv2.R;
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
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class GalleryFragment extends Fragment {
	DisplayImageOptions options;
	Button showBtn, takeBtn;
	private String upLoadServerUri = null;
	private String resetServerUri = null;
	private int fileUploadNum;
	private int checkedNum;
	private ArrayList<String> imageUrls;
	private ImageAdapter imageAdapter;
	private int serverResponseCode = 0;
	public static Object object;
	private static final int REQUEST_CODE_1 = 01 ;
	private ProgressDialog dialog;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View v = inflater.inflate(R.layout.galleryfragment, container, false);



		options = new DisplayImageOptions.Builder()
		.showImageOnLoading(R.drawable.ic_stub)
		.showImageForEmptyUri(R.drawable.ic_empty)
		.showImageOnFail(R.drawable.ic_error)
		.imageScaleType(ImageScaleType.EXACTLY)
		.cacheOnDisc()
		.considerExifParams(true)
		.bitmapConfig(Bitmap.Config.RGB_565)
		.build();

		//init configuration using displayImage 
		ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getActivity().getApplicationContext())
		.memoryCache(new WeakMemoryCache())
		.build();

		BaseActivity.imageLoader.init(config);


		showBtn = (Button) v.findViewById(R.id.galleryShowBtn);
		takeBtn = (Button) v.findViewById(R.id.galleryTakeBtn);
		upLoadServerUri = "http://203.246.112.116/UploadToServer.php";
		resetServerUri = "http://203.246.112.116/ResetServer.php";

		final String[] columns = { MediaStore.Images.Media.DATA, MediaStore.Images.Media._ID };
		final String orderBy = MediaStore.Images.Media.DATE_TAKEN;
		Cursor imagecursor = getActivity().managedQuery(
				MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns, null,
				null, orderBy + " DESC");

		this.imageUrls = new ArrayList<String>();

		for (int i = 0; i < imagecursor.getCount(); i++) {
			imagecursor.moveToPosition(i);
			int dataColumnIndex = imagecursor.getColumnIndex(MediaStore.Images.Media.DATA);
			imageUrls.add(imagecursor.getString(dataColumnIndex));

		}

		imageAdapter = new ImageAdapter(getActivity().getApplicationContext(), imageUrls);

		AbsListViewBaseActivity.listView = (GridView) v.findViewById(R.id.gridview);
		((GridView) AbsListViewBaseActivity.listView).setAdapter(imageAdapter);	

		//		System.gc()
		showBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if(ApiClientData.getObjectForKey("key") == null)
					Toast.makeText(getActivity(), "크롬캐스트를 연결해주세요!", Toast.LENGTH_SHORT).show();

				else{
					dialog = ProgressDialog.show(getActivity(), "전송중","Loading....",true);
					new Thread(new Runnable() {
						public void run() {
							ArrayList<String> sendImgList = imageAdapter.getCheckedItems();
							Log.e("EEEEEEEEEEEEERRRRROOORRR", Integer.toString(sendImgList.size()));
							resetServer();
							final int want_count = sendImgList.size();
							fileUploadNum = 0;

							for(int i=0;i<sendImgList.size();i++){
								uploadFile(sendImgList.get(i));
								Log.e("파일 명", sendImgList.get(i));
							}
							
							getActivity().runOnUiThread(new Runnable() {
								public void run() {
									if(fileUploadNum == want_count){
										dialog.dismiss();
										Intent showIntent = new Intent(getActivity(), ShowActivity.class);
										startActivity(showIntent);
									}
								}
							});   
						}
					}).start();   

					ArrayList<String> sendImgList = imageAdapter.getCheckedItems();

					checkedNum = sendImgList.size();

					if(checkedNum == 0){
						Toast.makeText(getActivity(), "사진을 선택해 주세요.", Toast.LENGTH_LONG).show();
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
					// TODO Auto-generated method stub
					Intent takeIntent = new Intent(getActivity(), TakeActivity.class);
					startActivity(takeIntent);
				}
			}
		});


		return v;
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

			//	           dialog.dismiss(); 

			Log.e("uploadFile", "Source File not exist :"+"file://"+imageUrls.get(0));

			getActivity().runOnUiThread(new Runnable() {
				public void run() {
					//					Toast.makeText(GalleryActivity.this, "Source File not exist ", Toast.LENGTH_LONG).show();
				}
			}); 

			return 0;

		}
		else
		{
			try { 

				// open a URL connection to the Servlet
				FileInputStream fileInputStream = new FileInputStream(sourceFile);

				URL url = new URL(upLoadServerUri);

				conn = (HttpURLConnection) url.openConnection(); 	// Open a HTTP  connection to  the URL
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
				}    

				//close the streams //
				fileInputStream.close();
				dos.flush();
				dos.close();


			} catch (MalformedURLException ex) {
				ex.printStackTrace();

				getActivity().runOnUiThread(new Runnable() {
					public void run() {
						//	                	  messageText.setText("MalformedURLException Exception : check script url.");
						//						Toast.makeText(getActivity().getApplicationContext(), "MalformedURLException", Toast.LENGTH_SHORT).show();
					}
				});

				Log.e("Upload file to server", "error: " + ex.getMessage(), ex);  
			} catch (Exception e) {

				//	              dialog.dismiss();  
				e.printStackTrace();

				getActivity().runOnUiThread(new Runnable() {
					public void run() {
						//	                	  messageText.setText("Got Exception : see logcat ");
						//						Toast.makeText(GalleryActivity.this, "Got Exception : see logcat ", Toast.LENGTH_SHORT).show();
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

	public class ImageAdapter extends BaseAdapter {
		ArrayList<String> mList;
		SparseBooleanArray mSparseBooleanArray;

		public ImageAdapter(Context context, ArrayList<String> imageList) {
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
			return imageUrls.size();
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
			final ViewHolder holder;
			View view = convertView;
			if (view == null) {
				view = getActivity().getLayoutInflater().inflate(R.layout.item_grid_image, parent, false);
				holder = new ViewHolder();
				assert view != null;
				holder.imageView = (ImageView) view.findViewById(R.id.image);
				holder.progressBar = (ProgressBar) view.findViewById(R.id.progress);
				view.setTag(holder);
			} else {
				holder = (ViewHolder) view.getTag();
			}

			//			CheckBox mCheckBox = (CheckBox) view.findViewById(R.id.galleryCheckBox);
			holder.checkbox = (CheckBox) view.findViewById(R.id.galleryCheckBox);

			BaseActivity.imageLoader.displayImage("file://"+imageUrls.get(position), holder.imageView, options, new SimpleImageLoadingListener() {
				@Override
				public void onLoadingStarted(String imageUri, View view) {
					holder.progressBar.setProgress(0);
					holder.progressBar.setVisibility(View.VISIBLE);


				}

				@Override
				public void onLoadingFailed(String imageUri, View view,
						FailReason failReason) {
					BaseActivity.imageLoader.stop();
					BaseActivity.imageLoader.clearDiscCache();
					BaseActivity.imageLoader.clearMemoryCache();
					holder.progressBar.setVisibility(View.GONE);
				}

				@Override
				public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {

					holder.progressBar.setVisibility(View.GONE);
				}
			}, new ImageLoadingProgressListener() {
				@Override
				public void onProgressUpdate(String imageUri, View view, int current,
						int total) {
					holder.progressBar.setProgress(Math.round(100.0f * current / total));
				}
			}
					);

			holder.checkbox.setTag(position);
			holder.checkbox.setChecked(mSparseBooleanArray.get(position));
			holder.checkbox.setOnCheckedChangeListener(mCheckedChangeListener);

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
			ProgressBar progressBar;
			CheckBox checkbox;
		}


	}	

}
