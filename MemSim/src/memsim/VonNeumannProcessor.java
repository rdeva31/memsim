/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package memsim;

/**
 *
 * @author sound
 */
public class VonNeumannProcessor extends Processor {
    private java.util.ArrayList<Core> coresList;
    private int numCores = 1;
    private boolean running = false;
    private final int MEMORY_SIZE = 1024;
    private Memory mem  = new Memory(MEMORY_SIZE);

    public VonNeumannProcessor()
    {
        Bound b = new Bound(0, 100); //FIXME bogus bounds
        coresList.add(new Core(mem, b, mem, b));
    }

    public int getCores()
    {
        return numCores;
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
                coresList.add(new Core(mem, new Bound(0, 100), mem, new Bound(0, 100)));//FIXME bogus bounds
    }

    public void run()
    {
        for (Core c : coresList)
            new Thread(c).run();
    }
}
