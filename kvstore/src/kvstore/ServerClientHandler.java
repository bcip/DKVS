package kvstore;

import static kvstore.KVConstants.*;

import java.net.Socket;

/**
 * This NetworkHandler will asynchronously handle the socket connections.
 * Uses a thread pool to ensure that none of its methods are blocking.
 */
public class ServerClientHandler implements NetworkHandler {

    private KVServer kvServer;
    private ThreadPool threadPool;

    /**
     * Constructs a ServerClientHandler with ThreadPool of a single thread.
     *
     * @param kvServer KVServer to carry out requests
     */
    public ServerClientHandler(KVServer kvServer) {
        this(kvServer, 1);
    }

    /**
     * Constructs a ServerClientHandler with ThreadPool of thread equal to
     * the number passed in as connections.
     *
     * @param kvServer KVServer to carry out requests
     * @param connections number of threads in threadPool to service requests
     */
    public ServerClientHandler(KVServer kvServer, int connections) {
        this.kvServer = kvServer;
        this.threadPool = new ThreadPool(connections);
    }

    /**
     * Creates a job to service the request for a socket and enqueues that job
     * in the thread pool. Ignore any InterruptedExceptions.
     *
     * @param client Socket connected to the client with the request
     */
    @Override
    public void handle(Socket client) {
    	try{
    		threadPool.addJob(new ClientHandler(client));
    	}
    	catch(Exception e){
    		//ignore
    	}
    }

    /**
     * Runnable class with routine to service a request from the client.
     */
    private class ClientHandler implements Runnable {

        private Socket client;

        /**
         * Construct a ClientHandler.
         *
         * @param client Socket connected to client with the request
         */
        public ClientHandler(Socket client) {
            this.client = client;
        }

        /**
         * Processes request from client and sends back a response with the
         * result. The delivery of the response is best-effort. If we are
         * unable to return a response, there is nothing else we can do.
         */
        @Override
        public void run() {
        	KVMessage response = null;
        	try{
        		KVMessage request = new KVMessage(client);
        		if(request.getMsgType().equals(PUT_REQ)){
        			String key = request.getKey();
        			String value = request.getValue();
        			kvServer.put(key, value);
        			
        			response = new KVMessage(RESP, SUCCESS);
        		}
        		else if(request.getMsgType().equals(GET_REQ)){
        			String key = request.getKey();
        			String value = kvServer.get(key);
        			
        			response = new KVMessage(RESP);
        			response.setKey(key);
        			response.setValue(value);
        		}
        		else if(request.getMsgType().equals(DEL_REQ)){
        			String key = request.getKey();
        			kvServer.del(key);
        			
        			response = new KVMessage(RESP, SUCCESS);
        		}
        		else{
        			throw new KVException(ERROR_INVALID_FORMAT);
        		}
        	}
        	catch(KVException e){
        		response = e.getKVMessage();
        	}
        	finally{
        		try{
        			response.sendMessage(client);
        		}
        		catch(Exception e){
        			//ignore
        		}
        	}
        }
    }

}
