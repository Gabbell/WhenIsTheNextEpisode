package com.gabrielbelanger.tools;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class JSonParsing {
	
	static HttpURLConnection connection;
	static JsonObject jobj;
	
	public static JsonObject getGson(URL inputurl){
		String receivedstring;
		try {
			connection = (HttpURLConnection) inputurl.openConnection();
	        InputStream in = new BufferedInputStream(connection.getInputStream());
	        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
	        receivedstring = reader.readLine();
	        System.out.println(receivedstring);
	        
	        jobj = new Gson().fromJson(receivedstring, JsonObject.class);
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
		finally{
			connection.disconnect();
		}
		
		return jobj;
	}
	
	//Verify if response is false
	public static boolean isValidData(JsonObject jobj){
		if (jobj.isJsonNull()){
			return false;
		}
		else
			return true;
	}
}
