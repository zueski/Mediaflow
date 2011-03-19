package house.neko.media.master.webdav;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NameClassPair;


/**
 *
 * @author andy
 */
public class DAVFileContext
{
	// get the object at path
	/**
	 *
	 * @param path
	 * @return
	 * @throws javax.naming.NamingException
	 */
	public Object lookup(String path)
		throws NamingException
	{
		return null;
	}

	// create a folder at the path
	/**
	 *
	 * @param path
	 * @throws javax.naming.NamingException
	 */
	public void createSubcontext(String path)
		throws NamingException
	{
	
	}
	
	// list the objects located at the path
	/**
	 *
	 * @param path
	 * @return
	 * @throws javax.naming.NamingException
	 */
	@SuppressWarnings("unchecked")
	public NamingEnumeration<javax.naming.NameClassPair> list(String path)
		throws NamingException
	{
		return new DAVFolderEnumeration<javax.naming.NameClassPair>();
	}
	
	// put a resource
	/**
	 *
	 * @param path
	 * @param o
	 * @throws javax.naming.NamingException
	 */
	public void bind(String path, Object o)
		throws NamingException
	{
	
	}

	// remove a resource
	/**
	 *
	 * @param path
	 * @throws javax.naming.NamingException
	 */
	public void unbind(String path)
		throws NamingException
	{
	
	}
	
	//
	/**
	 *
	 * @param path
	 * @return
	 */
	public CacheEntry lookupCache(String path)
	{
		return null;
	}
	
	/**
	 *
	 */
	public class CacheEntry
	{
		/**
		 *
		 */
		public long timestamp = -1;
		/**
		 *
		 */
		public String name = null;
		/**
		 *
		 */
		public ResourceAttributes attributes = null;
		/**
		 *
		 */
		public DAVFileContext resource = null;
		/**
		 *
		 */
		public DAVFileContext context = null;
		/**
		 *
		 */
		public boolean exists = true;
		/**
		 *
		 */
		public long accessCount = 0;
		/**
		 *
		 */
		public int size = 1;
		
		/**
		 *
		 */
		public void recycle()
		{
			timestamp = -1;
			name = null;
			attributes = null;
			resource = null;
			context = null;
			exists = true;
			accessCount = 0;
			size = 1;
		}
	}
	
	/**
	 *
	 * @param <E>
	 */
	public class DAVFolderEnumeration<E>
		implements NamingEnumeration
	{
		/**
		 *
		 */
		protected DAVFolderEnumeration()
		{
		}
		/**
		 *
		 * @return
		 */
		public boolean hasMoreElements()
		{	return false; }
		/**
		 *
		 * @return
		 * @throws javax.naming.NamingException
		 */
		public boolean hasMore()
			throws NamingException
		{	return hasMoreElements(); }
		/**
		 *
		 * @return
		 */
		public E nextElement()
		{	return null; }
		/**
		 *
		 * @return
		 * @throws javax.naming.NamingException
		 */
		public E next()
			throws NamingException
		{	return nextElement(); }
		/**
		 *
		 * @throws javax.naming.NamingException
		 */
		public void close()
           throws NamingException
		{
		}
	}
}