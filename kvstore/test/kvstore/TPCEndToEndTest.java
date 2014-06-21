package kvstore;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TPCEndToEndTest extends TPCEndToEndTemplate {
	
    @Test(timeout = 15000)
    public void testPutGetDel() throws KVException {
			System.out.println("Test Put and Get");
			client.put("key", "value");
			assertEquals("get failed", client.get("key"), "value");
			System.out.println();
			
			System.out.println("Test Del");
			client.put("key", "value");
			client.del("key");
			try{
				client.get("key");
			}catch(KVException e){
				assertEquals("del failed", e.getKVMessage().getMessage(), "Data Error: Key does not exist");
			}
			System.out.println();
    }
}
