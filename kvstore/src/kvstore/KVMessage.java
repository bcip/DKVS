package kvstore;

import static kvstore.KVConstants.*;

import java.io.*;
import java.net.*;

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
 * This is the object that is used to generate the XML based messages for
 * communication between clients and servers.
 */
public class KVMessage implements Serializable {

	private String msgType;
	private String key;
	private String value;
	private String message;

	public static final long serialVersionUID = 6473128480951955693L;

	/**
	 * Construct KVMessage with only a type.
	 * 
	 * @param msgType
	 *            the type of this KVMessage
	 */
	public KVMessage(String msgType) {
		this(msgType, null);
	}

	/**
	 * Construct KVMessage with type and message.
	 * 
	 * @param msgType
	 *            the type of this KVMessage
	 * @param message
	 *            the content of this KVMessage
	 */
	public KVMessage(String msgType, String message) {
		this.msgType = msgType;
		this.message = message;
	}

	/**
	 * Construct KVMessage from the InputStream of a socket. Parse XML from the
	 * InputStream with unlimited timeout.
	 * 
	 * @param sock
	 *            Socket to receive serialized KVMessage through
	 * @throws KVException
	 *             if we fail to create a valid KVMessage. Please see
	 *             KVConstants.java for possible KVException messages.
	 */
	public KVMessage(Socket sock) throws KVException {
		this(sock, 0);
	}

	/**
	 * Construct KVMessage from the InputStream of a socket. This constructor
	 * parses XML from the InputStream within a certain timeout or with an
	 * unlimited timeout if the provided argument is 0.
	 * 
	 * @param sock
	 *            Socket to receive serialized KVMessage through
	 * @param timeout
	 *            total allowable receipt time, in milliseconds
	 * @throws KVException
	 *             if we fail to create a valid KVMessage. Please see
	 *             KVConstants.java for possible KVException messages.
	 */
	public KVMessage(Socket sock, int timeout) throws KVException {
		// implement me
		try {
			this.message = this.key = this.value = this.msgType = null;
			DocumentBuilder builder = DocumentBuilderFactory.newInstance()
					.newDocumentBuilder();
			sock.setSoTimeout(timeout);
			NoCloseInputStream ncis = new NoCloseInputStream(
					sock.getInputStream());
			Document doc = builder.parse(ncis);
			Node KVMessage = doc.getElementsByTagName("KVMessage").item(0);
			if (KVMessage == null) {
				throw new KVException(new KVMessage(KVConstants.RESP,
						KVConstants.ERROR_INVALID_FORMAT));
			}
			Element KVElement = (Element) KVMessage;
			this.msgType = KVElement.getAttribute("type");
			Node Key = KVElement.getElementsByTagName("Key").item(0);
			Node Value = KVElement.getElementsByTagName("Value").item(0);
			Node Message = KVElement.getElementsByTagName("Message").item(0);
			if (Key != null) {
				this.key = Key.getTextContent();
			}
			if (Value != null) {
				this.value = Value.getTextContent();
			}
			if (Message != null) {
				this.message = Message.getTextContent();
			}
			 
			if (this.msgType.equals(KVConstants.PUT_REQ)) {
				if (Key == null || Value == null || Message != null) {
					throw new KVException(new KVMessage(KVConstants.RESP,
							KVConstants.ERROR_INVALID_FORMAT));
				}
			} else if (this.msgType.equals(KVConstants.GET_REQ)) {
				if (Key == null || Value != null || Message != null) {
					throw new KVException(new KVMessage(KVConstants.RESP,
							KVConstants.ERROR_INVALID_FORMAT));
				}
			} else if (this.msgType.equals(KVConstants.DEL_REQ)) {
				if (Key == null || Value != null || Message != null) {
					throw new KVException(new KVMessage(KVConstants.RESP,
							KVConstants.ERROR_INVALID_FORMAT));
				}
			} else if (this.msgType.equals(KVConstants.RESP)) {
				if (!((Key != null && Value != null && Message == null) || (Key == null
						&& Value == null && Message != null))) {
					throw new KVException(new KVMessage(KVConstants.RESP,
							KVConstants.ERROR_INVALID_FORMAT));
				}
			} else if (this.msgType.equals(KVConstants.REGISTER)) {
				if (Key != null || Value != null || Message == null) {
					throw new KVException(KVConstants.ERROR_INVALID_FORMAT);
				}
			} else if (this.msgType.equals(KVConstants.READY)) {
				if (Key != null || Value != null || Message != null) {
					throw new KVException(KVConstants.ERROR_INVALID_FORMAT);
				}
			} else if (this.msgType.equals(KVConstants.ABORT)) {
				if (Key != null || Value != null) {
					throw new KVException(KVConstants.ERROR_INVALID_FORMAT);
				}
			} else if (this.msgType.equals(KVConstants.COMMIT)) {
				if (Key != null || Value != null || Message != null) {
					throw new KVException(KVConstants.ERROR_INVALID_FORMAT);
				}
			} else if (this.msgType.equals(KVConstants.ACK)) {
				if (Key != null || Value != null || Message != null) {
					throw new KVException(KVConstants.ERROR_INVALID_FORMAT);
				}
			} else {
				throw new KVException(new KVMessage(KVConstants.RESP,
						KVConstants.ERROR_INVALID_FORMAT));
			}
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			KVMessage exceptMessage = new KVMessage(KVConstants.RESP,
					KVConstants.ERROR_PARSER);
			throw new KVException(exceptMessage);
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			KVMessage exceptMessage = new KVMessage(KVConstants.RESP,
					KVConstants.ERROR_PARSER);
			throw new KVException(exceptMessage);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			KVMessage exceptMessage = new KVMessage(KVConstants.RESP,
					KVConstants.ERROR_COULD_NOT_RECEIVE_DATA);
			throw new KVException(exceptMessage);
		}

	}

	/**
	 * Constructs a KVMessage by copying another KVMessage.
	 * 
	 * @param kvm
	 *            KVMessage with fields to copy
	 */
	public KVMessage(KVMessage kvm) {
		// implement me
		this.msgType = kvm.msgType;
		this.key = kvm.key;
		this.value = kvm.value;
		this.message = kvm.message;
	}

	/**
	 * Generate the serialized XML representation for this message. See the spec
	 * for details on the expected output format.
	 * 
	 * @return the XML string representation of this KVMessage
	 * @throws KVException
	 *             with ERROR_INVALID_FORMAT or ERROR_PARSER
	 */
	public String toXML() throws KVException {
		// implement me
		try {
			if (this.msgType == null) {
				throw new KVException(new KVMessage(KVConstants.RESP,
						KVConstants.ERROR_INVALID_FORMAT));
			}

			boolean shouldKey = false;
			boolean shouldValue = false;
			boolean shouldMessage = false;

			if (this.msgType.equals(KVConstants.PUT_REQ)) {
				shouldKey = true;
				shouldValue = true;
			} else if (this.msgType.equals(KVConstants.GET_REQ)) {
				shouldKey = true;
			} else if (this.msgType.equals(KVConstants.DEL_REQ)) {
				shouldKey = true;
			} else if (this.msgType.equals(KVConstants.RESP)) {
				if (this.message != null)
					shouldMessage = true;
				else {
					shouldValue = true;
					shouldKey = true;
				}
			} else if (this.msgType.equals(KVConstants.REGISTER)) {
				shouldMessage = true;
			} else if (this.msgType.equals(KVConstants.ABORT)) {
				if (this.message != null) {
					shouldMessage = true;
				}
			} else {
				if (!(this.msgType.equals(KVConstants.READY)
						|| this.msgType.equals(KVConstants.COMMIT) 
						|| this.msgType.equals(KVConstants.ACK))) {
					throw new KVException(new KVMessage(KVConstants.RESP,
							KVConstants.ERROR_INVALID_FORMAT));
				}
			}

			if (shouldKey) {
				if (this.key == null || this.key.length() == 0) {
					throw new KVException(new KVMessage(KVConstants.RESP,
							KVConstants.ERROR_INVALID_FORMAT));
				}
			}
			if (shouldValue) {
				if (this.value == null || this.value.length() == 0) {
					throw new KVException(new KVMessage(KVConstants.RESP,
							KVConstants.ERROR_INVALID_FORMAT));
				}
			}
			if (shouldMessage) {
				if (this.message == null || this.message.length() == 0) {
					throw new KVException(new KVMessage(KVConstants.RESP,
							KVConstants.ERROR_INVALID_FORMAT));
				}
			}
			
			DocumentBuilder builder = DocumentBuilderFactory.newInstance()
					.newDocumentBuilder();
			Document doc = builder.newDocument();
			Element KVMessage = doc.createElement("KVMessage");
			doc.appendChild(KVMessage);

			KVMessage.setAttribute("type", this.msgType);

			if (shouldKey) {
				Element Key = doc.createElement("Key");
				KVMessage.appendChild(Key);
				Key.appendChild(doc.createTextNode(this.key));
			}
			if (shouldValue) {
				Element Value = doc.createElement("Value");
				KVMessage.appendChild(Value);
				Value.appendChild(doc.createTextNode(this.value));
			}
			if (shouldMessage) {
				Element Message = doc.createElement("Message");
				KVMessage.appendChild(Message);
				Message.appendChild(doc.createTextNode(this.message));
			}
			
			return printDoc(doc);
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			throw new KVException(new KVMessage(KVConstants.RESP,
					KVConstants.ERROR_PARSER));
		}

	}

	/**
	 * Send serialized version of this KVMessage over the network. You must call
	 * sock.shutdownOutput() in order to flush the OutputStream and send an EOF
	 * (so that the receiving end knows you are done sending). Do not call close
	 * on the socket. Closing a socket closes the InputStream as well as the
	 * OutputStream, preventing the receipt of a response.
	 * 
	 * @param sock
	 *            Socket to send XML through
	 * @throws KVException
	 *             with ERROR_INVALID_FORMAT, ERROR_PARSER, or
	 *             ERROR_COULD_NOT_SEND_DATA
	 */
	public void sendMessage(Socket sock) throws KVException {
		// implement me
		try {
			String outputMessage = this.toXML();
			OutputStream outstream = sock.getOutputStream();
			byte[] sendData = outputMessage.getBytes("UTF-8");
			outstream.write(sendData);
			outstream.flush();
			sock.shutdownOutput();
			System.out.println(outputMessage);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			throw new KVException(new KVMessage(KVConstants.RESP,
					KVConstants.ERROR_COULD_NOT_SEND_DATA));
		}
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getMsgType() {
		return msgType;
	}

	@Override
	public String toString() {
		try {
			return this.toXML();
		} catch (KVException e) {
			// swallow KVException
			return e.toString();
		}
	}

	/*
	 * InputStream wrapper that allows us to reuse the corresponding
	 * OutputStream of the socket to send a response. Please read about the
	 * problem and solution here:
	 * http://weblogs.java.net/blog/kohsuke/archive/2005/07/socket_xml_pitf.html
	 */
	private class NoCloseInputStream extends FilterInputStream {
		public NoCloseInputStream(InputStream in) {
			super(in);
		}

		@Override
		public void close() {
		} // ignore close
	}

	/*
	 * http://stackoverflow.com/questions/2567416/document-to-string/2567428#2567428
	 */
	public static String printDoc(Document doc) {
		try {
			StringWriter sw = new StringWriter();
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer
					.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
			transformer.setOutputProperty(OutputKeys.METHOD, "xml");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

			transformer.transform(new DOMSource(doc), new StreamResult(sw));
			return sw.toString();
		} catch (Exception ex) {
			throw new RuntimeException("Error converting to String", ex);
		}
	}

}
