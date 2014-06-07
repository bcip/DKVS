package kvstore;

import static org.junit.Assert.*;

import org.junit.*;

public class KVCacheTest {

    /**
     * Verify the cache can put and get a KV pair successfully.
     */
    @Test
    public void singlePutAndGet() {
        KVCache cache = new KVCache(1, 4);
        cache.put("hello", "world");
        assertEquals("world", cache.get("hello"));
    }
    
    @Test
    public void testPutGetDelWithSecondChance(){
    	KVCache cache = new KVCache(1, 3);
    	cache.put("k1", "k1");
    	cache.put("k2", "k2");
    	cache.put("k3", "k3");
    	assertEquals("k1", cache.get("k1"));
    	cache.put("k1", "k2");
    	assertEquals("k2", cache.get("k1"));
    	cache.put("k4", "k4");
    	assertNotNull(cache.get("k1"));
    	assertNull(cache.get("k2"));
    	cache.put("k5", "k5");
    	assertNull(cache.get("k3"));
    	cache.del("k1");
    	assertNull(cache.get("k1"));
    	cache.put("k1", "k1");
    	cache.put("k2", "k2");
    	assertNull(cache.get("k4"));
    }

}
