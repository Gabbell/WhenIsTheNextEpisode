package com.gabrielbelanger.controller;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;

public class splashController implements Initializable {
	
	@FXML 
	AnchorPane homeRoot;
	
	@Override
	public void initialize(URL fxmlfilelocation, ResourceBundle resources) {
		
		FadeTransition ft = new FadeTransition(new Duration(1200),homeRoot);
		ft.setFromValue(1.0);
		ft.setToValue(0.0);
		ft.play();
		
	}
}
