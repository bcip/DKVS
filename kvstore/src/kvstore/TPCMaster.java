package kvstore;

import static kvstore.KVConstants.*;

import java.net.Socket;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TPCMaster {

    private int numSlaves;
    private KVCache masterCache;
    
	private TreeMap<Long, TPCSlaveInfo> slaves
			= new TreeMap<Long, TPCSlaveInfo>(new SlaveIDComparator());
    private ReentrantLock slavesLock = new ReentrantLock();

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
        	slaves.put(slave.getSlaveID(), slave);
        	if(slaves.size() == numSlaves)
        		slaves.notifyAll();
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
    	//TODO
        slavesLock.lock();
        try{
        	if(slaves.ceilingEntry(new Long(hashCode)) != null)
        		return slaves.ceilingEntry(new Long(hashCode)).getValue();
        	return slaves.firstEntry().getValue();
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
        return findFirstReplica(firstReplica.getSlaveID()+1);
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
    	masterCache.getLock(key).writeLock().lock();
    	try{
    		TPCSlaveInfo[] slaves = findCorrespondingSlaves(key);
    		Socket[] slaveSockets = new Socket[slaves.length];
    		boolean[] votes = new boolean[slaves.length];
    		
    		for(int i = 0; i < slaves.length; i++){
    			slaveSockets[i] = slaves[i].connectHost(TIMEOUT);
    		}
    		
    		for(int i = 0; i < slaves.length; i++){
    			try{
    				KVMessage response = new KVMessage(slaveSockets[i], TIMEOUT);
    				if(response.getMsgType().equals(READY))
    					votes[i] = true;
    			}
    			catch(KVException e){
    				//ignore
    			}
    		}
    		
    		boolean allGood = true;
    		for(int i = 0; i < slaves.length; i++)
    			if(votes[i] == false)
    				allGood = false;
    		
    		Arrays.fill(votes, false);
    		
    		KVMessage decision = null;
    		if(allGood){
    			decision = new KVMessage(COMMIT);
    		}
    		else{
    			decision = new KVMessage(ABORT);
    		}
    		
    		allGood = false;
    		while(!allGood){
    			allGood = true;
    			for(int i = 0; i < slaves.length; i++){
    				if(!votes[i]){
    					try{
	    					slaveSockets[i] = slaves[i].connectHost(TIMEOUT);
	    					decision.sendMessage(slaveSockets[i]);
	    					
    					}
    					catch (KVException e){
    						slaveSockets[i] = null;
    						allGood = false;
    					}
    				}
    			}
    			for(int i = 0; i < slaves.length; i++){
    				if(!votes[i] && slaveSockets[i] != null){
    					votes[i] = true;
    					try{
    						KVMessage response = new KVMessage(slaveSockets[i], TIMEOUT);
    						assert(response.getMsgType().equals(ACK));
    					}
	    				catch (KVException e){
							votes[i] = false;
							allGood = false;
	    				}
    				}
    			}
    		}
    		
    		if(decision.getMsgType().equals(ABORT))
    			//TODO which error to throw?
    			throw new KVException(ERROR_COULD_NOT_RECEIVE_DATA);

    		if(isPutReq){
    			masterCache.put(key, value);
    		}
    		else{
    			masterCache.del(value);
    		}
    	}
    	finally{
    		masterCache.getLock(key).writeLock().unlock();
    	}
    }
    
    
    /**
     * Try to get the value from the slave
     * @param slave
     * @param msg
     * @return value if success,
     * 		null otherwise
     */
    public String getFromSlave(TPCSlaveInfo slave, KVMessage msg) {
    	try{
    		Socket slaveSocket = slave.connectHost(TIMEOUT);
    		msg.sendMessage(slaveSocket);
    		KVMessage response = new KVMessage(slaveSocket);
    		if(response.getMsgType().equals(SUCCESS))
    			throw new KVException(response);
    		return response.getValue();
    	}
    	catch (KVException e){
    		return null;
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
    	
    	masterCache.getLock(key).readLock().lock();
    	try{
        	value = masterCache.get(key);
        	if(value != null)
        		return value;
        	
        	TPCSlaveInfo[] slaves = findCorrespondingSlaves(key);
        	
        	for(TPCSlaveInfo slave : slaves){
        		value = getFromSlave(slave, msg);
        		if(value != null)
        			break;
        	}
        	
        	if(value == null)
        		throw new KVException(ERROR_NO_SUCH_KEY);
        	
        	masterCache.put(key, value);
    	}
    	finally{
    		masterCache.getLock(key).readLock().unlock();
    	}
    	
    	return value;
    }
    
    public class SlaveIDComparator
    	implements Comparator<Long>{
    	public int compare(Long a, Long b){
    		if(isLessThanUnsigned(a.longValue(), b.longValue()))
    			return -1;
    		if(isLessThanEqualUnsigned(a.longValue(), b.longValue()))
    			return 0;
    		return 1;
    	}
    }

}
