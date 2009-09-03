package memsim;

/**
 *
 * @author sound
 */
public class Simulator {
    private Processor proc;

    public Simulator()
    {
       proc = new Processor();
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
