package com.gabrielbelanger.controller;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ResourceBundle;

import com.gabrielbelanger.tools.JSonParsing;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class homeController implements Initializable {
	
	@FXML
	TextField searchfield;
	
	@FXML
	Button exitbutton;
	
	@FXML
	ProgressIndicator syncprogress;
	
	@FXML
	ImageView poster;
	
	@FXML
	ComboBox<String> syncoption;
	
	String omdburl = "http://www.omdbapi.com/?";
	Gson json;
	HttpURLConnection connection;
	URL posterurl;
	
	@Override
	public void initialize(URL fxmlfilelocation, ResourceBundle resources) {
		//Setting up connection with OMDB
		try {
			connection = (HttpURLConnection)new URL(omdburl).openConnection();
			connection.setDoOutput(true);
			connection.setRequestMethod("GET");
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
		
		//Populating syncoption ComboBox
		syncoption.getItems().add("Sync episodes from now (default)");
		syncoption.getItems().add("Sync all episodes ");
		
		
	}
	//Enter has been pressed in the search field, search the database
	public void searchDatabase(ActionEvent event) throws MalformedURLException{
		String searchinput = searchfield.getText();
		//Replacing all spaces with hyphen for web syntax
		searchinput = searchinput.replace(' ','-');
		URL searchurl = new URL(omdburl + "t=" + searchinput);
		
		JsonObject jsondb = JSonParsing.getGson(searchurl);
		
		if (verifyResponse(jsondb)){
			poster.setImage(new Image(jsondb.get("Poster").toString().replace("\"","")));
		}
		else{
			System.out.println("Data not found");
		}
	}
	
	//X button was pressed, exit the program
	public void exitProgram(ActionEvent event){
		System.exit(0);
	}
	
	//Verify if response is false
	public boolean verifyResponse(JsonObject jsondb){
		if (jsondb.get("Response").toString().replace("\"", "").equals("False")){
			return false;
		}
		else
			return true;
	}
}
