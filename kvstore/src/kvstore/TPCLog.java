package kvstore;

import static kvstore.KVConstants.*;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class TPCLog {

    private String logPath;
    private KVServer kvServer;
    private ArrayList<KVMessage> entries;
    private KVMessage operation = null; // keep track of interrupted 2PC operation
    /**
     * Constructs a TPCLog to log KVMessages from the master.
     *
     * @param logPath path to location of log file for this server
     * @param kvServer reference to the KVServer of this slave
     */
    public TPCLog(String logPath, KVServer kvServer) throws KVException {
        this.logPath = logPath;
        this.kvServer = kvServer;
        this.entries = new ArrayList<KVMessage>();
        rebuildServer();
    }

    /**
     * Add an entry to the log and flush the entire log to disk.
     * You do not have to efficiently append entries onto the log stored on disk.
     *
     * @param entry KVMessage to write to the log
     */
    public void appendAndFlush(KVMessage entry) {
        entries.add(entry);
        flushToDisk();
    }

    /**
     * Get last entry in the log.
     *
     * @return last entry put into the log
     */
    public KVMessage getLastEntry() {
        if (entries.size() > 0) {
            return entries.get(entries.size() - 1);
        }
        return null;
    }

    /**
     * Load log from persistent storage at logPath.
     */
    @SuppressWarnings("unchecked")
    public void loadFromDisk() {
        ObjectInputStream inputStream = null;

        try {
            inputStream = new ObjectInputStream(new FileInputStream(logPath));
            entries = (ArrayList<KVMessage>) inputStream.readObject();
        } catch (Exception e) {
        } finally {
            // if log did not exist, creating empty entries list
            if (entries == null) {
                entries = new ArrayList<KVMessage>();
            }

            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Writes the log to persistent storage at logPath.
     */
    public void flushToDisk() {
        ObjectOutputStream outputStream = null;

        try {
            outputStream = new ObjectOutputStream(new FileOutputStream(logPath));
            outputStream.writeObject(entries);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.flush();
                    outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Load log and rebuild KVServer by iterating over log entries. You do not
     * need to restore the previous cache state (i.e. ignore GETS).
     *
     * @throws KVException if an error occurs in KVServer (though we expect none)
     */
    public void rebuildServer() throws KVException {
        // implement me
    	loadFromDisk();
    	for(KVMessage entry : entries){
    		if (entry.getMsgType().equals(PUT_REQ)
    				|| entry.getMsgType().equals(DEL_REQ)){
    			operation = entry;
    		} else if (entry.getMsgType().equals(COMMIT)){
    			if(operation != null){
	    			if(operation.getMsgType().equals(PUT_REQ)){
	    				kvServer.put(operation.getKey(), operation.getValue());
	    			} else if(operation.getMsgType().equals(DEL_REQ)) {
	    				kvServer.del(operation.getKey());
	    			} else {
	    				assert(false);
	    			}
    			}
    			operation = new KVMessage(KVConstants.ACK);
    		} else if (entry.getMsgType().equals(KVConstants.ABORT)){
    			operation = new KVMessage(KVConstants.ACK);
    		} else if (entry.getMsgType().equals(KVConstants.ACK)
    					&& operation.getMsgType().equals(KVConstants.ACK)) {
    			operation = null;
    		} {
    			assert(false);
    		}
    	}
    }
    
    public KVMessage getOperation(){
    	KVMessage log = operation;
    	operation = null;
		return log;
	}

}
