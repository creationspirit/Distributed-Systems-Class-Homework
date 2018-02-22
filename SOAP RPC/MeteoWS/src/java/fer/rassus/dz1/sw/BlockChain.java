/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fer.rassus.dz1.sw;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author nameless
 */
public class BlockChain {
    
    private final List<Block> chain;
    
    public BlockChain() {
        chain = Collections.synchronizedList(new ArrayList());
    }
    
    public Block peekLast() {
        if(!chain.isEmpty()) {
            return chain.get(chain.size()-1);
        }
        else return null;
    }
    
    public Block getBlock(int i) {
        return chain.get(i);
    }
    
    public boolean append(String username, String parameter, float averageValue) {
        return chain.add(new Block(username, parameter, averageValue));
    }
    
    public class Block {
        private final int previousHash;
        private final long reportTime;
        private final String username;
        private final String parameter;
        private final float value;
        
        public Block(String username, String parameter, float value) {
            this.reportTime = System.currentTimeMillis();
            Block b = peekLast();
            if(b == null) this.previousHash = -1;
            else this.previousHash = b.hashCode();
            this.username = username;
            this.parameter = parameter;
            this.value = value;        
        }

        public int getPreviousHash() {
            return previousHash;
        }

        public long getReportTime() {
            return reportTime;
        }

        public String getUsername() {
            return username;
        }

        public String getParameter() {
            return parameter;
        }

        public float getValue() {
            return value;
        }   
    }
    
    public class Measurement {
        private final String username;
        ArrayList<String> parameters;
        ArrayList<Float> values;
        
        public Measurement(String username) {
            this.username = username;
            parameters = new ArrayList();
            values = new ArrayList();
        }
        
        public boolean addParameter(String p) {
            return parameters.add(p);
        }
        
        public boolean addValue(float v) {
            return values.add(v);
        }
        
        public List<String> getParameters() {
            return parameters;
        }
        public List<Float> getValues() {
            return values;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Sensor: ").append(username).append("\n");
            for(int i = 0; i < parameters.size(); i++) {
                sb.append(parameters.get(i)).append(": ").append(values.get(i)).append("\t");
            }
            sb.append("\n");
            return sb.toString();
        }
      
    }
    
    public Set<Measurement> getState(Map<String, Sensor> registeredSensors) {
        
        Set<Measurement> state = new HashSet();
        long time;
        Block b;
        Measurement m;
        
        synchronized(chain) {
            Set<String> sensors = registeredSensors.keySet();
            for(String s: sensors) {
                time = -1;
                m = new Measurement(s);
                for(int i = chain.size()-1; i >= 0; i--) {
                    b = getBlock(i);
                    if(b.getUsername().equals(s)) {
                        if(time == -1) time = b.getReportTime();
                        if(b.getReportTime() < (time - 1000)) break;
                        m.addParameter(b.getParameter());
                        m.addValue(b.getValue());
                    }
                }
                state.add(m);
            }
        }
        return state;
    }
    
    public String stateToString(Set<Measurement> state) {
        StringBuilder sb = new StringBuilder();
        sb.append("Current global state:\n");
        state.forEach((m) -> {
            sb.append(m.toString());
        });
        return sb.toString();
    }
    
}
