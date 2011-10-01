package house.neko.media.device;

import house.neko.media.common.Media;
import house.neko.media.common.MediaLocation;
import house.neko.media.common.MimeType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;

import javax.sound.sampled.*;
import javax.sound.sampled.AudioFileFormat.Type;

public class MP3Converter //implements Converter
{
	static private javax.sound.sampled.AudioFileFormat.Type type = javazoom.spi.mpeg.sampled.file.MpegFileFormatType.MP3;
	static private javax.sound.sampled.AudioFormat audioFormat = null;
	static private int nLengthInFrames = AudioSystem.NOT_SPECIFIED;
	static private int nLengthInBytes = AudioSystem.NOT_SPECIFIED;
	
	static private java.util.Map setupDefaults(Media m)
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
	
	static public boolean writeToFile(Media m, MediaLocation l, File outFile) 
	{
		AudioInputStream inFileAIS = null;
		try 
		{
			InputStream inFile = l.getInputStream();
			// query file type
			AudioFileFormat inFileFormat = AudioSystem.getAudioFileFormat(inFile);
			inFile.reset(); // rewind
			java.util.Map properties = setupDefaults(m);
			AudioFileFormat mp3Format = new javazoom.spi.mpeg.sampled.file.MpegAudioFileFormat(type, audioFormat, nLengthInFrames, nLengthInBytes, properties);
			if(inFileFormat.getType() != mp3Format.getType()) 
			{
				// inFile is not AIFF, so let's try to convert it.
				inFileAIS = AudioSystem.getAudioInputStream(inFile);
				//inFileAIS.reset(); // rewind
				if(!AudioSystem.isFileTypeSupported(mp3Format.getType(), inFileAIS)) 
				{
					System.out.println("Warning: " + mp3Format.getType() + " conversion of "  + inFileFormat.getType() + " is not currently supported by AudioSystem.");
					inFileAIS = getStreamAsPCM(inFileAIS);
					if(inFileAIS == null)
					{
						System.out.println("Unable to get AIFF stream from input");
						return false;
					}
				}
				AudioSystem.write(inFileAIS, mp3Format.getType(), outFile);
				System.out.println("Successfully made " + mp3Format.getType() + " file, " + outFile.getAbsolutePath() + ", from " + inFileFormat.getType() + " file");
				inFileAIS.close();  // All done now
			} else {
				System.out.println("Input file " + inFileFormat.getType() + " is MP3. Conversion is unnecessary.");
			}
		} catch(UnsupportedAudioFileException e) {
			System.err.println("Error: " + m + " is not a supported audio file type from " + inFileAIS.getFormat() + ": " + e);
			e.printStackTrace();
			return false;
		} catch(IOException e) {
			System.err.println("Error: failure attempting to read "  + l.getLocationURLString() + ": " + e);
			e.printStackTrace();
			return false;
		} catch(IllegalArgumentException e) {
			System.err.println("Error: MP3 is not a supported audio file type from " + inFileAIS.getFormat() + ": " + e);
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	static public AudioInputStream getInputStream(Media m, MediaLocation l) 
	{
		AudioInputStream inFileAIS = null;
		try 
		{
			InputStream inFile = l.getInputStream();
			// query file type
			AudioFileFormat inFileFormat = AudioSystem.getAudioFileFormat(inFile);
			inFile.reset(); // rewind
			java.util.Map properties = setupDefaults(m);
			AudioFileFormat mp3Format = new javazoom.spi.mpeg.sampled.file.MpegAudioFileFormat(type, audioFormat, nLengthInFrames, nLengthInBytes, properties);
			if(inFileFormat.getType() != mp3Format.getType()) 
			{
				// inFile is not AIFF, so let's try to convert it.
				inFileAIS = AudioSystem.getAudioInputStream(inFile);
				//inFileAIS.reset(); // rewind
				if(!AudioSystem.isFileTypeSupported(mp3Format.getType(), inFileAIS)) 
				{
					System.out.println("Warning: " + mp3Format.getType() + " conversion of "  + inFileFormat.getType() + " is not currently supported by AudioSystem.");
					inFileAIS = getStreamAsPCM(inFileAIS);
					if(inFileAIS == null)
					{	System.out.println("Unable to get AIFF stream from input"); }
				}
			} else {
				System.out.println("Input file " + inFileFormat.getType() + " is MP3. Conversion is unnecessary.");
			}
		} catch(UnsupportedAudioFileException e) {
			System.err.println("Error: " + m + " is not a supported audio file type from " + inFileAIS.getFormat() + ": " + e);
			e.printStackTrace();
		} catch(IOException e) {
			System.err.println("Error: failure attempting to read "  + l.getLocationURLString() + ": " + e);
			e.printStackTrace();
		} catch(IllegalArgumentException e) {
			System.err.println("Error: MP3 is not a supported audio file type from " + inFileAIS.getFormat() + ": " + e);
			e.printStackTrace();
		}
		return inFileAIS;
	}
	
	static private AudioInputStream getStreamAsPCM(AudioInputStream is)
		throws IllegalArgumentException
	{	return AudioSystem.getAudioInputStream(AudioFormat.Encoding.PCM_SIGNED , is); }
}