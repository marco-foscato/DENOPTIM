package denoptim.task;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.swing.JProgressBar;

import denoptim.fragspace.FragmentSpace;
import denoptim.logging.DENOPTIMLogger;

/**
 * Manager for tasks submitted by the GUI. The main purpose is to launch and
 * manage calls placed upon requests coming from the GUI and aimed at
 * running DENOPTIM's main classes in DenoptimGA and FragSpaceExplorer.
 * 
 * @author Marco Foscato
 */

public class StaticTaskManager
{
	/**
	 * The only, static instance of this class
	 */
	private static final StaticTaskManager instance = new StaticTaskManager();
	
	/**
	 * The executor of threads
	 */
	private static ThreadPoolExecutor tpe; 

    /**
     * Maps the relation between a task that is submitted and its future handle
     */
	private static Map<Task,Future<?>> subToFutureMap = 
			new HashMap<Task,Future<?>>();
	
	/**
	 * Number of threads. Only one for now and as long as the 
	 * {@link FragmentSpace} and {@link DENOPTIMLogger} will be static objects.
	 */
	private static final int maxthreads = 1;
	
	/**
	 * Queue size
	 */
	private static final int queueSize = 10;
	
	/**
	 * Value indicated in the progress bar of the GUI.
	 */
	public static int valProgressBar = 1; //On startup the bar is filled.up.
	
	/**
     * Queue max indicator for progress bar in GUI.
     */
    public static int maxProgressBar = 1;

    /**
     * Reference to progress bar in GUI. Null when we start.
     */
    public static JProgressBar queueStatusBar = null;
    
    /**
     * Property change fired to indicate the need to update progress bar
     */
    public static final String PROGRESSBARUPDATE = "UPDATE-PROGRESS-BAR";

	/**
	 * Synchronisation lock for queue progress bar.
	 */
	private static final Object LOCK = new Object();
    
//------------------------------------------------------------------------------
    
    private StaticTaskManager()
    {
    	tpe = new ThreadPoolExecutor(maxthreads, maxthreads, Long.MAX_VALUE, 
    			TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(queueSize));
    	
    	// What to do in case of filled-up resources and queue rejecting tasks
        tpe.setRejectedExecutionHandler(new RejectedExecutionHandler()
        {
            @Override
            public void rejectedExecution(Runnable r,
            		ThreadPoolExecutor executor)
            {
                try
                {
                	// re-submit to the queue any rejected job
                    executor.getQueue().put(r);
                }
                catch (InterruptedException ex)
                {
                    ex.printStackTrace();
                }
            }
        });
        
        // What to do in case of shutdown signal
        Runtime.getRuntime().addShutdownHook(new ShutDownHook());

        tpe.prestartAllCoreThreads();
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Shutdown hook that stops all child tasks upon shutdown of JavaVM.
     */

    private class ShutDownHook extends Thread
    {
        @Override
        public void run()
        {
            tpe.shutdown();
            try
            {
                if (!tpe.awaitTermination(1, TimeUnit.SECONDS))
                {
                    tpe.shutdownNow(); // Cancel running asks
                }
            }
            catch (InterruptedException ie)
            {
            	stopAll();
                subToFutureMap.clear();
                tpe.purge();
                tpe.getQueue().clear();
                tpe.shutdownNow();
                
                // and stop possibly alive thread
                //TODO is this needed? It should not
                Thread.currentThread().interrupt();
            }
        }
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Gets the singleton instance of this class.
     * @return
     */
    public static StaticTaskManager getInstance()
    {
    	return instance;
    }

//------------------------------------------------------------------------------
    
    public static void addTodoTask()
    {
    	addTodoTasks(1);
    }
    
//------------------------------------------------------------------------------
    
    public static void addTodoTasks(int addedTasksCount)
    {
    	synchronized (LOCK) {
	    	int max = maxProgressBar;
	    	int val = valProgressBar;
	    	if (max==1 && val==1)
	    	{
	        	// NB: the progress bar is initialized to max=1/val=1
	    		// Thus it looks "filled-up", i.e., no pending/running task
	    	    maxProgressBar = addedTasksCount;
	    	    valProgressBar = 0;
	    	} else {
	    	    maxProgressBar = max+addedTasksCount;
		    	valProgressBar = val;
	    	}
    	}
    	if (queueStatusBar!=null)
    	{
    	    queueStatusBar.setMaximum(maxProgressBar);
    	    queueStatusBar.setValue(valProgressBar);
    	    queueStatusBar.repaint();
    	}
    }

//------------------------------------------------------------------------------
    
    public static void subtractDoneTask()
    {
    	subtractDoneTasks(1);
    }
//------------------------------------------------------------------------------
    
    public static void subtractDoneTasks(int doneTasksCount)
    {
    	synchronized (LOCK) {
	    	int max = maxProgressBar;
	    	int val = valProgressBar;
	    	valProgressBar = val+doneTasksCount;
	    	if (valProgressBar == maxProgressBar)
	    	{
	    	    maxProgressBar = 1;
	    		valProgressBar = 1;
	    	}
    	}
        if (queueStatusBar!=null)
        {
            queueStatusBar.setMaximum(maxProgressBar);
            queueStatusBar.setValue(valProgressBar);
            queueStatusBar.repaint();
        }
    }
    
//------------------------------------------------------------------------------
    
    /**
     * @return a string with HTML code, but no html head/tail tags. This string 
     * is meant to be inserted in a larger string with head/tail tags.
     */
    public static String getQueueSnapshot()
    {
    	String s = "Approximate queue status:<ul>"
    			+ "<li>Running tasks = " + tpe.getActiveCount() + "/"
    			+ tpe.getPoolSize() + "</li>"
    	    	+ "<li>Queue Size    = " + tpe.getQueue().size()+ "</li></ul>";
    	return s;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Submits a task and waits for completion.
     * @param task the task to submit.
     * @throws ExecutionException 
     * @throws InterruptedException 
     */
    public static void submitAndWait(Task task) throws InterruptedException, 
    ExecutionException
    {
        tpe.submit(task).get();
    }
	
//------------------------------------------------------------------------------
    
    public static void submit(Task task)
    {
    	task.setNotify(true);
    	addTodoTask();
    	Future<?> future = tpe.submit(task);
    	subToFutureMap.put(task,future);
    	//TODO: check how we can remove terminated tasks from the map
    }
    
//------------------------------------------------------------------------------
    
    public static void stop(Task task)
    {
		task.stopTask();
		if (subToFutureMap.containsKey(task))
		{
    		subToFutureMap.get(task).cancel(true);
    		subToFutureMap.remove(task);
		}
    }
    
//------------------------------------------------------------------------------
    
	public static void stopAll() 
	{
		for (Task task : subToFutureMap.keySet())
    	{
    		Future<?> expectation = subToFutureMap.get(task);
            expectation.cancel(true);
            task.stopTask();
        }
	}
	
//------------------------------------------------------------------------------

	public static boolean hasActive()
	{
		return tpe.getActiveCount() > 0;
	}

//------------------------------------------------------------------------------

	/**
	 * Adds a reference to the given progress bar. This to enable the task
	 * manager to report queue status.
	 * @param bar
	 */
    public static void setLinkToProgressBar(JProgressBar bar)
    {
        queueStatusBar = bar;
    }

//------------------------------------------------------------------------------
    
}
