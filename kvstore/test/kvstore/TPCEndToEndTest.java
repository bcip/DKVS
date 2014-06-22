package kvstore;

import static org.junit.Assert.*;

import org.junit.Test;

public class TPCEndToEndTest extends TPCEndToEndTemplate {
	@Test(timeout = 15000)
	public void testPutGet() throws KVException {

		int size = 10;
		String[] keys = new String[size];
		String[] vals = new String[size];
		for (Integer i = 0; i < size; i++) {
			keys[i] = "pkey" + i.toString();
			vals[i] = "pval" + i.toString();
		}

		System.out.println("Test Put and Get");
		for (Integer i = 0; i < size; i++) {
			System.out.println("Client put key=" + keys[i] + "  value="
					+ vals[i]);
			client.put(keys[i], vals[i]);
			System.out.println();
		}

		for (Integer i = 0; i < size; i++) {
			System.out.println("Test of get key=" + keys[i]);
			System.out.println("Expected to get value=" + vals[i]);
			assertEquals(client.get(keys[i]), vals[i]);
			System.out.println("Success");
			System.out.println();
		}
		System.out.println();
	}

	@Test(timeout = 15000)
	public void testDel() throws KVException {

		int size = 10;
		String[] keys = new String[size];
		String[] vals = new String[size];
		for (Integer i = 0; i < size; i++) {
			keys[i] = "dkey" + i.toString();
			vals[i] = "dval" + i.toString();
		}

		System.out.println("Test Del");
		for (Integer i = 0; i < size; i++) {
			System.out.println("Client put key=" + keys[i] + "  value="
					+ vals[i]);
			client.put(keys[i], vals[i]);
			System.out.println();
		}

		for (Integer i = 0; i < size; i++) {
			System.out.println("Del key=" + keys[i]);
			client.del(keys[i]);
			System.out.println();
		}
		for (Integer i = 0; i < size; i++) {
			try {
				client.get(keys[i]);
				fail();
			} catch (KVException e) {
				assertEquals("del failed", e.getKVMessage().getMessage(),
						"Data Error: Key does not exist");
				System.out.println("Success at del key=" + keys[i]);
				System.out.println();
			}
		}
		System.out.println();
	}
	
	@Test(timeout = 15000)
	public void testInvalidKeyAndInvalidValue() {
		System.out.println("Test put with null key");
		try {
			client.put(null, "test");
			fail();
		} catch (KVException e){
			assertEquals(e.getKVMessage().getMessage(), KVConstants.ERROR_INVALID_KEY);
			System.out.println("Success in null key");
		}
		System.out.println();
		
		System.out.println("Test put with empty key");
		try {
			client.put("", "test");
			fail();
		} catch (KVException e){
			assertEquals(e.getKVMessage().getMessage(), KVConstants.ERROR_INVALID_KEY);
			System.out.println("Success in empty key");
		}
		System.out.println();
		
		System.out.println("Test put with null value");
		try {
			client.put("test", null);
		} catch (KVException e){
			assertEquals(e.getKVMessage().getMessage(), KVConstants.ERROR_INVALID_VALUE);
			System.out.println("Success in null value");
		}
		System.out.println();
		
		System.out.println("Test put with empty value");
		try {
			client.put("test", "");
		} catch (KVException e){
			assertEquals(e.getKVMessage().getMessage(), KVConstants.ERROR_INVALID_VALUE);
			System.out.println("Success in empty value");
		}
		System.out.println();
	}
}
