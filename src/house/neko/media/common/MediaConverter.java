package house.neko.media.common;

import java.io.File;

import org.apache.commons.logging.Log;

public class MediaConverter
{
	private MediaLibrary library;
	private Log log;
	private MimeType flacMimeType;
	
	public MediaConverter(MediaLibrary library)
	{
		this.library = library; 
		this.log = ConfigurationManager.getLog(getClass());
		flacMimeType = new MimeType();
		flacMimeType.setFileExtension("flac");
		flacMimeType.setMimeType("audio/flac");
	}
	
	public void convertToFLAC(Media[] mediaList)
	{
		for(Media m : mediaList)
		{
			convertToFLAC(m, m.getLocation());
		}
	}
	
	public void convertToFLAC(Media m, MediaLocation l)
	{
		try
		{
			File alacFile = l.getFile();
			if(alacFile == null)
			{	log.error("URL to FLAC not currently supported!!!!"); return; }
			File flacFile = new File(alacFile.getParentFile(), alacFile.getName().replaceAll("^(.*)\\.m4a", "$1.flac"));
			if(!flacFile.getName().endsWith(".flac"))
			{	log.error("Default format is not FLAC, currently unsupported configuration: " + flacFile.getName()); return; }
			File tempFile = File.createTempFile("alacToFlac", ".wav");
			String[] decodeArgs = {"alac", "-f", tempFile.getAbsolutePath(), alacFile.getAbsolutePath()};
			String[] encodeArgs = {
									"flac", 
									"--totally-silent",
									"-8",
									"--tag=ALBUM=" + m.getAlbum(),
									"--tag=TITLE=" + m.getName(),
									"--tag=ARTIST=" + m.getArtist(),
									"--tag=TRACKNUMBER=" + m.getTrackNumber(),
									"-o", flacFile.getAbsolutePath(),
									tempFile.getAbsolutePath() 
								};
			Process execDecode = Runtime.getRuntime().exec(decodeArgs);
			if(execDecode.waitFor() != 0)
			{	log.error("Unable to decode file: " + alacFile.getAbsolutePath()); return; }
			Process execEncode = Runtime.getRuntime().exec(encodeArgs);
			int encodeReturn = execEncode.waitFor();
			if(encodeReturn == 0)
			{
				MediaLocation flacLocation = new MediaLocation();
				flacLocation.setMimeType(flacMimeType);
				flacLocation.setSize(flacFile.length());
				flacLocation.setLocationURLString(flacFile.toURI().toURL().toString());
				m.setLocalLocation(null);
				m.setLocalLocation(flacLocation);
				library.saveAllDirty();
				alacFile.delete();
			} else {
				log.error("Unable to encode file: " + alacFile.getAbsolutePath() + ", got " + encodeReturn);
				flacFile.delete();
			}
			tempFile.delete();
		} catch(Exception e) {
			log.error("Unable to convert " + l + " to FLAC", e);
		}
	}
}