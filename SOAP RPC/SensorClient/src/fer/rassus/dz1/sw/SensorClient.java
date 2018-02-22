/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fer.rassus.dz1.sw;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author nameless
 */
public class SensorClient implements ServerIf {
    
    private static final float MAX_LONGITUDE = 16.0f;
    private static final float MIN_LONGITUDE = 15.87f;
    private static final float MAX_LATITUDE = 45.85f;
    private static final float MIN_LATITUDE = 45.75f;
    private final int PORT;
    private static final int NUMBER_OF_THREADS = 4;
    private static final int BACKLOG = 10;
    private final AtomicInteger activeConnections;
    private ServerSocket serverSocket;
    private final ExecutorService executor;
    private final AtomicBoolean runningFlag;
    private final ArrayList<Measurement> measurements;
    private final float longitude;
    private final float latitude;
    private final String username;
    private final long startTime;
    
    public SensorClient (String username, int port) {
        startTime = System.currentTimeMillis();
        activeConnections = new AtomicInteger(0);
        executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
        runningFlag = new AtomicBoolean(true);
        measurements = new ArrayList<>();
        //parsing txt file into list of objects representing measurements
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("/home/nameless/NetBeansProjects/SensorClient/src/fer/rassus/dz1/sw/mjerenja.txt")));) {
            String line;
            br.readLine();
            ArrayList<Integer> intlist;
            while((line = br.readLine()) != null) {
                String[] sline = line.split(",", -1);
                intlist = new ArrayList();
                for(int i = 0; i < 6; i++) { 
                    if(sline[i].equals("")) intlist.add(-1);
                    else intlist.add(Integer.parseInt(sline[i]));
                }
                measurements.add(new Measurement(intlist.get(0), intlist.get(1), intlist.get(2), intlist.get(3), intlist.get(4), intlist.get(5)));
            }
        } catch (IOException ex) {
            System.err.println("Exception caught while trying to read measurements file:" + ex + "\n Exiting.");
            System.exit(-1);
        }
        Random rand = new Random();
        longitude = (MAX_LONGITUDE - MIN_LONGITUDE) * rand.nextFloat() + MIN_LONGITUDE;
        latitude = (MAX_LATITUDE - MIN_LATITUDE) * rand.nextFloat() + MIN_LATITUDE;
        this.username = username;
        this.PORT = port;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        SensorClient sensor = new SensorClient(args[0], Integer.parseInt(args[1]));
        
        //start all required services and run loop for accepting client requests
        sensor.startup();
        //initiate shutdown when startup is finished;
        sensor.shutdown();
    }
    
    //starts all required server services
    @Override
    public void startup() {
        //create server socket, bind it to the specified port on the local host
        //and set the max queue length for client requests
        try (ServerSocket serverSocket = new ServerSocket(PORT, BACKLOG);) {
            this.serverSocket = serverSocket;
            //set timeout to avoid blocking
            serverSocket.setSoTimeout(500);
            
            //registering on the remote service
            if(!register(username, latitude, longitude, serverSocket.getInetAddress().getHostAddress(), serverSocket.getLocalPort())) {
                System.err.println("Sensor was not able to register. Will not start");
                System.exit(-1);
            } else {
                System.out.println("Sensor registered successfully");
            }
            
            System.out.println("Waiting for measurement requests");
            //starting thread for taking measurements and remote service communication
            executor.execute(new WSComunnicator(runningFlag));
            //after startup is done procced with listening for client requests
            loop();
        } catch (IOException ex) {
            System.err.println("Exception caught when opening or setting the socket:" + ex);
        } finally {
            executor.shutdown();
        }
    }
    
    //the main loop for accepting client requests
    @Override
    public void loop() {
        while(runningFlag.get()) {
            try { //create a new socket, accept and listen for a connection to be made to this socket
                
                Socket clientSocket = serverSocket.accept();
                //execute tcp request handler in a new thread
                Runnable worker = new Worker(clientSocket, runningFlag, activeConnections);
                executor.execute(worker);
                activeConnections.set(activeConnections.get() + 1);
                
            }  catch (SocketTimeoutException ste) {
                //do nothing, check runningFlag flag
            } catch (IOException ex) {
                System.err.println("Exception caught when waiting for a connection:" + ex);
            }
        }
    }

    @Override
    public void shutdown() {
        while(activeConnections.get() > 0) {
            System.out.println("WARNING: There are still active connections:" + activeConnections.get());
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {}
        }
        if(activeConnections.get() == 0) {
            System.out.println("Server shutdown");
        }
    }

    @Override
    public boolean getRunningFlag() {
        return runningFlag.get();
    }

    @Override
    public void setRunningFlag(boolean flag) {
        runningFlag.set(flag);
    }
    
    //metoda bira simulaciju mjerenja ovisno o proteklom vremenu od pokretanja senzora
    public synchronized Measurement takeMeasurement() {
        long seed = System.currentTimeMillis() - startTime;
        int rowNumber = (int)((seed) / 1000L) % 100 + 1;
        Measurement m = measurements.get(rowNumber - 1);
        System.out.println("Seconds active: " + (seed/1000) + "\tFile row: " + rowNumber + "\n" + m.toString());
        return m;
    }
    
    
    //Runnable klasa za obradu tcp zahtjeva paralelnih klijenata senzora
    private class Worker implements Runnable {
    
        private final Socket clientSocket;
        private final AtomicInteger activeConnections;

        public Worker(Socket clientSocket, AtomicBoolean isRunning, AtomicInteger activeConnections) {
            this.clientSocket = clientSocket;
            this.activeConnections = activeConnections;
        }

        @Override
        public void run() {
            try (BufferedReader inFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    ObjectOutputStream outToClient = new ObjectOutputStream(clientSocket.getOutputStream())) {

            String recievedString;    
            while((recievedString = inFromClient.readLine()) != null) {
                if(recievedString.contains("terminating")) {
                    activeConnections.set(activeConnections.get() - 1);
                    System.out.println(recievedString);
                    clientSocket.close();
                    return;
                }
                if(recievedString.contains("request")) {
                    System.out.println(recievedString);
                    Measurement measurement = takeMeasurement();
                    outToClient.writeObject(measurement);
                }
            }

            } catch (IOException e) {
                //unexpected TCP termination
                System.err.println("Exception caught when trying to read or send data:" + e);
            }
            activeConnections.set(activeConnections.get() - 1);
        }
    
    }
    
    //Runnable klasa za obavljanje mjerenja i komunikacije sa posluziteljem
    private class WSComunnicator implements Runnable {
        
        private final ExecutorService inputexecutor;
        private final AtomicBoolean isMeasuring;
        private final BufferedReader br;
        
        public WSComunnicator(AtomicBoolean isRunning) {
            this.inputexecutor = Executors.newFixedThreadPool(1);
            this.isMeasuring = new AtomicBoolean(false);
            this.br = new BufferedReader(new InputStreamReader(System.in));
        }

        @Override
        public void run() {
            while(runningFlag.get()) {
                System.out.println("Press ENTER to start the measuring process");
                try {
                    br.readLine();
                    isMeasuring.set(true);
                    System.out.println("Measuring process started");                
                } catch (IOException ex) {
                    System.err.println("Exception caught when trying to read user input:" + ex);
                    continue;        
                }
                
                inputexecutor.execute(() -> {
                    System.out.println("Press ENTER again to stop measuring");
                    try {
                        br.readLine();
                    } catch (IOException ex) {
                        System.err.println("Exception caught when trying to read user input:" + ex);
                    }
                    System.out.println("Measuring process stopped");
                    isMeasuring.set(false);
                });
                
                fer.rassus.dz1.sw.client.UserAddress address = searchNeighbour(username);
                System.out.println("Found neighbour at address: " + address.getIPaddress() + " " + address.getPort());
                try {
                    Socket clientSocket = new Socket(address.getIPaddress(), address.getPort());
                    ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream());
                    DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());
                    Measurement m;
                    Measurement nm;
                    float co;
                    while(isMeasuring.get()) {
                        //take measurement every 10 seconds
                        Thread.sleep(10000);
                        m = takeMeasurement();
                        System.out.println("Requesting measurement from neighbour");
                        dos.writeBytes("Sensor " + username + " requesting measurement\n");
                        nm = (Measurement) ois.readObject();
                        storeProcedure(m, nm);   
                    }
                    dos.writeBytes("Sensor " + username + " terminating connection\n");

                } catch (IOException ex) {
                    System.err.println("Exception caught while read or write data:" + ex);
                } catch(InterruptedException ie) {

                } catch (ClassNotFoundException ex) {
                    Logger.getLogger(SensorClient.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            System.out.println("Commencing sensor shutdown");
            inputexecutor.shutdownNow();
            runningFlag.set(false);
        }
        
        private void storeProcedure(Measurement m, Measurement nm) {
            if(m.getTemperature() != 0  && m.getTemperature() != -1) {
                if(nm.getTemperature() == 0 || nm.getTemperature() == -1) {
                    storeMeasurement(username, "Temperature", m.getTemperature());
                } else storeMeasurement(username, "Temperature", (m.getTemperature()+nm.getTemperature())/2.f);
            }
            
            if(m.getPressure() != 0  && m.getPressure() != -1) {
                if(nm.getPressure() == 0 || nm.getPressure() == -1) {
                    storeMeasurement(username, "Pressure", m.getPressure());
                } else storeMeasurement(username, "Pressure", (m.getPressure()+nm.getPressure())/2.f);
            }
            
            if(m.getHumidity() != 0  && m.getHumidity() != -1) {
                if(nm.getHumidity() == 0 || nm.getHumidity() == -1) {
                    storeMeasurement(username, "Humidity", m.getHumidity());
                } else storeMeasurement(username, "Humidity", (m.getHumidity()+nm.getHumidity())/2.f);
            }
            
            if(m.getCO() != 0  && m.getCO() != -1) {
                if(nm.getCO() == 0 || nm.getCO() == -1) {
                    storeMeasurement(username, "CO", m.getCO());
                } else storeMeasurement(username, "CO", (m.getCO()+nm.getCO())/2.f);
            }
            
            if(m.getSO2() != 0  && m.getSO2() != -1) {
                if(nm.getSO2() == 0 || nm.getSO2() == -1) {
                    storeMeasurement(username, "SO2", m.getSO2());
                } else storeMeasurement(username, "SO2", (m.getSO2()+nm.getSO2())/2.f);
            }
            
            if(m.getNO2() != 0  && m.getNO2() != -1) {
                if(nm.getNO2() == 0 || nm.getNO2() == -1) {
                    storeMeasurement(username, "NO2", m.getNO2());
                } else storeMeasurement(username, "NO2", (m.getNO2()+nm.getNO2())/2.f);
            }
        }
        
    }

    private static boolean register(java.lang.String username, double latitude, double longitude, java.lang.String iPaddress, int clientport) {
        fer.rassus.dz1.sw.client.SensorService_Service service = new fer.rassus.dz1.sw.client.SensorService_Service();
        fer.rassus.dz1.sw.client.SensorService port = service.getSensorServicePort();
        return port.register(username, latitude, longitude, iPaddress, clientport);
    }

    private static fer.rassus.dz1.sw.client.UserAddress searchNeighbour(java.lang.String username) {
        fer.rassus.dz1.sw.client.SensorService_Service service = new fer.rassus.dz1.sw.client.SensorService_Service();
        fer.rassus.dz1.sw.client.SensorService port = service.getSensorServicePort();
        return port.searchNeighbour(username);
    }

    private static boolean storeMeasurement(java.lang.String username, java.lang.String parameter, float averageValue) {
        fer.rassus.dz1.sw.client.SensorService_Service service = new fer.rassus.dz1.sw.client.SensorService_Service();
        fer.rassus.dz1.sw.client.SensorService port = service.getSensorServicePort();
        return port.storeMeasurement(username, parameter, averageValue);
    }
}
