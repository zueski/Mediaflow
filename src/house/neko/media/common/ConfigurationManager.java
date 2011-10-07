package house.neko.media.common;

import java.io.FileInputStream;
import java.io.File;
import java.util.Properties;
import java.net.URL;
import java.net.URLClassLoader;

import org.apache.commons.configuration.CombinedConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.apache.commons.configuration.XMLConfiguration;

import org.apache.commons.configuration.tree.ConfigurationNode;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 *
 * @author andy
 */
public class ConfigurationManager
{
	//private static HierarchicalConfiguration config = null;
    private static LogFactory logfactory;
    private static Log log;
	private static XMLConfiguration config;
	private static java.io.File xmlfile;

	/**
	 *
	 * @param args
	 */
	synchronized public static void init(String args[])
	{
        logfactory = LogFactory.getFactory();
        log = logfactory.getLog(ConfigurationManager.class);
		if(config == null)
		{
			if(log.isTraceEnabled()) { log.trace("Initializing configuration"); }
			//config = new CombinedConfiguration(); 
			try
			{
				xmlfile = new java.io.File("MediaFlowSettings.xml");
				if(log.isTraceEnabled()) { log.trace("Loading settings from " + xmlfile.getAbsolutePath()); }
				config = new XMLConfiguration(xmlfile);
				if(config.isEmpty())
				{	config.setRootElementName("MediaFlowSettings"); }
				//((CombinedConfiguration) config).addConfiguration(xmlconfig);
				if(log.isTraceEnabled()) { log.trace("Version " + config.getString("Version")); }
			} catch(ConfigurationException ce) {
				log.error("Error loading config file", ce); 
			}
			//((CombinedConfiguration) config).addConfiguration(new SystemConfiguration());
			if(log.isTraceEnabled()) { log.trace("Initialized configuration ->" + config.getRootNode()); }
		} else {
			log.warn("Configuration already initialized");
		}
	}
	
	synchronized public static void save()
	{
		try
		{
			if(log.isTraceEnabled()) { log.trace("Saving settings to " + xmlfile.getAbsolutePath()); }
			config.save();
			if(log.isTraceEnabled()) { log.trace("Finished saving settings to " + xmlfile.getAbsolutePath()); }
		} catch(ConfigurationException ce) {
			log.error("Error saving configuration", ce); 
		}
	}
	
	public static void dumpConfig(HierarchicalConfiguration c)
	{
		if(!log.isTraceEnabled())
		{	return; }
		if(c == null)
		{
			log.trace("config is null");
		} else {
			log.trace("dumping config");
		}
		java.util.Iterator<String> i = c.getKeys();
		while(i.hasNext())
		{
			String k = i.next();
			log.trace(k + " -> '" + c.getString(k) + "'");
		}
	}
	
	/**
	 *
	 * @param path
	 * @return
	 */
	public static HierarchicalConfiguration getConfiguration(String path)
	{
		if(!config.containsKey(path))
		{
			if(log.isWarnEnabled())
			{	log.warn("Property path '" + path + "' not found!"); }
			config.setProperty(path, ""); 
		}
		return config.configurationAt(path, true); 
	}
    
	/**
	 *
	 * @param c
	 * @return
	 */
	public static Log getLog(Class c)
    {   return logfactory.getLog(c); }
}