package com.gabrielbelanger.controller;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.ResourceBundle;

import com.gabrielbelanger.tools.Entry;
import com.gabrielbelanger.tools.GoogleAuth;
import com.gabrielbelanger.tools.JSonParsing;
import com.gabrielbelanger.tools.LocalWatchlist;
import com.google.gson.JsonArray;
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

public class homeController implements Initializable {
	
	@FXML
	TextField searchfield;
	//Series/Movie info labels
	@FXML
	Label statuslabel,titlelabel,releaselabel, runtimelabel, ratinglabel, typelabel, idlabel;
	
	@FXML
	Label connectedlabel;
	
	@FXML
	ListView<String> watchlistview;

	@FXML
	Button exitbutton,connectbutton,watchlistbutton,purgeButton;
	
	@FXML
	ProgressIndicator syncprogress,searchprogress;
	
	@FXML
	ImageView poster;
	
	@FXML
	ComboBox<String> syncoption;
	
	private LocalWatchlist localwatch = new LocalWatchlist();
	private String mazeurl = "http://api.tvmaze.com/";
	private HttpURLConnection connection;
	
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
				URL searchurl;
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
								runtimelabel.setText(jsondb.get("runtime").toString().replace("\"", ""));
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

	public void getNextEpisode(ActionEvent event) throws MalformedURLException{
		System.out.println();
		URL searchurl = new URL(mazeurl + "shows/" + localwatch.getEntry(watchlistview.getSelectionModel().getSelectedIndex()).getId());
 		JsonObject jsondb = JSonParsing.getGson(searchurl);
 		
 		//Trying to find next episode, if null, return error message
 		try{
 			searchurl = new URL(jsondb.get("_links").getAsJsonObject().get("nextepisode").getAsJsonObject().get("href").toString().replace("\"", ""));
 		}
 		catch (NullPointerException e){
 			searchurl = null;
 			System.out.println("No next episode was found");
 		}
 		
 		jsondb = JSonParsing.getGson(searchurl);
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
	public void syncWithCalendar(){ //FIXME Temporary Function Implementation
		localwatch.print();
	}
	public void googleConnect() throws IOException{
		GoogleAuth.getCalendarService();
	}
	
	@Deprecated
	public void getNextEpisodeDepecrated(ActionEvent event) throws MalformedURLException{
		String episodeid = localwatch.getEntry(watchlistview.getSelectionModel().getSelectedIndex()).getId();
		JsonObject jsondb;
		JsonArray episodearray;
		int seasonindex = 0; // Season index
		int episodeindex = 0; // Episode index
		Calendar currentdate = Calendar.getInstance();// Current data
		Calendar episodedate = Calendar.getInstance();// Episode date
		String[] datestring;
		int year,month,date;
		
		do{
			seasonindex++;
			URL searchurl = new URL(mazeurl + "i=" + episodeid + "&Season=" + seasonindex);
			jsondb = JSonParsing.getGson(searchurl);
		}
		while(JSonParsing.isValidData(jsondb));
		
		seasonindex--; //Last valid season
		URL searchurl = new URL(mazeurl + "i=" + episodeid + "&Season=" + seasonindex);
		jsondb = JSonParsing.getGson(searchurl);
		
		//Going through episodes and comparing with current data to get next episode
		episodearray = jsondb.getAsJsonArray("Episodes");
		do{
			jsondb = episodearray.get(episodeindex).getAsJsonObject(); //Using same "jsondb" for other purpose
			
			datestring = jsondb.get("Released").toString().replace("\"","").split("-");
			year = Integer.parseInt(datestring[0]);
			month = Integer.parseInt(datestring[1]);
			date = Integer.parseInt(datestring[2]);
			
			episodedate.set(year, month - 1, date);
			episodeindex++;
		}
		while(episodeindex < episodearray.size() && episodedate.compareTo(currentdate) < 0);
		
		episodeindex--;
		System.out.println(episodearray.get(episodeindex));
	}
}
