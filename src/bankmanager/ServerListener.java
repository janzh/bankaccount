package bankmanager;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

import bankaccount.messages.Message;

public class ServerListener extends Thread
{
	private boolean isRunning;
	private ServerSocket serverSocket;
	private Replica replica;
	
	public ServerListener(Replica replica)
	{
		this.replica = replica;
		isRunning = true;
		try
		{
			serverSocket = new ServerSocket(replica.getLocationData().getPort());
		}
		catch(IOException e)
		{
			System.out.println("IOException while trying to listen!");
		}
	}
	
	public void run()
	{
		Socket socket = null;
		ObjectInputStream in;
		while(isRunning)
		{
			try
			{
				socket = serverSocket.accept();
				in = new ObjectInputStream(socket.getInputStream());
				// TODO: Show message
				replica.setMessage((Message)in.readObject());
			}
			catch(IOException | ClassNotFoundException e)
			{
				System.out.println("IOException while trying to accept connection!");
				e.printStackTrace();
			}
			finally
			{
				try
				{
					if(socket != null)
						socket.close();
				}
				catch(Exception e){}
			}
		}
		try
		{
			if(serverSocket != null)
				serverSocket.close();
		}
		catch(Exception e){}
	}
	
	public void kill()
	{
		isRunning = false;
	}

}