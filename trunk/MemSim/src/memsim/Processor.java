/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package memsim;

/**
 *
 * @author rdeva
 */
public abstract class Processor implements Runnable {
    public abstract void setCores(int cores);
    public abstract int getCores();

}
