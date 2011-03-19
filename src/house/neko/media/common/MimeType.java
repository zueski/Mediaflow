package house.neko.media.common;


public class MimeType implements java.io.Serializable
{
	private static final long serialVersionUID = 22L;

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
}