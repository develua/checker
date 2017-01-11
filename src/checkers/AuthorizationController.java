package checkers;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javax.swing.JOptionPane;

public class AuthorizationController implements Initializable
{

	@FXML
	private TextField loginTF;
	@FXML
	private PasswordField passwordPF;
	@FXML
	private Button btnEnter;

	public void initialize(URL url, ResourceBundle rb)
	{

	}

	@FXML
	private void enterBtnAction(ActionEvent event)
	{
		if (loginTF.getText().equals("") || passwordPF.getText().equals(""))
			return;

		loginTF.setDisable(true);
		passwordPF.setDisable(true);
		btnEnter.setDisable(true);

		Thread thread = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					CookieManager manager = new CookieManager();
					manager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
					CookieHandler.setDefault(manager);

					String url = "http://192.162.101.126/enter";
					URL obj = new URL(url);
					HttpURLConnection con = (HttpURLConnection) obj.openConnection();

					//add reuqest header
					con.setRequestMethod("POST");
					con.setRequestProperty("User-Agent", "Mozilla/5.0");

					String urlParameters = "login=" + loginTF.getText() + "&password=" + passwordPF.getText();

					// Send post request
					con.setDoOutput(true);
					DataOutputStream wr = new DataOutputStream(con.getOutputStream());
					wr.writeBytes(urlParameters);
					wr.flush();
					wr.close();

					BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
					StringBuffer response = new StringBuffer();
					String inputLine;
					
					while ((inputLine = in.readLine()) != null)
						response.append(inputLine);
					in.close();

					CookieStore cookieJar = manager.getCookieStore();
					List<HttpCookie> cookies = cookieJar.getCookies();

					for (HttpCookie cookie : cookies)
						if (cookie.getName().equals("PHPSESSID"))
							Checkers.setSessionID(cookie.getValue());

					if (con.getURL().toString().indexOf("enter") == -1)
						Platform.runLater(new Runnable()
						{
							@Override
							public void run()
							{
								try
								{
									Parent root = FXMLLoader.load(getClass().getResource("GameWindow.fxml"));
									Scene stageGame = new Scene(root);
									Stage stage = (Stage) loginTF.getScene().getWindow();
									stage.setScene(stageGame);
									stage.centerOnScreen();
								}
								catch (IOException ex)
								{
									Logger.getLogger(AuthorizationController.class.getName()).log(Level.SEVERE, null, ex);
								}
							}
						});
					else
					{
						JOptionPane.showMessageDialog(null, "Не удалось авторизоваться!", "Ошибка", JOptionPane.WARNING_MESSAGE);
						loginTF.setDisable(false);
						passwordPF.setDisable(false);
						btnEnter.setDisable(false);
					}

				}
				catch (MalformedURLException ex)
				{
					Logger.getLogger(AuthorizationController.class.getName()).log(Level.SEVERE, null, ex);
				}
				catch (ProtocolException ex)
				{
					Logger.getLogger(AuthorizationController.class.getName()).log(Level.SEVERE, null, ex);
				}
				catch (IOException ex)
				{
					Logger.getLogger(AuthorizationController.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		});

		thread.setDaemon(true);
		thread.start();

	}

}
