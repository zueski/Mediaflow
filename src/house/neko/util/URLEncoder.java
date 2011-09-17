package house.neko.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.BitSet;

/**
 *
 * @author andy
 */
public class URLEncoder
{
	/**
	 *
	 */
	protected static final char[] hexadecimal = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

	//Array containing the safe characters set.
	/**
	 *
	 */
	protected BitSet safeCharacters = new BitSet(256);

	/**
	 *
	 */
	public URLEncoder()
	{
		for (char i = 'a'; i <= 'z'; i++) 
		{	addSafeCharacter(i); }
		for (char i = 'A'; i <= 'Z'; i++) 
		{	addSafeCharacter(i); }
		for (char i = '0'; i <= '9'; i++) 
		{	addSafeCharacter(i); }
	}

	/**
	 *
	 * @param c
	 */
	public void addSafeCharacter( char c )
	{	safeCharacters.set( c ); }

	/**
	 *
	 * @param path
	 * @return
	 */
	public String encode(String path)
	{
		int maxBytesPerChar = 10;
		int caseDiff = ('a' - 'A');
		StringBuffer rewrittenPath = new StringBuffer(path.length());
		ByteArrayOutputStream buf = new ByteArrayOutputStream(maxBytesPerChar);
		OutputStreamWriter writer = null;
		try 
		{
			writer = new OutputStreamWriter(buf, "UTF8");
		} catch (Exception e) {
			e.printStackTrace();
			writer = new OutputStreamWriter(buf);
		}

		for (int i = 0; i < path.length(); i++) 
		{
			int c = (int) path.charAt(i);
			if (safeCharacters.get(c)) 
			{
				rewrittenPath.append((char)c);
			} else {
				// convert to external encoding before hex conversion
				try 
				{
					writer.write((char)c);
					writer.flush();
				} catch(IOException e) {
					buf.reset();
					continue;
				}
				byte[] ba = buf.toByteArray();
				for (int j = 0; j < ba.length; j++) 
				{
					// Converting each byte in the buffer
					byte toEncode = ba[j];
					rewrittenPath.append('%');
					int low = toEncode & 0x0f;
					int high = (toEncode & 0xf0) >> 4;
					rewrittenPath.append(hexadecimal[high]);
					rewrittenPath.append(hexadecimal[low]);
				}
				buf.reset();
			}
		}
		return rewrittenPath.toString();
	}
}
