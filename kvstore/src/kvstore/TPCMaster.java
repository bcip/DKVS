package kvstore;

import static kvstore.KVConstants.*;

import java.io.IOException;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TPCMaster {

    private int numSlaves;
    private KVCache masterCache;
    
	private TreeMap<Long, TPCSlaveInfo> slaves
			= new TreeMap<Long, TPCSlaveInfo>(new SlaveIDComparator());
    private ReentrantLock slavesLock = new ReentrantLock();
    private Condition enoughSlaves = slavesLock.newCondition();

    public static final int TIMEOUT = 3000;

    /**
     * Creates TPCMaster, expecting numSlaves slave servers to eventually register
     *
     * @param numSlaves number of slave servers expected to register
     * @param cache KVCache to cache results on master
     */
    public TPCMaster(int numSlaves, KVCache cache) {
        this.numSlaves = numSlaves;
        this.masterCache = cache;
        // implement me
    }

    /**
     * Registers a slave. Drop registration request if numSlaves already
     * registered.Note that a slave re-registers under the same slaveID when
     * it comes back online.
     *
     * @param slave the slaveInfo to be registered
     */
    public void registerSlave(TPCSlaveInfo slave) {
        slavesLock.lock();
        try{
        	synchronized(slaves){
        		if(slaves.containsKey(slave.getSlaveID())) {
        			slaves.get(slave.getSlaveID()).updateInfo(slave.getHostname(), slave.getPort());
        		} else {
        			slaves.put(slave.getSlaveID(), slave);
        		}
        	}
        	if(slaves.size() == numSlaves)
        		enoughSlaves.signalAll();
        }
        finally{
        	slavesLock.unlock();
        }
    }

    /**
     * Converts Strings to 64-bit longs. Borrowed from http://goo.gl/le1o0W,
     * adapted from String.hashCode().
     *
     * @param string String to hash to 64-bit
     * @return long hashcode
     */
    public static long hashTo64bit(String string) {
        long h = 1125899906842597L;
        int len = string.length();

        for (int i = 0; i < len; i++) {
            h = (31 * h) + string.charAt(i);
        }
        return h;
    }

    /**
     * Compares two longs as if they were unsigned (Java doesn't have unsigned
     * data types except for char). Borrowed from http://goo.gl/QyuI0V
     *
     * @param n1 First long
     * @param n2 Second long
     * @return is unsigned n1 less than unsigned n2
     */
    public static boolean isLessThanUnsigned(long n1, long n2) {
        return (n1 < n2) ^ ((n1 < 0) != (n2 < 0));
    }

    /**
     * Compares two longs as if they were unsigned, uses isLessThanUnsigned
     *
     * @param n1 First long
     * @param n2 Second long
     * @return is unsigned n1 less than or equal to unsigned n2
     */
    public static boolean isLessThanEqualUnsigned(long n1, long n2) {
        return isLessThanUnsigned(n1, n2) || (n1 == n2);
    }
    
    public TPCSlaveInfo findFirstReplica(long hashCode) {
        slavesLock.lock();
        if(slaves.size() < numSlaves){
        	try{
        		enoughSlaves.await();
        	}
        	catch (InterruptedException e){
        		//ignore
        	}
        }
        try{
        	if(slaves.ceilingKey(new Long(hashCode)) == null)
        		return slaves.firstEntry().getValue();
        	return slaves.ceilingEntry(new Long(hashCode)).getValue();
        	
        }
        finally{
        	slavesLock.unlock();
        }
    }

    /**
     * Find primary replica for a given key.
     *
     * @param key String to map to a slave server replica
     * @return SlaveInfo of first replica
     */
    public TPCSlaveInfo findFirstReplica(String key) {
        return findFirstReplica(hashTo64bit(key));
    }

    /**
     * Find the successor of firstReplica.
     *
     * @param firstReplica SlaveInfo of primary replica
     * @return SlaveInfo of successor replica
     */
    public TPCSlaveInfo findSuccessor(TPCSlaveInfo firstReplica) {
    	if(slaves.higherEntry(firstReplica.getSlaveID()) == null) 
            return slaves.firstEntry().getValue();
        return slaves.higherEntry(firstReplica.getSlaveID()).getValue();
    }
    
    public TPCSlaveInfo[] findCorrespondingSlaves(String key){
    	TPCSlaveInfo[] slaves = new TPCSlaveInfo[2];
    	slaves[0] = findFirstReplica(key);
    	slaves[1] = findSuccessor(slaves[0]);
    	return slaves;
    }

    /**
     * Perform 2PC operations from the master node perspective. This method
     * contains the bulk of the two-phase commit logic. It performs phase 1
     * and phase 2 with appropriate timeouts and retries.
     *
     * See the spec for details on the expected behavior.
     *
     * @param msg KVMessage corresponding to the transaction for this TPC request
     * @param isPutReq boolean to distinguish put and del requests
     * @throws KVException if the operation cannot be carried out for any reason
     */
    public synchronized void handleTPCRequest(KVMessage msg, boolean isPutReq)
            throws KVException {

    	String key = msg.getKey();
    	String value = null;
    	if(isPutReq)
    		value = msg.getValue();
    	masterCache.getLock(key).lock();
    	try{
    		TPCSlaveInfo[] slaves = findCorrespondingSlaves(key);
    		
    		KVMessage decision = null;
    		///*
    		CollectVote cv1 = new CollectVote(slaves[0], msg);
    		CollectVote cv2 = new CollectVote(slaves[1], msg);
        	
        	Thread t1 = new Thread(cv1);
        	Thread t2 = new Thread(cv2);
        	
        	t1.start();
        	t2.start();
        	
        	try {
        		t1.join();
            	t2.join();
        	} catch (InterruptedException e){
        		System.out.println(e.getMessage());
        	}
        	//*/
        	boolean isCommit = // collectVotes(slaves, msg); 
        			cv1.vote != null && cv1.vote.getMsgType().equals(READY) &&
        			cv2.vote != null && cv2.vote.getMsgType().equals(READY);
    		if(isCommit){
    			decision = new KVMessage(COMMIT);
    		}
    		else{
    			decision = new KVMessage(ABORT);
    		}
    		///*
    		AnnounceDecision ad1 = new AnnounceDecision(slaves[0], decision);
    		AnnounceDecision ad2 = new AnnounceDecision(slaves[1], decision);
        	
        	Thread t3 = new Thread(ad1);
        	Thread t4 = new Thread(ad2);
        	
        	t3.start();
        	t4.start();
        	
        	try {
        		t3.join();
            	t4.join();
        	} catch (InterruptedException e){
        		System.out.println(e.getMessage());
        	}
    		//*/
    		//announceDecision(slaves, decision);
    		if(decision.getMsgType().equals(ABORT))
    			//TODO which error to throw?
    			throw new KVException(ERROR_COULD_NOT_RECEIVE_DATA);

    		if(isPutReq){
    			masterCache.put(key, value);
    		}
    		else{
    			masterCache.del(key);
    		}
    	}
    	finally{
    		masterCache.getLock(key).unlock();
    	}
    }
    
    private class CollectVote implements Runnable {
    	KVMessage vote = null;
    	TPCSlaveInfo info;
    	KVMessage request = null;
    	
    	public CollectVote(TPCSlaveInfo _info, KVMessage _request){
    		this.info = _info;
    		this.request = _request;
    	}
    	
    	@Override
    	public void run(){
    		Socket slaveSocket = null;
    		try{
    			 slaveSocket = info.connectHost(TIMEOUT);
    			request.sendMessage(slaveSocket);
    			vote = new KVMessage(slaveSocket, TIMEOUT);
    		}catch (KVException e) {
    			vote = new KVMessage(ABORT);
    		}finally {
    			if(slaveSocket != null){
					try {
						info.closeHost(slaveSocket);
					} catch (KVException e) {
					}
    			}
    		}
    	}
    }
    
    private class AnnounceDecision implements Runnable {
    	TPCSlaveInfo info;
    	KVMessage decision = null;
    	
    	public AnnounceDecision(TPCSlaveInfo _info, KVMessage _decision){
    		this.info = _info;
    		this.decision = _decision;
    	}
    	
    	@Override
    	public void run(){
    		Socket slaveSocket = null;
    		boolean hasAck = false;
    		while(true){
    			if(hasAck)
    				break;
    			try{
    				slaveSocket = info.connectHost(TIMEOUT);
    				decision.sendMessage(slaveSocket);
    			}catch(KVException e){
    			}
    			if(slaveSocket != null){
    				try {
    					KVMessage response = new KVMessage(slaveSocket, TIMEOUT);
    					hasAck = true;
    				}catch (KVException e) {
    					//ignore
    				}finally{
    					try{
    						info.closeHost(slaveSocket);
    					}catch (KVException e){
    						//ignore
    					}
    				}
    			}
    		}
    	}
    }
    
    private class GetFromSlave implements Runnable {
    	KVMessage vote = null;
    	TPCSlaveInfo info;
    	KVMessage request = null;
    	String value = null;
    	KVMessage excepMsg = null;
    	
    	public GetFromSlave(TPCSlaveInfo _info, KVMessage _request){
    		this.info = _info;
    		this.request = _request;
    	}
    	
    	@Override
    	public void run(){
    		Socket slaveSocket = null;
    		try{
    			slaveSocket = info.connectHost(TIMEOUT);
    			request.sendMessage(slaveSocket);
    			KVMessage response = new KVMessage(slaveSocket, TIMEOUT);
    			value = response.getValue();
    			if(value == null)
    				excepMsg = response;
    		}catch (KVException e){
    			excepMsg = e.getKVMessage();
    		}
    		finally{
    			if(slaveSocket != null){
    				try {
    					info.closeHost(slaveSocket);
    				} catch (KVException e) {
   					}
   				}
    		}
    	}
    }
    
    /**
	 * Perform GET operation in the following manner:
	 * - Try to GET from cache, return immediately if found
	 * - Try to GET from first/primary replica
	 * - If primary succeeded, return value
	 * - If primary failed, try to GET from the other replica
	 * - If secondary succeeded, return value
	 * - If secondary failed, return KVExceptions from both replicas
	 *
	 * @param msg KVMessage containing key to get
	 * @return value corresponding to the Key
	 * @throws KVException with ERROR_NO_SUCH_KEY if unable to get
	 *         the value from either slave for any reason
	 */
	public String handleGet(KVMessage msg) throws KVException {
	    // implement me
		String key = msg.getKey();
		String value = null;
		masterCache.getLock(key).lock();
		try{
	    	value = masterCache.get(key);
	    	
	    	if(value != null){
	    		return value;
	    	}
	    	TPCSlaveInfo[] slaves = findCorrespondingSlaves(key);
	    	
	    	GetFromSlave gfs1 = new GetFromSlave(slaves[0], msg);
	    	GetFromSlave gfs2 = new GetFromSlave(slaves[1], msg);
	    	
	    	Thread t1 = new Thread(gfs1);
	    	Thread t2 = new Thread(gfs2);
	    	
	    	t1.start();
	    	t2.start();
	    	
	    	try {
        		t1.join();
        		if(gfs1.value != null)
    	    		value = gfs1.value;
            	t2.join();
            	if(gfs2.value != null)
    	    		value = gfs2.value;
        	} catch (InterruptedException e){
        		System.out.println(e.getMessage());
        	}
	    	
	    	//value = getFromSlaves(slaves, msg);
	    	
	    	if(value == null)
	    		throw new KVException(ERROR_NO_SUCH_KEY);
	    	
	    	masterCache.put(key, value);
		}
		finally{
			masterCache.getLock(key).unlock();
		}
		
		return value;
	}

	/**
	 * Send request to slaves, and check votes
	 * @param slaves
	 * @param request
	 * @return true, if all vote to commit
	 */
	private boolean collectVotes(TPCSlaveInfo[] slaves, KVMessage request){
		Socket[] slaveSockets = new Socket[slaves.length];
		try{
			for(int i = 0; i < slaves.length; i++){
				slaveSockets[i] = slaves[i].connectHost(TIMEOUT);
				request.sendMessage(slaveSockets[i]);
			}
			for(int i = 0; i < slaves.length; i++){
				KVMessage vote = new KVMessage(slaveSockets[i], TIMEOUT);
				if(!vote.getMsgType().equals(READY))
					return false;
			}
			return true;
		}
		catch (KVException e){
			return false;
		}
		finally{
			for(int i = 0; i < slaves.length; i++){
				if(slaveSockets[i] != null){
					try {
						slaves[i].closeHost(slaveSockets[i]);
					} catch (KVException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}

	/**
	 * announce to slaves, repeat until every slave ack
	 * @param slaves
	 * @param decision
	 */
	private void announceDecision(TPCSlaveInfo[] slaves, KVMessage decision){
		Socket[] slaveSockets = new Socket[slaves.length];
		boolean[] hasAck = new boolean[slaves.length];
		while(true){
			int i;
			for(i = 0; i < slaves.length; i++){
				if(!hasAck[i])
					break;
			}
			if(i == slaves.length)
				break;
			
			for(i = 0; i < slaves.length; i++){
				if(!hasAck[i]){
					try {
						slaveSockets[i] = slaves[i].connectHost(TIMEOUT);
						decision.sendMessage(slaveSockets[i]);
					} catch (KVException e) {
						//ignore
					}
				}
			}
			
			for(i = 0; i < slaves.length; i++){
				if(slaveSockets[i] != null){
					try {
						KVMessage response = new KVMessage(slaveSockets[i], TIMEOUT);
						hasAck[i] = true;
					}
					catch (KVException e) {
						//ignore
					}
					finally{
						try{
							slaves[i].closeHost(slaveSockets[i]);
						}
						catch (KVException e){
							//ignore
						}
					}
				}
			}
		}
	}

	/**
	 * Try to get the value from the slave
	 * @param slave
	 * @param msg
	 * @return value if success,
	 * 		null otherwise
	 */
	private String getFromSlaves(TPCSlaveInfo[] slaves, KVMessage msg) {
		Socket[] slaveSockets = new Socket[slaves.length];
		try{
			for(int i = 0; i < slaves.length; i++){
				slaveSockets[i] = slaves[i].connectHost(TIMEOUT);
				msg.sendMessage(slaveSockets[i]);
			}
			for(int i = 0; i < slaves.length; i++){
				KVMessage response = new KVMessage(slaveSockets[i], TIMEOUT);
				String val = response.getValue();
				if(val != null)
					return val;
			}
		}catch (KVException e){
			return null;
		}
		finally{
			for(int i = 0; i < slaves.length; i++){
				if(slaveSockets[i] != null){
					try {
						slaves[i].closeHost(slaveSockets[i]);
					} catch (KVException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		return null;
	}

	public class SlaveIDComparator implements Comparator<Long>{
    	public int compare(Long a, Long b){
    		if(isLessThanUnsigned(a.longValue(), b.longValue()))
    			return -1;
    		else if(isLessThanEqualUnsigned(a.longValue(), b.longValue()))
    			return 0;
    		return 1;
    	}
    }

}
