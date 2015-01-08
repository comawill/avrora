/**
 * Copyright (c) 2013-2014, TU Braunschweig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * Neither the name of the University of California, Los Angeles nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package avrora.sim.platform.devices;

import avrora.sim.RWRegister;
import avrora.sim.mcu.Microcontroller;
import avrora.sim.mcu.Microcontroller.Pin.InputListener;
import avrora.sim.mcu.SPI;
import avrora.sim.mcu.SPIDevice;

/**
 * AT45DB flash module.
 *
 * @author Sebastian Willenborg
 */
public class AT45DB implements SPIDevice, InputListener {

    static final int OP_READ_DEVICE_ID = 0x9f;
    static final int OP_READ_STATUS_REG = 0xd7;
    static final int OP_READ_MEMORY_PAGE = 0xd2;
    static final int OP_WRITE_BUFFER1 = 0x84;
    static final int OP_WRITE_BUFFER2 = 0x87;
    static final int OP_BUFFER1_TO_PAGE_ERASE = 0x83;
    static final int OP_BUFFER2_TO_PAGE_ERASE = 0x86;
    static final int OP_CONFIG = 0x3d;


    private class StatusReg0 extends RWRegister {

        static final int RDY = 7;
        static final int COMP = 6;
        static final int DENSITY4 = 5;
        static final int DENSITY3 = 4;
        static final int DENSITY2 = 3;
        static final int DENSITY0 = 2;
        static final int PROTECT = 1;
        static final int PAGE_SIZE = 0;

        @Override
        public byte read() {
            byte result = 0;
            if (ready) {
                result |= (1 << RDY);
            }
            result |= (0b1011 << DENSITY0);
            if (page_power_2) {
                result |= (1 << PAGE_SIZE);
            }
            return result;
        }
    }

    private class StatusReg1 extends RWRegister {

        static final int RDY = 7;
        static final int EPE = 5;
        static final int SLE = 3;
        static final int PS2 = 2;
        static final int PS1 = 1;
        static final int ES = 0;

        @Override
        public byte read() {
            byte result = 0;
            if (ready) {
                result |= (1 << RDY);
            }
            return result;
        }
    }

    boolean ready = true;

    Microcontroller.Pin.Input CS = null;
    int opcode = 0x00;
    int addr0 = 0x00;
    int addr1 = 0x00;
    int addr2 = 0x00;
    int byte_nr = 0;
    int b_address;
    int p_address;
    byte buffer1[] = new byte[528];
    int b1_pointer;
    byte buffer2[] = new byte[528];
    byte block[][] = new byte[4096][528];
    int b2_pointer;
    StatusReg0 status_reg0 = new StatusReg0();
    StatusReg1 status_reg1 = new StatusReg1();
    boolean silence = false;
    /**
     * Device is configured for "power of two" binary page size (512bytes) if
     * not set 528bytes
     */
    boolean page_power_2 = false;
    public void connectCS(Microcontroller.Pin.Input cs) {
        CS = cs;
        CS.registerListener(this);
    }

    @Override
    public SPI.Frame exchange(SPI.Frame frame) {
        // Only act if chip select is low
        if (CS != null && CS.read()) {
            return SPI.newFrame((byte) 0x00);
        }

        // Parse opcode and address stuff
        byte data = 0x00;
        if (byte_nr == 0) {
            opcode = frame.data & 0xff;
            byte_nr++;
            return SPI.newFrame((byte) 0x00);
        } else if (byte_nr == 1) {
            addr0 = frame.data & 0xff;
        } else if (byte_nr == 2) {
            addr1 = frame.data & 0xff;
        } else if (byte_nr == 3) {
            addr2 = frame.data & 0xff;
            if (page_power_2) {
                p_address = addr1 >> 1 | (addr0 & 0x1f) << 7;
                b_address = addr2 | (addr2 & 0x1) << 8;
            } else {
                p_address = addr1 >> 2 | (addr0 & 0x1f) << 6;
                b_address = addr2 | (addr2 & 0x3) << 8;
            }
        }

        // Handle opcodes
        switch (opcode) {
            case OP_READ_DEVICE_ID:
                if (byte_nr == 1) {
                    data = 0x1f;
                } else if (byte_nr == 2) {
                    data = 0x26;
                } else if (byte_nr == 3) {
                    data = 0x00;
                }
                break;
            case OP_READ_STATUS_REG:
                if (byte_nr == 1) {
                    data = status_reg0.read();
                } else if (byte_nr == 2) {
                    data = status_reg1.read();
                }
                break;
            case OP_READ_MEMORY_PAGE:
                if (byte_nr > 7) {
                    data = block[p_address][b_address];
                    b_address++;
                }
                break;
            case OP_WRITE_BUFFER1:
                if (byte_nr == 3) {
                    b1_pointer = b_address;
                } else if (byte_nr > 2) {
                    buffer1[b1_pointer] = frame.data;
                    b1_pointer++;
                }
                break;
            case OP_WRITE_BUFFER2:
                if (byte_nr == 3) {
                    b2_pointer = b_address;
                } else if (byte_nr > 2) {
                    buffer2[b2_pointer] = frame.data;
                    b2_pointer++;
                }
                break;
            case OP_BUFFER1_TO_PAGE_ERASE:
                if (byte_nr == 3) {
                    if (b_address > 0) {
                        for (int i = 0; i < b_address; i++) {
                            block[p_address][i] = 0;
                        }
                    }
                    System.arraycopy(buffer1, b_address, block[p_address], b_address, 528 - b_address);
                }
                break;
            case OP_BUFFER2_TO_PAGE_ERASE:
                if (byte_nr == 3) {
                    if (b_address > 0) {
                        for (int i = 0; i < b_address; i++) {
                            block[p_address][i] = 0;
                        }
                    }
                    System.arraycopy(buffer2, b_address, block[p_address], b_address, 528 - b_address);
                }
                break;
            case OP_CONFIG:
                if (byte_nr == 3) {
                    if (addr0 == 0x2a && addr1 == 0x80) {
                        if (addr2 == 0xa7) {
                            page_power_2 = false;
                        } else if (addr2 == 0xa6) {
                            page_power_2 = true;
                        }
                    }
                }
                break;
        }

        byte_nr++;
        return SPI.newFrame(data);
    }

    @Override
    public void connect(SPIDevice d) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public void onInputChanged(Microcontroller.Pin.Input input, boolean csValue) {
        if (csValue) {
            byte_nr = 0;
        }
    }

}
