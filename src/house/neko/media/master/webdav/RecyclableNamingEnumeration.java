package house.neko.media.master.webdav;

import java.util.Enumeration;
import java.util.Vector;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

/**
 * Naming enumeration implementation.
 *
 * @author Remy Maucherat
 * @version $Revision$ $Date$
 */

public class RecyclableNamingEnumeration 
    implements NamingEnumeration {


    // ----------------------------------------------------------- Constructors


	/**
	 * 
	 * @param entries
	 */
	public RecyclableNamingEnumeration(Vector entries)
	{
        this.entries = entries;
        recycle();
    }


    // -------------------------------------------------------------- Variables


    /**
     * Entries.
     */
    protected Vector entries;


    /**
     * Underlying enumeration.
     */
    protected Enumeration enumeration;


    // --------------------------------------------------------- Public Methods


    /**
     * Retrieves the next element in the enumeration.
	 * @return
	 * @throws NamingException
	 */
    public Object next()
        throws NamingException {
        return nextElement();
    }


    /**
     * Determines whether there are any more elements in the enumeration.
	 * @return
	 * @throws NamingException
	 */
    public boolean hasMore()
        throws NamingException {
        return enumeration.hasMoreElements();
    }


    /**
     * Closes this enumeration.
	 * @throws NamingException
	 */
    public void close()
        throws NamingException {
    }


	/**
	 * 
	 * @return
	 */
	public boolean hasMoreElements()
	{
        return enumeration.hasMoreElements();
    }


	/**
	 * 
	 * @return
	 */
	public Object nextElement()
	{
        return enumeration.nextElement();
    }


    // -------------------------------------------------------- Package Methods


    /**
     * Recycle.
     */
    void recycle() {
    	enumeration = entries.elements();
    }


}

