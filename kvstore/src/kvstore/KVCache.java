package kvstore;

import java.util.LinkedList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.xml.parsers.*;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * A set-associate cache which has a fixed maximum number of sets (numSets).
 * Each set has a maximum number of elements (MAX_ELEMS_PER_SET).
 * If a set is full and another entry is added, an entry is dropped based on
 * the eviction policy.
 */
public class KVCache implements KeyValueInterface {
	
	private int numSets = 100;
	private int maxElemsPerSet = 10;
	private Entry[][] cache;
	private LinkedList<Entry>[] entryQueue;
	private ReentrantLock[] cacheLock;
	
    /**
     * Constructs a second-chance-replacement cache.
     *
     * @param numSets the number of sets this cache will have
     * @param maxElemsPerSet the size of each set
     */
    @SuppressWarnings("unchecked")
    public KVCache(int numSets, int maxElemsPerSet) {
        // implement me
    	this.numSets = numSets;
    	this.maxElemsPerSet = maxElemsPerSet;
    	cache = new Entry[numSets][];
    	entryQueue =(LinkedList<Entry>[]) new LinkedList<?>[numSets];
    	cacheLock = new ReentrantLock[numSets];
    	for(int i = 0; i < numSets; i++){
    		cache[i] = new Entry[maxElemsPerSet];
    		entryQueue[i] = new LinkedList<Entry>();
    		cacheLock[i] = new ReentrantLock();
    		for(int j = 0; j < maxElemsPerSet; j++){
    			cache[i][j] = new Entry();
    		}
    	}
    }

    /**
     * Retrieves an entry from the cache.
     * Assumes access to the corresponding set has already been locked by the
     * caller of this method.
     *
     * @param  key the key whose associated value is to be returned.
     * @return the value associated to this key or null if no value is
     *         associated with this key in the cache
     */
    @Override
    public String get(String key) {
        // implement me
        String res = null;
        int setId = this.getSetId(key);
        for(int i = 0; i < this.maxElemsPerSet; i++){
        	Entry entry = cache[setId][i];
        	if(entry.valid && entry.key.equals(key)){
        		entry.referenceBit = true;
        		res = entry.value;
        	}
        }
        return res;
    }

    /**
     * Adds an entry to this cache.
     * If an entry with the specified key already exists in the cache, it is
     * replaced by the new entry. When an entry is replaced, its reference bit
     * will be set to True. If the set is full, an entry is removed from
     * the cache based on the eviction policy. If the set is not full, the entry
     * will be inserted behind all existing entries. For this policy, we suggest
     * using a LinkedList over an array to keep track of entries in a set since
     * deleting an entry in an array will leave a gap in the array, likely not
     * at the end. More details and explanations in the spec. Assumes access to
     * the corresponding set has already been locked by the caller of this
     * method.
     *
     * @param key the key with which the specified value is to be associated
     * @param value a value to be associated with the specified key
     */
    @Override
    public void put(String key, String value) {
        // implement me
    	int setId = this.getSetId(key);
    	Entry emptyEntry = null;
    	for (int i = 0; i < this.maxElemsPerSet; i++){
    		Entry entry = cache[setId][i];
    		if(entry.valid){
    			if(entry.key.equals(key)){
    				entry.value = value;
    				entry.referenceBit = true;
    				return;
    			}
    		}else{
    			emptyEntry = entry;
    		}
    	}
    	if(emptyEntry != null){
    		emptyEntry.key = key;
    		emptyEntry.value = value;
    		emptyEntry.valid = true;
    		emptyEntry.referenceBit = false;
    		entryQueue[setId].addLast(emptyEntry);
    		return;
    	}else{
    		Entry entry = entryQueue[setId].removeFirst();
    		while(entry.referenceBit){
    			entry.referenceBit = false;
    			entryQueue[setId].addLast(entry);
    			entry = entryQueue[setId].removeFirst();
    		}
    		entry.key = key;
    		entry.value = value;
    		entryQueue[setId].addLast(entry);
    		return;
    	}
    }

    /**
     * Removes an entry from this cache.
     * Assumes access to the corresponding set has already been locked by the
     * caller of this method. Does nothing if called on a key not in the cache.
     *
     * @param key key with which the specified value is to be associated
     */
    @Override
    public void del(String key) {
        // implement me
    	int setId = this.getSetId(key);
    	for(int i = 0; i < this.maxElemsPerSet; i++){
    		Entry entry = cache[setId][i];
    		if(entry.valid && entry.key.equals(key)){
    			entry.valid = false;
    			entryQueue[setId].remove(entry);
    			return;
    		}
    	}
    }

    /**
     * Get a lock for the set corresponding to a given key.
     * The lock should be used by the caller of the get/put/del methods
     * so that different sets can be modified in parallel.
     *
     * @param  key key to determine the lock to return
     * @return lock for the set that contains the key
     */
    public ReentrantLock getLock(String key) {
        // implement me
        return cacheLock[this.getSetId(key)];
    }

    /**
     * Get the id of the set for a particular key.
     *
     * @param  key key of interest
     * @return set of the key
     */
    private int getSetId(String key) {
        return Math.abs(key.hashCode()) % numSets;
    }

    /**
     * Serialize this store to XML. See spec for details on output format.
     * This method is best effort. Any exceptions that arise can be dropped.
     */
    public String toXML() {
        // implement me
    	try {
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = builder.newDocument();
			Element KVCache = doc.createElement("KVCache");
			doc.appendChild(KVCache);
			for(int i = 0; i < this.numSets; i++){
				Element Set = doc.createElement("Set");
				Set.setAttribute("Id", "" + i);
				KVCache.appendChild(Set);
				for(int j = 0; j < this.maxElemsPerSet; j++){
					Entry entry = cache[i][j];
					if(entry.valid){
						Element CacheEntry = doc.createElement("CacheEntry");
						CacheEntry.setAttribute("isReferenced", ""+entry.referenceBit);
						Element Key = doc.createElement("Key");
						Element Value = doc.createElement("Value");
						Key.appendChild(doc.createTextNode(entry.key));
						Value.appendChild(doc.createTextNode(entry.value));
						CacheEntry.appendChild(Key);
						CacheEntry.appendChild(Value);
					}
				}
			}
			
			return KVMessage.printDoc(doc);
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			return null;
		}
    }

    @Override
    public String toString() {
        return this.toXML();
    }
    
    private class Entry{
    	public String key;
    	public String value;
    	public boolean referenceBit;
    	public boolean valid;
    	
    	public Entry(){
    		this.valid = false;
    		this.referenceBit = false;
    		this.key = "";
    		this.value = "";
    	}
    }

}
