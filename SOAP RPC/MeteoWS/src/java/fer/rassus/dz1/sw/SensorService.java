/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fer.rassus.dz1.sw;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jws.WebService;
import javax.jws.WebMethod;
import javax.jws.WebParam;

/**
 *
 * @author nameless
 */
@WebService(serviceName = "SensorService")
public class SensorService {
    
    private final Map<String, Sensor> registeredSensors;
    private final BlockChain blockChain;
    ExecutorService pool;
    
    public SensorService() {
        registeredSensors = Collections.synchronizedMap(new HashMap());
        blockChain = new BlockChain();
        pool = Executors.newFixedThreadPool(1);
        pool.execute(() -> {
            while(blockChain != null) {
                try {
                    Thread.sleep(20000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(SensorService.class.getName()).log(Level.SEVERE, null, ex);
                }
                System.out.println(blockChain.stateToString(blockChain.getState(registeredSensors)));
            }
        });
        pool.shutdown();
    }

    /**
     * Web service operation
     */
    @WebMethod(operationName = "register")
    public boolean register(@WebParam(name = "username") String username, @WebParam(name = "latitude") double latitude, @WebParam(name = "longitude") double longitude, @WebParam(name = "IPaddress") String IPaddress, @WebParam(name = "port") int port) {
        if(registeredSensors.containsKey(username)) {
            return false;
        } else {
            registeredSensors.put(username, new Sensor(username, latitude, longitude, IPaddress, port));
            return true;
        }
    }


    /**
     * Web service operation
     */
    @WebMethod(operationName = "storeMeasurement")
    public boolean storeMeasurement(@WebParam(name = "username") String username, @WebParam(name = "parameter") String parameter, @WebParam(name = "averageValue") float averageValue) {
        System.out.println("Adding " + username + " measurement to the blockchain, timestamp: " + System.currentTimeMillis());
        return blockChain.append(username, parameter, averageValue);
    }

    /**
     * Web service operation
     * @return address object
     */
    @WebMethod(operationName = "searchNeighbour")
    public UserAddress searchNeighbour(@WebParam(name = "username") String username) {
        Sensor sensor = registeredSensors.get(username);
        //implement here: no neighbours case
        Sensor neighbour = null;
        double distance = Double.MAX_VALUE;
        
        for(Map.Entry<String, Sensor> entry : registeredSensors.entrySet()) {
            if(entry.getKey().equals(username)) continue;
            else {
              double d = Sensor.computeDistance(sensor, entry.getValue());
              if(d <= distance) {
                  distance = d;
                  neighbour = entry.getValue();
              }
            }
        }
        if(neighbour != null) return neighbour.getAddress();
        else return null;
    }


}
