package com.gabrielbelanger.tools;

import com.google.api.client.util.DateTime;

public class DateManipulator {
	
	public DateManipulator(){
		
	}
	
	public DateTime addMinutes(DateTime startstamp, int minutes){
		
		long newEpochTime = startstamp.getValue() + (minutes*60)*1000;
 		DateTime endstamp = new DateTime(newEpochTime);	
 		
 		return endstamp;
	}
}
