package house.neko.media.slave;

import house.neko.media.common.*;

import java.util.Observer;
import java.util.Observable;
import java.awt.Container;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JButton;
import javax.swing.JLabel;

import org.apache.commons.logging.Log;

/**
 *
 * @author andy
 */
public final class MediaPlayerControls extends Container implements Runnable, ActionListener, Observer
{
	private static final long serialVersionUID = 3L;

	private Log log = null;
	private MediaPlayer player;
	private EventMapper eventMapper;

	private JButton play_pause_button;
	private JButton stop_button;
	private JButton skip_backwards_button;
	private JButton skip_forwards_button;

	private JLabel display;

	/**
	 *
	 * @param player
	 * @param eventMapper
	 */
	public MediaPlayerControls(MediaPlayer player, EventMapper eventMapper)
	{
		this.log = ConfigurationManager.getLog(getClass());
		
		this.player = player;
		player.addObserver(this);
		this.eventMapper =  eventMapper;

		setLayout(new GridLayout(2, 4, 2, 2));

		play_pause_button = new JButton("Play");
		play_pause_button.setActionCommand("play");
		play_pause_button.addActionListener(this);

		stop_button = new JButton("Stop");
		stop_button.setActionCommand("stop");
		stop_button.addActionListener(this);

		skip_backwards_button = new JButton("|<");
		skip_backwards_button.setActionCommand("skipbackward");
		skip_backwards_button.addActionListener(this);

		skip_forwards_button = new JButton(">|");
		skip_forwards_button.setActionCommand("skipforward");
		skip_forwards_button.addActionListener(this);

		display = new JLabel();

		add(skip_backwards_button);
		add(stop_button);
		add(play_pause_button);
		add(skip_forwards_button);
		add(display);

		Thread t = new Thread(this, "MediaPlayerControler");
		t.start();
	}

	/**
	 *
	 * @param e
	 */
	public void actionPerformed(ActionEvent e)
	{
		String cmd = e.getActionCommand();
		log.trace("Action " + cmd);
		if("play".equals(cmd))
		{
			player.start();
			play_pause_button.setActionCommand("pause");
			play_pause_button.setText("Pause");
		} else if("pause".equals(cmd)) {
			player.pause();
			play_pause_button.setActionCommand("play");
			play_pause_button.setText("Play");
		} else if("stop".equals(cmd)) {
			player.stop();
			play_pause_button.setActionCommand("play");
			play_pause_button.setText("Play");
		} else if("skipbackward".equals(cmd)) {
			player.skipBackward();
		} else  if("skipforward".equals(cmd)) {
			player.skipForward();
		}
	}

	/**
	 *
	 */
	public void run()
	{
		//long currtime = 0L;
		
		while(true)
		{
			try
			{
				int currtime = (int) player.getCurrentPlayTime() / 1000000;
				int secs = currtime % 60;
				int mins = currtime / 60 % 60;
				display.setText((mins < 10 ? "0" + mins : mins)+":" + (secs < 10 ? "0" + secs : secs));
				Thread.sleep(200L);
			} catch(Throwable t) {
				t.printStackTrace();
			}
		}
	}
	
	public void update(Observable o, Object arg)
	{
		//if(log.isTraceEnabled())  log.trace("Observed " + o + " " + arg);
		System.out.println("Observed " + o + " " + arg);
		if(o == player)
		{
			if(arg == player.STOP)
			{
				actionPerformed(new ActionEvent(player, 0, "stop")); 
			} else if(arg == player.PLAY) {
				actionPerformed(new ActionEvent(player, 0, "play")); 
			}
		}
	}
}