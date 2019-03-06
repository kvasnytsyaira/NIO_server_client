import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class Server {
    private static int count = 0;
    private static HashMap<String, String> clientsId = new HashMap<>();
    private static HashMap<String, String> clientMessage = new HashMap<>();
    static private String idToSend = "";

    @SuppressWarnings("unused")
    public static void main(String[] args) throws IOException{

        Selector selector = Selector.open();
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        InetSocketAddress addressSocket = new InetSocketAddress("localhost", 8090);
        serverSocketChannel.bind(addressSocket);
        serverSocketChannel.configureBlocking(false);
        SelectionKey selectKy = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        while (true) {

            log("i'm a server and i'm waiting for new connection and buffer select...");
            selector.select();
            Set<SelectionKey> selectorKeys = selector.selectedKeys();
            Iterator<SelectionKey> selectionKeyIterator = selectorKeys.iterator();

            while (selectionKeyIterator.hasNext()) {
                SelectionKey myKey = selectionKeyIterator.next();
                try {
                    if (myKey.isAcceptable()) {
                        SocketChannel accept = serverSocketChannel.accept();
                        accept.configureBlocking(false);
                        accept.register(selector, SelectionKey.OP_READ);
                        idToSend = getId();
                        clientsId.put(idToSend, accept.getRemoteAddress().toString());
                        clientMessage.put(idToSend, "you are " + idToSend);
                        log("Connection Accepted: " + clientsId + "\n");

                    }
                    else if (myKey.isReadable()) {
                        readFromClient(myKey, selector);
                    } else if (myKey.isWritable()) {
                        writeToClient(myKey, selector);
                    }
                }catch (Exception e){
                    myKey.channel().close(); }

                selectionKeyIterator.remove();


            }
        }
    }

    private static void writeToClient(SelectionKey myKey, Selector selector) throws IOException {
        SocketChannel socketChannel1 = (SocketChannel) myKey.channel();
        ByteBuffer byteBuffer = ByteBuffer.allocate(256);
        System.out.println("Writing to " + idToSend);
        String s = "]";
        if (!(clientsId.get(idToSend) == null) && clientsId.get(idToSend).equals(socketChannel1.getRemoteAddress().toString())) {
            s = clientMessage.get(idToSend);

            clientMessage.put(idToSend, "]");

        }

        byteBuffer.put(s.getBytes());
        byteBuffer.flip();
        socketChannel1.write(byteBuffer);
        byteBuffer.clear();

        socketChannel1.register(selector, SelectionKey.OP_READ);
    }

    private static void readFromClient(SelectionKey myKey, Selector selector) throws IOException {

        SocketChannel socketChannel1 = (SocketChannel) myKey.channel();
        ByteBuffer byteBuffer = ByteBuffer.allocate(256);
        int read = socketChannel1.read(byteBuffer);

        String result = "";
        while (read > 0) {
            byteBuffer.flip();
            while (byteBuffer.hasRemaining()) {
                result += (char) byteBuffer.get();
            }
            byteBuffer.clear();
            read = socketChannel1.read(byteBuffer);
        }
        byteBuffer.clear();
        System.out.println(result);

        if (!result.equals("ping")) {
            idToSend = result.substring(0, 3);
            clientMessage.put(idToSend, result.substring(3, result.length()));
        }
        if (result.equals("ping")) {
            if (clientMessage.get(idToSend).equals("]")) {
                clientMessage.put(idToSend, "]");
            }
        }
        log("Message received: " + clientMessage);

        if (result.equals("end")) {
            socketChannel1.close();
            log("\nIt'result time to close connection   ");
        }
        socketChannel1.register(selector, SelectionKey.OP_WRITE);


    }

    private static void log(String str) {
        System.out.println(str);
    }

    private static String getId() {
        String id = "";
        count++;
        return id + (100 + count);
    }
}