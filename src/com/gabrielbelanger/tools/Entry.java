package com.gabrielbelanger.tools;

import java.io.Serializable;

public class Entry implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String title;
	private String release;
	private String runtime;
	private String rating;
	private String score;
	private String type;
	private String id;
	
	public Entry(){
		
	}
	
	public Entry(String title, String release, String runtime, String rating, String type, String id){
		this.title = title;
		this.release = release;
		this.runtime = runtime;
		this.rating = rating;
		this.type = type;
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getRelease() {
		return release;
	}

	public void setRelease(String release) {
		this.release = release;
	}

	public String getRuntime() {
		return runtime;
	}

	public void setRuntime(String runtime) {
		this.runtime = runtime;
	}

	public String getRating() {
		return rating;
	}

	public void setRating(String rating) {
		this.rating = rating;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
}
