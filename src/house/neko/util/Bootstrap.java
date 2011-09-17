/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package house.neko.util;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;
import java.util.Vector;
/**
 *
 * @author andy
 */
public class Bootstrap 
{

	/**
	 *
	 * @param args
	 * @throws java.lang.Exception
	 */
	static public void main(String[] args)
			throws Exception
	{
		loadPropertiesToSystem("/commons-logging.properties");
		Properties p = loadProperties("/Bootstrap.properties");
		
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		System.setProperty("apple.awt.window.position.forceSafeCreation", "true");
		System.setProperty("apple.awt.window.position.forceSafeCreation", "true");
		System.setProperty("apple.awt.window.position.forceSafeProgrammaticPositioning", "true");
		System.setProperty("com.apple.mrj.application.apple.menu.about.name", p.getProperty("house.neko.util.Bootstrap.applicationName"));
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		
		String jars = p.getProperty("house.neko.util.Bootstrap.jars");
		if(jars != null)
		{
			File jarsdir = new File(jars);
			if(jarsdir.exists())
			{
				Vector<URL> v = new Vector<URL>();
				//System.out.println(Bootstrap.class.getClassLoader().getSystemResource("house/neko/util/Bootstrap.class").toString().replaceAll("^.*?(file:.*?\\.jar).*$", "$1"));
				URL me = new URL(Bootstrap.class.getClassLoader().getSystemResource("house/neko/util/Bootstrap.class").toString().replaceAll("^.*?(file:.*?\\.jar).*$", "$1"));
				v.add(me);
				loadFilesToURLVector(jarsdir, v);
				URL[] urllist = v.toArray(new URL[v.size()]);
				ChildFirstClassLoader cl = new ChildFirstClassLoader(urllist);
				Bootstrap.class.getClassLoader().clearAssertionStatus();
				Class c = cl.loadClass(p.getProperty("house.neko.util.Bootstrap.class"));
				c.newInstance();
			}
		}
	}
	
	static private void loadFilesToURLVector(File dir, Vector<URL> v)
			throws Exception
	{
		if(dir != null && dir.isDirectory() && v != null)
		{
			File[] ls = dir.listFiles();
			for(int i = 0; i < ls.length; i++)
			{
				if(ls[i].isFile() && ls[i].getName().endsWith(".jar"))
				{
					System.out.println("Found lib " + ls[i].getPath());
					v.add(ls[i].toURI().toURL());
				} else if(ls[i].isDirectory()) {
					loadFilesToURLVector(ls[i], v);
				}
				
			}
		}
	}
	
	static private void loadPropertiesToSystem(String file_name)
			throws Exception
	{
		InputStream data = null;
		Properties props = System.getProperties();

		data = Bootstrap.class.getResourceAsStream(file_name);
		if(data == null)
		{  
			System.err.println("Unable to load properties '" + file_name + "'");
			System.exit(1);
		}
		System.out.println("Found data " + data);
		
		props.load(data);
		data.close();
		
		try
		{
			java.util.logging.Logger rootLogger = java.util.logging.LogManager.getLogManager().getLogger("");
			//rootLogger.setLevel(java.util.logging.Level.parse(props.getProperty("house.neko.util.Bootstrap.level")));
			rootLogger.setLevel(java.util.logging.Level.SEVERE);
		} catch(Exception e) {
			System.err.println("Unable to set logging level: " + e);
		}
		data = null;
	}
	
	static private Properties loadProperties(String file_name)
			throws Exception
	{
		InputStream data = null;
		Properties props = null;

		data = Bootstrap.class.getResourceAsStream(file_name);
		if(data == null)
		{  
			System.err.println("Unable to load properties '" + file_name + "'");
			System.exit(1);
		}
		System.out.println("Found data " + data);
		
		props = new Properties();

		props.load(data);
		data.close();

		data = null;
		
		return props;
	}
}
