package com.gabrielbelanger.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class LocalWatchlist {
	
	private File watchfile = new File(System.getProperty("user.home") + "/Documents/WINE/local.watchlist");
	private ArrayList<Entry> watcharray = new ArrayList<Entry>();
	private ObservableList<String> obsarray = FXCollections.observableArrayList();
	
	private ObjectOutputStream oostream;
	private ObjectInputStream oistream;
	
	@SuppressWarnings("unchecked")
	public LocalWatchlist(){
		//Verifying if local watchlist exist, if not, create one. Assign input and output streams
        try {
            if (!watchfile.exists()){
                watchfile.getParentFile().mkdirs();
                watchfile.createNewFile();
                oostream = new ObjectOutputStream(new FileOutputStream(watchfile));
            }
            else{
                System.out.println("Detected Existing Local Watchfile");
                oistream = new ObjectInputStream(new FileInputStream(watchfile));
                watcharray = (ArrayList<Entry>) oistream.readObject();
                oistream.close();

                oostream = new ObjectOutputStream(new FileOutputStream(watchfile)); //Appending since header is already there
            }
		}
        catch (ClassNotFoundException | IOException e) {
            System.out.println("Your watchlist is corrupted. Please purge it.");
        }
	}
	
	public void addEntry(Entry inputentry) throws IOException{	
		watcharray.add(inputentry); //Add entry to the watcharray
		obsarray.add(inputentry.getTitle());
		
		//Sorts the lists alphabetically
		Collections.sort(watcharray, new Comparator<Entry>() {
	        @Override
	        public int compare(Entry e1, Entry e2) {
	            return e1.getTitle().compareToIgnoreCase(e2.getTitle());
	        }
	    });
		
		Collections.sort(obsarray, new Comparator<String>() {
	        @Override
	        public int compare(String s1, String s2) {
	            return s1.compareToIgnoreCase(s2);
	        }
	    });
	}
	public void removeEntry(int i){
		obsarray.remove(i);
		watcharray.remove(i);
	}

    public void purgeIt(){
        try {
            watchfile.delete();
            watchfile.createNewFile();
            oostream = new ObjectOutputStream(new FileOutputStream(watchfile));
        }
        catch (IOException e){
            System.out.println("There was an error in purging the watchlist. Could not create a new one.");
        }
    }

	public boolean isDuplicate(Entry inputentry){
		//Verifying for duplicates
		for(int i = 0; i < watcharray.size(); i++){
			if (inputentry.getId().equals(watcharray.get(i).getId())){
				return true;
			}
		}
		return false;
	}
	public void dumpArray() throws IOException{
		oostream.writeObject(watcharray);
	}
	
	//Setters
	public void setWatchArray(ArrayList<Entry> watcharray){
		this.watcharray = watcharray;
	}
	//Getters
	public int getSize(){
		if(watcharray!=null){
			return watcharray.size();
		}
		else{
			return 0;
		}
	}
	public Entry getEntry(int i){
		return watcharray.get(i);
	}

	public ObservableList<String> getArray(){
		obsarray.clear();
		for(int i = 0; i < watcharray.size(); i++){
			obsarray.add(watcharray.get(i).getTitle());
		}
		return obsarray;
	}

	public void print(){
		for(int i = 0; i < watcharray.size(); i++){
			System.out.println(watcharray.get(i).getTitle() + "\t\t\t" + watcharray.get(i).getId());
		}
	}
	
	public void closeStream() throws IOException{
		oostream.flush();
		oostream.close();
	}
}
