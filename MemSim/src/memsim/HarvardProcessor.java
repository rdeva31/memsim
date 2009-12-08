package memsim;

import java.io.IOException;
import java.util.ArrayList;

/**
 *
 * @author sound
 */
class HarvardProcessor extends Processor {
    private java.util.ArrayList<Core> coresList;
    private int numCores = 1;
    private boolean running = false;
    private final int MEMORY_SIZE = 2048, MEMORY_SEGMENT_SIZE = 256;

    private Memory sharedProgramMemory = new Memory(MEMORY_SIZE),
            sharedDataMemory = new Memory(MEMORY_SIZE);
    public HarvardProcessor()
    {
        coresList = new java.util.ArrayList<Core>(numCores);
        coresList.add(new Core(sharedProgramMemory, new Bound(0,MEMORY_SEGMENT_SIZE),
                sharedDataMemory, new Bound(0,MEMORY_SEGMENT_SIZE))); //FIXME bogus bounds
    }

    public void setCores(int numCores)
    {
        if (numCores <= 0)
            throw new IllegalArgumentException("numCores must be > 0");
        else if (running)
            throw new IllegalArgumentException("Can't change the number of cores when processor is running");
        
        if (numCores < this.numCores)
            coresList.removeAll(coresList.subList(numCores, this.numCores - 1));
        else if (numCores > this.numCores)
            for (int c = this.numCores + 1; c <= numCores; ++c)
                coresList.add(new Core(sharedProgramMemory, new Bound((numCores - 1) * MEMORY_SEGMENT_SIZE
                        , numCores * MEMORY_SEGMENT_SIZE)
                        , sharedDataMemory
                        , new Bound((numCores - 1) * MEMORY_SEGMENT_SIZE, numCores * MEMORY_SEGMENT_SIZE)));//FIXME bogus bounds

        this.numCores = numCores;
    }

    public ArrayList<Core> getCoreList() {
        return coresList;
    }
    
    public int getCores()
    {
        return numCores;
    }

    public void run()
    {
        for (Core c : coresList)
            new Thread(c).start();
    }
}
