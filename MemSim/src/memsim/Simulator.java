package memsim;

/**
 *
 * @author rdeva
 */
public class Simulator {
    private Processor proc;

    public Simulator()
    {
       this(1);
    }

    public Simulator(int numCores)
    {
        proc = ProcessorFactory.createProcessor(ProcessorFactory.MemoryArchitecture.HARVARD);
        proc.setCores(numCores);
    }

    public void start()
    {
        proc.run();
    }

    public void stop()
    {
        //TODO implement this
    }
}
