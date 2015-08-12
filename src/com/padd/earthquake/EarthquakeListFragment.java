package com.padd.earthquake;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.json.*;

import android.app.ListFragment;
import android.location.Location;
import android.net.ParseException;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ArrayAdapter;


public class EarthquakeListFragment extends ListFragment {
	ArrayAdapter<Quake> aa;
	ArrayList<Quake> earthquakes=new ArrayList<Quake>();
	private static final String TAG="EARTHQUAKE";
	private Handler handler=new Handler();
	
	public void refreshEarthquakes() {
		URL url;
		try {
			String quakeFeed=getString(R.string.quake_feed);
			url=new URL(quakeFeed);
			URLConnection connection;
			connection=url.openConnection();
			connection.setConnectTimeout(10000);
			connection.setReadTimeout(5000);
			HttpURLConnection httpConnection=(HttpURLConnection)connection;
			int responseCode=httpConnection.getResponseCode();
			if (responseCode==HttpURLConnection.HTTP_OK)
			{
				DocumentBuilderFactory dbf=DocumentBuilderFactory.newInstance();
				DocumentBuilder db=dbf.newDocumentBuilder();
				InputStream in = new BufferedInputStream(httpConnection.getInputStream());
				InputStreamReader rd = new InputStreamReader(in,"utf-8");
				int c = 0;
				StringBuffer temp = new StringBuffer();
				while((c = rd.read())!= -1){
				       temp.append((char)c);
				}
				in.close();
				String linelist = temp.toString();
				//while ((lines = reader.readLine()) != null){  
				        //lines = new String(lines.getBytes(), "utf-8");  
					//System.out.println(lines); 
					//linelist=linelist+lines;
				//}
				Log.d(TAG, "==========================");
				Log.d(TAG,String.valueOf(temp.length()));
				Log.d(TAG,linelist);
				Pattern p = Pattern.compile("(\\{.+?\\})");
				Matcher m = p.matcher(linelist);
				List<String> result=new ArrayList<String>();
				while(m.find()){
					result.add(m.group());
				}
				String[] lines=(String[])result.toArray(new String[result.size()]);
				String[] linestemp=new String[lines.length];
				for(int j=0;j<lines.length;j++)
				{
					linestemp[j]=lines[lines.length-1-j];
				}
				 StringBuilder sb = new StringBuilder();
				   boolean first = true;
				   for (String item : linestemp)
				   {
				      if (first)
				         {first = false;}
				      else
				         {sb.append(",");}
				      sb.append(item);
				   }
				   linelist=sb.toString();
				   linelist="["+linelist+"]";
				JSONArray jsonarray = new JSONArray(linelist);
				//分析地震源
				
				//清除旧的地震数据
				earthquakes.clear();
				for (int i = 0; i < jsonarray.length(); i++) {   
					JSONObject jsonobj= jsonarray.getJSONObject(i);
					String details=jsonobj.getString("LOCATION_C");
					String hostname="http://www.ceic.ac.cn/";
					String linkString=hostname+jsonobj.getString("CATA_ID")+".html";
					String dt=jsonobj.getString("O_TIME");
					SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd' 'hh:mm:ss");
					Date qdate=new GregorianCalendar(0,0,0).getTime();
					try
					{
						qdate=sdf.parse(dt);
					} catch (ParseException e) {
						Log.d(TAG, "Date parsing exception");
					}
					
					Location l=new Location("dummyGPS");
					l.setLatitude(Double.parseDouble(jsonobj.getString("EPI_LAT")));
					l.setLongitude(Double.parseDouble(jsonobj.getString("EPI_LON")));
					double magnitude=Double.parseDouble(jsonobj.getString("M"));
					final Quake quake=new Quake(qdate,details,l,magnitude,linkString);
					Log.d(TAG, "3333333333333333333333");
					Log.d(TAG,dt);
					
					handler.post(new Runnable() {
						public void run() {
							addNewQuake(quake);
						}
					});
				}
			}
		} catch(MalformedURLException e) {
			Log.d(TAG, "MalformedURLException");
		} catch (IOException e) {
			Log.d(TAG, "IOException",e);
		} catch (ParserConfigurationException e) {
			Log.d(TAG, "Parser Configuration Exception");
		}  catch (Exception e) {
			Log.d(TAG, "2222222",e);
		}
		finally {Log.d(TAG, "1111111111111111111111");}
	}
	private void addNewQuake(Quake _quake)
	{
		earthquakes.add(_quake);
		aa.notifyDataSetChanged();
	}
	
	@Override
	public void onActivityCreated(Bundle saveInstanceState) {
		super.onActivityCreated(saveInstanceState);
		
		int layoutID=android.R.layout.simple_list_item_1;
		aa=new ArrayAdapter<Quake>(getActivity(),layoutID,earthquakes);
		setListAdapter(aa);
		Thread t=new Thread(new Runnable() {
			public void run() {
				refreshEarthquakes();
			}
		});
		t.start();
	}
}
