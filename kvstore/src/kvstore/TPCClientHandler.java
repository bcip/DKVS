package kvstore;

import static kvstore.KVConstants.*;

import java.io.IOException;
import java.net.Socket;

/**
 * This NetworkHandler will asynchronously handle the socket connections.
 * It uses a threadPool to ensure that none of it's methods are blocking.
 */
public class TPCClientHandler implements NetworkHandler {

    private TPCMaster tpcMaster;
    private ThreadPool threadPool;

    /**
     * Constructs a TPCClientHandler with ThreadPool of a single thread.
     *
     * @param tpcMaster TPCMaster to carry out requests
     */
    public TPCClientHandler(TPCMaster tpcMaster) {
        this(tpcMaster, 1);
    }

    /**
     * Constructs a TPCClientHandler with ThreadPool of a single thread.
     *
     * @param tpcMaster TPCMaster to carry out requests
     * @param connections number of threads in threadPool to service requests
     */
    public TPCClientHandler(TPCMaster tpcMaster, int connections) {
        this.tpcMaster = tpcMaster;
        this.threadPool = new ThreadPool(connections);
    }

    /**
     * Creates a job to service the request on a socket and enqueues that job
     * in the thread pool. Ignore InterruptedExceptions.
     *
     * @param client Socket connected to the client with the request
     */
    @Override
    public void handle(Socket client) {
    	try{
    		threadPool.addJob(new ClientHandler(client));
    	}
    	catch (InterruptedException e){
    		//ignore
    	}
    }

    /**
     * Runnable class containing routine to service a request from the client.
     */
    private class ClientHandler implements Runnable {

        private Socket client = null;

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
         * unable to return any response, there is nothing else we can do.
         */
        @Override
        public void run() {
        	KVMessage response = null;
        	try{
        		KVMessage request = new KVMessage(client);
        		if(request.getMsgType().equals(PUT_REQ)){
        			tpcMaster.handleTPCRequest(request, true);
        			
        			response = new KVMessage(RESP, SUCCESS);
        		}
        		else if(request.getMsgType().equals(GET_REQ)){
        			String key = request.getKey();
        			String value = tpcMaster.handleGet(request);
        			
        			response = new KVMessage(RESP);
        			response.setKey(key);
        			response.setValue(value);
        		}
        		else if(request.getMsgType().equals(DEL_REQ)){
        			tpcMaster.handleTPCRequest(request, false);
        			
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
        			client.close();
        		}
        		catch(KVException e){
        			//ignore
        		} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        	}
        }
    }

}
