package house.neko.media.common;

/**
 *
 * @author andy
 */
public class Message implements java.io.Serializable
{
	private static final long serialVersionUID = 8L;

	/**
	 *
	 */
	public static final int MEDIA = 0;

	/**
	 *
	 */
	public int type;
	/**
	 *
	 */
	public byte[] payload;
}