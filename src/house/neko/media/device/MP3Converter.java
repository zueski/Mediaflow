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
	static private int nLengthInFrames = 0;
	static private int nLengthInBytes = 0;
	static private java.util.Map properties = null;
	
	static private void setupDefaults()
	{
		
		properties = new java.util.concurrent.ConcurrentHashMap();
	}
	
	static public boolean writeToFile(Media m, MediaLocation l, File outFile) 
	{
		setupDefaults();
		AudioInputStream inFileAIS = null;
		try 
		{
			InputStream inFile = l.getInputStream();
			// query file type
			AudioFileFormat inFileFormat = AudioSystem.getAudioFileFormat(inFile);
			inFile.reset(); // rewind
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
		setupDefaults();
		AudioInputStream inFileAIS = null;
		try 
		{
			InputStream inFile = l.getInputStream();
			// query file type
			AudioFileFormat inFileFormat = AudioSystem.getAudioFileFormat(inFile);
			inFile.reset(); // rewind
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
	{	return AudioSystem.getAudioInputStream(AudioFormat.Encoding.PCM_SIGNED, is); }
	
	static public void main(String[] args)
		throws Exception
	{
		Media m = new Media();
		MediaLocation l = new MediaLocation();
		l.setLocationURLString("file:/home/music/songs/Belle%20_%20Sebastian/The%20Boy%20with%20the%20Arab%20Strap/Ease%20Your%20Feet%20In%20The%20Sea.flac");
		m.setLocalLocation(l);
		File out = new File("test.mp3");
		System.out.println("Writing out to " + out.getAbsolutePath());
		writeToFile(m, l, out);
		
	}
}