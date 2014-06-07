package kvstore;

import java.io.IOException;
import java.net.InetAddress;

import org.junit.*;

public class EndToEndTemplate {

    KVClient client;
	KVClient another_client;
    ServerRunner serverRunner;

    @Before
    public void setUp() throws IOException, InterruptedException {
        String hostname = InetAddress.getLocalHost().getHostAddress();

        SocketServer ss = new SocketServer(hostname, 8080);
        KVServer kvs = new KVServer(100, 10);
        ServerClientHandler sch = new ServerClientHandler(kvs, 2); 
        ss.addHandler(sch);
        
        serverRunner = new ServerRunner(ss, "server");
        serverRunner.start();

        client = new KVClient(hostname, 8080);
        another_client = new KVClient(hostname, 8080);
    }

    @After
    public void tearDown() throws InterruptedException {
        serverRunner.stop();
    }

}
