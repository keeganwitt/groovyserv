/*
 * Copyright 2009-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jggug.kobo.groovyserv

class ChunkedOutputStream extends OutputStream {

    final static String HEADER_STREAM_ID = "Channel"
    final static String HEADER_SIZE = "Size"

    char streamIdentifier

    static WeakHashMap<ThreadGroup, OutputStream>map = [:]

    public ChunkedOutputStream(char id) {
        streamIdentifier = id
    }

    @Override
    public void flush() throws IOException {
        getCurrentOutputStream().flush()
    }

    @Override
    public void close() throws IOException {
        getCurrentOutputStream().close()
    }

    @Override
    public void write(int b) {
        byte[] buf = new byte[1]
        buf[0] = (b>>8*0) & 0x0000ff
        write(buf, 0, 1)
    }

    @Override
    public void write(byte[] b, int off, int len) {
        OutputStream out = getCurrentOutputStream()
        if (System.getProperty("groovyserver.verbose") == "true") {
            GroovyServer.originalErr.println("Server==>Client")
            GroovyServer.originalErr.println(" id="+streamIdentifier)
            GroovyServer.originalErr.println(" size="+len)
            Dump.dump(GroovyServer.originalErr, b, off, len)
        }
        //GroovyServer.originalErr.println("TID="+Thread.currentThread().id)
        out.write((HEADER_STREAM_ID + ": " + streamIdentifier + "\n").bytes)
        out.write((HEADER_SIZE + ": " + len + "\n").bytes)
        out.write("\n".bytes)
        out.write(b, off, len)
    }

    public void bind(OutputStream out, ThreadGroup tg) {
        map[tg] = out
    }

    private OutputStream getCurrentOutputStream() {
        return check(map[Thread.currentThread().getThreadGroup()])
    }

    private OutputStream check(OutputStream out) {
        if (out == null) {
            throw new IllegalStateException("System.out/err can't access from this thread: " + 
                Thread.currentThread() + ":" + Thread.currentThread().id + ":" + Thread.currentThread().getThreadGroup())
        }
        return out
    }
}