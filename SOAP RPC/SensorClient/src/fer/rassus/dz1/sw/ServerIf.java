/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fer.rassus.dz1.sw;

/**
 *
 * @author nameless
 */
public interface ServerIf {
    
    //Server startup. Starts all services offered by the server.
    public void startup();
    
    //Server loops while in running mode. Server must be active to accept client requests.
    public void loop();
    
    //Server shutdown. Shuts down all services started during startup. 
    public void shutdown();
    
    //Gets the running flag that indicates server running status.
    // @return running flag
    public boolean getRunningFlag();
    
    //Sets the running flag that indicates server running status.
    // @param flag running flag
    public void setRunningFlag(boolean flag);
    
}
