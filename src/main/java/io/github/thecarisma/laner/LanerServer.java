package io.github.thecarisma.laner;

import io.github.thecarisma.util.TRunnable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class LanerServer implements Runnable, TRunnable {

    private ArrayList<LanerListener> lanerListeners = new ArrayList<>();
    ServerSocket serverSocket;
    private String ipAddress;
    private int port;
    private int backlog = 50;
    private boolean isRunning = false;

    public LanerServer(String ipAddress, int port, int backlog) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.backlog = backlog;
    }

    public LanerServer(String ipAddress, int port, int backlog, LanerListener lanerListener) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.backlog = backlog;
        this.lanerListeners.add(lanerListener);
    }

    public LanerServer(String ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public LanerServer(String ipAddress, int port, LanerListener lanerListener) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.lanerListeners.add(lanerListener);
    }

    public LanerServer(int port) throws UnknownHostException {
        this.ipAddress = LanerNetworkInterface.getIPV4Address();
        this.port = port;
    }

    public LanerServer(int port, LanerListener lanerListener) throws UnknownHostException {
        this.ipAddress = LanerNetworkInterface.getIPV4Address();
        this.port = port;
        this.lanerListeners.add(lanerListener);
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }

    public int getBacklog() {
        return backlog;
    }

    public ArrayList<LanerListener> getLanerListeners() {
        return lanerListeners;
    }

    public void addLanerListener(LanerListener lanerListener) {
        this.lanerListeners.add(lanerListener);
    }

    public void removeLanerListener(LanerListener lanerListener) {
        this.lanerListeners.remove(lanerListener);
    }

    @Override
    public void run() {
        startServer();
        try {
            while (isRunning) {
                Socket clientSocket = null;
                try {
                    clientSocket = serverSocket.accept();
                } catch (IOException e) {
                    System.err.println("Accept failed.");
                    System.exit(1);
                }
                LanerPrintWriter out = new LanerPrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String inputLine, outputLine;
                broadcastToListeners(new LanerServerStream(out, in));
                if (out.isOpen()) {
                    out.close();
                }
                try {
                    in.close();
                } catch (IOException ex){}
                if (!clientSocket.isClosed()) {
                    clientSocket.close();
                }
            }
        } catch (IOException ex) {
            //broadcastToListeners(Object o);
            ex.printStackTrace();
            try {
                stop();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void startServer() {
        try {
            serverSocket = new ServerSocket(port, backlog, InetAddress.getByName(ipAddress));
            isRunning = true;
        } catch (IOException e) {
            //broadcastToListeners(Object o);
            e.printStackTrace();
        }

    }

    public void stop() throws IOException {
        //possible send a closing byte to the server to
        //initiate immediate closing
        if (isRunning) {
            LanerNetworkInterface.isReachable(ipAddress, port, 100);
            if (serverSocket != null) {
                serverSocket.close();
                isRunning = false;
            }
        }
    }

    private void broadcastToListeners(Object o) {
        for (LanerListener lanerListener : lanerListeners) {
            lanerListener.report(o);
        }
    }

    public static class LanerServerStream {
        public PrintWriter out;
        public BufferedReader in;

        public LanerServerStream(PrintWriter out, BufferedReader in) {
            this.out = out;
            this.in = in;
        }
    }

}
