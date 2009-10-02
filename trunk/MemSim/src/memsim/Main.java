package memsim;

/**
 *
 * @author rdeva
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        Processor p = ProcessorFactory.createProcessor(ProcessorFactory.MemoryArchitecture.VON_NEUMANN);
        p.setCores(1);
        try
        {
            System.out.println("runnning");
            p.setCoreMemoryContents(0, new java.io.File("C:\\Documents and Settings\\sound\\Desktop\\b.out"), null);
            p.run();
        }
        catch (Exception e)
        {
                e.printStackTrace(System.err);
        }
    }

}
