package house.neko.util;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 *
 * @author andy
 */
public abstract class Queue
{
	private Log log;
	private ConcurrentLinkedQueue<QueueTask> queue;
	
	public Queue(String name)
	{
		this.log = LogFactory.getFactory().getLog(getClass());
	}
	
	public void addTask(Callback function, Callback notify)
	{
		QueueTask task = new QueueTask(function, notify);
		queue.add(task);
	}
	
	private class QueueTask 
	{
		private Callback function;
		private Callback notify;
		public QueueTask(Callback function, Callback notify)
		{
			this.function = function;
			this.notify = notify;
		}
		
		public void doWork()
		{
			if(function != null)
			{	function.invoke(); }
			if(notify != null)
			{	notify.invoke(); }
		}
	}
}
