package kvstore;

import static kvstore.KVConstants.*;

import java.io.IOException;
import java.net.Socket;

/**
 * This NetworkHandler will asynchronously handle the socket connections.
 * Uses a thread pool to ensure that none of its methods are blocking.
 */
public class TPCRegistrationHandler implements NetworkHandler {

    private ThreadPool threadPool;
    private TPCMaster master;

    /**
     * Constructs a TPCRegistrationHandler with a ThreadPool of a single thread.
     *
     * @param master TPCMaster to register slave with
     */
    public TPCRegistrationHandler(TPCMaster master) {
        this(master, 1);
    }

    /**
     * Constructs a TPCRegistrationHandler with ThreadPool of thread equal to the
     * number given as connections.
     *
     * @param master TPCMaster to carry out requests
     * @param connections number of threads in threadPool to service requests
     */
    public TPCRegistrationHandler(TPCMaster master, int connections) {
        this.threadPool = new ThreadPool(connections);
        this.master = master;
    }

    /**
     * Creates a job to service the request on a socket and enqueues that job
     * in the thread pool. Ignore any InterruptedExceptions.
     *
     * @param slave Socket connected to the slave with the request
     */
    @Override
    public void handle(Socket slave) {
        // implement me
    	try {
				threadPool.addJob(new RegistrationHandler(slave));
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    }

    /**
     * Runnable class containing routine to service a registration request from
     * a slave.
     */
    public class RegistrationHandler implements Runnable {

        public Socket slave = null;

        public RegistrationHandler(Socket slave) {
            this.slave = slave;
        }

        /**
         * Parse registration request from slave and add register with TPCMaster.
         * If able to successfully parse request and register slave, send back
         * a successful response according to spec. If not, send back a response
         * with ERROR_INVALID_FORMAT.
         */
        @Override
        public void run() {
            // implement me
        	try {
				KVMessage regMsg = new KVMessage(slave);
				String msg = regMsg.getMessage();
				if(!regMsg.getMsgType().equals("register")){
					(new KVMessage(RESP, ERROR_INVALID_FORMAT)).sendMessage(slave);
					return;
				}
				TPCSlaveInfo slaveInfo = new TPCSlaveInfo(msg);
				master.registerSlave(slaveInfo);
				String respmsg = "Successfully registered " + msg;
				KVMessage respMsg = new KVMessage(RESP, respmsg);
				respMsg.sendMessage(slave);
			} catch (KVException e) {
			} finally {
				try {
					slave.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
        }
    }
}
