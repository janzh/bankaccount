package bankaccount;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

import bankaccount.messages.Message;

public class Communication {
	
	public static void sendMessage(NodeLocationData receiverLocation, Message msg){
		Socket socket = null;
		ObjectOutputStream out = null;
		try {
			InetAddress inetAddress = InetAddress.getByName(receiverLocation.getHost());
			
			int port = receiverLocation.getPort();

			socket = new Socket(inetAddress, port);
			
			out = new ObjectOutputStream(socket.getOutputStream());
			out.writeObject(msg);
			out.flush();

		} catch (UnknownHostException ex) {
		} catch (IOException ex) {
		} finally {
			try
			{
				if(out != null)
					out.close();
				if(socket != null)
					socket.close();
			}
			catch(IOException e){}
		}	
	}

	public static void sendMessage(Replica receiver, String message){
		Socket socket = null;
		try {
			InetAddress inetAddress = InetAddress.getByName(receiver.getLocationData().getHost());
			int port = receiver.getLocationData().getPort();
			//InetAddress inetAddress = InetAddress.getByName("localhost");
			//int port = 8001;

			socket = new Socket(inetAddress, port);

			PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);
			printWriter.println(message);

		} catch (UnknownHostException ex) {
		} catch (IOException ex) {
		} finally {
			if( socket != null){
				try { socket.close(); }
				catch (IOException ex) {}
			}
		}	
	}

	public void receiveMessage(Replica receiver){
		int port = receiver.getLocationData().getPort();
		//int port = 8001;
		Socket socket = null;
		try {
			ServerSocket serverSocket = new ServerSocket(port);

			socket = serverSocket.accept();
			Scanner scanner = new Scanner(socket.getInputStream());

			String line = scanner.nextLine();
		} catch (IOException ex) {
		} finally {
			if (socket != null) {
				try { socket.close(); }
				catch (IOException ex) {}
			}
		}
	}

}
