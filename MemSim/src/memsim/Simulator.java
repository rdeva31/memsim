package memsim;

/**
 *
 * @author sound
 */
public class Simulator {
    private Processor proc;

    public Simulator()
    {
       this(1);
    }

    public Simulator(int numCores)
    {
        proc = new Processor(numCores, Processor.MemoryArchitecture.VON_NEUMANN);
    }

    public void start()
    {
        proc.run();
    }

    public void stop()
    {
        proc.stop();
    }
}
