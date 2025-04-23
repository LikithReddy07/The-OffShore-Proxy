package offshore.proxy;

import java.io.*;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OffShoreServer {

    public void start(int port) throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Proxy server started on port " + port);

        Socket clientSocket = serverSocket.accept();
        System.out.println("Client connected: " + clientSocket.getInetAddress());

        ExecutorService executor = Executors.newCachedThreadPool();
        DataInputStream clientIn = new DataInputStream(clientSocket.getInputStream());
        DataOutputStream clientOut = new DataOutputStream(clientSocket.getOutputStream());

        while (true) {
            try {
                // 1. Read one request
                ByteArrayOutputStream headerBuf = new ByteArrayOutputStream();
                int c;
                while ((c = clientIn.read()) != -1) {
                    headerBuf.write(c);
                    byte[] arr = headerBuf.toByteArray();
                    if (arr.length >= 4 &&
                            arr[arr.length-4] == '\r' &&
                            arr[arr.length-3] == '\n' &&
                            arr[arr.length-2] == '\r' &&
                            arr[arr.length-1] == '\n') {
                        break; // end of HTTP headers
                    }
                }
                if (headerBuf.size() == 0) continue;

                String header = headerBuf.toString();
                System.out.println("Received:\n" + header);

                String firstLine = header.split("\r\n")[0];
                boolean isConnect = firstLine.contains("CONNECT");

                String host;
                if (isConnect) {
                    String[] parts = firstLine.split(" ")[1].split(":");
                    host = parts[0];
                    port = Integer.parseInt(parts[1]);
                } else {
                    host = extractHost(header);
                    port = 80;
                }

                if (host == null) {
                    System.out.println("Host not found. Skipping...");
                    continue;
                }

                // 2. Connect to target server
                Socket serverSocketConn = null;
                try {
                    serverSocketConn = new Socket(host, port);
                    serverSocketConn.setTcpNoDelay(true);

                    System.out.println("Forwarding request to server: " + host + ":" + port);

                    if (isConnect) {
                        clientOut.write("HTTP/1.1 200 Connection Established\r\n\r\n".getBytes());
                        clientOut.flush();
                    } else {
                        serverSocketConn.getOutputStream().write(headerBuf.toByteArray());
                        serverSocketConn.getOutputStream().flush();
                    }

                    // 3. Pipe data in background
                    Socket finalServerSocket = serverSocketConn;
                    executor.submit(() -> {
                        try {
                            pipe(clientIn, finalServerSocket.getOutputStream());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                    pipe(serverSocketConn.getInputStream(), clientOut);

                } catch (IOException e) {
                    System.out.println("Error connecting to server: " + e.getMessage());
                } finally {
                    // Always close server connection after a request cycle
                    if (serverSocketConn != null && !serverSocketConn.isClosed()) {
                        try { serverSocketConn.close(); } catch (IOException ignored) {}
                    }
                }

            } catch (IOException e) {
                System.out.println("Error reading from client, closing: " + e.getMessage());
                break;
            }
        }

        System.out.println("Client disconnected. Closing everything...");
        clientSocket.close();
        serverSocket.close();
    }

    private static String extractHost(String headers) {
        for (String line : headers.split("\r\n")) {
            if (line.toLowerCase().startsWith("host:")) {
                return line.split(":")[1].trim();
            }
        }
        return null;
    }

    private static void pipe(InputStream in, OutputStream out) {
        byte[] buf = new byte[8192];
        int n;
        try {
            while ((n = in.read(buf)) != -1) {
                System.out.println("Received response from proxy server: " + new String(buf, 0, n));
                out.write(buf, 0, n);
                out.flush();
            }
        } catch (IOException ignored) {
        }
    }
}
