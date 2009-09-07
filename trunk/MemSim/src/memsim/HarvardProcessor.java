/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package memsim;

/**
 *
 * @author sound
 */
class HarvardProcessor extends Processor {
    private java.util.ArrayList<Core> coresList;
    private int numCores = 1;
    private boolean running = false;
    private final int MEMORY_SIZE = 1024;

    public HarvardProcessor()
    {
        coresList = new java.util.ArrayList<Core>(numCores);
        coresList.add(new Core(new Memory(MEMORY_SIZE), new Bound(0,100), new Memory(MEMORY_SIZE), new Bound(0,100))); //FIXME bogus bounds
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
            for (int c = this.numCores; c <= numCores; ++c)
                coresList.add(new Core(new Memory(MEMORY_SIZE), new Bound(0,100), new Memory(MEMORY_SIZE), new Bound(0,100)));//FIXME bogus bounds
    }

    public int getCores()
    {
        return numCores;
    }

    public void run()
    {
        for (Core c : coresList)
            new Thread(c).run();
    }
}
