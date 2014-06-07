package kvstore;

import static kvstore.KVConstants.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
 * This is a basic key-value store. Ideally this would go to disk, or some other
 * backing store.
 */
public class KVStore implements KeyValueInterface {

    private ConcurrentHashMap<String, String> store;

    /**
     * Construct a new KVStore.
     */
    public KVStore() {
        resetStore();
    }

    private void resetStore() {
        this.store = new ConcurrentHashMap<String, String>();
    }

    /**
     * Insert key, value pair into the store.
     *
     * @param  key String key
     * @param  value String value
     */
    @Override
    public void put(String key, String value) {
        store.put(key, value);
    }

    /**
     * Retrieve the value corresponding to the provided key
     * @param  key String key
     * @throws KVException with ERROR_NO_SUCH_KEY if key does not exist in store
     */
    @Override
    public String get(String key) throws KVException {
        String retVal = this.store.get(key);
        if (retVal == null) {
            KVMessage msg = new KVMessage(KVConstants.RESP, ERROR_NO_SUCH_KEY);
            throw new KVException(msg);
        }
        return retVal;
    }

    /**
     * Delete the value corresponding to the provided key.
     *
     * @param  key String key
     * @throws KVException with ERROR_NO_SUCH_KEY if key does not exist in store
     */
    @Override
    public void del(String key) throws KVException {
        if(key != null) {
            if (!this.store.containsKey(key)) {
                KVMessage msg = new KVMessage(KVConstants.RESP, ERROR_NO_SUCH_KEY);
                throw new KVException(msg);
            }
            this.store.remove(key);
        }
    }

    /**
     * Serialize the store to XML. See the spec for specific output format.
     * This method is best effort. Any exceptions that arise can be dropped.
     * @throws Exception 
     */
    public String toXML() throws Exception {
        // implement me
		try {
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

			Document doc = builder.newDocument();
			doc.setXmlStandalone(true);
			Element KVStoreElement = doc.createElement("KVStore");
			doc.appendChild(KVStoreElement);
			Enumeration<String> keysEnumerator = this.store.keys();
			for (int i = 0; i < store.size(); i++){
				Element KVPairElement = doc.createElement("KVPair");
				KVStoreElement.appendChild(KVPairElement);

				String key = keysEnumerator.nextElement();
				Element keyElement = doc.createElement("Key");
				keyElement.appendChild(doc.createTextNode(key));
				KVPairElement.appendChild(keyElement);

				String value = store.get(key);
				Element valueElement = doc.createElement("Value");
				valueElement.appendChild(doc.createTextNode(value));
				KVPairElement.appendChild(valueElement);

			}
			return KVMessage.printDoc(doc);
		} catch (ParserConfigurationException e) {
			throw new Exception(ERROR_PARSER);
		}
    }

    @Override
    public String toString() {
        try {
			return this.toXML();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
    }

    /**
     * Serialize to XML and write the output to a file.
     * This method is best effort. Any exceptions that arise can be dropped.
     *
     * @param fileName the file to write the serialized store
     * @throws Exception 
     */
    public void dumpToFile(String fileName) throws Exception {
        // implement me
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
			writer.write(toXML());
	    	writer.close();
		} catch (IOException e) {
			throw new Exception(ERROR_PARSER);
		}
    }

    /**
     * Replaces the contents of the store with the contents of a file
     * written by dumpToFile; the previous contents of the store are lost.
     * The store is cleared even if the file does not exist.
     * This method is best effort. Any exceptions that arise can be dropped.
     *
     * @param fileName the file containing the serialized store data
     * @throws Exception 
     */
    public void restoreFromFile(String fileName) throws Exception {
        resetStore();

        // implement me
        
        try{
    		File xmlFile = new File(fileName);
    		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    		Document doc = builder.parse(xmlFile);
    		
    		this.resetStore();
    		
    		NodeList KVPairList = doc.getElementsByTagName("KVPair");
    		
    		for (int i = 0; i < KVPairList.getLength(); i++){
    			Element KVPairElement = (Element) KVPairList.item(i);
    			String key = KVPairElement.getElementsByTagName("Key").item(0).getTextContent();
    			String value = KVPairElement.getElementsByTagName("Value").item(0).getTextContent();
    			this.store.put(key, value);
    		}
    	} catch(Exception e){
    		throw new Exception(ERROR_INVALID_FORMAT);
    	}
    }
}
