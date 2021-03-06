/**
 * Copyright (c) 2004-2005, Regents of the University of California All rights
 * reserved.
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
package avrora.sim.platform.sensors;

/**
 * The <code>Sensor</code> class represents a sensor device that contains a
 * reference to the <code>SensorSource</code> instance feeding data to the
 * device.
 *
 * @author Ben L. Titzer
 * @author Enrico Jorns
 */
public abstract class Sensor {

    /**
     * Set the source for this sensor.
     *
     * The source determines how the sensor will get its data.
     *
     * @see SensorSource
     * @see SetSensorSource
     * @see RandomSensorSource
     * @see ReplaySensorSource
     *
     * @param src Source to use
     */
    public abstract void setSensorSource(SensorSource src);

    /**
     * Returns info about input channels of this sensor.
     *
     * @return Input channels
     */
    public abstract Channel[] getChannels();

    public class Channel {

        public String name;   // name to identify
        public String unit;   // unit
        public double ubound; // upper bound of accepted value
        public double lbound; // lower bound of accepted value
        public double defval; // default value returned if powered off e.g.

        public Channel(String name, String unit, double lbound, double ubound, double defval) {
            this.name = name;
            this.unit = unit;
            this.lbound = lbound;
            this.ubound = ubound;
            this.defval = defval;
        }

        public Channel(String name, double lbound, double ubound, double defval) {
            this(name, null, lbound, ubound, defval);
        }

    }

}
