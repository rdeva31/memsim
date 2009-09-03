/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package memsim;

/**
 *
 * @author sound
 */
public class Processor implements Runnable {
    
    public enum MemoryArchitecture {HARVARD, VON_NEUMANN};
    
    private java.util.ArrayList<Core> coresList;
    private java.util.ArrayList<Memory> memoryList;
    
    /**
     * Create a processor
     * @param numCores the number of cores that the processor is to have
     * @throws IllegalArgumentException if numCores <= 0
     */
    public Processor(int numCores, MemoryArchitecture memType) throws IllegalArgumentException
    {
        //do some validation
        if (numCores <= 0)
            throw new IllegalArgumentException("numCores must be > 0");
        else
        {
            coresList = new java.util.ArrayList<Core>(numCores);
            memoryList = new java.util.ArrayList<Memory>((memType == MemoryArchitecture.HARVARD) ? numCores : 1);
        }

        //if mem architecture is Harvard, each core needs a memory unit
        //                       von Neumann, cores share same memory unit
        if (memType == MemoryArchitecture.HARVARD)
        {
            for (int c = 0; c < numCores; ++c)
                memoryList.add(new Memory());
        }
        else if (memType == MemoryArchitecture.VON_NEUMANN)
            memoryList.add(new Memory());

        //assign each core a memory unit
        /* TODO: need a graceful way of communication between core and memory.
         * especially for von Neumann, where memory is mapped to each core.
         * Idea: Each core has a boundsBound field, which it passes to Memory
         */
        
        for (int c = 0; c < numCores; ++c)
            if (memType == MemoryArchitecture.HARVARD)
                coresList.add(new Core(memoryList.get(c)));
            else if (memType == MemoryArchitecture.VON_NEUMANN)
                coresList.add(new Core(memoryList.get(0)));

    }

    public void run()
    {
        for (Core c : coresList)
            new Thread(c).start();
    }

    public void stop()
    {
        //TODO: fill in shit
    }

}
