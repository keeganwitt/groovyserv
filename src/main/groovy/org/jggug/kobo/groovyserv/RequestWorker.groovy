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

import java.util.concurrent.atomic.AtomicReference

import static org.jggug.kobo.groovyserv.Protocol.*


/**
 * @author UEHARA Junji
 * @author NAKANO Yasuharu
 */
class RequestWorker implements Runnable {

    private static currentDir

    private ClientConnection conn

    RequestWorker(clientConnection) {
        this.conn = clientConnection
    }

    @Override
    void run() {
        try {
DebugUtils.debugLog "run:1"
            Map<String, List<String>> headers = conn.readHeaders()

DebugUtils.debugLog "run:2"
            def cwd = headers[HEADER_CURRENT_WORKING_DIR][0]
DebugUtils.debugLog "run:3"
            if (currentDir != null && currentDir != cwd) {
                throw new GroovyServerException(
                    "Can't change current directory because of another session running on different dir: " +
                    headers[HEADER_CURRENT_WORKING_DIR][0])
            }
            setCurrentDir(cwd)
DebugUtils.debugLog "run:4"

            process(headers)
DebugUtils.debugLog "run:5"
            ensureAllThreadToStop()
DebugUtils.debugLog "run:6"
            conn.sendExit(0)
DebugUtils.debugLog "run:7"
        }
        catch (ExitException e) {
            // GroovyMain2 throws ExitException when it catches ExitException.
DebugUtils.debugLog "run:ex1" + e
            conn.sendExit(e.exitStatus)
        }
        catch (Throwable e) {
DebugUtils.debugLog "run:ex2" + e
            DebugUtils.errLog("unexpected error", e)
            conn.sendExit(0)
        }
        finally {
            setCurrentDir(null)

            conn.close()
            DebugUtils.verboseLog "client connection is closed: $connection"
        }
    }

    private process(headers) {
        if (headers[HEADER_CP] != null) {
            addClasspath(headers[HEADER_CP][0])
        }

        List args = headers[HEADER_ARG]
        for (Iterator<String> it = args.iterator(); it.hasNext(); ) {
            String s = it.next()
            if (s == "-cp") {
                it.remove()
                if (!it.hasNext()) {
                    throw new GroovyServerException("classpath option is invalid.")
                }
                String classpath = it.next()
                addClasspath(classpath)
                it.remove()
            }
        }
        GroovyMain2.main(args as String[])
    }

    private ensureAllThreadToStop() {
        ThreadGroup tg = Thread.currentThread().threadGroup
        Thread[] threads = new Thread[tg.activeCount()]
        int tcount = tg.enumerate(threads)
        while (tcount != threads.size()) {
            threads = new Thread[tg.activeCount()]
            tcount = tg.enumerate(threads)
        }
        for (int i=0; i<threads.size(); i++) {
            if (threads[i] != Thread.currentThread() && threads[i].isAlive()) {
                if (threads[i].isDaemon()) {
                    threads[i].interrupt()
                }
                else {
                    threads[i].interrupt()
                    threads[i].join()
                }
            }
        }
    }

    private synchronized static setCurrentDir(dir) {
        if (dir != currentDir) {
            currentDir = dir
            System.setProperty('user.dir', currentDir)
            PlatformMethods.chdir(currentDir)
            addClasspath(currentDir)
        }
    }

    private static addClasspath(newPath) { // FIXME this method is awful...
        if (newPath == null || newPath == "") {
            System.properties.remove("groovy.classpath")
            return
        }
        def pathes = newPath.split(File.pathSeparator) as LinkedHashSet
        pathes << newPath
        System.setProperty("groovy.classpath", pathes.join(File.pathSeparator))
    }

}
