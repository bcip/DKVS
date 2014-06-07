package kvstore;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class EndToEndTest extends EndToEndTemplate {

    @Test
    public void testPutGetDel() throws KVException {
    	
    	System.out.println("Test Put Get Del.");
    	try{
    	System.out.println("testing put");
		client.put("fuzzy", "wuzzy");
		System.out.println("put ok");

		System.out.println("putting (fuzzy, wuzzy) (again)");
		client.put("fuzzy", "wuzzy");
		System.out.println("ok");

		System.out.println("putting (key2, value2)");
		client.put("key2", "value2");
		System.out.println("ok");

		System.out.println("getting key=fuzzy");			
		String value = client.get("fuzzy");					
		System.out.println("returned: " + value);
		assertEquals("wuzzy", value);

		System.out.println("getting key=key2");			
		String value2 = client.get("key2");					
		System.out.println("returned: " + value2);
		assertEquals("value2", value2);

		System.out.println("putting (fuzzy, ursa)");
		client.put("fuzzy", "ursa");
		System.out.println("ok");

		System.out.println("getting key=fuzzy");			
		String value3 = client.get("fuzzy");					
		System.out.println("returned: " + value3);
		assertEquals("ursa", value3);

		System.out.println("deleting key=key2");			
		client.del("key2");					
		System.out.println("ok");

		System.out.println("deleting key=key2 (again), should throw exception");			
		client.del("key2");		
    	}catch(KVException e){
    		assertEquals("Data Error: Key does not exist", e.getKVMessage().getMessage());
    	}

    	
        client.put("foo", "bar");
        assertEquals("bar", client.get("foo"));
		client.del("foo");
		try{
			client.get("foo");
		}catch(KVException e){
			assertEquals("Data Error: Key does not exist", e.getKVMessage().getMessage());
		}
    }
    
}
