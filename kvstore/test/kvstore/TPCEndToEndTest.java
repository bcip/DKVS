package kvstore;

import static org.junit.Assert.*;

import java.util.Random;

import org.junit.Test;

public class TPCEndToEndTest extends TPCEndToEndTemplate {
	
	@Test(timeout = 15000)
    public void testPutGet2() throws KVException {

		
		System.out.println("Test Put and Get 2");
			int size = 1;
			String[] keys = new String[size];
			String[] vals = new String[size];
			for(Integer i = 0; i < size; i++){
				keys[i] = "pkey" + i.toString();
				vals[i] = "pval" + i.toString();
			}

			

			for(Integer i = 0; i < size; i++){
				assertEquals(client.get(keys[i]), vals[i]);
				System.out.println("Success");
			}
			System.out.println();
			
			try{
				client.get("key1");
			}catch(KVException e){
				System.out.println("get:" + e.getKVMessage().getMessage());
			}
	}
	//*/
	@Test(timeout = 15000)
	public void testPutGet() throws KVException {

		int size = 2;
		String[] keys = new String[size];
		String[] vals = new String[size];
		for (Integer i = 0; i < size; i++) {
			keys[i] = "pkey" + i.toString();
			vals[i] = "pval" + i.toString();
		}

		System.out.println("Test Put and Get 1");
		for (Integer i = 0; i < size; i++) {
			System.out.println("Client put key=" + keys[i] + "  value="
					+ vals[i]);
			try{
				client.put(keys[i], vals[i]);
			}catch(KVException e){
				System.out.println("put:" + e.getKVMessage().getMessage());
			}
			System.out.println();
		}

		for (Integer i = 0; i < size; i++) {
			System.out.println("Test of get key=" + keys[i]);
			System.out.println("Expected to get value=" + vals[i]);
			assertEquals(client.get(keys[i]), vals[i]);
			System.out.println("Success");
			System.out.println();
		}
		client.del(keys[1]);
		try{
			client.del(keys[1]);
		}catch(KVException e){
			System.out.println("del:" + e.getKVMessage().getMessage());
		}
		System.out.println();
	}
	
	/*
	@Test(timeout = 15000)
    public void testPutGet3() throws KVException {

			int size = (int)(100 * (new Random().nextDouble()));
			String[] keys = new String[size];
			String[] vals = new String[size];
			for(Integer i = 0; i < size; i++){
				keys[i] = "pkey" + i.toString();
				vals[i] = "pval" + i.toString();
			}

			System.out.println("Test Put and Get");
			for(Integer i = 0; i < size; i++)
				client.put(keys[i], vals[i]);

			for(Integer i = 0; i < size; i++)
				assertEquals(client.get(keys[i]), vals[i]);
			System.out.println();
	}

	@Test(timeout = 15000)
    public void testPutGet4() throws KVException {

			int size = (int)(100 * (new Random().nextDouble()));
			String[] keys = new String[size];
			String[] vals = new String[size];
			for(Integer i = 0; i < size; i++){
				keys[i] = "pkey" + i.toString();
				vals[i] = "pval" + i.toString();
			}

			System.out.println("Test Put and Get");
			for(Integer i = 0; i < size; i++)
				client.put(keys[i], vals[i]);

			for(Integer i = 0; i < size; i++)
				assertEquals(client.get(keys[i]), vals[i]);
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
	
	///*
	@Test(timeout = 20000)
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
		
		System.out.println("Test del with nonexist value");
		try {
			client.del("nonexist");
		} catch (KVException e){
			assertEquals(e.getKVMessage().getMessage(), KVConstants.ERROR_NO_SUCH_KEY);
			System.out.println("Success in nonexist key");
		}
		System.out.println();
		
		System.out.println("Test del with too long value");
		try {
			client.del("111111111111111111111111111111111111111111111111111111111"
					+ "111111111111111111111111111111111111111111111111111111111"
					+ "111111111111111111111111111111111111111111111111111111111"
					+ "111111111111111111111111111111111111111111111111111111111"
					+ "111111111111111111111111111111111111111111111111111111111"
					+ "111111111111111111111111111111111111111111111111111111111"
					+ "111111111111111111111111111111111111111111111111111111111"
					+ "111111111111111111111111111111111111111111111111111111111"
					+ "111111111111111111111111111111111111111111111111111111111"
					+ "111111111111111111111111111111111111111111111111111111111"
					+ "111111111111111111111111111111111111111111111111111111111"
					);
		} catch (KVException e){
			assertEquals(e.getKVMessage().getMessage(), KVConstants.ERROR_OVERSIZED_KEY);
			System.out.println("Success in too long key");
		}
		System.out.println();
		
		System.out.println("Test del with empty value");
		try {
			client.del("");
		} catch (KVException e){
			assertEquals(e.getKVMessage().getMessage(), KVConstants.ERROR_INVALID_KEY);
			System.out.println("Success in empty value");
		}
		System.out.println();
		
		System.out.println("Test del with null value");
		try {
			client.del(null);
		} catch (KVException e){
			assertEquals(e.getKVMessage().getMessage(), KVConstants.ERROR_INVALID_KEY);
			System.out.println("Success in empty value");
		}
		System.out.println();
	}
	*/
}
