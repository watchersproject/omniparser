package io.github.thecarisma;

import java.net.InetAddress;
import java.util.*;

public class NetworkDevices implements Runnable {

    private ArrayList<LanerListener> lanerListeners = new ArrayList<>();
    private String ipAddress;
    private Map<String, NetworkDevice> networkDevices = new HashMap<>();
    final int[] ports = { 22, 25, 80, 5555, 7680  };

    public ArrayList<LanerListener> getLanerListeners() {
        return lanerListeners;
    }

    public void addLanerListener(LanerListener lanerListener) {
        this.lanerListeners.add(lanerListener);
    }

    public void removeLanerListener(LanerListener lanerListener) {
        this.lanerListeners.remove(lanerListener);
    }

    public NetworkDevices(String ipAddress, LanerListener lanerListener) {
        this.lanerListeners.add(lanerListener);
        this.ipAddress = ipAddress;
    }

    public NetworkDevices(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    @Override
    public void run() {
        try {
            final String preDeviceAddr = ipAddress.substring(0, ipAddress.lastIndexOf(".") + 1);
            int addlimit = 254;
            int cores = (Runtime.getRuntime().availableProcessors() / 2) + 50;
            final int devPerThread = addlimit / cores;
            ArrayList<Thread> threads = new ArrayList<>();
            for (int i = 0; i < cores; ++i) {
                final int finalI = i;
                final Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        for (int j = devPerThread * finalI; j < devPerThread * (finalI + 1); ++j) {
                            try {
                                InetAddress addr = InetAddress.getByName(preDeviceAddr + j);
                                NetworkDevice networkDevice ;
                                if (networkDevices.containsKey(preDeviceAddr + j)) {
                                    networkDevice = networkDevices.get(preDeviceAddr + j);
                                } else {
                                    networkDevice = new NetworkDevice(Status.UNKNOWN, addr);
                                    networkDevices.put(preDeviceAddr + j, networkDevice);
                                }
                                if (addr.isReachable(10000)) {
                                    if (networkDevice.status != Status.CONNECTED) {
                                        networkDevice.status = Status.CONNECTED;
                                        networkDevice.statusChanged = true;
                                    }
                                } else {
                                    boolean doBreak = false;
                                    for (int port : ports) {
                                        if (LanerNetworkInterface.isReachable(preDeviceAddr + j, port, 1000)) {
                                            if (networkDevice.status != Status.CONNECTED) {
                                                networkDevice.status = Status.CONNECTED;
                                                networkDevice.statusChanged = true;
                                            }
                                            doBreak = true;
                                            break;
                                        }
                                    }
                                    if (!doBreak && networkDevice.status == Status.CONNECTED) {
                                        networkDevice.status = Status.DISCONNECTED;
                                        networkDevice.statusChanged = true;
                                    }
                                }
                                if (networkDevice.status != Status.UNKNOWN && networkDevice.statusChanged) {
                                    networkDevice.statusChanged = false;
                                    broadcastToListeners(networkDevice);
                                }

                            } catch (Throwable e) {}
                        }
                    }
                });
                threads.add(t);
                t.start();
            }
            for (Thread t : threads){
                t.join();
            }
            run();
        } catch (Throwable e) {}
    }

    private void broadcastToListeners(Object o) {
        for (LanerListener lanerListener : lanerListeners) {
            lanerListener.report(o);
        }
    }

    public static enum Status {
        CONNECTED,
        DISCONNECTED,
        UNKNOWN
    }

    public static class NetworkDevice {
        public boolean statusChanged = true;
        public Status status = Status.UNKNOWN ;
        public InetAddress inetAddress;

        private NetworkDevice(Status status, InetAddress inetAddress) {
            this.status = status;
            this.inetAddress = inetAddress;
        }

        public String toString() {
            return super.toString() + "#" + status + "," + inetAddress.getHostAddress();
        }
    }

}
