/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package memsim;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author sound
 */
public class MemoryTest {

    public MemoryTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getSize method, of class Memory.
     */
    @Test
    public void testGetSize() {
        Memory instance = null;
        try
        {
            instance = new Memory(0);
            fail("0 size is not valid");
        }
        catch (IllegalArgumentException e)
        {
            try
            {
                instance = new Memory(64);
                fail("< 128 size is not valid");
            }
            catch (IllegalArgumentException e2)
            {
            }
        }

        instance = new Memory(Memory.MIN_SIZE);
        assertTrue(instance.getSize() == Memory.MIN_SIZE);
    }


    /**
     * Test basic read/writes
     */
    @Test
    public void testBasicReadWrite()
    {
         try
         {
             Memory mem = new Memory(Memory.MIN_SIZE);
             Bound b = new Bound(0, Memory.MIN_SIZE);
             int randomData = (int)(Math.random() * 10);
             int randomMemAddr = ((int)(Math.random() * 10000)) % b.getHigherBound();
             randomMemAddr = (randomMemAddr >> 2) << 2; //make it word aligned

             //test writing words
             mem.writeWord(randomMemAddr, randomData, b);
             assertEquals(mem.readWord(randomMemAddr, b) , randomData);

             //test writing bytes
             randomMemAddr = ((int)(Math.random() * 10000)) % b.getHigherBound();
             randomData = (int)(Math.random() * 1000) % 256; //a one byte data

             mem.writeByte(randomMemAddr, (byte)randomData, b);
             assertTrue(randomData == mem.readByte(randomMemAddr, b));
         }
         catch (Exception e)
         {
             e.printStackTrace();
             fail("fault in testBasicReadWrite");
         }
    }

    /**
     * Test invalid reads and writes
     */
    @Test
    public void testInvalidReadWrite()
    {
        Memory m = new Memory(Memory.MIN_SIZE);
        Bound b = new Bound(0, Memory.MIN_SIZE);

        //write to unaligned mem addr
        try
        {
            m.writeWord(1, 1, b);
            fail("managed to write to unaligned mem addr");
        }
        catch(Exception e)
        {
        }

        //write to negative addr
        try
        {
            m.writeWord(-4, 1, b);
            fail("wrote to a negative addr");
        }
        catch(Exception e){}

        //write to negative addr
        try
        {
            m.writeByte(-4, (byte)1, b);
            fail("wrote to a negative addr");
        }
        catch(Exception e){}

        //read from negative addr
        try
        {
            m.readByte(-4, b);
            fail("read from a negative addr");
        }
        catch(Exception e){}

        try
        {
            m.readWord(-4, b);
            fail("read from a negative addr");
        }
        catch(Exception e){}

    }
    
    /**
     * Test reads and writes, but be more creative.
     * Attempting to create an array with it's index as content.
     * E.g. memory[c] = c
     */
    @Test
    public void testHardReadWrite()
    {
        try
        {
            Memory m = new Memory(Memory.MIN_SIZE);
            Bound b = new Bound(0, Memory.MIN_SIZE);
            for (int c = 0; c * 4 < b.getHigherBound(); ++c)
                m.writeWord(c * 4, c, b);

            for (int c = 0; c < b.getHigherBound(); c += 4)
            {
                int byte1 = m.readByte(c, b),
                    byte2 = m.readByte(c + 1, b),
                    byte3 = m.readByte(c + 2, b),
                    byte4 = m.readByte(c + 3, b);
                int index = c / 4;

                assertTrue(((index >> 24) & 0xff) == byte1);
                assertTrue(((index >> 16) & 0xff) == byte2);
                assertTrue(((index >> 8) & 0xff) == byte3);
                assertTrue((index & 0xff) == byte4);
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
            fail("fault in testHardReadWrite");
        }
    }
}