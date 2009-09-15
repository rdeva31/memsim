/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package memsim.exceptions;

/**
 *
 * @author sound
 */
public class UnknownFormatException extends Exception {

    public UnknownFormatException()
    {
        super();
    }

    public UnknownFormatException(String msg)
    {
        super(msg);
    }

    public UnknownFormatException(Throwable cause)
    {
        super(cause);
    }

    public UnknownFormatException(String msg, Throwable cause)
    {
        super(msg, cause);
    }
}
