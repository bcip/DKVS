package kvstore;

import static kvstore.KVConstants.*;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * Client API used to issue requests to key-value server.
 */	
public class KVClient implements KeyValueInterface {

    private String server;
    private int port;
    
    private static final int MAX_KEY_SIZE = 256;
    private static final int MAX_VAL_SIZE = 256 * 1024;

    /**
     * Constructs a KVClient connected to a server.
     *
     * @param server is the DNS reference to the server
     * @param port is the port on which the server is listening
     */
    public KVClient(String server, int port) {
        this.server = server;
        this.port = port;
    }

    private void checkKey(String key) throws KVException{
    	if(key == null || key.length() == 0){
    		throw new KVException(new KVMessage(RESP, ERROR_INVALID_KEY));
    	}
    	if(key.length() > MAX_KEY_SIZE){
    		throw new KVException(new KVMessage(RESP, ERROR_OVERSIZED_KEY));
    	}
    }
    private void checkValue(String value) throws KVException{
    	if(value == null || value.length() == 0 || value.length() > MAX_VAL_SIZE){
    		throw new KVException(new KVMessage(RESP, ERROR_INVALID_VALUE));
    	}
    	if(value.length() > MAX_VAL_SIZE){
    		throw new KVException(new KVMessage(RESP, ERROR_OVERSIZED_VALUE));
    	}
    }
    
    /**
     * Creates a socket connected to the server to make a request.
     *
     * @return Socket connected to server
     * @throws KVException if unable to create or connect socket
     * @throws SocketException 
     */
    private Socket connectHost() throws KVException {
        // implement me
		try {
			Socket socket = new Socket(this.server, this.port);
			socket.setSoTimeout(TIMEOUT_MILLISECONDS);
	        return socket;
		} catch (UnknownHostException e1){
			KVMessage unMsg = new KVMessage(RESP, ERROR_COULD_NOT_CREATE_SOCKET);
			throw new KVException(unMsg);
		} catch (IOException e2) {
			KVMessage ioMsg = new KVMessage(RESP, ERROR_COULD_NOT_CONNECT);
			throw new KVException(ioMsg);
		}
    }

    /**
     * Closes a socket.
     * Best effort, ignores error since the response has already been received.
     *
     * @param  sock Socket to be closed
     * @throws KVException 
     */
    private void closeHost(Socket sock) throws KVException {
        // implement me
    	if(sock == null){
    		return;
    	}
    	try {
			sock.close();
		} catch (IOException e) {
			KVMessage ioMsg = new KVMessage(RESP, ERROR_COULD_NOT_CLOSE);
			throw new KVException(ioMsg);
		}
    }

    /**
     * Issues a PUT request to the server.
     *
     * @param  key String to put in server as key
     * @throws KVException if the request was not successful in any way
     */
    @Override
    public void put(String key, String value) throws KVException {
        // implement me
		try {
			checkKey(key);
			checkValue(value);
			
			Socket socket = connectHost();
			KVMessage putMsg = new KVMessage(PUT_REQ);
			putMsg.setKey(key);
			putMsg.setValue(value);
			putMsg.sendMessage(socket);
			KVMessage respMsg = new KVMessage(socket);
			closeHost(socket);
			if(!respMsg.getMessage().equals("Success")){
				KVMessage excpMsg = new KVMessage(RESP, respMsg.getMessage());
				throw new KVException(excpMsg);
			}
		} catch (KVException e) {
			throw e;
		}
    }

    /**
     * Issues a GET request to the server.
     *
     * @param  key String to get value for in server
     * @return String value associated with key
     * @throws KVException if the request was not successful in any way
     */
    @Override
    public String get(String key) throws KVException {
        // implement me
    	try{
    		checkKey(key);
    		
    		Socket socket = connectHost();
    		KVMessage getMsg = new KVMessage(GET_REQ);
    		getMsg.setKey(key);
    		getMsg.sendMessage(socket);
    		KVMessage respMsg = new KVMessage(socket);
    		closeHost(socket);
    		if(respMsg.getValue() == null){
    			KVMessage excpMsg = new KVMessage(RESP, respMsg.getMessage());
    			throw new KVException(excpMsg);
    		}else{
    			return respMsg.getValue();
    		}
    	}catch (KVException e){
    		throw e;
    	}
    }

    /**
     * Issues a DEL request to the server.
     *
     * @param  key String to delete value for in server
     * @throws KVException if the request was not successful in any way
     */
    @Override
    public void del(String key) throws KVException {
        // implement me
    	try{
    		checkKey(key);
    		
    		Socket socket = connectHost();
    		KVMessage delMsg = new KVMessage(DEL_REQ);
    		delMsg.setKey(key);
    		delMsg.sendMessage(socket);
    		KVMessage respMsg = new KVMessage(socket);
    		closeHost(socket);
    		if(!respMsg.getMessage().equals("Success")){
    			KVMessage excpMsg = new KVMessage(RESP, respMsg.getMessage());
    			throw new KVException(excpMsg);
    		}
    	}catch (KVException e){
    		throw e;
    	}
    }


}
