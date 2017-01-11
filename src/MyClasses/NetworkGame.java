package MyClasses;

import checkers.GameWindowController;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

public class NetworkGame implements Runnable
{
	private GameWindowController socket;
	private WebSocketClient client;
	
	public NetworkGame(GameWindowController socket)
	{
		this.socket = socket;
		this.client = new WebSocketClient();
	}
	
	public void stop()
	{
		try
		{
			client.stop();
		}
		catch (Exception ex)
		{
			Logger.getLogger(NetworkGame.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	
	@Override
	public void run()
	{
		String destUri = "ws://192.162.101.126:20002";
		
		try
		{
			client.start();
			URI echoUri = new URI(destUri);
			ClientUpgradeRequest request = new ClientUpgradeRequest();
			client.connect(socket, echoUri, request);
			socket.awaitClose(1, TimeUnit.DAYS);
		}
		catch (Throwable t)
		{
			t.printStackTrace();
		}
		finally
		{
			try
			{
				client.stop();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
		
}
