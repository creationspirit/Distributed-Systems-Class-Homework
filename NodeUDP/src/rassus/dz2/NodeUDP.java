package rassus.dz2;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicIntegerArray;

public class NodeUDP {

    private static final int NUMBER_OF_THREADS = 8;
    private static final String measurementPath = "C:\\Users\\nameless\\IdeaProjects\\NodeUDP\\res\\mjerenja.txt";
    private static final String configPath = "C:\\Users\\nameless\\IdeaProjects\\NodeUDP\\res\\net_config.txt";

    private final ArrayList<Integer> measurements;
    private final AtomicIntegerArray vectorStamp;
    private int thisVectorPos;
    private final ArrayList<NodeInfo> systemConfig;
    private final EmulatedSystemClock systemClock;
    private SimpleSimulatedDatagramSocket serverUdpSocket;
    private final ExecutorService executor;
    private final int PORT;
    private final ConcurrentMap<Integer, Datagram> unconfirmed;
    private final MeasurementStorage storage;

    public NodeUDP(int port) {
        systemClock = new EmulatedSystemClock();
        measurements = new ArrayList<>();
        systemConfig = new ArrayList<>();
        executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(measurementPath)));
             BufferedReader config_br = new BufferedReader(new InputStreamReader(
                     new FileInputStream(configPath)))) {
            String line;
            br.readLine();
            while((line = br.readLine()) != null) {
                measurements.add(Integer.parseInt(line.split(",", -1)[3]));
            }
            while((line = config_br.readLine()) != null) {
                String[] string_array = line.split(" ", -1);
                systemConfig.add(new NodeInfo(string_array[0], string_array[1], Integer.parseInt(string_array[2])));
            }
        } catch (IOException ex) {
            System.err.println("Exception caught while reading files:" + ex + "\n Exiting.");
            System.exit(-1);
        }
        PORT = port;
        unconfirmed = new ConcurrentHashMap<>();
        vectorStamp = new AtomicIntegerArray(systemConfig.size());
        for(int i = 0; i < systemConfig.size(); i++) {
            if(systemConfig.get(i).getPORT() == PORT) thisVectorPos = i;
        }
        storage = new MeasurementStorage();
    }

    public static void main(String[] args) {
        NodeUDP node = new NodeUDP(Integer.parseInt(args[0]));
        node.startup();
    }

    public void startup() {
        try(SimpleSimulatedDatagramSocket serverudpsocket = new SimpleSimulatedDatagramSocket(PORT, 0.2, 1000)) {
            this.serverUdpSocket = serverudpsocket;
            //serverUdpSocket.setSoTimeout(5000);
            executor.execute(new ClientWorker());
            executor.execute(new RetransmissionWorker());
            executor.execute(new ReportWorker());
            loop();
        } catch (SocketException se) {
            System.err.println("Exception caught while initializing server socket:" + se + "\n Exiting.");
        } finally {
            executor.shutdown();
        }
    }

    public void loop() {
        while(true) {
            byte[] recvBuf = new byte[5000];
            DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
            try {
                serverUdpSocket.receive(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }

            executor.execute(new ResponseWorker(recvBuf.clone(), packet.getAddress(), packet.getPort()));
        }
    }

    private void sendTo(Object o, String hostName, int desPort, DatagramSocket dSock) {
        try {
            InetAddress address = InetAddress.getByName(hostName);
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream(5000);
            ObjectOutputStream os = new ObjectOutputStream(new BufferedOutputStream(byteStream));
            os.flush();
            os.writeObject(o);
            os.flush();
            byte[] sendBuf = byteStream.toByteArray();
            DatagramPacket packet = new DatagramPacket(sendBuf, sendBuf.length, address, desPort);
            dSock.send(packet);
            os.close();
        } catch (UnknownHostException e) {
            System.err.println("Unknown hostname" + e);
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ResponseWorker implements Runnable {

        private byte[] dp;

        public ResponseWorker(byte[] dp, InetAddress responseAddr, int responsePort) {
            this.dp = dp;
        }

        @Override
        public void run() {
            try {
                ByteArrayInputStream byteStream = new ByteArrayInputStream(dp);
                ObjectInputStream is = new ObjectInputStream(new BufferedInputStream(byteStream));
                Datagram datagram = (Datagram) is.readObject();
                is.close();
                if(!datagram.isConfirmation()) {
                    //System.out.println("Recieved measurement " + datagram.getHashCode());
                    if(!storage.checkIfStored(datagram.getHashCode())) {
                        storage.storeMeasurement(datagram);
                    }
                    vectorStamp.incrementAndGet(thisVectorPos);
                    synchronized(vectorStamp) {
                        for(int i = 0; i < vectorStamp.length(); i++) {
                            if(i != thisVectorPos && vectorStamp.get(i) < datagram.getVectorStamp().get(i)) {
                                vectorStamp.incrementAndGet(i);
                            }
                        }
                    }
                    SimpleSimulatedDatagramSocket confirmationSocket = new SimpleSimulatedDatagramSocket(0.2, 1000);
                    //System.out.println("Trying to send confirmation to: " + responseAddr.getHostName() + " " + responsePort);
                    sendTo(new Datagram(true, datagram.getHashCode()), datagram.getResponseHostName(), datagram.getResponsePORT(), confirmationSocket);
                    //System.out.println("CONFIRMATION SENT for measurement: " + datagram.getHashCode());
                } else {
                    //System.out.println("Confirmation received for "  + datagram.getHashCode());
                    Datagram dg = unconfirmed.remove(datagram.getHashCode());
                    //if(dg != null) System.out.println("Removed entry from Map: " + dg.getHashCode());
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                System.err.println("Class not found:" + e + "\n Exiting.");
            }
        }
    }

    private class ClientWorker implements Runnable {

        @Override
        public void run() {
            Datagram d;
            try(SimpleSimulatedDatagramSocket clientUdpSocket = new SimpleSimulatedDatagramSocket(0.2, 1000)) {
                while(true) {
                    Thread.sleep(1000);
                    int value = takeMeasurement();
                    for(NodeInfo ni : systemConfig) {
                        if(ni.getPORT() != PORT) {
                            vectorStamp.incrementAndGet(thisVectorPos);
                            d = new Datagram(value, ni.getHostName(), ni.getPORT(),
                                    systemConfig.get(thisVectorPos).hostName, systemConfig.get(thisVectorPos).PORT, vectorStamp, systemClock.currentTimeMillis(),
                                    systemConfig.get(thisVectorPos).name);
                            System.out.println("Mjerenje: " + value + " Vektor: " + vectorStamp.toString());
                            sendTo(d, ni.getHostName(), ni.getPORT(), clientUdpSocket);
                            //System.out.println("Mesasurement sent to " + ni.getName() + ": " + d.getMeasurement() + " " + d.getHashCode());
                            unconfirmed.put(d.hashCode(), d);
                        } else {
                            d = new Datagram(value, ni.getHostName(), ni.getPORT(),
                                    systemConfig.get(thisVectorPos).hostName, systemConfig.get(thisVectorPos).PORT, vectorStamp, systemClock.currentTimeMillis(),
                                    systemConfig.get(thisVectorPos).name);
                            storage.storeMeasurement(d);
                        }
                    }
                }
            } catch (SocketException e) {
                System.err.println("Exception caught while initializing client socket:" + e + "\n Exiting.");
            } catch (InterruptedException e) {
                System.err.println("Thread sleep interupted:" + e + "\n Exiting.");
            }
        }

        private int takeMeasurement() {
            int rowNumber = (int)(systemClock.currentTimeMillis() / 1000L) % 100;
            return measurements.get(rowNumber);
        }
    }

    private class RetransmissionWorker implements Runnable {

        @Override
        public void run() {
            try(SimpleSimulatedDatagramSocket retransmitUdpSocket = new SimpleSimulatedDatagramSocket(0.2, 1000)) {
                while(true) {
                    Thread.sleep(1100);
                    //System.out.println("Retransmiting unconfirmed packets");
                    if(!unconfirmed.isEmpty()) {
                        for (Datagram d : unconfirmed.values()) {
                            sendTo(d, d.getHostName(), d.getPORT(), retransmitUdpSocket);
                            //System.out.println("Retransmited measurement " + d.getHashCode());
                        }
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
    }

    private class ReportWorker implements Runnable {

        @Override
        public void run() {
            try {
                while(true) {
                    Thread.sleep(5000);
                    System.out.println(storage.getReport());
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private class NodeInfo {
        private final String name;
        private final String hostName;
        private final int PORT;

        public NodeInfo(String name, String hostName, int PORT) {
            this.name = name;
            this.hostName = hostName;
            this.PORT = PORT;
        }

        public String getName() {
            return name;
        }

        public String getHostName() {
            return hostName;
        }

        public int getPORT() {
            return PORT;
        }
    }

}
