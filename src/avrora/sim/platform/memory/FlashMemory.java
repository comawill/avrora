/*
 * Copyright (c) 2015, TU Braunschweig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the Institute nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
package avrora.sim.platform.memory;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * The <code>FlashMemory</code> class represents a flash Memory device that is
 * able to load and save its content from/into a file
 *
 * @author Sebastian Willenborg
 */
public abstract class FlashMemory {

    protected int pagesize;
    protected int pages;

    protected RandomAccessFile file = null;

    public int getPageSize() {
        return pagesize;
    }

    public void setPageSize(int size) {
        for (int valid : getAvailablePageSizes()) {
            if (valid == size) {
                pagesize = size;
                return;
            }
        }
        throw new RuntimeException("Invalid PageSize");

    }

    public abstract int[] getAvailablePageSizes();

    public int getNumberOfPages() {
        return pages;
    }

    public void setNumberOfPages(int pages) {
        for (int valid : getAvailablePageNumbers()) {
            if (valid == pages) {
                this.pages = pages;
                return;
            }
        }
        throw new RuntimeException("Invalid PageSize");
    }

    public abstract int[] getAvailablePageNumbers();

    /**
     * Save into a stream
     *
     * @param destination Stream to save into
     * @throws java.io.IOException
     */
    public void setOutputStream(RandomAccessFile destination) throws IOException {
        file = destination;
    }

    public void loadPage(int page, byte[] buffer) throws IOException {
        if (file == null || page >= pages) {
            return;
        }
        if (file.length() < (page + 1) * pagesize) {
            file.setLength((page + 1) * pagesize);
        }

        file.seek(page * pagesize);
        file.read(buffer, 0, pagesize);

    }

    public void savePage(int page, byte[] buffer) throws IOException {
        if (file == null || page >= pages) {
            return;
        }
        if (file.length() < (page + 1) * pagesize) {
            file.setLength((page + 1) * pagesize);
        }
        file.seek(page * pagesize);
        file.write(buffer, 0, pagesize);
    }

}
