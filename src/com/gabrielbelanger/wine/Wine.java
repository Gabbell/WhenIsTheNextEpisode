package com.gabrielbelanger.wine;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Wine extends Application{
	
	public static final int WIDTH = 600;
	public static final int HEIGHT = 400;
	
	private Scene mainScene;
	
	private Parent splashRoot;
	private Parent homeRoot; 
	
	public static void main (String[]args){
		Application.launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		FXMLLoader splashLoader = new FXMLLoader(getClass().getResource("/res/Splash.fxml"));
		
		splashRoot = splashLoader.load();
		
		mainScene = new Scene(splashRoot, WIDTH, HEIGHT);
		
		primaryStage.initStyle(StageStyle.UNDECORATED);
		primaryStage.setTitle("When Is the Next Episode?");
		primaryStage.setScene(mainScene);
		primaryStage.show();
		
		showHome();
		
	}
	
	public void showHome() throws IOException{
		FXMLLoader homeLoader = new FXMLLoader(getClass().getResource("/res/Home.fxml"));
		homeRoot = homeLoader.load();
		
		mainScene.setRoot(homeRoot);
	}
}
