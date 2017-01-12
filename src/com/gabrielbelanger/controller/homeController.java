package com.gabrielbelanger.controller;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

import com.gabrielbelanger.tools.Entry;
import com.gabrielbelanger.tools.DateManipulator;
import com.gabrielbelanger.tools.GoogleAuth;
import com.gabrielbelanger.tools.JSonParsing;
import com.gabrielbelanger.tools.LocalWatchlist;
import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Calendar;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.gson.JsonObject;

import javafx.application.Platform;
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
import javafx.scene.text.Text;

public class homeController implements Initializable {
	
	@FXML
	TextField searchField;
	//Series/Movie info labels
	@FXML
	Label statusLabel, titleLabel, releaseLabel, runtimeLabel, ratingLabel, typeLabel, idLabel;
	
	@FXML
	Label connectLabel;
	
	@FXML
	ListView<String> watchlistView;

	@FXML
	Button exitButton, connectButton, watchlistButton,purgeButton;
	
	@FXML
	ProgressIndicator syncProgress, searchProgress;
	
	@FXML
	ImageView poster;
	
	@FXML
	Circle statusCircle;
	
	@FXML
	ComboBox<Text> syncOption;
	
	private LocalWatchlist localWatch;
	private DateManipulator dateManip;
	private String mazeURL;
	private URL searchURL;
	private HttpURLConnection connection;
	private com.google.api.services.calendar.Calendar service;
	
	private String calendarId;
	private boolean isConnected;
    private boolean syncSingle;

	private Text optionSyncSingle;
	private Text optionSyncAll;

	public homeController(){
		localWatch = new LocalWatchlist();
		dateManip = new DateManipulator();
		mazeURL = "http://api.tvmaze.com/";

		isConnected = false;
        syncSingle = false;

        optionSyncAll = new Text("Sync All Next Episodes");
		optionSyncSingle = new Text("Sync Next Episode");
	}

	@Override
	public void initialize(URL fxmlfilelocation, ResourceBundle resources) {
		//Setting up connection with TVMAZE
		try {
			connection = (HttpURLConnection)new URL(mazeURL).openConnection();
			connection.setDoOutput(true);
			connection.setRequestMethod("GET");
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	
		//Populating syncOption ComboBox
		syncOption.getItems().removeAll(syncOption.getItems());
		syncOption.getItems().addAll(optionSyncAll, optionSyncSingle);
        optionSyncAll.setId("syncEpisodes");
        optionSyncSingle.setId("syncSingle");
        syncOption.getSelectionModel().selectFirst();
		
		//After initialize
		Platform.runLater(new Runnable() {
            @Override public void run() {
                searchField.requestFocus();
                
        		watchlistView.setItems(localWatch.getArray());
        		watchlistView.getSelectionModel().selectFirst(); //Select first item in watchlist to prevent out of bounds index
            }
        });
		
		//Shutdown hook to dump array into the file
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
		    public void run() {
				try {
					//Dumps array in the watchlist file and closes stream
					localWatch.dumpArray();
					localWatch.closeStream();
				} catch (IOException e) {
					e.printStackTrace();
				}
		    }
		}));
	}

    public void refreshOptions(){

        if(syncOption.getSelectionModel().getSelectedIndex() == 0){
            syncSingle = true;
        }
        else if (syncOption.getSelectionModel().getSelectedIndex() == 1){
            syncSingle = false;
        }

    }
	
	//Enter has been pressed in the search field, search the database
	public void searchDatabase() throws MalformedURLException {
		searchProgress.setVisible(true);
		Thread t = new Thread(new Runnable() {

			public void run() {

				String searchinput = searchField.getText();
				// Replacing all spaces with hyphen for web syntax
				searchinput = searchinput.replace(' ', '+');

				try {
					searchURL = new URL(mazeURL + "singlesearch/shows?q=" + searchinput);

					JsonObject jsondb = JSonParsing.getGson(searchURL);

					Platform.runLater(new Runnable() {

						@Override
						public void run() {
							if (JSonParsing.isValidData(jsondb)) {
								statusLabel.setText(""); // Data is valid, do not show wrong data message
								titleLabel.setText(jsondb.get("name").toString().replace("\"", ""));
								releaseLabel.setText(jsondb.get("premiered").toString().replace("\"", ""));
								runtimeLabel.setText(jsondb.get("runtime").toString().replace("\"", "") + " minutes");
								ratingLabel.setText(jsondb.get("rating").getAsJsonObject().get("average").toString().replace("\"", ""));
								typeLabel.setText(jsondb.get("type").toString().replace("\"", ""));
								idLabel.setText(jsondb.get("id").toString().replace("\"", ""));

								// Checking existence of poster before setting it
								if (!jsondb.get("image").toString().equals("\"N/A\"")) {
									poster.setImage(new Image(jsondb.get("image").getAsJsonObject().get("medium").toString().replace("\"", "")));
								}

								watchlistButton.setVisible(true);
								statusLabel.setText("Data updated");
							} 
							else {
								watchlistButton.setVisible(false);
								statusLabel.setText("Could not find data");
							}

							searchProgress.setVisible(false);

						}
					});

				} catch (MalformedURLException e) {
					e.printStackTrace();
				}

			}
		});
		t.start();
	}

	public void syncNext() throws IOException{
		syncProgress.setVisible(true);
		
		Thread t = new Thread(new Runnable() {

		public void run() {
			try {
				ArrayList<Event> episodeArray = getEvent();
				if(episodeArray!= null){
                    for(Event episodeevent: episodeArray) {
                        service.events().insert(calendarId, episodeevent).execute();
                    }
				}
			} 
			catch (IOException e) {
				e.printStackTrace();
			}
			
			Platform.runLater(new Runnable() {
				public void run() {
					syncProgress.setVisible(false);
				}
			});
		}
		});
		t.start();
		
		statusLabel.setText("Next episode synced");
	}
	
	//Button Methods
	public void addToWatchlist() throws IOException{
		Entry entry = new Entry(titleLabel.getText(), releaseLabel.getText(), runtimeLabel.getText(),
				ratingLabel.getText(), typeLabel.getText(), idLabel.getText());
		
		//Verifying if a duplicate exist in the list and adding accordingly
		if(!localWatch.isDuplicate(entry)){
			localWatch.addEntry(entry);
			statusLabel.setText("Entry added to watchlist");
		}
		else{
			statusLabel.setText("Duplicate found, entry not added");
		}
	}
	
	//Remove the selected entry from the LocalWatchlist
	public void removeFromWatchlist(){
		int i = watchlistView.getSelectionModel().getSelectedIndex();
		localWatch.removeEntry(i);
	}
	
	//Removes every entry in the LocalWatchlist
	public void purgeWatchlist(){
		int initialSize = localWatch.getSize();
		
		for (int i = 0; i < initialSize; i++){
			localWatch.removeEntry(0);
		}
	}
	
	//X button was pressed, exit the program
	public void exitProgram() throws IOException{
		System.exit(0);
	}
	
	public void glowOn(MouseEvent event){
		((Node) event.getSource()).setEffect(new Glow(1));
	}
	public void glowOff(MouseEvent event){
		((Node) event.getSource()).setEffect(null);
	}
	
	public void googleConnect(){
		Thread t = new Thread(new Runnable() {

			public void run() {
				//Attempting to connect to calendar service
				try {
					service = GoogleAuth.getCalendarService();
					createWineCalendar();
					
					Platform.runLater(new Runnable() { public void run() {
						statusCircle.setFill(Color.GREEN); //Connection was successful, light is green
						statusLabel.setText("Google account connected");
					}});
					
					isConnected = true;
				}
				catch (IOException e) {
					System.out.println("Credentials were not obtained");
					isConnected = false;
					
					e.printStackTrace();
				}
			}
		});
		t.start();
	}
	public void syncEpisodes(){
        syncAllNext();
	}

    public void syncAllNext(){
        syncProgress.setVisible(true);
        statusLabel.setText("Synchronization started");

        Thread t = new Thread(new Runnable() {
            public void run() {

                BatchRequest batch = service.batch();

                JsonBatchCallback<Event> callback = new JsonBatchCallback<Event>() {
                    public void onSuccess(Event arg0, HttpHeaders arg1) throws IOException {

                    }
                    public void onFailure(GoogleJsonError arg0, HttpHeaders arg1) throws IOException {
                    }
                };

                if(isConnected){
                    try {
                        service.calendars().delete(calendarId).execute();
                        createWineCalendar();

                        watchlistView.getSelectionModel().selectFirst();

                        for(int i = 0; i < localWatch.getSize(); i++){
                            ArrayList<Event> episodeArray = getEvent();

                            for(Event episodeEvent: episodeArray) {
                                service.events().insert(calendarId, episodeEvent).queue(batch, callback);
                                Platform.runLater(new Runnable() {
                                    public void run(){
                                        statusLabel.setText("Next episodes synced");
                                    }
                                });
                            }

                            watchlistView.getSelectionModel().selectNext();
                        }

                        batch.execute();
                    }
                    catch (IOException e){
                        e.printStackTrace();
                    }

                    Platform.runLater(new Runnable() {
                        public void run() {
                            syncProgress.setVisible(false);
                            statusLabel.setText("Synchronization done");
                        }
                    });
                }
                else {
                    statusLabel.setText("Google account not connected");
                }
            }
        });

        t.start();
    }

	public ArrayList<Event> getEvent() throws MalformedURLException{

        ArrayList<Event> episodearray = new ArrayList<>();
 		//Trying to find next episode, if null, return error message
 		try{

            String id = localWatch.getEntry(watchlistView.getSelectionModel().getSelectedIndex()).getId();
 			searchURL = new URL(mazeURL + "shows/" + id);
 	 		JsonObject jsondb = JSonParsing.getGson(searchURL);
 	 		
 			searchURL = new URL(jsondb.get("_links").getAsJsonObject().get("nextepisode").getAsJsonObject().get("href").toString().replace("\"", ""));
 	 		jsondb = JSonParsing.getGson(searchURL);
 	 		
 	 		if(isConnected){

                String currentSeason = jsondb.get("season").toString();
                int episodePointer = Integer.parseInt(jsondb.get("number").toString());

                while(true) {

                    searchURL = new URL(mazeURL + "shows/" + id + "/episodebynumber?season=" + currentSeason + "&number=" + episodePointer);
                    jsondb = JSonParsing.getGson(searchURL);

                    //Creating episode event
                    Event episodeevent = new Event()
                            .setSummary(localWatch.getEntry(watchlistView.getSelectionModel().getSelectedIndex()).getTitle())
                            .setDescription(jsondb.get("name").toString() + "\n" + "Season " + currentSeason + ", Episode " + episodePointer);

                    //Get airdate and airtime of next episode as string
                    String datestring = jsondb.get("airstamp").toString().replace("\"", "");
                    int runtime = Integer.parseInt(jsondb.get("runtime").toString());

                    DateTime startstamp = new DateTime(datestring);
                    DateTime endstamp = dateManip.addMinutes(startstamp, runtime);


                    EventDateTime starttime = new EventDateTime()
                            .setDateTime(startstamp);
                    EventDateTime endtime = new EventDateTime()
                            .setDateTime(endstamp);
                    episodeevent.setStart(starttime);
                    episodeevent.setEnd(endtime);

                    episodearray.add(episodeevent);
                    episodePointer++;
                }
 		 	}

 	 		else {
		 		statusLabel.setText("Google account not connected");
 	 		}
 		}
 		//No next episode was found
 		catch (NullPointerException e){
 			searchURL = null;
		 		Platform.runLater(new Runnable() {
	 				public void run(){
	 					statusLabel.setText("No next episode found");
	 				}
	 			});

            return episodearray;
 		}
 		return null;
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