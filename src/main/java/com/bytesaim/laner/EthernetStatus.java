package com.bytesaim.laner;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;

public class EthernetStatus extends NetworkInterfaceStatus {

    protected String networkInterfaceIPV4Address = "";

    ArrayList<String> interfacesName = new ArrayList<>();

    public EthernetStatus(LanerListener lanerListener, int delayInSeconds) {
        super(null, lanerListener, delayInSeconds);
    }

    public EthernetStatus(int delayInSeconds) {
        super(null, delayInSeconds);
    }

    public EthernetStatus(LanerListener lanerListener) {
        super(null, lanerListener);
    }

    //if the device has more than one eth networkInterfaces up
    public void onlyCheckForInterfaceWith(String networkInterfaceIPV4Address) {
        this.networkInterfaceIPV4Address = networkInterfaceIPV4Address;
    }

    public void checkAllEthernet() {
        this.networkInterfaceIPV4Address = "";
    }

    @Override
    protected boolean isConnected() throws SocketException {
        boolean containsEth = false;
        ArrayList<NetworkInterface> networkInterfaces =  LanerNetworkInterface.getNetworkInterfacesNoLoopback();
        for (NetworkInterface networkInterface : networkInterfaces) {
            if (networkInterface.getName().startsWith("eth")) {
                if (!this.networkInterfaceIPV4Address.isEmpty()) {
                    ArrayList<InetAddress> addresses = LanerNetworkInterface.getInetAddresses(networkInterface);
                    for (InetAddress address : addresses) {
                        if (this.networkInterfaceIPV4Address.equals(address.getHostAddress())) {
                            containsEth = true;
                            break;
                        }
                    }
                } else {
                    containsEth = true;
                    break;
                }
            }
        }
        return containsEth;
    }


}
