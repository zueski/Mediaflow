package house.neko.media.common;

import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;

import java.io.File;
import java.io.IOException;

import java.awt.image.BufferedImage;

import java.util.Iterator;
import java.util.Vector;

import org.apache.commons.configuration.HierarchicalConfiguration;

import org.apache.commons.logging.Log;

/**
 *
 * @author andy
 */
public class MediaMetaDataReader 
{
	private Log log;
	
	public MediaMetaDataReader()
	{
		this.log = ConfigurationManager.getLog(MediaLibrary.class);
	}
	
	public Media getMediaFromFile(File f)
		throws IOException
	{
		try
		{
			if(log.isDebugEnabled()) log.debug("Reading tags from " + f);
			AudioFile af = AudioFileIO.read(f);
			AudioHeader header = af.getAudioHeader();
			String type = header.getFormat();
			if(log.isDebugEnabled()) log.debug(f + " is type " + type);
			Tag tag = af.getTag();
			Media m = new Media();
			m.setName(getTag(tag, FieldKey.TITLE));
			m.setAuthor(getTag(tag, FieldKey.COMPOSER));
			m.setArtist(getTag(tag, FieldKey.ARTIST));
			m.setAlbum(getTag(tag, FieldKey.ALBUM));
			try { m.setTrackNumber(Integer.parseInt(getTag(tag, FieldKey.TRACK))); } catch(Exception e) { }
			MediaLocation l = new MediaLocation();
			m.setLocalLocation(l);
			l.setLocationURLString(f.toURI().toURL().toString());
			l.setSize(f.length());
			l.setMimeType(MimeType.getInstanceFromType(type));
			m.setID(Media.generateID());
			if(log.isDebugEnabled()) log.debug("Created media" + m);
			return m;
		} catch(CannotReadException cre) {
			IOException ioe = new IOException("Unable to read file " + f);
			ioe.initCause(cre);
			throw ioe;
		} catch(TagException te) {
			IOException ioe = new IOException("Unable to parse tags for file " + f);
			ioe.initCause(te);
			throw ioe;
		} catch(ReadOnlyFileException rfe) {
			IOException ioe = new IOException("Should not be writing tags here?! " + f);
			ioe.initCause(rfe);
			throw ioe;
		} catch(InvalidAudioFrameException iafe) {
			IOException ioe = new IOException("Bad audio frame " + f);
			ioe.initCause(iafe);
			throw ioe;
		}
	}
	
	private String getTag(Tag tag, FieldKey fieldKey)
	{
		String s = tag.getFirst(fieldKey);
		return (s != null && s.length() > 0) ? s : null;
	}
}