package house.neko.util;

import java.net.URL;
import java.net.URLClassLoader;

/**
 *
 * @author andy
 */
public class ChildFirstClassLoader extends URLClassLoader
{

	/**
	 *
	 * @param urls
	 */
	public ChildFirstClassLoader(URL[] urls)
    {   super(urls); }

	/**
	 *
	 * @param urls
	 * @param parent
	 */
	public ChildFirstClassLoader(URL[] urls, ClassLoader parent)
    {   super(urls, parent); }

	/**
	 *
	 * @param url
	 */
	@Override
    public void addURL(URL url)
    {   super.addURL(url); }

	/**
	 *
	 * @param name
	 * @return
	 * @throws java.lang.ClassNotFoundException
	 */
	@Override
    public Class loadClass(String name) throws ClassNotFoundException 
    {   return loadClass(name, false); }

    /**
    * We override the parent-first behavior established by 
    * java.lang.Classloader.
    * <p>
    * The implementation is surprisingly straightforward.
	 * @param name
	 * @param resolve 
	 * @return 
	 * @throws ClassNotFoundException
	 */
    @Override
    protected Class loadClass(String name, boolean resolve)
        throws ClassNotFoundException 
    {
        // First, check if the class has already been loaded
        Class c = findLoadedClass(name);
        // if not loaded, search the local (child) resources
        if(c == null) 
        {
            try 
            {
                c = findClass(name);
            } catch(ClassNotFoundException cnfe) { }
        }
        // if we could not find it, delegate to parent
        // Note that we don't attempt to catch any ClassNotFoundException
        if(c == null) 
        {
            if(getParent() != null) 
            {
                c = getParent().loadClass(name);
            } else {
                c = getSystemClassLoader().loadClass(name);
            }
        }
        if(resolve) 
        {   resolveClass(c); }
        return c;
    }

    /**
    * Override the parent-first resource loading model established by
    * java.lang.Classloader with child-first behavior.
	 * @param name
	 * @return
	 */
    @Override
    public URL getResource(String name) 
    {
        URL url = findResource(name);
        // if local search failed, delegate to parent
        if(url == null) 
        {   url = getParent().getResource(name); }
        return url;
    }
}
