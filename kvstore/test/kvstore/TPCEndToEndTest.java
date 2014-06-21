package kvstore;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TPCEndToEndTest extends TPCEndToEndTemplate {

	@Test(timeout = 15000)
    public void testPutGet() throws KVException {
			System.out.println("Test Put and Get");
			int size = 5;
			String[] keys = new String[size];
			String[] vals = new String[size];
			String[] anvals = new String[size];
			for(Integer i = 0; i < size; i++){
				keys[i] = "key" + i.toString();
				vals[i] = "val" + i.toString();
				anvals[i] = "anval" + i.toString();
			}
			for(Integer i = 0; i < size; i++)
				client.put(keys[i], vals[i]);
			for(Integer i = 0; i < size; i++)
				assertEquals("error", client.get(keys[i]), vals[i]);
			System.out.println();
		}
/*
    @Test(timeout = 15000)
    public void testDelGet() throws KVException {
			System.out.println("Test Del");
			int size = 5;
			String[] keys = new String[size];
			String[] vals = new String[size];
			String[] anvals = new String[size];
			for(Integer i = 0; i < size; i++){
				keys[i] = "key" + i.toString();
				vals[i] = "val" + i.toString();
				anvals[i] = "anval" + i.toString();
			}
			for(Integer i = 0; i < size; i++)
				client.put(keys[i], vals[i]);
			for(Integer i = 0; i < size; i++)
				client.del(keys[i]);
			try{
				client.get(keys[0]);
			}catch(KVException e){
				assertEquals("del failed", e.getKVMessage().getMessage(), "Data Error: Key does not exist");
			}
			System.out.println();
    }
  
    @Test(timeout = 15000)
    public void testPut() throws KVException {
			System.out.println("Test Del");
			int size = 5;
			String[] keys = new String[size];
			String[] vals = new String[size];
			String[] anvals = new String[size];
			for(Integer i = 0; i < size; i++){
				keys[i] = "key" + i.toString();
				vals[i] = "val" + i.toString();
				anvals[i] = "anval" + i.toString();
			}
			for(Integer i = 0; i < size; i++)
				client.put(keys[i], vals[i]);
			for(Integer i = 0; i < size; i++)
				client.put(keys[i], anvals[i]);
			for(Integer i = 0; i < size; i++)
				assertEquals("put fails", client.get(keys[i]), anvals[i]);
			System.out.println();
    }*/
}
