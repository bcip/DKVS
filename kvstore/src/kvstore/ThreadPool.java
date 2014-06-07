package kvstore;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public class ThreadPool {

    /* Array of threads in the threadpool */
    private Thread threads[];
    private BlockingQueue<Runnable> jobQueue;


    /**
     * Constructs a Threadpool with a certain number of threads.
     *
     * @param size number of threads in the thread pool
     */
    public ThreadPool(int size) {
        threads = new Thread[size];

        jobQueue = new LinkedBlockingQueue<Runnable>();
        
        for(int i = 0; i < size; i++){
        	threads[i] = new WorkerThread(this);
        	threads[i].start();
        }
        
    }

    /**
     * Add a job to the queue of jobs that have to be executed. As soon as a
     * thread is available, the thread will retrieve a job from this queue if
     * if one exists and start processing it.
     *
     * @param r job that has to be executed
     * @throws InterruptedException if thread is interrupted while in blocked
     *         state. Your implementation may or may not actually throw this.
     */
    public void addJob(Runnable r) throws InterruptedException {
        jobQueue.put(r);
    }

    /**
     * Block until a job is present in the queue and retrieve the job
     * @return A runnable task that has to be executed
     * @throws InterruptedException if thread is interrupted while in blocked
     *         state. Your implementation may or may not actually throw this.
     */
    private Runnable getJob() throws InterruptedException {
        return jobQueue.take();
    }

    /**
     * A thread in the thread pool.
     */
    private class WorkerThread extends Thread {

        private ThreadPool threadPool;

        /**
         * Constructs a thread for this particular ThreadPool.
         *
         * @param pool the ThreadPool containing this thread
         */
        public WorkerThread(ThreadPool pool) {
            threadPool = pool;
        }

        /**
         * Scan for and execute tasks.
         */
        @Override
        public void run() {
            while(true){
            	try{
            		Runnable r = threadPool.getJob();
            		if(r != null)
            			r.run();
            	}
            	catch(Exception e){
            		//ignore
            	}
            }
        }
    }
}
