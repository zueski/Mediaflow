package house.neko.media.common;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 *
 * @author andy
 */
public class EventMapper
{
	private MediaPlayer player;

	private static Log log;

	/**
	 *
	 */
	public EventMapper()
	{
		player = null;
        log = LogFactory.getFactory().getLog(EventMapper.class);
	}
	
	/**
	 *
	 * @param player
	 */
	public EventMapper(MediaPlayer player)
	{	this.player = player; }
	
	/**
	 *
	 * @param player
	 */
	public void setPlayer(MediaPlayer player)
	{	this.player = player; }
	
	/**
	 *
	 * @return
	 */
	public ActionListener getListenerForPlay()
	{
		if(player == null)
		{
			log.error("Unable to get play ActionListener for player, player not set!");
			return null;
		}
		final MediaPlayer myPlayer = player;
		return new ActionListener() 
		{
			public void actionPerformed(ActionEvent e) 
        	{	myPlayer.play(); }
        };
	}
	
	/**
	 *
	 * @return
	 */
	public ActionListener getListenerForStop()
	{
		if(player == null)
		{
			log.error("Unable to get stop ActionListener for player, player not set!");
			return null;
		}
		final MediaPlayer myPlayer = player;
		return new ActionListener() 
		{
			public void actionPerformed(ActionEvent e) 
        	{	myPlayer.stop(); }
        };
	}
}