package house.neko.media.common;

import java.io.InputStream;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.File;
import java.net.URL;
import java.net.URI;
import java.net.MalformedURLException;
import java.net.URISyntaxException;



public class MediaLocation implements java.io.Serializable
{
	private static final long serialVersionUID = 22L;

	private String locationURLString;
	private Long bytecount;
	private MimeType mimeType;
	private boolean locationValid;
	
	public MediaLocation()
	{
		locationURLString = null;
		mimeType = null;
		bytecount = null;
		locationValid = false;
	}
	
	public void setLocationURLString(String locationURLString)
	{	this.locationURLString = locationURLString; }
	public String getLocationURLString()
	{	return locationURLString; }

	public void setMimeType(MimeType mimeType)
	{	this.mimeType = mimeType; }
	public MimeType getMimeType()
	{	return mimeType; }

	public void setSize(Long bytecount)
	{	this.bytecount = bytecount; }
	public Long getSize()
	{	return this.bytecount; }
	
	public boolean isLocationValid()
	{	return this.locationValid; }
	public void setLocationValid(boolean locationValid)
	{	this.locationValid = locationValid; }
	
	
	/**
	 *
	 * @return
	 * @throws java.io.IOException
	 */
	public InputStream getInputStream()
		throws IOException
	{
		try
		{
			URI uri = getLocationURI();
			BufferedInputStream is = null;
			String scheme = uri.getScheme();
			if("file".equals(scheme))
			{
				File f = new File(uri);
				if(!f.canRead())
				{	throw new IOException("Unable to read file '" + f.getAbsolutePath() + "'"); }
				is = new BufferedInputStream(new FileInputStream(f));
			} else if("http".equals(scheme) || "https".equals(scheme)) {
				is = new BufferedInputStream(uri.toURL().openConnection().getInputStream());
			} else {
				throw new IOException("Unknown scheme for URI '" + uri+ "'");
			}
			return is;
		} catch(Exception e) {
			IOException ioe = new IOException("Exception getting input stream for '" + locationURLString + "'");
			ioe.initCause(e);
			throw ioe;
		}
	}
	
	public File getFile()
		throws IOException
	{
		try
		{
			URI uri = getLocationURI();
			if("file".equals(uri.getScheme()))
			{
				File f = new File(uri);
				return f;
			}
			return null;
		} catch(Exception e) {
			IOException ioe = new IOException("Exception getting file for '" + locationURLString + "'");
			ioe.initCause(e);
			throw ioe;
		}
	}
	
	private java.net.URI getLocationURI()
	{
		URI uri = null;
		try
		{	uri = new java.net.URI(locationURLString); }
		catch(Exception e) { e.printStackTrace(); }
		return uri;
	}
	
	private java.net.URL getLocationURL()
	{
		URL url = null;
		try
		{	url = new java.net.URL(locationURLString); }
		catch(Exception e) { e.printStackTrace(); }
		return url;
	}
	
	public boolean equals(Object obj)
	{
		if(obj == null || !(obj instanceof MediaLocation))
		{	return false; }
		MediaLocation other = (MediaLocation) obj;
		if(locationURLString != null)
		{
			if(!locationURLString.equals(other.getLocationURLString()))
			{	return false; }
			if(mimeType != null)
			{
				return mimeType.equals(other.getMimeType());
			} else {
				return other.getMimeType() == null;
			}
		} else {
			if(other.getLocationURLString() != null)
			{	return false; }
			if(mimeType != null)
			{
				return mimeType.equals(other.getMimeType());
			} else {
				return other.getMimeType() == null;
			}
		}
	}
	
	public String toString()
	{	return "MediaLocation@" + locationURLString; }
}