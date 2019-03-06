import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;

public class Client {

    static volatile StringBuffer stringBuffer = new StringBuffer();
    private static HashMap<SocketAddress, String> IdKey = new HashMap<>();
    //0 - author encode new message
    //1 - receiver encode message
    //2 - author decode message
    //3 - receiver decode message

    public static void main(String[] args) throws IOException {

        Runnable runnable = () -> {
            String s = "";
            do {
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                try {
                    s = reader.readLine();
                    stringBuffer.append(s);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            } while (!s.equals("end"));
        };
        Thread thread = new Thread(runnable);
        thread.start();


        InetSocketAddress socketAddress = new InetSocketAddress("localhost", 8090);
        SocketChannel socketChannel = SocketChannel.open(socketAddress);
        log("Connecting to Server on port 8090...");

        boolean resetFlag = true;
        long timestamp = 0;
        do {
            ByteBuffer byteBuffer = ByteBuffer.allocate(256);
            if (resetFlag) {
                timestamp = new Date().getTime();
                resetFlag = false;
            }
            if (stringBuffer.length() != 0) {
                String output = stringBuffer.toString();
                writeRead(byteBuffer, output, socketChannel);
            } else {
                if (new Date().getTime() - timestamp > 1000) {
                    String ping = "ping";
                    writeRead(byteBuffer, ping, socketChannel);
                    resetFlag = true;
                }
            }

        } while (true);
    }

    private static void log(String str) {
        System.out.println(str);
    }

    private static void writeRead(ByteBuffer byteBuffer, String output1, SocketChannel socketChannel) throws IOException {

        byte[] bytes;

        if (output1.equals("quit")) {
            socketChannel.close();
        }
        if (output1.equals("ping")) {
            bytes = output1.getBytes();
            byteBuffer.put(bytes);

        } else {
            String idtosend = output1.substring(0, 3);
            String message = output1.substring(3);
            IdKey.put(socketChannel.getRemoteAddress(), generateKey(message));

            String phase = "_";
            String messageToEncode = phase.concat(message);
            byte[] encode = encode(messageToEncode, IdKey.get(socketChannel.getRemoteAddress()));
            String encodedMessage = encode.toString();
            String messagetosend = idtosend.concat(encodedMessage);
            byteBuffer.put(messagetosend.getBytes());

        }
        byteBuffer.flip();
        socketChannel.write(byteBuffer);
        byteBuffer.clear();
        stringBuffer.setLength(0);

//////////////////////////////////////
        int read = socketChannel.read(byteBuffer);
        StringBuilder clientStringBuilder = new StringBuilder();
        while (read > 0) {
            byteBuffer.flip();
            while (byteBuffer.hasRemaining()) {
                clientStringBuilder.append(byteBuffer.get());
            }
            byteBuffer.clear();
            read = 0;
        }
        String IncomeMessage = clientStringBuilder.toString();
        byte[] incomeMessageBytes = IncomeMessage.getBytes();

        if (IncomeMessage.contains("you are")) {
            System.out.println(IncomeMessage);
        } else {
            if (!IncomeMessage.contains("]")) {
                System.out.println("returned :" + IncomeMessage + "}");
            } else {
                if (IncomeMessage.contains("]")) {

                } else if (incomeMessageBytes[3] == 0) {
                    byte[] encode = encode(IncomeMessage, IdKey.get(socketChannel.getRemoteAddress()));
                    socketChannel.write(byteBuffer.put(encode));
                } else if (incomeMessageBytes[3] == 1) {
                    byte[] substring = IncomeMessage.substring(3).getBytes();
                    String decode = IncomeMessage.substring(0, 3).concat(decode(substring, IdKey.get(socketChannel.getRemoteAddress()), socketChannel));
                    socketChannel.write(byteBuffer.put(decode.getBytes()));
                } else if (incomeMessageBytes[3] == 2) {
                    String decode = decode(incomeMessageBytes, IdKey.get(socketChannel.getRemoteAddress()), socketChannel);
                    System.out.println(decode);
                }
            }
        }
        byteBuffer.clear();
    }


    private static byte[] encode(String pText, String pKey) {
        byte[] txt = pText.substring(3).getBytes();
        byte[] key = pKey.getBytes();
        byte[] res = new byte[pText.length()];

        if (res[0] == 95) {
            res[0] = (byte) 0;
        }
        if (res[0] == 0) {
            res[0] = (byte) 1;
        }
        for (int i = 1; i < txt.length; i++) {
            res[i] = (byte) (txt[i] ^ key[i % key.length]);
        }
        String id = pText.substring(0, 3);
        String encodedMessage = "";
        for (int i = 0; i < res.length; i++) {
            char re = (char) res[i];
            encodedMessage += re;
        }
        String s = id.concat(encodedMessage);
        return s.getBytes();
    }


    public static String decode(byte[] pText, String pKey, SocketChannel socketChannel) throws IOException {
        byte[] res = new byte[pText.length];
        byte[] key = pKey.getBytes();

        if (pText[0] == 1) {
            pText[0] = (byte) 2;
        }
        if (pText[0] == 2) {
            pText[0] = 95;
        }
        for (int i = 1; i < pText.length; i++) {
            res[i] = (byte) (pText[i] ^ key[i % key.length]);
        }
        IdKey.remove(socketChannel.getRemoteAddress());
        return String.valueOf(pText);
    }


    private static String generateKey(String message) {
        int length = message.length();
        Random random = new Random();
        String key = "";
        for (int i = 0; i < length; i++) {
            key += random.nextInt(9);
        }
        return key;
    }

    ;
}