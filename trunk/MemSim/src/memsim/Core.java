
package memsim;

import memsim.exceptions.*;
import java.math.BigInteger;

/**
 *
 * @author rdeva
 */
public class Core implements Runnable {

    /**
     *
    Register        Description
    R0              Argument1, Return Value, Temporary register
    R1              Argument2, Second 32-bits if double/int Return Value, Temporary register
    R2-R3           Arguments, Temporary registers
    R4-R10          Permenant registers. R7 is THUMB frame pointer
    R11             ARM frame pointer. Permanent register
    R12             Temporary register
    R13             Stack pointer. Permanent register
    R14             Link register. Permanent register
    R15             Program Counter
     */
    final static int NUM_REGISTERS = 16;
    private int registers[] = new int[NUM_REGISTERS];
    private int cpsrRegister = 0;
    private final int N_MASK = 1 << 31, //masks to helpout when fiddling w/ CPSR reg
                      Z_MASK = 1 << 30,
                      C_MASK = 1 << 29,
                      V_MASK = 1 << 28;
    private Memory programMem, dataMem;
    private Bound programBound, dataBound;

    public Memory getDataMemory() {
        return dataMem;
    }

    public Memory getProgramMemory() {
        return programMem;
    }

    public Core(Memory programMem, Bound programBound, Memory dataMem, Bound dataBound) {
        this.programBound = programBound;
        this.dataBound = dataBound;
        this.programMem = programMem;
        this.dataMem = dataMem;
        registers[15] = 0;
    }

    public void run() {
        for (int c = 0; c < programMem.getSize(); c += 4)
            try
            {
                exec(programMem.readWord(c, programBound));
            }
            catch (Exception e)
            {
                System.err.println("some exception occured while running a program");
                e.printStackTrace(System.err);
            }

    }

    private void exec(int instruction) throws UnknownFormatException, UnimplementedInstructionException,
            MemoryAccessException {

        int op = removeTrailingZeroes(instruction & generateMask(25, 28));
        
        switch (op) {
            case 0x0:

                if (removeTrailingZeroes(instruction & generateMask(4,4)) == 0) {
                    if (removeTrailingZeroes(instruction & generateMask(24,24)) == 1
                        && removeTrailingZeroes(instruction & generateMask(23,23)) == 0
                        && removeTrailingZeroes(instruction & generateMask(20,20)) == 0) {

                        parseInstrExt0(instruction);
                    } else {
                        /* dataproc immediate shift */

                        //return ARMV4_TypeDataProcessing;
                    }
                } else if (removeTrailingZeroes(instruction & generateMask(7,7)) == 0) {
                    if (removeTrailingZeroes(instruction & generateMask(24,24)) == 1
                        && removeTrailingZeroes(instruction & generateMask(23,23)) == 0
                        && removeTrailingZeroes(instruction & generateMask(20,20)) == 0) {
                        /* misc */
                        parseInstrExt1(instruction);
                    } else {
                        /* dataproc register shift */

                        /* ParseInstrDataProc(instr); */
                        //return ARMV4_TypeDataProcessing;
                    }
                } else if (removeTrailingZeroes(instruction & generateMask(4,4)) == 1
                            && removeTrailingZeroes(instruction & generateMask(7,7)) == 1) {
                    /* Multipliers, extra load/stores */
                    parseInstrExt2(instruction);
                }
                break;
            case 0x1:
                if (removeTrailingZeroes(instruction & generateMask(24,24)) == 1
                    && removeTrailingZeroes(instruction & generateMask(23,23)) == 0
                    && removeTrailingZeroes(instruction & generateMask(20,20)) == 0) {
                    
                    if (removeTrailingZeroes(instruction & generateMask(21,21)) == 1) {
                        /* MSR immediate */

                        //return ARMV4_TypeStatusRegister;
                    } else {
                        /* undefined */

                        //return ARMV4_TypeUndefined;
                    }
                } else {
                    /* dataproc immediate */

                    //return ARMV4_TypeDataProcessing;
                }
                break;
            case 0x2:
                /* load/store immediate offset */

                //return ARMV4_TypeLoadStoreSingle;
                break;
            case 0x3:
                if (removeTrailingZeroes(instruction & generateMask(4,4)) == 0) {
                    /* load/store register offset */

                    //return ARMV4_TypeLoadStoreSingle;
                } else {
                    /* undefined */

                    //return ARMV4_TypeUndefined;
                }
                break;
            case 0x4:
                /* load/store multiple */

                //return ARMV4_TypeLoadStoreMultiple;
                break;
            case 0x5:
                /* B and BL */
                /* ParseInstrBranch(instr); */

                //return ARMV4_TypeBranch;
                break;
            case 0x6:
                /* co-processor load/store (LDC/STC) and DSP register transfers (undefined)*/

                //return ARMV4_TypeLoadStoreCoprocessor;
                break;
            case 0x7:
                if (removeTrailingZeroes(instruction & generateMask(24,24)) == 1) {
                    /* SWI */
                    throw new UnimplementedInstructionException("SWI is not implemented");
                    //return ARMV4_TypeSoftwareInterrupt;
                } else {
                    if (removeTrailingZeroes(instruction & generateMask(4,4)) == 1) {
                        /* co-processor register transfers (MRC/MCR) */
                        throw new UnimplementedInstructionException("MRC/MCR not implemented");
                        //return ARMV4_TypeRegisterTransferCoprocessor;
                    } else {
                        /* co-processor data processing (CDP) */
                        throw new UnimplementedInstructionException("CDP is not implemented");
                        //return ARMV4_TypeDataProcessingCoprocessor;
                    }
                }
                //break;
            default:
                throw new UnknownFormatException("instruction cannot be parsed");
                //return ARMV4_TypeUndefined;
        }
        //return ARMV4_TypeUndefined;

    }

    public void parseInstrExt0(int instr) throws
            UnimplementedInstructionException, UnknownFormatException
    {
        /* this function depends on prior check of whether bit4 is zero */
        if (removeTrailingZeroes((instr & generateMask(24, 27))) == 0) {
            if (removeTrailingZeroes((instr & generateMask(21, 21))) == 1) {
                /* TODO: Add bits[15:12] = 1 [11:8] = 0 */
                /* msr */
                throw new UnimplementedInstructionException("msr is not implemented");
                //return ARMV4_TypeStatusRegister;
            } else {
                /* TODO: Add bits[19:16] = 1 [11:8] = 0 */
                /* mrs */
                throw new UnimplementedInstructionException("mrs is not implemented");
               // return ARMV4_TypeStatusRegister;
            }
        } else {
            if (removeTrailingZeroes((instr & generateMask(7, 7))) == 1) {
                /* Undefined DSP instruction */
                throw new UnknownFormatException("instruction cannot be parsed");
                //return ARMV4_TypeUndefined;
            } else {
                throw new UnknownFormatException("instruction cannot be parsed");
                //return ARMV4_TypeUndefined;
            }
        }
        //return ARMV4_TypeUndefined;
    }

    private void parseInstrExt1(int instr) throws 
            UnknownFormatException, UnimplementedInstructionException {
        /* opcode = bit[22:21] and bit[7:4] */
        int op = (instr >> 21) & 0x3;
        switch (removeTrailingZeroes(instr & generateMask(24, 27))) {
            case 0x1:
                /* BX or (undefined) CLZ */
                if (op == 0x1) {
                    /* BX */
                    int rd = removeTrailingZeroes(instr & generateMask(0, 4));
                    registers[14] = registers[15] + 4; //link register := PC + 4
                    registers[15] = registers[rd]; //PC := Rn
                    //return ARMV4_TypeBranch;
                } else {
                    throw new UnknownFormatException("instruction cannot be parsed");
                    //return ARMV4_TypeUndefined;
                }
                break;
            case 0x3:
                /* (undefined) BLX */
                throw new UnimplementedInstructionException("BLX has not been implemented");
                //return ARMV4_TypeUndefined;
                //break;
            case 0x5:
                /* (undefined) DSP add/sub */
                throw new UnimplementedInstructionException("DSP Add/Sub has not been implemented");
                //return ARMV4_TypeUndefined;
                //break;
            case 0x7:
                /* (undefined) BKPT */
                throw new UnimplementedInstructionException("BKPT has not been implemented");
                //return ARMV4_TypeUndefined;
                //break;
            default:
                //return ARMV4_TypeUndefined;
        }
        /* make the compiler shut up */
        //return ARMV4_TypeUndefined;
    }


    private void parseInstrExt2(int instr) {
        /* MUL/MLA,SMLAL,SMULL,UMLAL,UMULL, STRH/LDRH, LDRSB, LDRH and two undefined*/
        int op = (instr >> 5) & 0x3;
        switch (op) {
            /* Multiply, multiply long, swap */
            case 0x0:
                if (removeTrailingZeroes(instr & generateMask(22, 25)) == 0) {
                    if (removeTrailingZeroes(instr & generateMask(21, 21)) == 1) {
                        /* mla */
                        boolean updateStatus = 
                                removeTrailingZeroes(instr & generateMask(20,20)) == 1;
                        int rd = removeTrailingZeroes(instr & generateMask(16, 19));
                        int rn = removeTrailingZeroes(instr & generateMask(12, 15));
                        int rs = removeTrailingZeroes(instr & generateMask(8, 11));
                        int rm = removeTrailingZeroes(instr & generateMask(0, 3));

                        //to take care of overflows, do math in BigInt and mask
                        //down to 32 bits if necessary

                        BigInteger rmBig = new BigInteger(Integer.toString(registers[rm])),
                                rsBig = new BigInteger(Integer.toString(registers[rs])),
                                rnBig = new BigInteger(Integer.toString(registers[rn])),
                                rdBig;

                        rdBig = rnBig.add(rmBig.multiply(rsBig));
                        //can it fit in 32 bits?
                        if (rdBig.compareTo(new BigInteger(((1 << 32) - 1) + "")) <= 0)
                        {
                            registers[rd] = rdBig.intValue();
                        }
                        else
                        {
                            //isolate the lower 32 bits, discard anything else
                            registers[rd] =
                                    rdBig.and(new BigInteger(((1 << 32) - 1) + "")).intValue();
                        }

                        //update the status registers if need be
                        if (updateStatus)
                        {
                            //note in ARMv4 and under, C flag was unpredictable,
                            //but in ARMv5 and up, C flag was uneffected.
                            //simulator assumes v5+
                            if (registers[rd] < 0) //update N flag
                                cpsrRegister |= N_MASK;

                            if (registers[rd] == 0)
                                cpsrRegister |= Z_MASK;

                        }
                            
                        //return ARMV4_TypeMultiplication;
                    } else {
                        /* mul */

                        //return ARMV4_TypeMultiplication;
                    }
                } else if (removeTrailingZeroes(instr & generateMask(23, 24)) == 1) {
                    switch (removeTrailingZeroes(instr & generateMask(21, 22))) {
                        case 0x0:

                            //return ARMV4_TypeMultiplication; /* smull */
                            break;
                        case 0x1:

                            //return ARMV4_TypeMultiplication; /* smlal */
                            break;
                        case 0x2:

                            //return ARMV4_TypeMultiplication; /* umull */
                            break;
                        case 0x3:

                            //return ARMV4_TypeMultiplication; /* umlal */
                            break;
                    }
                } else if (((instr >> 20) & 0x1B) == 0x10) {
                    if (removeTrailingZeroes(instr & generateMask(22, 22)) == 1) {
                        /* swapb */

                        //return ARMV4_TypeAtomicSwap;
                    } else {
                        /* swp */

                        //return ARMV4_TypeAtomicSwap;
                    }
                }
                break;
            /* Load/store half-word ldrh/strh*/
            case 0x1:

                //return ARMV4_TypeLoadStoreExtra;
                break;
            /* Load signed half-word */
            case 0x2:
            case 0x3:
                /* TODO: check for L = 0 && S = 1 which is unpredictable */
                if (removeTrailingZeroes(instr & generateMask(22, 22)) == 0
                    && removeTrailingZeroes(instr & generateMask(20, 20)) == 1) {
                    /* Register offset */
                    if (removeTrailingZeroes(instr & generateMask(5, 5)) == 1) {
                        /* ldrsh */

                        //return ARMV4_TypeLoadStoreExtra;
                    } else {
                        /* ldrsb */

                        //return ARMV4_TypeLoadStoreExtra;
                    }
                } else if (removeTrailingZeroes(instr & generateMask(22, 22)) == 0
                        && removeTrailingZeroes(instr & generateMask(20, 20)) == 0) {
                    /* Immediate offset */
                    if (removeTrailingZeroes(instr & generateMask(5, 5)) == 0) {
                        /* ldrsh */

                        //return ARMV4_TypeLoadStoreExtra;
                    } else {
                        /* ldrsb */

                        //return ARMV4_TypeLoadStoreExtra;
                    }
                } else {
                    //return ARMV4_TypeUndefined;
                }
                break;
            default:
                //return ARMV4_TypeUndefined;
        }
        //return ARMV4_TypeUndefined; /* Make the compiler shut up */
    }


    private int generateMask(int from_bit, int to_bit) {
        if (from_bit < 0 || to_bit > 31 || from_bit > to_bit) {
            throw new IllegalArgumentException("this must be true: from_bit >= 0 and to_bit <= 31 and from_bit <= to_bit");
        }

        return ((2 << (to_bit - from_bit)) - 1) << from_bit;
    }

    private int removeTrailingZeroes(int x) {
        int guard = 0;
        while ((x & 1) == 0) {
            x = x >> 1;
            ++guard;

            if (guard > 32) //if x == 0 is passed in, then this might be infinite loop
            {
                break;
            }
        }

        return x;

    }
}
