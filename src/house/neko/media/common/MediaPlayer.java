package house.neko.media.common;

import java.util.Observable;
import java.io.*;
import javax.sound.sampled.*;
import javax.sound.midi.*;
//import java.util.concurrent.locks.LockSupport;

import org.apache.commons.logging.Log;

// TODO: convert setInputStream to setMediaLocation

/**
 *
 * @author andy
 */
public final class MediaPlayer extends Observable
{
	public static final String PLAY = "play";
	public static final String STOP = "stop";
	
	private Log log;
	private Player player;
	private Thread playerThread;
	//LockSupport playlock;

	/**
	 *
	 */
	public MediaPlayer()
	{
		this.log = ConfigurationManager.getLog(getClass());
		player = new Player();
		playerThread = new Thread(player, "MediaPlayer");
		playerThread.start();
	}

	/**
	 *
	 */
	public void play()
	{	start(); }

	/**
	 *
	 */
	public void start()
	{
		player.stop = false;
		player.pause = false;
		player.play();
		playerThread.interrupt();
	}

	/**
	 *
	 */
	public void pause()
	{	player.pause(); }

	/**
	 *
	 */
	public void stop()
	{	player.stop(); }

	/**
	 *
	 */
	public void skipBackward()
	{

	}

	/**
	 *
	 */
	public void skipForward()
	{

	}

	/**
	 *
	 * @return
	 */
	public long getCurrentPlayTime()
	{
		if(player.line != null)
		{	return player.line.getMicrosecondPosition(); }
		else
		{	return 0L; }
	}

	/**
	 *
	 * @param i
	 */
	//public void setInputStream(InputStream i)
	//{	player.inputStream = i; }
	
	public void setMediaLocation(MediaLocation l)
	{	player.mediaLocation = l; }

	/**
	 *
	 */
	protected class Player implements Runnable
	{
		private AudioInputStream ain = null;  // We read audio data from here
		/**
		 *
		 */
		public SourceDataLine line = null;   // And write it here.
		private boolean isLineOpen = false;
		private AudioFormat format = null;
		private DataLine.Info info = null;

		/**
		 *
		 */
		//protected InputStream inputStream;
		protected MediaLocation mediaLocation = null;
		/**
		 *
		 */
		public boolean stop = false;
		/**
		 *
		 */
		public boolean pause = false;
		/**
		 *
		 */
		public boolean quit = false;
		/**
		 *
		 */
		public void play()
		{
			if(line != null && !line.isRunning())
			{	line.start(); }
			notifyObservers(PLAY);
		}
		
		/**
		 *
		 */
		public void pause()
		{
			player.pause = true;
			if(line != null && line.isRunning())
			{	line.stop(); }
		}
		
		/**
		 *
		 */
		public void stop()
		{
			if(log.isTraceEnabled())
			{	log.trace("Stopping player"); }
			player.stop = true;
			notifyObservers(STOP);
			if(line != null && line.isRunning())
			{
				line.stop();
				line.flush();
			}
		}
		
		/**
		 *
		 */
		public synchronized void run()
		{
			if(log.isTraceEnabled()) log.trace("Starting to play!");
			MediaLocation currLocation;
			LOOP:while(!quit)
			{
				if(log.isTraceEnabled()) log.trace("resetting currLocation to mediaLocation");
				try
				{
					if(stop || pause)
					{
						if(log.isTraceEnabled()) log.trace("Parking player@" + System.currentTimeMillis());
						try
						{
							wait();
						} catch(InterruptedException ie) {
							if(log.isTraceEnabled()) log.trace("Player was unparked from interruption");
						}
						stop = false;
						pause = false;
					}
					currLocation = mediaLocation;
					if(currLocation == null)
					{
						if(log.isWarnEnabled()) log.warn("Location is null, yeilding");
						stop = true;
						Thread.yield();
						continue LOOP;
					}
					// Get an audio input stream
					if(log.isTraceEnabled()) log.trace("Creating audio stream from location");
					if(ain != null)
					{	ain.close(); }
					try
					{
						ain = AudioSystem.getAudioInputStream(currLocation.getInputStream()); 
					} catch(Exception e) {
						log.error("Unable to create input stream for location " + currLocation, e);
						setChanged();
						stop();
						continue LOOP;
					}
					// Get information about the format of the stream
					format = ain.getFormat();
					info = new DataLine.Info(SourceDataLine.class, format);
					if(log.isTraceEnabled())
					{	log.trace("Got format: " + format); }
					// If the format is not supported directly (i.e. if it is not PCM
					// encoded), then try to transcode it to PCM.
					if(!AudioSystem.isLineSupported(info))
					{
						if(log.isTraceEnabled()) log.trace("Creating transcoder to PCM");
						// This is the PCM format we want to transcode to.
						// The parameters here are audio format details that you
						// shouldn't need to understand for casual use.
						AudioFormat pcm = new AudioFormat(format.getSampleRate(), 16, format.getChannels(), true, false);
						// Get a wrapper stream around the input stream that does the
						// transcoding for us.
						ain = AudioSystem.getAudioInputStream(pcm, ain);
						// Update the format and info variables for the transcoded data
						format = ain.getFormat();
						info = new DataLine.Info(SourceDataLine.class, format);
					}
					// Open the line through which we'll play the streaming audio.
					if(log.isTraceEnabled()) log.trace("Getting line");
					line = (SourceDataLine) AudioSystem.getLine(info);
					if(log.isTraceEnabled()) log.trace("Setting format to " + format);
					line.open(format);
					log.trace("Starting line out");
					line.start();
					int framesize = format.getFrameSize();
					if(log.isTraceEnabled()) log.trace("Got frame size of " + framesize);
					byte[] buffer = new byte[4096 * framesize]; // the buffer
					int numbytes = 0;		// how many bytes
					long played = 0;
					int bytesread = 0;
					int bytestowrite = 0;
					int remaining = 0;
					if(!line.isRunning())
					{
						line.flush();
						line.start();
					}
					// We'll exit the loop when we reach the end of stream
					if(log.isTraceEnabled()) log.trace("About to start playing");
					PLAY:while(!stop)
					{
						if(currLocation != mediaLocation)
						{
							//log.trace("Player -- inputStream changed, stopping");
							//stop = true;
							//break PLAY;
						}
						if(pause)
						{
							if(log.isTraceEnabled()) log.trace("pause flag set, pausing player");
							try
							{
								wait(); 
							} catch(InterruptedException ie) {
								if(log.isTraceEnabled()) log.trace("player was interrupted from pause");
							}
						}
						try
						{
							// First, read some bytes from the input stream.
							bytesread = ain.read(buffer, numbytes, buffer.length - numbytes);
							// If there were no more bytes to read, we're done.
							if(bytesread == -1)
							{
								if(log.isTraceEnabled()) log.trace("Player -- End of stream reached, stopping");
								ain.close();
								if(log.isTraceEnabled()) log.trace("clearing currLocation");
								currLocation = null;
								break PLAY;
							}
							numbytes += bytesread;
							played += (long) bytesread;
							// We must write bytes to the line in an integer multiple of
							// the framesize.  So figure out how many bytes we'll write.
							bytestowrite = (numbytes / framesize) * framesize;
							// Now write the bytes. The line will buffer them and play
							// them. This call will block until all bytes are written.
							line.write(buffer, 0, bytestowrite);
							// If we didn't have an integer multiple of the frame size,
							// then copy the remaining bytes to the start of the buffer.
							remaining = numbytes - bytestowrite;
							if (remaining > 0)
							{    System.arraycopy(buffer, bytestowrite, buffer, 0, remaining); }
							numbytes = remaining;
						} catch(Throwable t) {
							log.error("Failure playing", t);
							ain.close();
							if(log.isTraceEnabled()) log.trace("clearing currLocation");
							currLocation = null;
							line.stop();
						}
					}
					// Now block until all buffered sound finishes playing.
					if(line.isRunning())
					{	line.drain(); }
					if(log.isTraceEnabled()) log.trace("Player stopping line out (done playing?)");
					line.stop();
					stop = true;
				} catch(LineUnavailableException e) {
					log.error("Error playing stream", e);
				} catch(IOException e) {
					log.error("Error playing stream", e);
				}
				if(log.isTraceEnabled()) log.trace("Player reached end of control loop, starting over");
			}
			try { if(line != null) { line.close(); } } catch(Exception e) { log.error(e); }
			try { if(ain != null) { ain.close( ); } } catch(Exception e) { log.error(e); }
			if(log.isTraceEnabled()) log.trace("Player exiting!");
		}

		/**
		 *
		 */
		@Override
		protected void finalize()
		{
			try { if(line != null) { line.close(); } } catch(Exception e) { log.error(e); }
			try { if(ain != null) { ain.close( ); } } catch(Exception e) { log.error(e); }
			try { super.finalize(); } catch(Throwable t) { log.error(t); }
		}
	}

}