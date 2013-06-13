package house.neko.media.common;

/**
 *
 * @author andy
 */
public class MediaListEntry implements java.io.Serializable
{
	public int position;
	public Media media;
	
	public MediaListEntry(Media m, int pos)
	{
		position = pos;
		media = m;
	}
}