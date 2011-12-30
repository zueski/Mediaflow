package house.neko.media.device;

import house.neko.media.common.Media;
import house.neko.media.common.MediaLocation;
import house.neko.media.common.MimeType;
import house.neko.media.common.ConfigurationManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;

import javax.sound.sampled.*;
import javax.sound.sampled.AudioFileFormat.Type;

import org.apache.commons.configuration.HierarchicalConfiguration;

import org.apache.commons.logging.Log;

public class FlacConverter //implements Converter
{
	private javax.sound.sampled.AudioFileFormat.Type type = javazoom.spi.mpeg.sampled.file.MpegFileFormatType.MP3;
	private javax.sound.sampled.AudioFormat audioFormat = null;
	private int nLengthInFrames = AudioSystem.NOT_SPECIFIED;
	private int nLengthInBytes = AudioSystem.NOT_SPECIFIED;
	
	private Log log;
	
	public FlacConverter()
	{
		this.log = ConfigurationManager.getLog(getClass());
		if(log.isTraceEnabled()) { log.trace("Intializing Flac Converter"); }
	}
	
	private java.util.Map setupDefaults(Media m)
	{
		java.util.Map<String, Object> p = new java.util.concurrent.ConcurrentHashMap<String, Object>();
		String name = m.getName();
		if(name != null)
		{	p.put("title", name); }
		String artist = m.getArtist();
		if(artist != null)
		{	p.put("author", artist); }
		String album = m.getAlbum();
		if(album != null)
		{	p.put("album", album); }
		
		p.put("mp3.vbr", "true");
		p.put("mp3.channels", 2);
		p.put("mp3.frequency.hz", 44100);
		p.put("mp3.vbr.scale", 8);
		p.put("mp3.mode", 1);
		return p;
	}
	
	private org.kc7bfi.jflac.sound.spi.FlacAudioFormat getAudioFormat(Media m, AudioInputStream inFileAIS)
		throws IOException
	{
		AudioFormat inputFormat = inFileAIS.getFormat();
		org.kc7bfi.jflac.metadata.StreamInfo streamInfo = new org.kc7bfi.jflac.metadata.StreamInfo(new org.kc7bfi.jflac.io.BitInputStream(inFileAIS), 0, false);
		return new org.kc7bfi.jflac.sound.spi.FlacAudioFormat(streamInfo);
	}
	
	public AudioInputStream getInputStream(Media m, MediaLocation l) 
	{
		AudioInputStream inFileAIS = null;
		try 
		{
			InputStream inFile = l.getInputStream();
			// query file type
			AudioFileFormat inFileFormat = AudioSystem.getAudioFileFormat(inFile);
			inFile.reset(); // rewind
			java.util.Map properties = setupDefaults(m);
			AudioFileFormat mp3FileFormat = new javazoom.spi.mpeg.sampled.file.MpegAudioFileFormat(type, audioFormat, nLengthInFrames, nLengthInBytes, properties);
			if(inFileFormat.getType() != mp3FileFormat.getType()) 
			{
				// inFile is not AIFF, so let's try to convert it.
				inFileAIS = AudioSystem.getAudioInputStream(inFile);
				AudioFormat mp3Format = getAudioFormat(m, inFileAIS);
				//inFileAIS.reset(); // rewind
				if(!AudioSystem.isFileTypeSupported(mp3FileFormat.getType(), inFileAIS)) 
				{
					System.out.println("Warning: " + mp3FileFormat.getType() + " conversion of "  + inFileFormat.getType() + " is not currently supported by AudioSystem.");
					inFileAIS = getStreamAsPCM(inFileAIS);
					if(inFileAIS == null)
					{
						if(log.isWarnEnabled()) { log.warn("Unable to get PCM stream from input"); }
						return null;
					}
					if(log.isTraceEnabled()) { log.trace("getting as MP3"); }
					inFileAIS = AudioSystem.getAudioInputStream(mp3Format, inFileAIS);
					if(inFileAIS == null)
					{
						if(log.isWarnEnabled()) { log.warn("Unable to get MP3 stream from input"); }
						return null;
					}
					if(log.isTraceEnabled()) { log.trace("got MP3: " + mp3Format); }
				}
			} else {
				if(log.isTraceEnabled()) { log.trace("Input file " + inFileFormat.getType() + " is MP3. Conversion is unnecessary."); }
			}
		} catch(UnsupportedAudioFileException e) {
			log.error("Error: " + m + " is not a supported audio file type to FLAC", e);
		} catch(IOException e) {
			log.error("Error: failure attempting to read "  + l.getLocationURLString(), e);
		} catch(IllegalArgumentException e) {
			log.error("Error: MP3 is not a supported audio file type from " + inFileAIS.getFormat(), e);
		}
		return inFileAIS;
	}
	
	private AudioInputStream getStreamAsPCM(AudioInputStream is)
		throws IllegalArgumentException
	{	return AudioSystem.getAudioInputStream(AudioFormat.Encoding.PCM_SIGNED , is); }
}