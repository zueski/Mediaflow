package house.neko.media.common;


public class MimeType implements java.io.Serializable
{
	private static final long serialVersionUID = 23L;
	
	public final static String TYPE_FLAC = "FLAC";
	private static MimeType _TYPE_FLAC = null;
	public final static String TYPE_MP3 = "MPEG-1 Layer 3";
	private static MimeType _TYPE_MP3 = null;

	private String fileExtension;
	private String mimeType;
	private String mimeSubType;
	private Long fileCreatorID;  // Mac file attribute
	private Long fileTypeID; // Mac file attribute
	private Integer localID;
	
	public MimeType()
	{
		fileExtension = null;
		mimeType = null;
		mimeSubType = null;
		fileCreatorID = null;
		fileTypeID = null;
		localID = null;
	}
	
	public void setFileExtension(String fileExtension)
	{	this.fileExtension = fileExtension; }
	public String getFileExtension()
	{	return fileExtension; }

	public void setMimeType(String mimeType)
	{	this.mimeType = mimeType; }
	public String getMimeType()
	{	return mimeType; }

	public void setMimeSubType(String mimeSubType)
	{	this.mimeSubType = mimeSubType; }
	public String getMimeSubType()
	{	return mimeSubType; }

	public void setFileCreatorID(Long fileCreatorID)
	{	this.fileCreatorID = fileCreatorID; }
	public Long getFileCreatorID()
	{	return fileCreatorID; }

	public void setFileTypeID(Long fileTypeID)
	{	this.fileTypeID = fileTypeID; }
	public Long getFileTypeID()
	{	return fileTypeID; }

	public void setLocalID(Integer localID)
	{	this.localID = localID; }
	public Integer getLocalID()
	{	return localID; }
	
	public String toString()
	{	return mimeType + ":" + mimeSubType + ":" + fileExtension + "(" + localID + ")"; }
	
	public static MimeType getInstanceFromType(String type)
	{
		if(type == null)
		{	return null; }
		if(type.startsWith(TYPE_FLAC))
		{
			if(_TYPE_FLAC == null)
			{
				synchronized(TYPE_FLAC)
				{
					_TYPE_FLAC = new MimeType();
					_TYPE_FLAC.setFileExtension("flac");
					_TYPE_FLAC.setMimeType("audio");
					_TYPE_FLAC.setMimeSubType("flac");
				}
			}
			return _TYPE_FLAC;
		} else if(type.startsWith(TYPE_MP3)) {
			if(_TYPE_MP3 == null)
			{
				synchronized(TYPE_MP3)
				{
					_TYPE_MP3 = new MimeType();
					_TYPE_MP3.setFileExtension("mp3");
					_TYPE_MP3.setMimeType("audio");
					_TYPE_MP3.setMimeSubType("mpeg");
				}
			}
			return _TYPE_MP3;
		}
		return null;
	}
}