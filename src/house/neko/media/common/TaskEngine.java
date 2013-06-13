package house.neko.media.common;


import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class TaskEngine
{
	public final String DEFAULT = "DEFAULT";
	
	private ConcurrentHashMap<String, ExecutorService> queues;
	
	public TaskEngine()
	{
		queues = new ConcurrentHashMap<String, ExecutorService>();
		queues.put(DEFAULT, Executors.newSingleThreadExecutor());
	}
	
	public void addQueue(String queueName, int threads)
		throws IllegalArgumentException
	{
		queues.put(queueName, Executors.newFixedThreadPool(threads));
	}
	
	public void submit(Runnable r)
	{	submit(DEFAULT, r); }
	
	public void submit(String q, Runnable r)
	{
		ExecutorService s = queues.get(q);
		if(s == null)
		{	s = queues.get(DEFAULT); }
		s.submit(r);
	}
}