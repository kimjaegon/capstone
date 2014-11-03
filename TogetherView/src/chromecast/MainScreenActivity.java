package chromecast;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.URLUtil;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.example.chromecastv2.R;

public class MainScreenActivity extends Activity {
	String ServerIpAddress = "http://203.246.112.116/uploads/";
	String[] imgUrl;
	int imgSize=0,current_index=0;
	ImageButton prevBtn, nextBtn;

	private WebView mWebView = null;
    private final Handler handler = new Handler();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if(android.os.Build.VERSION.SDK_INT > 9) {

			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
			StrictMode.setThreadPolicy(policy);

		}
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_mainscreen);
		prevBtn=(ImageButton)findViewById(R.id.main_prevbtn);
		nextBtn=(ImageButton)findViewById(R.id.main_nextbtn);
		hello_server();
		
		prevBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				current_index--;
				if(current_index < 0 ) 
					current_index = imgSize-1;
				
				setImage(imgUrl[current_index]);
			}
		});
		
		nextBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				current_index++;
				if(current_index == imgSize) 
					current_index = 0;
				
				setImage(imgUrl[current_index]);
			}
		});
	}
	
	private void hello_server()
	{
		setImageUrlList();
		setImage(imgUrl[current_index]);
	}
	
	private void setImage(String target_imgUrl)
	{
		
		Bitmap imgBitmap = GetImageFromURL(target_imgUrl);
		if (imgBitmap != null)
		{
			ImageView imgView = (ImageView)findViewById(R.id.main_ImageView);
			imgView.setImageBitmap(imgBitmap);
		}
	}
	
	private void setImageUrlList()
	{
		int i = 1;
		while(URLIsReachable(ServerIpAddress+i)){
			i++;
		}
		
		imgSize = i-1;//사이즈개수
		imgUrl = new String[imgSize];
		for(i=0;i<imgSize;i++)
			imgUrl[i] = ServerIpAddress+(i+1);
		
		current_index = 0;//받는구문
	}
	

	private Bitmap GetImageFromURL(String strImageURL)
	{
		Bitmap imgBitmap = null;
		try
		{
			URL url = new URL(strImageURL);
			URLConnection conn = url.openConnection();
			conn.connect();
			int nSize = conn.getContentLength();
			BufferedInputStream bis = new BufferedInputStream(conn.getInputStream(), nSize);
			imgBitmap = BitmapFactory.decodeStream(bis);
			bis.close();
		}

		catch (Exception e)
		{
			e.printStackTrace();
		}

		return imgBitmap;
	}
	
	public boolean URLIsReachable(String urlString)
	{
	    try
	    {
	        URL url = new URL(urlString);
	        HttpURLConnection urlConnection = (HttpURLConnection) url
	                .openConnection();
	        int responseCode = urlConnection.getResponseCode();
	        urlConnection.disconnect();
	        if(responseCode == 200)
	        	return true;
	        else
	        	return false;
	        
	    } catch (Exception e)
	    {
	        return false;
	    }
	}
}