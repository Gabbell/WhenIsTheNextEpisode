package com.gabrielbelanger.wine;

import java.io.IOException;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Wine extends Application{
	
	public static final int WIDTH = 600;
	public static final int HEIGHT = 450;
	
    private double xOffset = 0;
    private double yOffset = 0;
    private Boolean beingDragged = false;
	
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
		
		primaryStage.getIcons().add(new Image("/img/icon.png"));
		primaryStage.initStyle(StageStyle.UNDECORATED);
		primaryStage.setTitle("When Is the Next Episode?");
		primaryStage.setScene(mainScene);
		primaryStage.show();
		
		showHome(primaryStage);
	}
	
	public void showHome(Stage primaryStage) throws IOException{
		FXMLLoader homeLoader = new FXMLLoader(getClass().getResource("/res/Home.fxml"));
		homeRoot = homeLoader.load();
		
		//Make window draggable
		homeRoot.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
            	if(event.getY() < 30){
            		beingDragged = true;
            	}
            	else
            		beingDragged = false;
            	
                xOffset = event.getSceneX();
                yOffset = event.getSceneY();
            }
        });
        homeRoot.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
            	if(beingDragged){
	                primaryStage.setX(event.getScreenX() - xOffset);
	                primaryStage.setY(event.getScreenY() - yOffset);
            	}
            }
        });
		
		mainScene.setRoot(homeRoot);
	}
}
