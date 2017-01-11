package checkers;

import MyClasses.NetworkGame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.WindowEvent;
import javax.swing.Timer;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@WebSocket(maxTextMessageSize = 64 * 1024)
public class GameWindowController implements Initializable
{

	@FXML
	private GridPane board;
	@FXML
	private Pane blackout;
	@FXML
	private Pane substrate;
	@FXML
	private Label alertMessage;
	@FXML
	private ProgressBar restTimePB1;
	@FXML
	private ProgressBar restTimePB2;
	@FXML
	private Label countDownL1;
	@FXML
	private Label countDownL2;
	@FXML
	private Label userNameL1;
	@FXML
	private Label userNameL2;

	private int roomID = 1;
	private int userNum = 1;
	private int currTurn = 1;
	private ImageView selectChecker = null;
	private int rowMovi, colMovi;
	private Timer timer;
	private int time;

	private final CountDownLatch closeLatch;
	@SuppressWarnings("unused")
	private Session session;
	private NetworkGame game;

	public GameWindowController()
	{
		this.closeLatch = new CountDownLatch(1);

		game = new NetworkGame(this);
		Thread threadApp = new Thread(game);
		threadApp.setDaemon(true);
		threadApp.start();

		// обратный отсчет времени
		timer = new Timer(1000, new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				Platform.runLater(new Runnable()
				{
					@Override
					public void run()
					{
						ProgressBar linkPB;
						Label linkT;

						if (currTurn == 1)
						{
							linkPB = (userNum == 1) ? restTimePB1 : restTimePB2;
							linkT = (userNum == 1) ? countDownL1 : countDownL2;
						}
						else
						{
							linkPB = (userNum == 1) ? restTimePB2 : restTimePB1;
							linkT = (userNum == 1) ? countDownL2 : countDownL1;
						}

						linkT.setText(String.valueOf(time));
						double procent = (1.0 / 60.0) * time;
						linkPB.setProgress(procent);
						time--;

						if (time == -1)
						{
							timer.stop();
							sendEventTimeEnd();
						}
					}
				});
			}
		});
	}

	public void initialize(URL location, ResourceBundle resources)
	{
		Platform.runLater(new Runnable()
		{
			@Override
			public void run()
			{
				board.getScene().getWindow().setOnCloseRequest(new EventHandler<WindowEvent>()
				{
					public void handle(WindowEvent event)
					{
						game.stop();
					}
				});
			}
		});

		// событие клик по €чейке
		board.setOnMouseClicked(new EventHandler<javafx.scene.input.MouseEvent>()
		{
			public void handle(javafx.scene.input.MouseEvent event)
			{
				try
				{
					if (selectChecker == null)
						return;

					int row = (int) Math.ceil(event.getSceneY() / 80) - 2;
					int col = (int) Math.ceil(event.getSceneX() / 80) - 2;

					if (row % 2 != col % 2)
						return;

					Node element = getNodeFromGridPane(board, col, row);
					String color = (userNum == 1) ? "white" : "black";

					if (element != null)
						return;

					// отправка координат
					JSONObject dataSend = new JSONObject();
					dataSend.accumulate("room_id", roomID);
					dataSend.accumulate("action", 2);

					JSONObject coord = new JSONObject();
					coord.append("current_pos", GridPane.getRowIndex(selectChecker));
					coord.append("current_pos", GridPane.getColumnIndex(selectChecker));
					coord.append("moving_pos", row);
					coord.append("moving_pos", col);

					dataSend.accumulate("data_step", coord);
					dataSend.accumulate("session_id", Checkers.getSessionID());

					session.getRemote().sendString(dataSend.toString());

					rowMovi = row;
					colMovi = col;
				}
				catch (JSONException ex)
				{
					Logger.getLogger(GameWindowController.class.getName()).log(Level.SEVERE, null, ex);
				}
				catch (IOException ex)
				{
					Logger.getLogger(GameWindowController.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		});
	}

	@OnWebSocketConnect
	public void onConnect(Session session)
	{
		System.out.printf("Got connect: %s%n", session);
		this.session = session;
	}

	@OnWebSocketMessage
	public void onMessage(String msg)
	{
		Platform.runLater(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					JSONObject jsonObj = new JSONObject(msg);
					int action = (int) jsonObj.get("action");

					switch (action)
					{
						case 200:
							JSONObject dataSend = new JSONObject();
							dataSend.accumulate("game_id", 4);
							dataSend.accumulate("count_place", 2);
							dataSend.accumulate("action", 1);
							dataSend.accumulate("data", null);
							dataSend.accumulate("session_id", Checkers.getSessionID());
							
							System.out.println(dataSend);
							
							session.getRemote().sendString(dataSend.toString());
							break;
						case 1:
							userNum = (int) jsonObj.get("turnId");
							roomID = (int) jsonObj.get("roomId");
							break;
						case 2:
							JSONArray users = jsonObj.getJSONArray("users");
							String user1,
							 user2;

							if (userNum == 1)
							{
								user1 = users.getJSONObject(1).getString("login");
								user2 = users.getJSONObject(0).getString("login");
							}
							else
							{
								user1 = users.getJSONObject(0).getString("login");
								user2 = users.getJSONObject(1).getString("login");
							}

							userNameL1.setText(user1);
							userNameL2.setText(user2);

							arrangement();
							alertMessage.setVisible(false);
							substrate.setVisible(false);
							blackout.setVisible(false);

							startTimer();
							break;
						case 3:
							if (jsonObj.get("continuedKick") == JSONObject.NULL)
								currTurn = (currTurn == 1) ? 2 : 1;

							int rowCurr = GridPane.getRowIndex(selectChecker);
							int colCurr = GridPane.getColumnIndex(selectChecker);
							dlowChecker(rowCurr, colCurr, rowMovi, colMovi);

							board.getChildren().remove(selectChecker);
							selectChecker.setEffect(null);

							if (jsonObj.get("dataCrown") != JSONObject.NULL)
								selectChecker.setImage(new Image("images/checker_" + selectChecker.getId() + "_crown.png"));

							board.add(selectChecker, colMovi, rowMovi);
							selectChecker = null;

							startTimer();
							break;
						case 4:
							if (jsonObj.get("continuedKick") == JSONObject.NULL)
								currTurn = (currTurn == 1) ? 2 : 1;

							int rc = jsonObj.getJSONObject("dataStep").getJSONArray("current_pos").getInt(0);
							int cc = jsonObj.getJSONObject("dataStep").getJSONArray("current_pos").getInt(1);
							int rm = jsonObj.getJSONObject("dataStep").getJSONArray("moving_pos").getInt(0);
							int cm = jsonObj.getJSONObject("dataStep").getJSONArray("moving_pos").getInt(1);
							dlowChecker(rc, cc, rm, cm);

							ImageView element = (ImageView) getNodeFromGridPane(board, cc, rc);
							board.getChildren().remove(element);

							if (jsonObj.get("dataCrown") != JSONObject.NULL)
								element.setImage(new Image("images/checker_" + element.getId() + "_crown.png"));

							board.add(element, cm, rm);

							startTimer();
							break;
						case 5:
							String userWin = jsonObj.getString("userWin");
							alertMessage.setText("»гра завершена, победил пользователь: " + userWin + "!");
							blackout.setVisible(true);
							substrate.setVisible(true);
							alertMessage.setVisible(true);
							
							timer.stop();
							game.stop();
							break;
					}

					System.out.printf("Got msg: %s%n", msg);
				}
				catch (JSONException ex)
				{
					Logger.getLogger(GameWindowController.class.getName()).log(Level.SEVERE, null, ex);
				}
				catch (IOException ex)
				{
					Logger.getLogger(GameWindowController.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		});
	}

	public boolean awaitClose(int duration, TimeUnit unit)
	{
		try
		{
			return this.closeLatch.await(duration, unit);
		}
		catch (InterruptedException ex)
		{
			Logger.getLogger(GameWindowController.class.getName()).log(Level.SEVERE, null, ex);
		}

		return false;
	}

	@OnWebSocketClose
	public void onClose(int statusCode, String reason)
	{
		System.out.printf("Connection closed: %d - %s%n", statusCode, reason);
		this.session = null;
		this.closeLatch.countDown();
	}

	// растановка шашек
	private void arrangement()
	{
		Platform.runLater(new Runnable()
		{
			@Override
			public void run()
			{
				for (int i = 0; i < 8; i++)
					for (int j = 0; j < 8; j++)
						if (i % 2 == j % 2)
						{
							String color = "";

							if (i < 3)
								color = (userNum == 1) ? "black" : "white";
							else if (i > 4)
								color = (userNum == 1) ? "white" : "black";
							else
								continue;

							ImageView checker = new ImageView("images/checker_" + color + ".png");
							checker.setOnMouseClicked(new EventHandler<javafx.scene.input.MouseEvent>()
							{
								public void handle(javafx.scene.input.MouseEvent event)
								{
									if (currTurn != userNum)
										return;

									String userColor = (userNum == 1) ? "white" : "black";

									if (checker.getId() != userColor)
										return;

									if (selectChecker != null)
										selectChecker.setEffect(null);

									selectChecker = checker;
									DropShadow ds = new DropShadow(50, Color.RED);
									checker.setEffect(ds);
								}
							});
							checker.setId(color);
							checker.setFitWidth(50);
							checker.setFitHeight(50);
							board.add(checker, j, i);
						}
			}
		});

	}

	// получавем содержимое €чейки на доске
	private Node getNodeFromGridPane(GridPane gridPane, int col, int row)
	{
		for (Node node : gridPane.getChildren())
			if (GridPane.getColumnIndex(node) == col && GridPane.getRowIndex(node) == row)
				return node;

		return null;
	}

	// запуск таймера
	private void startTimer()
	{
		timer.stop();
		countDownL1.setText("0");
		countDownL2.setText("0");
		restTimePB1.setProgress(0);
		restTimePB2.setProgress(0);
		time = 60;
		timer.start();
	}

	// отправка уведомнени€ о завершение времени
	private void sendEventTimeEnd()
	{
		try
		{
			timer.stop();
			JSONObject dataSend = new JSONObject();
			dataSend.accumulate("room_id", roomID);
			dataSend.accumulate("action", 3);
			dataSend.accumulate("session_id", Checkers.getSessionID());

			if (session.isOpen())
				session.getRemote().sendString(dataSend.toString());
		}
		catch (JSONException ex)
		{
			Logger.getLogger(GameWindowController.class.getName()).log(Level.SEVERE, null, ex);
		}
		catch (IOException ex)
		{
			Logger.getLogger(GameWindowController.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	// удаление шашек
	private void dlowChecker(int rowCurr, int colCurr, int rowMove, int colMove)
	{
		int r = rowCurr, c = colCurr;

		while (true)
		{
			r = (rowCurr > rowMove) ? r - 1 : r + 1;
			c = (colCurr > colMove) ? c - 1 : c + 1;

			if (r == rowMove && c == colMove)
				break;

			Node node = getNodeFromGridPane(board, c, r);

			if (node != null)
				board.getChildren().remove(node);
		}
	}
	
}
