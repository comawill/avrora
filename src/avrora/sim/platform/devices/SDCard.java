/**
 * Copyright (c) 2013-2015, TU Braunschweig All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of the University of California, Los Angeles nor the names
 * of its contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package avrora.sim.platform.devices;

import avrora.sim.mcu.Microcontroller;
import avrora.sim.mcu.SPI;
import avrora.sim.mcu.SPIDevice;
import avrora.sim.platform.memory.FlashMemory;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SD-Card simulation flash module.
 *
 * @author Sebastian Willenborg
 */
public class SDCard extends FlashMemory implements SPIDevice, Microcontroller.Pin.InputListener {
    final static int GO_IDLE_STATE = 0;
    final static int SEND_IF_COND = 8;
    final static int SEND_CSD = 9;
    final static int APP_CMD = 55;
    final static int APP_SEND_OP_COND = 41;
    final static int READ_OCR = 58;
    final static int READ_SINGLE_BLOCK = 17;
    final static int WRITE_BLOCK = 24;
    final static int SET_WR_BLOCK_ERASE_COUNT = 23;
    final static int WRITE_MULTIPLE_BLOCK = 25;

    final static int OCR = 0x80 | 0x40;

    byte[] buffer = new byte[512];
    //byte[][] storage = new byte[1024 * 30][512];
    //RandomAccessFile storage;
    public SDCard() {
        setPageSize(512);
        setNumberOfPages(1024 * 1024);
    }
    public class CSDRegister {
        final static byte CSD_STRUCTURE = 1;
        byte TAAC = 0x0E;
        byte NSAC = 0;
        byte TRAN_SPEED = 0x32;
        short CCC = 0b010110110101;
        short READ_BL_LEN = 9;
        byte READ_BL_PARTIAL = 0;
        byte WRITE_BLK_MISALIGN = 0;
        byte READ_BLK_MISALIGN = 0;
        byte DSR_IMP = 0;
        int blocks = 1024 * 30;
        int C_SIZE = blocks / 1024 - 1;
        byte ERASE_BLK_EN = 1;
        byte SECOTR_SIZE = 0x7F;
        byte WP_GRP_SIZE = 0;
        byte WP_GRP_ENABLE = 0;
        byte R2W_FACTOR = 2;
        byte WRITE_BL_LEN = 9;
        byte WRITE_BL_PARTIAL = 0;
        byte FILE_FORMAT_GRP = 0;
        byte COPY = 0;
        byte PERM_WRITE_PROTECT = 0;
        byte TMP_WRITE_PROTECT = 0;
        byte FILE_FORMAT = 0;
        byte[] data = new byte[16];


        public CSDRegister() {
            update_data();
        }

        private void update_data() {
            /*127-120*/
            data[0] = (CSD_STRUCTURE << 6) & 0xff;
            /*119-112*/
            data[1] = TAAC;
            /*111-104*/
            data[2] = NSAC;
            /*103- 96*/
            data[3] = TRAN_SPEED;
            /* 95- 88*/
            data[4] = (byte) ((CCC >> 4) & 0xff);
            /* 87- 80*/
            data[5] = (byte) (((CCC << 4) | (READ_BL_LEN & 0x0f)) & 0xff);
            /* 79- 72*/
            data[6] = (byte) ((READ_BL_PARTIAL & 0x1) << 7 | (WRITE_BLK_MISALIGN & 0x1) << 6 | (READ_BLK_MISALIGN & 0x1) << 5 | (DSR_IMP & 0x1) << 4);
            /* 71- 64*/
            data[7] = (byte) ((C_SIZE >> 16) & 0x3f);
            /* 63- 56*/
            data[8] = (byte) ((C_SIZE >> 8) & 0xff);
            /* 55- 48*/
            data[9] = (byte) (C_SIZE & 0xff);
            /* 47- 40*/
            data[10] = (byte) ((SECOTR_SIZE << 1) & 0x3f | (ERASE_BLK_EN & 0x1) << 5);
            /* 39- 32*/
            data[11] = (byte) (((SECOTR_SIZE << 7) | (WP_GRP_SIZE & 0x7f)) & 0xff);
            /* 31- 24*/
            data[12] = (byte) ((WRITE_BL_LEN >> 2) & 0x3 | (R2W_FACTOR & 0x7) << 2 | (WP_GRP_ENABLE & 0x1) << 7);
            /* 23- 16*/
            data[13] = (byte) (((WRITE_BL_PARTIAL & 0x1) << 5 | (WRITE_BL_LEN & 0x3) << 6) & 0xff);
            /* 15-  8*/
            data[14] = (byte) ((FILE_FORMAT & 0x3) << 2 | (TMP_WRITE_PROTECT & 0x1) << 4 | (PERM_WRITE_PROTECT & 0x1) << 5 | (COPY & 0x1) << 6 | (FILE_FORMAT_GRP & 0x1) << 7); //00 FILE_FORMAT 2 tmp_write_r
            byte crc = 0;// TODO: generate crc
            /*  7-  0*/
            data[15] = (byte) ((crc << 1 | 1) & 0xff);

        }

        private byte getByte(int i) {
            return data[i];
        }

    }

    CSDRegister CSD = new CSDRegister();
    public class SDSPICommand {

        int bytenr = 0;
        byte[] data = new byte[6];

        public SDSPICommand() {

        }

        void proceedByte(byte input) {
            if (isComplete()) {
                return;
            }
            data[bytenr] = input;
            bytenr++;
        }

        boolean isComplete() {

            return bytenr >= 6;
        }

        int getCommand() {
            return data[0] & 0x3f;
        }

        byte getArgument(int index) {
            return data[1 + index];
        }

        private int getAddress() {
            return (data[1] & 0xff) << 24 | (data[2] & 0xff) << 16 | (data[3] & 0xff) << 8 | (data[4] & 0xff);
        }
    }
    Microcontroller.Pin.Input CS = null;

    boolean prepare = true;
    int additional = 0;
    int indataaddr = -1;
    int blocknr;
    SDSPICommand command;
    boolean acmd = false;
    @Override
    public SPI.Frame exchange(SPI.Frame frame) {
        if (CS.read()) {
            return SPI.newFrame((byte) 0x00);
        } else if ((prepare && (frame.data & 0xc0) != 0x40)) {
            return SPI.newFrame((byte) 0xff);
        } else if (file == null) {
            return SPI.newFrame((byte) 0x00);
        } else if (prepare) {
            prepare = false;
            command = new SDSPICommand();
            additional = 0;
        }
        if (!command.isComplete()) {
            command.proceedByte(frame.data);
            return SPI.newFrame((byte) 0x00);
        }
        byte data = (byte) 0xff;
        if (!acmd) {
            switch (command.getCommand()) {
                case GO_IDLE_STATE:
                    data = 0x01;
                    prepare = true;
                    break;
                case SEND_IF_COND:
                    if (additional == 0) {
                        data = 0x01;
                    } else if (additional <= 4) {
                        data = command.getArgument(additional - 1);
                    }
                    if (additional == 4) {
                        prepare = true;
                    }
                    break;
                case APP_CMD:
                    data = 0x00;
                    acmd = true;
                    prepare = true;
                    break;
                case READ_OCR:
                    if (additional == 0) {
                        data = 0x00;
                    } else if (additional < 4) {
                        data = (byte) ((OCR >> (8 * (additional - 1))) & 0xff);
                    }
                    if (additional == 4) {
                        prepare = true;
                    }
                    break;
                case SEND_CSD:
                    if (additional == 0) {
                        data = 0x00;
                    } else if (additional == 1) {
                        data = (byte) 0xfe;
                    } else if (additional < 1 + 16) {
                        data = CSD.getByte(additional - 2);
                    } else if (additional < 1 + 16 + 2) {
                        data = (byte) 0xff;
                    }
                    if (additional == 1 + 16 + 2) {
                        prepare = true;
                    }
                    break;
                case READ_SINGLE_BLOCK:
                    if (additional == 0) {
                        data = 0x00;
                        indataaddr = -1;
                        blocknr = command.getAddress();
                        try {
                            loadPage(blocknr, buffer);
                        } catch (IOException ex) {
                            Logger.getLogger(SDCard.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    } else if (indataaddr == -1 && additional == 1) {
                        indataaddr = 0;
                        data = (byte) 0xfe;
                    } else if (indataaddr != -1) {
                        if (indataaddr < 512) {
                            data = buffer[indataaddr];
                            indataaddr++;
                        } else if (indataaddr < 512 + 2) {
                            indataaddr++;
                            data = (byte) 0xff;
                        }
                        if (indataaddr == 512 + 2) {
                            indataaddr = -1;
                            prepare = true;
                        }
                    }
                    break;
                case WRITE_BLOCK:
                    if (additional == 0) {
                        data = 0x00;
                        indataaddr = -1;
                        blocknr = command.getAddress();
                    } else if (indataaddr == -1 && (frame.data & 0xff) == 0xfe) {
                        indataaddr = 0;
                        data = 0x00;
                    } else if (indataaddr != -1) {
                        if (indataaddr < 512) {
                            buffer[indataaddr] = frame.data;
                            indataaddr++;
                            data = 0x00;
                        } else if (indataaddr < 512 + 2) {
                            try {
                                savePage(blocknr, buffer);
                            } catch (IOException ex) {
                                Logger.getLogger(SDCard.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            data = 0x00;
                            indataaddr++;
                        } else if (indataaddr == 512 + 2) {
                            indataaddr = -1;
                            data = 0b101;
                            prepare = true;
                        }
                    }
                    break;
                case WRITE_MULTIPLE_BLOCK:
                    if (additional == 0) {
                        data = 0x00;
                        blocknr = command.getAddress();
                        indataaddr = -1;
                    } else if (indataaddr == -1 && (frame.data & 0xff) == 0xfc) {
                        indataaddr = 0;
                        blocknr++;
                        data = 0x00;
                    } else if (indataaddr == -1 && (frame.data & 0xff) == 0xfd) {
                        prepare = true;
                        data = 0x00;
                    } else if (indataaddr != -1) {
                        if (indataaddr < 512) {
                            buffer[indataaddr] = frame.data;
                            indataaddr++;
                            data = 0x00;
                        } else if (indataaddr < 512 + 2) {
                            data = 0x00;
                            try {
                                savePage(blocknr, buffer);
                            } catch (IOException ex) {
                                Logger.getLogger(SDCard.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            indataaddr++;
                        } else if (indataaddr == 512 + 2) {
                            indataaddr = -1;
                            blocknr++;
                            data = 0b101;
                        }
                    }
                    break;
                default:
                    data = (byte) 0x01;
                    prepare = false;
            }
        } else {
            switch (command.getCommand()) {
                case APP_SEND_OP_COND:
                    prepare = true;
                    acmd = false;
                    data = 0x00;
                    break;
                case SET_WR_BLOCK_ERASE_COUNT:
                    data = 0;
                    prepare = true;
                    acmd = false;
                    break;
                default:
                    data = (byte) 0x01;
                    prepare = true;
                    acmd = false;
            }
        }
        additional++;

        // Parse opcode and address stuff

        return SPI.newFrame(data);
    }

    @Override
    public void connect(SPIDevice d) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void connectCS(Microcontroller.Pin.Input cs) {
        CS = cs;
        CS.registerListener(this);
    }

    @Override
    public void onInputChanged(Microcontroller.Pin.Input input, boolean csValue) {
    }

    @Override
    public int[] getAvailablePageSizes() {
        return new int[]{512};
    }

    @Override
    public int[] getAvailablePageNumbers() {
        return new int[]{1024, 1024 * 1024, 1024 * 2 * 1024};
    }

}