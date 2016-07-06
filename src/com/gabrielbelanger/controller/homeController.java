package com.gabrielbelanger.controller;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ResourceBundle;

import com.gabrielbelanger.tools.Entry;
import com.gabrielbelanger.tools.DateManipulator;
import com.gabrielbelanger.tools.GoogleAuth;
import com.gabrielbelanger.tools.JSonParsing;
import com.gabrielbelanger.tools.LocalWatchlist;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Calendar;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.gson.JsonObject;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class homeController implements Initializable {
	
	@FXML
	TextField searchfield;
	//Series/Movie info labels
	@FXML
	Label statuslabel,titlelabel,releaselabel, runtimelabel, ratinglabel, typelabel, idlabel;
	
	@FXML
	Label connectlabel;
	
	@FXML
	ListView<String> watchlistview;

	@FXML
	Button exitbutton,connectbutton,watchlistbutton,purgeButton;
	
	@FXML
	ProgressIndicator syncprogress,searchprogress;
	
	@FXML
	ImageView poster;
	
	@FXML
	Circle statuscircle;
	
	@FXML
	ComboBox<String> syncoption;
	
	private LocalWatchlist localwatch = new LocalWatchlist();
	private DateManipulator datemanip = new DateManipulator();
	private String mazeurl = "http://api.tvmaze.com/";
	private URL searchurl;
	private HttpURLConnection connection;
	private com.google.api.services.calendar.Calendar service;
	
	private String calendarId;
	private boolean isConnected = false;
	
	@Override
	public void initialize(URL fxmlfilelocation, ResourceBundle resources) {
		//Setting up connection with OMDB
		try {
			connection = (HttpURLConnection)new URL(mazeurl).openConnection();
			connection.setDoOutput(true);
			connection.setRequestMethod("GET");
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	
		//Populating syncoption ComboBox
		syncoption.getItems().add("Sync episodes from now (default)");
		syncoption.getItems().add("Sync all episodes ");
		syncoption.setValue("Sync episodes from now (default)");
		
		//After initialize
		Platform.runLater(new Runnable() {
            @Override public void run() {
                searchfield.requestFocus();
                
        		watchlistview.setItems(localwatch.getArray());
        		watchlistview.getSelectionModel().selectFirst(); //Select first item in watchlist to prevent out of bounds index
            }
        });
		
		//Shutdown hook to dump array into the file
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
		    public void run() {
				try {
					//Dumps array in the watchlist file and closes stream
					localwatch.dumpArray();
					localwatch.closeStream();
				} catch (IOException e) {
					e.printStackTrace();
				}
		    }
		}));
	}
	
	//Enter has been pressed in the search field, search the database
	public void searchDatabase(ActionEvent event) throws MalformedURLException {
		searchprogress.setVisible(true);
		Thread t = new Thread(new Runnable() {

			public void run() {

				String searchinput = searchfield.getText();
				// Replacing all spaces with hyphen for web syntax
				searchinput = searchinput.replace(' ', '+');

				try {
					searchurl = new URL(mazeurl + "singlesearch/shows?q=" + searchinput);

					JsonObject jsondb = JSonParsing.getGson(searchurl);

					Platform.runLater(new Runnable() {

						@Override
						public void run() {
							if (JSonParsing.isValidData(jsondb)) {
								statuslabel.setText(""); // Data is valid, do not show wrong data message
								titlelabel.setText(jsondb.get("name").toString().replace("\"", ""));
								releaselabel.setText(jsondb.get("premiered").toString().replace("\"", ""));
								runtimelabel.setText(jsondb.get("runtime").toString().replace("\"", "") + " minutes");
								ratinglabel.setText(jsondb.get("rating").getAsJsonObject().get("average").toString().replace("\"", ""));
								typelabel.setText(jsondb.get("type").toString().replace("\"", ""));
								idlabel.setText(jsondb.get("id").toString().replace("\"", ""));

								// Checking existence of poster before setting it
								if (!jsondb.get("image").toString().equals("\"N/A\"")) {
									poster.setImage(new Image(jsondb.get("image").getAsJsonObject().get("medium").toString().replace("\"", "")));
								}

								watchlistbutton.setVisible(true);
								statuslabel.setText("Data updated");
							} 
							else {
								watchlistbutton.setVisible(false);
								statuslabel.setText("Could not find data");
							}

							searchprogress.setVisible(false);

						}
					});

				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		});
		t.start();
	}

	public void syncNextEpisode() throws IOException{
		System.out.println();
		syncprogress.setVisible(true);
		
		//Creating new thread for syncing
		Thread t = new Thread(new Runnable() {
			public void run() {
 		
		 		//Trying to find next episode, if null, return error message
		 		try{
		 			searchurl = new URL(mazeurl + "shows/" + localwatch.getEntry(watchlistview.getSelectionModel().getSelectedIndex()).getId());
		 	 		JsonObject jsondb = JSonParsing.getGson(searchurl);
		 	 		
		 			searchurl = new URL(jsondb.get("_links").getAsJsonObject().get("nextepisode").getAsJsonObject().get("href").toString().replace("\"", ""));
		 	 		jsondb = JSonParsing.getGson(searchurl);
		 	 		
		 	 		if(isConnected){
		 		 		
		 		 		//Creating episode event
		 		 		Event episodeevent = new Event()
		 		 			    .setSummary(localwatch.getEntry(watchlistview.getSelectionModel().getSelectedIndex()).getTitle())
		 		 			    .setDescription(jsondb.get("name").toString() + "\n" + "Season " + jsondb.get("season").toString() + ", Episode " + jsondb.get("number").toString());
		 		 		
		 		 		//Get airdate and airtime of next episode as string
		 		 		String datestring = jsondb.get("airstamp").toString().replace("\"", "");
		 		 		int runtime = Integer.parseInt(jsondb.get("runtime").toString());
		 		 		
		 		 		DateTime startstamp = new DateTime(datestring);
		 		 		DateTime endstamp = datemanip.addMinutes(startstamp, runtime);
		 		 		
		 		 			    
		 		 		EventDateTime starttime = new EventDateTime()
		 		 				.setDateTime(startstamp);	
		 		 		EventDateTime endtime = new EventDateTime()
		 		 				.setDateTime(endstamp);
		 		 		episodeevent.setStart(starttime);
		 		 		episodeevent.setEnd(endtime);
		
		 		 		//End time was found, insert event in calendar
		 		 		service.events().insert(calendarId, episodeevent).execute();
		 		 		Platform.runLater(new Runnable() {
		 		 				public void run(){
		 		 		statuslabel.setText("Next episode synced");}});
		 		 	}
		 	 		
		 	 		else {
		 		 		Platform.runLater(new Runnable() {
	 		 				public void run(){
	 		 		statuslabel.setText("Google account not connected");}});
		 	 		}
		 		}
		 		//No next episode was found
		 		catch (NullPointerException e){
		 			searchurl = null;
	 		 		Platform.runLater(new Runnable() {
 		 				public void run(){
 		 		statuslabel.setText("No next episode found");}});
		 		}
		 		//URL was misformed
		 		catch (MalformedURLException e) {
					e.printStackTrace();
				} 
		 		catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		
		t.start();
		
		syncprogress.setVisible(false);
	}
	
	//Button Methods
	public void addToWatchlist() throws IOException{
		Entry entry = new Entry(titlelabel.getText(), releaselabel.getText(), runtimelabel.getText(),
				ratinglabel.getText(), typelabel.getText(), idlabel.getText());
		
		//Verifying if a duplicate exist in the list and adding accordingly
		if(!localwatch.isDuplicate(entry)){
			localwatch.addEntry(entry);
			statuslabel.setText("Entry added to watchlist");
		}
		else{
			statuslabel.setText("Duplicate found, entry not added");
		}
	}
	
	//Remove the selected entry from the LocalWatchlist
	public void removeFromWatchlist(){
		int i = watchlistview.getSelectionModel().getSelectedIndex();
		localwatch.removeEntry(i);
	}
	
	//Removes every entry in the LocalWatchlist
	public void purgeWatchlist(){
		int initialSize = localwatch.getSize();
		
		for (int i = 0; i < initialSize; i++){
			localwatch.removeEntry(0);
		}
	}
	
	//X button was pressed, exit the program
	public void exitProgram(ActionEvent event) throws IOException{
		System.exit(0);
	}
	
	public void glowOn(MouseEvent event){
		((Node) event.getSource()).setEffect(new Glow(1));
	}
	public void glowOff(MouseEvent event){
		((Node) event.getSource()).setEffect(null);
	}
	
	public void googleConnect(){
		//Attempting to connect to calendar service
		try {
			service = GoogleAuth.getCalendarService();
			statuscircle.setFill(Color.GREEN); //Connection was successful, light is green
			isConnected = true;
			
			createWineCalendar();
		}
		catch (IOException e) {
			System.out.println("Credentials were not obtained");
			isConnected = false;
			
			e.printStackTrace();
		}
	}
	public void syncWithCalendar() throws IOException{
		statuslabel.setText("Started synchronization");
		//TODO Delete previous calendar
		
		watchlistview.getSelectionModel().selectFirst();
		for(int i = 0 ; i < localwatch.getSize(); i++){
			syncNextEpisode();
			watchlistview.getSelectionModel().selectNext();
		}
		
		statuslabel.setText("Synchronization done");
	}
	public void createWineCalendar() throws IOException{
		boolean alreadyExists = false;
		
		//Going through the users calendar to find the WINE calendar and fetching the ID
		String pageToken = null;
		do {
		  CalendarList calendarList = service.calendarList().list().setPageToken(pageToken).execute();
		  java.util.List<CalendarListEntry> items = calendarList.getItems();

		  for (CalendarListEntry calendarListEntry : items) {
		    if(calendarListEntry.getSummary().equals("WINE")){
		    	alreadyExists = true;
		    	calendarId = calendarListEntry.getId();
		    }
		  }
		  pageToken = calendarList.getNextPageToken();
		} 
		while (pageToken != null);
		
		//If calendar does not exist, create one
		if(!alreadyExists){
			// Create a new calendar
			com.google.api.services.calendar.model.Calendar calendar = new Calendar();
			calendar.setSummary("WINE");
			calendar.setTimeZone("America/Montreal");

			// Insert the new calendar
			Calendar createdCalendar = service.calendars().insert(calendar).execute();

			calendarId = createdCalendar.getId();
		}
	}
}