package checkers;

import MyClasses.MyProxy;
import java.net.Authenticator;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Checkers extends Application
{
	private static String sessionID;
	
	@Override
	public void start(Stage stage) throws Exception
	{
//		System.setProperty("http.proxyHost", "10.3.0.3");
//        System.setProperty("http.proxyPort", "3128");
//        Authenticator.setDefault(new MyProxy("inet", "123456"));

		Parent root = FXMLLoader.load(getClass().getResource("Authorization.fxml"));
		
		Scene scene = new Scene(root);

		stage.setResizable(false);
		stage.setTitle("Шашки");
		
		stage.setScene(scene);
		stage.show();
	}
	
	public static void main(String[] args)
	{
		launch(args);
	}

	public static String getSessionID()
	{
		return sessionID;
	}

	public static void setSessionID(String sessionID)
	{
		Checkers.sessionID = sessionID;
	}
	
	
}
