package house.neko.media.master;

import house.neko.media.common.Media;

import java.net.Socket;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**********************************************
 * @author Andrew Tomaszews
 * @date 2006.04.12
 * This class assumes that the server has already
 * taken care of authentication.
 **********************************************/


public class ServerWorker extends Thread
{
	private Server server;
	private Socket socket;
	private ObjectInputStream receivestream;
	private ObjectOutputStream sendstream;

	/**
	 *
	 * @param p
	 * @param s
	 * @throws java.io.IOException
	 */
	public ServerWorker(Server p, Socket s)
		throws java.io.IOException
	{
		socket = s;
		server = p;
		receivestream = new ObjectInputStream(s.getInputStream());
		sendstream = new ObjectOutputStream(socket.getOutputStream());
	}


	/**
	 *
	 */
	public void run()
	{
		try
		{
			RECEIVELOOP:while(socket != null && !socket.isClosed())
			{
				try
				{
					Object o = receivestream.readObject();
					if(o == null)
					{	continue RECEIVELOOP; }
					if(o instanceof Media)
					{
						server.addMedia((Media) o);
					}
				} catch(java.io.IOException ioe) {
					System.out.println("Exception on worker thread: " + ioe);
				}
			}
		} catch(Throwable t) {
			System.out.println("Exception on worker thread: " + t);
		}
	}

	/**
	 *
	 * @param o
	 * @throws java.io.IOException
	 */
	public void send(Object o)
		throws java.io.IOException
	{
		if(o != null)
		{
			try
			{	sendstream.writeObject(o); }
			catch(java.io.InvalidClassException ice)
			{	System.out.println("Excepction on worker thread: " + ice); }
			catch(java.io.NotSerializableException nse)
			{	System.out.println("Excepction on worker thread: " + nse); }
		}
	}
}