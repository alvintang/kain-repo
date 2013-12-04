package com.example.kainv1;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Random;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class GenerateMap extends Activity {

	private int userIcon, foodIcon, drinkIcon, shopIcon, otherIcon;
	private GoogleMap theMap;
	private LocationManager locMan;
	private Marker userMarker;
	private Marker[] placeMarkers;
	private MarkerOptions[] places;
	private final int MAX_PLACES = 20;
	
	private TextView radiusText,shopList;
	private int nearbySearchRadius;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.map_generate);
		
		userIcon = R.drawable.yellow_point;
		foodIcon = R.drawable.red_point;
		drinkIcon = R.drawable.blue_point;
		shopIcon = R.drawable.green_point;
		otherIcon = R.drawable.purple_point;
		LatLng init = new LatLng(0,0);
		String radiusString = "1000";
		
		//initialize text part
		radiusText = (TextView) findViewById(R.id.radius);
		shopList = (TextView) findViewById(R.id.shoplist);
		Bundle extras = getIntent().getExtras();
		if(extras!=null){
			radiusString=extras.getString("radius");
		}
		radiusText.setText(radiusString);
		nearbySearchRadius=Integer.parseInt(radiusString);
		
		//initialize the map
		if(theMap==null){
			theMap = ((MapFragment)getFragmentManager().findFragmentById(R.id.the_map)).getMap();
			if(theMap!=null){
				theMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
				placeMarkers = new Marker[MAX_PLACES];
				updatePlaces();
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.my_map, menu);
		return true;
	}
	
	private void updatePlaces(){
		//update location
		locMan = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		Location lastLoc = locMan.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		double lat = lastLoc.getLatitude();
		double lng = lastLoc.getLongitude();
		LatLng lastLatLng=new LatLng(lat,lng);
		if(userMarker!=null) userMarker.remove();
		
		userMarker = theMap.addMarker(new MarkerOptions()
			.position(lastLatLng)
			.title("You are here")
			.icon(BitmapDescriptorFactory.fromResource(userIcon))
			.snippet("Your last recorded location"));

		CameraPosition initialCam = new CameraPosition.Builder()
		.target(lastLatLng)
		.zoom(15)
		.bearing(0)
		.tilt(45)
		.build();

		theMap.animateCamera(CameraUpdateFactory.newCameraPosition(initialCam),3000,null);
		//theMap.animateCamera(CameraUpdateFactory.zoomTo(15));
		
//		String placesSearchStr = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=";
		String latVal=String.valueOf(lat);
		String lngVal=String.valueOf(lng);
		String url;
		try {
	        url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location="
	        +URLEncoder.encode(latVal, "UTF-8")
	        +","
	        +URLEncoder.encode(lngVal, "UTF-8")
	        +"&radius="
	        +nearbySearchRadius
	        +"&sensor="
	        +URLEncoder.encode("true", "UTF-8")
	        +"&types="
	        +URLEncoder.encode("food|bar|restaurant|bakery", "UTF-8")
	        +"&key="
	        +URLEncoder.encode("AIzaSyC7MBems8mlFZ-kvmbphQwcJkzzRAY4hDE", "UTF-8");
	        new GetPlaces().execute(url);
	    } catch (UnsupportedEncodingException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
	    }
		
/*		+lat+","+lng+
			    "&radius=1000&sensor=true" +
			    "&types=food|bar|store|museum|art_gallery"+
			    "&key=AIzaSyC7MBems8mlFZ-kvmbphQwcJkzzRAY4hDE";
		System.out.println("map url:"+placesSearchStr);
		
		new GetPlaces().execute(placesSearchStr);*/
	}
	
	private class GetPlaces extends AsyncTask<String,Void,String>{
		@Override
		protected String doInBackground(String... placesURL){
			StringBuilder placesBuilder = new StringBuilder();
			for(String placeSearchURL: placesURL){
				HttpClient placesClient = new DefaultHttpClient();
				try{
					
					HttpGet placesGet = new HttpGet(placeSearchURL);
					HttpResponse placesResponse = placesClient.execute(placesGet);
					StatusLine placeSearchStatus = placesResponse.getStatusLine();
					
					if(placeSearchStatus.getStatusCode()==200){
						HttpEntity placesEntity = placesResponse.getEntity();
						InputStream placesContent = placesEntity.getContent();
						InputStreamReader placesInput = new InputStreamReader(placesContent);
						BufferedReader placesReader = new BufferedReader(placesInput);
						String lineIn;
						while((lineIn = placesReader.readLine())!=null){
							placesBuilder.append(lineIn);
						}
					}
					
				}catch(Exception e){
					e.printStackTrace();
				}
			}
			return placesBuilder.toString();
		}
		
		protected void onPostExecute(String result){
			if(placeMarkers!=null){
				for(int pm=0; pm<placeMarkers.length; pm++){
					if(placeMarkers[pm]!=null){
						placeMarkers[pm].remove();
					}
				}
			}
			
			try{
				JSONObject resultObject = new JSONObject(result);
				JSONArray placesArray = resultObject.getJSONArray("results");
				places = new MarkerOptions[placesArray.length()];
				for(int p=0; p<placesArray.length(); p++){
					boolean missingValue=false;
					LatLng placeLL = null;
					String placeName="";
					String vicinity="";
					int currIcon = otherIcon;
					System.out.println("maptest p="+p);
					try{
						missingValue=false;
						JSONObject placeObject = placesArray.getJSONObject(p);
						JSONObject loc = placeObject.getJSONObject("geometry").getJSONObject("location");
						placeLL = new LatLng(Double.valueOf(loc.getString("lat")),Double.valueOf(loc.getDouble("lng")));
						JSONArray types = placeObject.getJSONArray("types");
						for(int t=0; t<types.length(); t++){
							String thisType=types.getString(t).toString();
							System.out.println("maptest t="+t);
							System.out.println("maptest type="+thisType);
							if(thisType.contains("food")){
								currIcon = foodIcon;
								break;
							}
							if(thisType.contains("bar")){
								currIcon = drinkIcon;
								break;
							}
							if(thisType.contains("store")){
								currIcon = shopIcon;
								break;
							}
						}
						vicinity = placeObject.getString("vicinity");
						placeName = placeObject.getString("name");
					}catch(JSONException jse){
						missingValue = true;
						jse.printStackTrace();
					}
					if(missingValue){
						places[p]=null;
					}else{
						places[p]=new MarkerOptions()
								.position(placeLL)
								.title(placeName)
								.icon(BitmapDescriptorFactory.fromResource(currIcon))
								.snippet(vicinity);
					}
						
				}
			}catch(Exception e){
				e.printStackTrace();
			}
			int p=0, markersAdded=0;
			
			if(places!=null && placeMarkers!=null){
				for(p=0; p<places.length && p<placeMarkers.length;p++){
					if(places[p]!=null){
						placeMarkers[p]=theMap.addMarker(places[p]);
						markersAdded++;
					}
				}
			}
			//select Random result
			System.out.println("kainv1 markersAdded:"+markersAdded);
			Random r = new Random();
			int selected;
			if(markersAdded>0){
				selected=r.nextInt(markersAdded-1);
				System.out.println("kainv1 random number:"+selected);
				shopList.setText(placeMarkers[selected].getTitle()+"\n"+placeMarkers[selected].getSnippet());
				placeMarkers[selected].showInfoWindow();
			}
			else{
				selected=0;
				shopList.setText("No nearby places found");
			}
			

			//error kapag less than 20 yung nakukuha
			for(int i=0; i<markersAdded;i++){
				System.out.println("kain placemarkers"+i+" "+placeMarkers[i].getTitle());
			}
		}
	}
}