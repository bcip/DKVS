package kvstore;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TPCEndToEndTest extends TPCEndToEndTemplate {
	@Test(timeout = 15000)
    public void testPutGet() throws KVException {
			
			int size = 10;
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
		for(Integer i = 0; i < size; i++){
			keys[i] = "dkey" + i.toString();
			vals[i] = "dval" + i.toString();
		}

		
			System.out.println("Test Del");
			for(Integer i = 0; i < size; i++)
				client.put(keys[i], vals[i]);
			
			for(Integer i = 0; i < size; i++)
				client.del(keys[i]);
			for(Integer i = 0; i < size; i++){
				try{
					client.get(keys[i]);
				}catch(KVException e){
					assertEquals("del failed", e.getKVMessage().getMessage(), "Data Error: Key does not exist");
				}
			}
			System.out.println();
		}
}
