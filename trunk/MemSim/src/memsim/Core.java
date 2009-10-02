/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package memsim;

import memsim.exceptions.*;

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
        System.out.printf("lokking at %x\n", instruction);
        switch (op) {
            case 0x0:

                if (removeTrailingZeroes(instruction & generateMask(4,4)) == 0) {
                    if (removeTrailingZeroes(instruction & generateMask(24,24)) == 1
                        && removeTrailingZeroes(instruction & generateMask(23,23)) == 0
                        && removeTrailingZeroes(instruction & generateMask(20,20)) == 0) {

                        parseInstrExt0(instruction);
                    } else {
                        /* dataproc immediate shift */
                        System.out.println("dataprocessing immediate shift");
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
                        System.out.println("dataprocessing register shift");
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
                        System.out.println("typestatus register");
                        //return ARMV4_TypeStatusRegister;
                    } else {
                        /* undefined */
                        System.out.println("undefined shit");
                        //return ARMV4_TypeUndefined;
                    }
                } else {
                    /* dataproc immediate */
                    System.out.println("data proc immediate");
                    //return ARMV4_TypeDataProcessing;
                }
                break;
            case 0x2:
                /* load/store immediate offset */
                System.out.println("loadstore single w/ immediate offset");
                //return ARMV4_TypeLoadStoreSingle;
                break;
            case 0x3:
                if (removeTrailingZeroes(instruction & generateMask(4,4)) == 0) {
                    /* load/store register offset */
                    System.out.println("load store single w/ register offset");
                    //return ARMV4_TypeLoadStoreSingle;
                } else {
                    /* undefined */
                    System.out.println("undefined shit");
                    //return ARMV4_TypeUndefined;
                }
                break;
            case 0x4:
                /* load/store multiple */
                System.out.println("load store multiple");
                //return ARMV4_TypeLoadStoreMultiple;
                break;
            case 0x5:
                /* B and BL */
                /* ParseInstrBranch(instr); */
                System.out.println("branch instr");
                //return ARMV4_TypeBranch;
                break;
            case 0x6:
                /* co-processor load/store (LDC/STC) and DSP register transfers (undefined)*/
                System.out.println("either ldc/stc");
                //return ARMV4_TypeLoadStoreCoprocessor;
                break;
            case 0x7:
                if (removeTrailingZeroes(instruction & generateMask(24,24)) == 1) {
                    /* SWI */
                    System.out.println("software interrupt");
                    //return ARMV4_TypeSoftwareInterrupt;
                } else {
                    if (removeTrailingZeroes(instruction & generateMask(4,4)) == 1) {
                        /* co-processor register transfers (MRC/MCR) */
                        System.out.println("mrc or mcr");
                        //return ARMV4_TypeRegisterTransferCoprocessor;
                    } else {
                        /* co-processor data processing (CDP) */
                        System.out.println("cdp");
                        //return ARMV4_TypeDataProcessingCoprocessor;
                    }
                }
                break;
            default:
                //return ARMV4_TypeUndefined;
        }
        //return ARMV4_TypeUndefined;

    }

    public void parseInstrExt0(int instr) {
        /* this function depends on prior check of whether bit4 is zero */
        if (removeTrailingZeroes((instr & generateMask(24, 27))) == 0) {
            if (removeTrailingZeroes((instr & generateMask(21, 21))) == 1) {
                /* TODO: Add bits[15:12] = 1 [11:8] = 0 */
                /* msr */
                System.out.println("msr!");
                //return ARMV4_TypeStatusRegister;
            } else {
                /* TODO: Add bits[19:16] = 1 [11:8] = 0 */
                /* mrs */
                System.out.println("mrs!");
               // return ARMV4_TypeStatusRegister;
            }
        } else {
            if (removeTrailingZeroes((instr & generateMask(7, 7))) == 1) {
                /* Undefined DSP instruction */
                System.out.println("undefined shit");
                //return ARMV4_TypeUndefined;
            } else {
                System.out.println("undefined shit");
                //return ARMV4_TypeUndefined;
            }
        }
        //return ARMV4_TypeUndefined;
    }

    private void parseInstrExt1(int instr) {
        /* opcode = bit[22:21] and bit[7:4] */
        int op = (instr >> 21) & 0x3;
        switch (removeTrailingZeroes(instr & generateMask(24, 27))) {
            case 0x1:
                /* BX or (undefined) CLZ */
                if (op == 0x1) {
                    /* BX */
                    System.out.println("bx!");
                    //return ARMV4_TypeBranch;
                } else {
                    System.out.println("undefined shit (clz)");
                    //return ARMV4_TypeUndefined;
                }
                break;
            case 0x3:
                /* (undefined) BLX */
                System.out.println("undefined shit (blx)");
                //return ARMV4_TypeUndefined;
                break;
            case 0x5:
                /* (undefined) DSP add/sub */
                System.out.println("undefined shit (add/sub)");
                //return ARMV4_TypeUndefined;
                break;
            case 0x7:
                /* (undefined) BKPT */
                System.out.println("undefined shit(bkpt)");
                //return ARMV4_TypeUndefined;
                break;
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
                        System.out.println("mla!");
                        //return ARMV4_TypeMultiplication;
                    } else {
                        /* mul */
                        System.out.println("mul!");
                        //return ARMV4_TypeMultiplication;
                    }
                } else if (removeTrailingZeroes(instr & generateMask(23, 24)) == 1) {
                    switch (removeTrailingZeroes(instr & generateMask(21, 22))) {
                        case 0x0:
                            System.out.println("smull!");
                            //return ARMV4_TypeMultiplication; /* smull */
                            break;
                        case 0x1:
                            System.out.println("smlal");
                            //return ARMV4_TypeMultiplication; /* smlal */
                            break;
                        case 0x2:
                            System.out.println("umull");
                            //return ARMV4_TypeMultiplication; /* umull */
                            break;
                        case 0x3:
                            System.out.println("umlal");
                            //return ARMV4_TypeMultiplication; /* umlal */
                            break;
                    }
                } else if (((instr >> 20) & 0x1B) == 0x10) {
                    if (removeTrailingZeroes(instr & generateMask(22, 22)) == 1) {
                        /* swapb */
                        System.out.println("swapb!");
                        //return ARMV4_TypeAtomicSwap;
                    } else {
                        /* swp */
                        System.out.println("swp");
                        //return ARMV4_TypeAtomicSwap;
                    }
                }
                break;
            /* Load/store half-word ldrh/strh*/
            case 0x1:
                System.out.println("loadstore extra");
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
                        System.out.println("ldrsh! w/reg offset");
                        //return ARMV4_TypeLoadStoreExtra;
                    } else {
                        /* ldrsb */
                        System.out.println("ldrsb! w/ reg offset");
                        //return ARMV4_TypeLoadStoreExtra;
                    }
                } else if (removeTrailingZeroes(instr & generateMask(22, 22)) == 0
                        && removeTrailingZeroes(instr & generateMask(20, 20)) == 0) {
                    /* Immediate offset */
                    if (removeTrailingZeroes(instr & generateMask(5, 5)) == 0) {
                        /* ldrsh */
                        System.out.println("ldrsh@ w/ imm offset ");
                        //return ARMV4_TypeLoadStoreExtra;
                    } else {
                        /* ldrsb */
                        System.out.println("ldrsb w/ immediate");
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
