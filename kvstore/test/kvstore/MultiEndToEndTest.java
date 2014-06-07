package kvstore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class MultiEndToEndTest extends EndToEndTemplate {
	
	@Test
    public void testMultiClients() {
    	System.out.println("Test Multi Clients Case.");
    	int len = 2;
    	String[] keys = new String[len];
    	String[] vals = new String[len];
    	for(int i = 0; i < 2; i++){
    		keys[i] = "key" + (new Integer(i)).toString();
    		vals[i] = "val" + (new Integer(i)).toString();
    	}
    	
    	try{
    		client.put(keys[0], vals[0]);
    		assertEquals(vals[0], another_client.get(keys[0]));
    		another_client.put(keys[0], vals[1]);
    		assertEquals(vals[1], client.get(keys[0]));
    		another_client.del(keys[0]);
    		assertNull(client.get(keys[0]));
    		client.del(keys[0]);
    	} catch (KVException kve) {
    		assertEquals("Data Error: Key does not exist", kve.getKVMessage().getMessage());
    	}
    	System.out.println("Multi Clients Test End.");
    }

}
