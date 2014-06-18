package kvstore;

import static kvstore.KVConstants.*;

import java.io.IOException;
import java.net.*;
import java.util.regex.*;

/**
 * Data structure to maintain information about SlaveServers
 */
public class TPCSlaveInfo {

    private long slaveID;
    private String hostname;
    private int port;
 // Regex to parse slave info
    private static final Pattern SLAVE_INFO_REGEX = Pattern.compile("^(.*)@(.*):(.*)$");

    /**
     * Construct a TPCSlaveInfo to represent a slave server.
     *
     * @param info as "SlaveServerID@Hostname:Port"
     * @throws KVException ERROR_INVALID_FORMAT if info string is invalid
     */
    public TPCSlaveInfo(String info) throws KVException {
        // implement me
    	try {
            Matcher slaveInfoMatcher = SLAVE_INFO_REGEX.matcher(info);

            if (!slaveInfoMatcher.matches()) {
                throw new IllegalArgumentException();
            }

            slaveID = Long.parseLong(slaveInfoMatcher.group(1));
            hostname = slaveInfoMatcher.group(2);
            port = Integer.parseInt(slaveInfoMatcher.group(3));
        } catch (Exception ex) {
            throw new KVException(new KVMessage(
                RESP, ERROR_INVALID_FORMAT));
        }
    }

    public long getSlaveID() {
        return slaveID;
    }

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }

    public void updateInfo(String _hostname, int _port) {
    	hostname = _hostname;
    	port = _port;
    }
    
    /**
     * Create and connect a socket within a certain timeout.
     *
     * @return Socket object connected to SlaveServer, with timeout set
     * @throws KVException ERROR_SOCKET_TIMEOUT, ERROR_COULD_NOT_CREATE_SOCKET,
     *         or ERROR_COULD_NOT_CONNECT
     */
    public Socket connectHost(int timeout) throws KVException {
        // implement me
    	try {
            Socket sock = new Socket();
            sock.setSoTimeout(timeout);
            sock.connect(new InetSocketAddress(hostname, port), TIMEOUT_MILLISECONDS);
            return sock;
        } catch (UnknownHostException ex) {
            throw new KVException(new KVMessage(
                RESP, ERROR_COULD_NOT_CONNECT));
        } catch (IOException ex) {
            throw new KVException(new KVMessage(
                RESP, ERROR_COULD_NOT_CREATE_SOCKET));
        }
    }

    /**
     * Closes a socket.
     * Best effort, ignores error since the response has already been received.
     *
     * @param sock Socket to be closed
     * @throws KVException 
     */
    public void closeHost(Socket sock) throws KVException {
        // implement me
    	try {
			sock.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			throw new KVException(new KVMessage(
	                RESP, ERROR_COULD_NOT_CLOSE));
		}
    }
}
