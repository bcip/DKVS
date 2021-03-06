package kvstore;

import static kvstore.KVConstants.*;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Implements NetworkHandler to handle 2PC operation requests from the Master/
 * Coordinator Server
 */
public class TPCMasterHandler implements NetworkHandler {

	private long slaveID;
	private KVServer kvServer;
	private TPCLog tpcLog;
	private ThreadPool threadpool;

	private static final int MAX_KEY_SIZE = 256;
	private static final int MAX_VAL_SIZE = 256 * 1024;

	/**
	 * Constructs a TPCMasterHandler with one connection in its ThreadPool
	 * 
	 * @param slaveID
	 *            the ID for this slave server
	 * @param kvServer
	 *            KVServer for this slave
	 * @param log
	 *            the log for this slave
	 */
	public TPCMasterHandler(long slaveID, KVServer kvServer, TPCLog log) {
		this(slaveID, kvServer, log, 1);
	}

	/**
	 * Constructs a TPCMasterHandler with a variable number of connections in
	 * its ThreadPool
	 * 
	 * @param slaveID
	 *            the ID for this slave server
	 * @param kvServer
	 *            KVServer for this slave
	 * @param log
	 *            the log for this slave
	 * @param connections
	 *            the number of connections in this slave's ThreadPool
	 */
	public TPCMasterHandler(long slaveID, KVServer kvServer, TPCLog log,
			int connections) {
		this.slaveID = slaveID;
		this.kvServer = kvServer;
		this.tpcLog = log;
		this.threadpool = new ThreadPool(connections);
	}

	private void checkKey(String key) throws KVException {
		if (key == null || key.length() == 0) {
			throw new KVException(new KVMessage(RESP, ERROR_INVALID_KEY));
		}
		if (key.length() > MAX_KEY_SIZE) {
			throw new KVException(new KVMessage(RESP, ERROR_OVERSIZED_KEY));
		}
	}

	private void checkValue(String value) throws KVException {
		if (value == null || value.length() == 0
				|| value.length() > MAX_VAL_SIZE) {
			throw new KVException(new KVMessage(RESP, ERROR_INVALID_VALUE));
		}
		if (value.length() > MAX_VAL_SIZE) {
			throw new KVException(new KVMessage(RESP, ERROR_OVERSIZED_VALUE));
		}
	}

	/**
	 * Registers this slave server with the master.
	 * 
	 * @param masterHostname
	 * @param server
	 *            SocketServer used by this slave server (which contains the
	 *            hostname and port this slave is listening for requests on
	 * @throws KVException
	 *             with ERROR_INVALID_FORMAT if the response from the master is
	 *             received and parsed but does not correspond to a success as
	 *             defined in the spec OR any other KVException such as those
	 *             expected in KVClient in project 3 if unable to receive and/or
	 *             parse message
	 */
	public void registerWithMaster(String masterHostname, SocketServer server)
			throws KVException {
		// implement me
		try {
			Socket master = new Socket(masterHostname, 9090);
			KVMessage regMsg = new KVMessage(REGISTER, slaveID + "@"
					+ server.getHostname() + ":" + server.getPort());
			regMsg.sendMessage(master);
			KVMessage respMsg = new KVMessage(master);
			master.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Creates a job to service the request on a socket and enqueues that job in
	 * the thread pool. Ignore any InterruptedExceptions.
	 * 
	 * @param master
	 *            Socket connected to the master with the request
	 */
	@Override
	public void handle(Socket master) {
		try {
			threadpool.addJob(new MasterHandler(master));
		} catch (InterruptedException e) {
			// ignore
		}
	}

	/**
	 * Runnable class containing routine to service a message from the master.
	 */
	private class MasterHandler implements Runnable {

		private Socket master;

		/**
		 * Construct a MasterHandler.
		 * 
		 * @param master
		 *            Socket connected to master with the message
		 */
		public MasterHandler(Socket master) {
			this.master = master;
		}

		/**
		 * Processes request from master and sends back a response with the
		 * result. This method needs to handle both phase1 and phase2 messages
		 * from the master. The delivery of the response is best-effort. If we
		 * are unable to return any response, there is nothing else we can do.
		 */
		@Override
		public void run() {
			KVMessage response = null;
			try {
				KVMessage request = new KVMessage(master);

				if (request.getMsgType().equals(GET_REQ)) {
					String key = request.getKey();
					checkKey(key);
					String value = kvServer.get(key);
					response = new KVMessage(RESP);
					response.setKey(key);
					response.setValue(value);
				} else if (request.getMsgType().equals(DEL_REQ)
						|| request.getMsgType().equals(PUT_REQ)) {
					tpcLog.appendAndFlush(request);
					if (request.getMsgType().equals(PUT_REQ)) {
						checkValue(request.getValue());
						checkKey(request.getKey());
					} else
						kvServer.get(request.getKey());
					response = new KVMessage(READY);
				}
				if (request.getMsgType().equals(ABORT)) {
					tpcLog.appendAndFlush(request);
					response = new KVMessage(ACK);
				} else if (request.getMsgType().equals(COMMIT)) {
					request = tpcLog.getLastEntry();
					tpcLog.appendAndFlush(new KVMessage(COMMIT));
					if (request.getMsgType().equals(DEL_REQ)) {
						String key = request.getKey();
						checkKey(key);
						kvServer.del(key);
					} else if (request.getMsgType().equals(PUT_REQ)) {
						String key = request.getKey();
						String value = request.getValue();
						checkKey(key);
						checkValue(value);
						kvServer.put(key, value);
					}
					response = new KVMessage(ACK);
				}
				if (response == null) {
					throw new KVException(ERROR_INVALID_FORMAT);
				}

				// }
			} catch (KVException e) {
				response = e.getKVMessage();
			}

			try {
				response.sendMessage(master);
				if (response.getMsgType().equals(ACK))
					tpcLog.appendAndFlush(response);
			} catch (KVException e) {
				// ignore
			}

			try {
				master.close();
			} catch (IOException e1) {
			}
		}
	}
}
