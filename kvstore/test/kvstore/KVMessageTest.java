package kvstore;

import static kvstore.KVConstants.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.*;
import java.net.*;
import java.util.*;

import org.junit.*;
public class KVMessageTest {

    private Socket sock;

    private static final String TEST_INPUT_DIR = "test/kvstore/test-inputs/";

    @Test
    public void successfullyParsesPutReq() throws KVException {
        setupSocket("putreq.txt");
        KVMessage kvm = new KVMessage(sock);
        assertNotNull(kvm);
        assertEquals(PUT_REQ, kvm.getMsgType());
        assertNull(kvm.getMessage());
        assertNotNull(kvm.getKey());
        assertNotNull(kvm.getValue());
    }

    @Test
    public void successfullyParsesPutResp() throws KVException {
        setupSocket("putresp.txt");
        KVMessage kvm = new KVMessage(sock);
        assertNotNull(kvm);
        assertEquals(RESP, kvm.getMsgType());
        assertTrue(SUCCESS.equalsIgnoreCase(kvm.getMessage()));
        assertNull(kvm.getKey());
        assertNull(kvm.getValue());
    }
    
    @Test
    public void successfullyParsesGetReq() throws KVException {
    	setupSocket("getreq.txt");
        KVMessage kvm = new KVMessage(sock);
        assertNotNull(kvm);
        assertEquals(GET_REQ, kvm.getMsgType());
        assertNull(kvm.getMessage());
        assertNotNull(kvm.getKey());
        assertNull(kvm.getValue());
    }
    
    @Test
    public void successfullyParsesGetResp() throws KVException {
        setupSocket("getresp.txt");
        KVMessage kvm = new KVMessage(sock);
        assertNotNull(kvm);
        assertEquals(RESP, kvm.getMsgType());
        assertNull(kvm.getMessage());
        assertNotNull(kvm.getKey());
        assertNotNull(kvm.getValue());
    }
    
    @Test
    public void unsuccessfullyParsesPutReq() throws KVException {
        setupSocket("invalidputreq.txt");
        try{
        	KVMessage kvm = new KVMessage(sock);
        	assertNotNull(kvm);
            assertEquals(PUT_REQ, kvm.getMsgType());
            assertNull(kvm.getMessage());
            assertNotNull(kvm.getKey());
            assertNotNull(kvm.getValue());
        }catch(KVException e){
        	assertEquals(RESP, e.getKVMessage().getMsgType());
        	assertEquals(ERROR_INVALID_FORMAT, e.getKVMessage().getMessage());
        }
    }
    
    @Test
    public void successfullyToXml() throws KVException {
    	setupSocket("putreq.txt");
    	KVMessage kvm = new KVMessage(sock);
    	assertNotNull(kvm);
    	String kvmToXml = kvm.toXML();
    	Socket stringSock = mock(Socket.class);
    	try {
            doNothing().when(stringSock).setSoTimeout(anyInt());
            when(stringSock.getInputStream()).thenReturn(new StringBufferInputStream(kvmToXml));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    	KVMessage kvm1 = new KVMessage(stringSock);
    	assertEquals(kvm.getMsgType(), kvm1.getMsgType());
    	assertEquals(kvm.getMessage(), kvm1.getMessage());
    	assertEquals(kvm.getKey(), kvm1.getKey());
    	assertEquals(kvm.getValue(), kvm1.getValue());
    }

    /* Begin helper methods */

    private void setupSocket(String filename) {
        sock = mock(Socket.class);
        File f = new File(System.getProperty("user.dir"), TEST_INPUT_DIR + filename);
        try {
            doNothing().when(sock).setSoTimeout(anyInt());
            when(sock.getInputStream()).thenReturn(new FileInputStream(f));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
