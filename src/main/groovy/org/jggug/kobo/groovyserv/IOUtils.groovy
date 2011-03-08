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

import java.util.concurrent.Future
import java.util.concurrent.ExecutionException
import java.util.concurrent.CancellationException

import static java.util.concurrent.TimeUnit.*


/**
 * @author NAKANO Yasuharu
 */
 class IOUtils {

    /**
     * @throws Throwable when ExecutionException is occured, throw an inner exception wrapped by ExecutionException
     * @throws CancellationException
     * @throws InterruptedException
     */
    static awaitFuture(Future future) {
        try {
            future.get()
        } catch (ExecutionException e) {
            throw e.cause
        }
    }

    static close(closeable) {
        try {
            if (closeable) closeable.close()
        } catch (IOException e) {
            DebugUtils.errorLog("Failed to close", e)
        }
    }

    /**
     * @throws InterruptedIOException
     * @throws IOException
     */
    static readLines(InputStream ins) {
        def lines = []
        def line
        while ((line = readLine(ins)) != "") { // because in request, an empty line means the end of header part
            lines << line
        }
        return lines
    }

    /**
     * @throws InterruptedIOException
     * @throws IOException
     */
    static readLine(InputStream ins) {
        def baos = new ByteArrayOutputStream()
        int ch
        while ((ch = ins.read()) != -1) {
            if (ch == '\n') { // LF (fixed)
                break
            }
            baos.write((byte) ch)
        }
        return baos.toString()
    }

 }
