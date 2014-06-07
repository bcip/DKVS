package kvstore;

import static kvstore.KVConstants.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.io.*;
import java.net.*;

import org.junit.*;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(KVClient.class)
public class KVClientTest {

    KVClient client;
    Socket sock;

    private static final String TEST_INPUT_DIR = "test/kvstore/test-inputs/";
    private static File tempFile;
    
    private void setupSocket(String filename) throws Exception {
        sock = mock(Socket.class);
        whenNew(Socket.class).withArguments(anyString(), anyInt()).thenReturn(sock);
        File f = new File(System.getProperty("user.dir"), TEST_INPUT_DIR + filename);
        doNothing().when(sock).setSoTimeout(anyInt());
        when(sock.getOutputStream()).thenReturn(new FileOutputStream(tempFile));
        when(sock.getInputStream()).thenReturn(new FileInputStream(f));
    }

    @BeforeClass
    public static void setupTempFile() throws IOException {
        tempFile = File.createTempFile("TestKVClient-", ".txt");
        tempFile.deleteOnExit();
    }

    @Before
    public void setupClient() throws IOException {
        String hostname = InetAddress.getLocalHost().getHostAddress();
        client = new KVClient(hostname, 8080);
    }

    @Test(timeout = 20000)
    public void testInvalidKey() {
    	System.out.println("Test put invalid key in KVClient.");
        try {
            client.put("", "bar");
            fail("Didn't fail on empty key");
        } catch (KVException kve) {
            String errorMsg = kve.getKVMessage().getMessage();
            assertEquals(errorMsg, ERROR_INVALID_KEY);
            System.out.println("Ok.");
        }
    }

    @Test(timeout = 20000)
	public void testInvalidValue(){
    	System.out.println("Test put invalid value in KVClient.");
		try {
			client.put("key", "");
			fail("Didn't fail on empty value");
		} catch (KVException kve) {
			String errorMsg = kve.getKVMessage().getMessage();
			assertEquals(errorMsg, ERROR_INVALID_VALUE);
			System.out.println("Ok.");
		}
	}

    @Test(timeout = 20000)
	public void testInvalidPut(){
    	System.out.println("Test invalid put in KVClient.");
		try {
			setupSocket("invalidputreq.txt");
			client.put("key", null);
			fail("Didn't fail on null value");
		} catch (KVException kve) {
			String errorMsg = kve.getKVMessage().getMessage();
			assertEquals(errorMsg, ERROR_INVALID_VALUE);
			System.out.println("Ok.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
    
    @Test(timeout = 20000)
	public void testErrorResp(){
    	System.out.println("Test error response in KVClient.");
		try {
			setupSocket("putreq.txt");
			setupSocket("errorresp.txt");
			client.put("key", "value");
			fail("Didn't fail on errorresp");
		} catch (KVException kve) {
			String errorMsg = kve.getKVMessage().getMessage();
			assertEquals(errorMsg, "Error Message");
			System.out.println("Ok.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
    
    @Test(timeout = 20000)
    public void testSocket(){
    	System.out.println("Test socket in KVClient.");
    	try {
    		System.out.println("Test get in KVClient.");
    		setupSocket("getreq.txt");
    		setupSocket("getresp.txt");
    		assertEquals(client.get("key"), "value");
    		System.out.println("Ok");
    		
    		System.out.println("Test put in KVClient.");
    		setupSocket("putreq.txt");
    		setupSocket("putresp.txt");
    		client.put("key", "value");
    		System.out.println("Ok");
    		
    		System.out.println("Test del in KVClient.");
    		setupSocket("delreq.txt");
    		setupSocket("delresp.txt");
    		client.del("key");
    		System.out.println("Ok");
    	}catch (KVException kve) {
    		String errorMsg = kve.getKVMessage().getMessage();
    		System.out.println(errorMsg);
    	}catch (Exception e) {
    		e.printStackTrace();
    	}
    }


}
