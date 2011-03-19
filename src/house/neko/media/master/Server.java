package house.neko.media.master;

import house.neko.media.common.ConfigurationManager;
import house.neko.media.common.MediaLibrary;
import house.neko.media.common.Media;

import java.util.Vector;
import java.net.Socket;
import java.net.ServerSocket;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.channels.IllegalBlockingModeException;

import org.apache.commons.configuration.HierarchicalConfiguration;

/**
 *
 * @author andy
 */
public class Server implements Runnable
{
	private MediaLibrary library;
	private ServerSocket serversocket;
	private Vector<ServerWorker> workers;
	private HierarchicalConfiguration config;

	/**
	 *
	 */
	public Server()
	{
		config = ConfigurationManager.getConfiguration("Master");
		library = new MediaLibrary(config);

		workers = new Vector<ServerWorker>();
	}


	/**
	 *
	 */
	public void run()
	{
		try
		{
			while(true)
			{
				try
				{
					Socket s = serversocket.accept();
					ServerWorker worker = new ServerWorker(this, s);
					workers.add(worker);
					worker.start();
				} catch(IOException ioe) {
					System.out.println("Exception in server: " + ioe);
				} catch(SecurityException se) {
					System.out.println("Exception in server: " + se);
				} catch(IllegalBlockingModeException ibme) {
					System.out.println("Exception in server: " + ibme);
				}
			}
		} catch(Throwable t) {
			System.out.println("Exception in server: " + t);
		}
	}

	// worker interface
	/**
	 *
	 * @param m
	 */
	public void addMedia(Media m)
	{
		//
	}
}