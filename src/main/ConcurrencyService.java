package main;

import java.io.PrintStream;
import java.sql.SQLException;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import db.DbManager;
import db.PathwayImpl;
import gui.FeverMainFrame;


public class ConcurrencyService {
	 private final ThreadPoolExecutor pool;
	 private final int poolSize;
	 private final int qCapacity = 1 << 7;
	 private final long timeout = 3;
 	
	 // Use this to limit the number of paths to be analyzed
	 private final int debugLimiter = FeverMainFrame.DODEBUG ? 1000 : Integer.MAX_VALUE;
	 
	 private final PathwayImpl tainedPath = 
			 new PathwayImpl(Long.MIN_VALUE, "LAST_PATH_IN_QUEUE", "N/A", "N/A"); 
	
	private BlockingQueue<PathwayImpl> bq;
	private DbManager dbMan;
	private Dataset ds;
	private volatile boolean started;
	
	public final ConcurrentHashMap<PathwayImpl,AnalysisResult> 
		analyzedPaths = new ConcurrentHashMap<PathwayImpl,AnalysisResult>(1<<15);;

	public static Logger logger = Logger.getLogger(PathwayImpl.class.getName());

	public ConcurrencyService(Dataset data, DbManager db){
		
		this.ds = data;
		this.bq = new LinkedBlockingQueue<PathwayImpl>(qCapacity);
		this.dbMan = db;
		
		// Initial size to hold approx 32K pathways 
		// (this number should not never be exceeded in real life) 
		this.started = false;
		
		int nThreads = AnalysisParams.getInstance().getNbrOfThreads();
		poolSize = (nThreads == AnalysisParams.NTHR_NOT_SET) ? 
					Runtime.getRuntime().availableProcessors() : nThreads;
		pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(poolSize, new FeverThreadFactory(-1));
		
	}
	 
	    public void serve() throws InterruptedException {
	    	try {
	    		
	    		logger.info("ConcurrencyService is running with a max of " +
	    					poolSize + " consumer threads in the pool." + 
	    					System.lineSeparator());
	    		
	    		ds.finalize();
	    		started = true;
	    		
	    		
	    		/*	create a producer thread that retrieves the relevant information 
	    		 * 	regarding pathways, proteins and their relationships with one another	*/
	    		Thread producerThread = new Thread(new QueryingAction(), "fever-query-thread");
	    		logger.info("Starting producer/query thread"  + 
    						System.lineSeparator());
	    		
	    		producerThread.setPriority(Thread.NORM_PRIORITY - 2);
	    		producerThread.setUncaughtExceptionHandler(
	    				new Thread.UncaughtExceptionHandler(){
	    					@Override
	    					public void uncaughtException(Thread t, Throwable e){
	    						logger.severe(e.getStackTrace().toString());
	    					}
	    				});
	    		producerThread.start();
    		
    			logger.info("Producer thread initiated with " 
    					+ producerThread.getPriority() 
    					+ " priority."  + System.lineSeparator());
	    			
	    			/*	create a logger thread to monitor the status of the different components
		    		 * 	of the queues, the pool, and the collection of processed objects */
	    		Thread loggerThread = new Thread(new PeriodicLogAction(null), "fever-logger-thread");
	    		loggerThread.setUncaughtExceptionHandler(
	    				new Thread.UncaughtExceptionHandler(){
	    					@Override
	    					public void uncaughtException(Thread t, Throwable e){
	    						logger.severe(e.getStackTrace().toString());
	    					}
	    				});

	    		loggerThread.start();
	    		
	    		
	    		while((producerThread.getState() != Thread.State.TERMINATED) || !bq.isEmpty()){
	    			
	    			PathwayImpl p = bq.poll(timeout, TimeUnit.MINUTES);
	    			
	    			if(p != null){
	    				if (p.equals(tainedPath))
		    				break;
		    				    			
		    			pool.submit(new AnalysisAction(p, ds, dbMan, analyzedPaths));
	    			}else 
	    				logger.warning("Timed out while waiting for a pathway...");
	    				
	    		}
	    		
		      } catch (Exception ex) {
		    	  logger.severe("Unexpected error in core analysis, terminating execution!" 
		    			  + System.lineSeparator() 
		    			  + DebugToolbox.getStackTraceAsString(ex)
		    			  + System.lineSeparator());
		    	  System.err.println("Unexpected error in core analysis, terminating execution!");
					
		      }finally{
				  logger.info("All paths are queried and queued..." 
						  		+ System.lineSeparator()
						  		+ "Initiating a timely shutdown of the pool.."
						  		+ System.lineSeparator());
				  
				  DebugToolbox.dumpMemoryInfo(true);				
				  pool.shutdown();
				  
				  long 	totalTasks = pool.getTaskCount(), 
						compTasks = pool.getCompletedTaskCount(),
				  		tasksRemaining = totalTasks - compTasks,
				  		timeout = 10 * tasksRemaining / poolSize;
				  
				  logger.info("Shutdown initiated, "
						  + "thread pool will terminate once all paths are processed, "
						  + "or will timeout in : " + (int)(timeout/60) + " minutes..." 
						  + System.lineSeparator() 
						  + compTasks + " of " +  totalTasks + " pathways have been analyzed so far. "
						  + System.lineSeparator());
				
				  pool.awaitTermination(timeout, TimeUnit.SECONDS);
				  
				  logger.info(
						  "A total of " + DebugToolbox.getNbrProcTimes() 
						  + " tasks analyzed. Mean process time is: " 
						  + DebugToolbox.getMeanProcTimeAsString() 
						  +  " milliseconds." + System.lineSeparator());
				  
				  //DebugToolbox.dumpScores();
				  logger.info("#DEBUG: Conserv will terminate..."+ System.lineSeparator());
		      }

	 	}
	    
	    public int getPoolSize(){	return this.poolSize;	}
	    
	    public Map<PathwayImpl,AnalysisResult> getAnalyzedPaths(){	return this.analyzedPaths;	}
	    
	    public boolean hasStarted() {return this.started;}
	    
	    public boolean isDone(){
	    	if(this.started)
	    		return pool.isTerminated();
	    	else
	    		return false;
	    }
	    
	    
	    /** A custom implementation of ThreadFactory which names pool threads accordingly and
	     * 	allows setting priority of the created threads.	Delivers non daemon threads.
	     *  
	     * 	@param threadPriority - desired level or priority for created threads, if this value 
	     * 	is smaller than Thread.MIN_PRIORITY or greater than Thread.MAX_PRIORITY 
	     * 	a default value of Thread.NORM_PRIORITY is used. 
	     * */
	    protected class FeverThreadFactory implements ThreadFactory {
	        private final ThreadGroup group;
	        private final AtomicInteger threadNumber = new AtomicInteger(1);
	        private final String namePrefix;
	        private final int DEF_PRIORITY = Thread.NORM_PRIORITY;
	        private final int priority;

	        FeverThreadFactory(int threadPriority) {
	        	SecurityManager s = System.getSecurityManager();
	            group = (s != null)? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
	            namePrefix = "fever-pool-thread-";
	            
	            boolean validPriority = threadPriority > Thread.MIN_PRIORITY && 
	            						threadPriority < Thread.MAX_PRIORITY;
	            						
	            this.priority = validPriority ? threadPriority : DEF_PRIORITY;
	               
	    	}
	        
	        public Thread  newThread(Runnable r) {
	        	Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(),0);
	        	
	            if (t.isDaemon())
	                t.setDaemon(false);
	            
	            t.setPriority(this.priority);     
	            return t;
	        }
	    }
	    
	    /** A Runnable object that retrieves information and
	     *  dumps the queried pathways into the blocking queue */
	    protected class QueryingAction implements Runnable {
	    	
	    	public void run() {
				logger.config("QueryAction started on thread: " + 
						Thread.currentThread().getName() + 
						System.lineSeparator());
				
				// TODO: Sanity check, do it this in a better way
				if(!ds.isFinalized())
					throw new RuntimeException("Dataset not finalized");
				if(ds.isMock())
					throw new RuntimeException("Dataset is mock");
				
				try {
					int i = 0;
					outer: for(String prot : ds.getProteinsIds()){
						inner: for(PathwayImpl path : dbMan.getAllPathsWithProtein(prot)){
							if(i++ > debugLimiter)
								break outer;
							else
								bq.put(path);
						}
					}
					
					logger.info("Total number of queried paths: " + i);
					
				} catch (SQLException e) {
					logger.severe("Unexpected database error: "
							+ System.lineSeparator() 
							+ DebugToolbox.getStackTraceAsString(e)
							+ System.lineSeparator());
					
					System.err.println("Database error occured, please check the logs for more information!");
					
				} catch (InterruptedException e) {
					logger.severe("Unexpected interrupt on ConServ: "
							+ System.lineSeparator() 
							+ DebugToolbox.getStackTraceAsString(e)
							+ System.lineSeparator());
					
					System.err.println("Analysis interrupted due to an unexpected error, "
											+ "please check the logs for more information");
				}
				bq.offer(tainedPath);	
			}
	  	}
	  
	    protected class PeriodicLogAction implements Runnable {
	    	private final PrintStream ps;
	    	private final long period;
	    	private final static long DEF_PERIOD = 30000;
	        private final String nL = System.getProperty("line.separator");
	    	private volatile boolean loop;
	    	private int counter = 0;
	    	private ConcurrencyService cs; 
	    	private int inQueryQueue, inPoolQueue, 
							completedTasks, inProccessedSet,duplicates;
			
			boolean sanityCheck;
			StringBuffer sb;
			
	    	
		  	PeriodicLogAction(PrintStream ps, long timePeriod) {	
		  		this.ps = ps;
		  		this.period = timePeriod;
		  		this.loop = true;
		  		this.cs = ConcurrencyService.this;
		  	}
		  	
		  	PeriodicLogAction(PrintStream ps) {	
		  		this(ps,DEF_PERIOD);
		  	}
		  	
		  	public PeriodicLogAction() {
				this(null,DEF_PERIOD);
			}
	    
			public void run() {
		  		logger.config("PeriodicLogAction started on thread: " + 
						Thread.currentThread().getName() + 
						System.lineSeparator());  			
	  				
		  		while(loop){
		  			// log # of pathways created, analyzed and are in queue
		  			outputLogInfo();
		  			
		  			// wait designated time period
		  			try {
						Thread.sleep(period);
					} catch (InterruptedException e) {
						System.err.println("Concurrency Service is interrupted! This should not have happened...");
						e.printStackTrace();
					}
					if(cs.isDone()){
						this.loop = false;
						outputLogInfo();
					}
		  		}
		  	}

		  	private void outputLogInfo(){
		  		
		  		synchronized (pool) {
		  			Queue 	queryQueue = cs.bq,
							poolQueue = cs.pool.getQueue();
		  			Map<PathwayImpl,AnalysisResult> processedSet = cs.analyzedPaths;
					
			  		inQueryQueue = queryQueue.size();
		  			inPoolQueue = poolQueue.size();
		  			completedTasks = (int) pool.getCompletedTaskCount();
		  			inProccessedSet = processedSet.size();
		  			duplicates = AnalysisAction.duplicates.get();
		  			sanityCheck = (completedTasks == inProccessedSet + duplicates);
				}
		  		
	  			sb = new StringBuffer();
	  			sb.append("Checkpoint ").append(++counter).append(": ")
	  				.append("QQ: ").append(inQueryQueue).append("\t")
	  				.append("PQ: ").append(inPoolQueue).append("\t")
	  				.append("CT: ").append(completedTasks).append("\t")
	  				.append("AP: ").append(inProccessedSet).append("\t")
	  				.append("DP: ").append(duplicates).append("\t")
	  				.append("Sanity: ").append(sanityCheck);
	  			
	  			if(ps == null)
	  				logger.info(sb.toString()  + nL);
	  			else
	  				ps.println(sb.toString());
		  	}
	    }
}
