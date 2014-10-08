/*
 * Copyright 2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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
package groovyx.groovyserv.test

import static org.junit.Assert.*

/**
 * Utilities only for tests.
 */
class TestUtils {

    static ProcessResult executeClientCommandWithEnv(List<String> args, Map<String, String> envMap, Closure closure = null) {
        def out = new ByteArrayOutputStream()
        def err = new ByteArrayOutputStream()
        def p = createProcessBuilder([clientExecutablePath, * args], envMap).start()
        p.consumeProcessOutput(out, err)
        if (closure) closure.call(p)
        p.waitFor()
        return new ProcessResult(out: out.toString(), err: err.toString(), process: p)
    }

    static ProcessResult executeClientCommand(List<String> args, Closure closure = null) {
        return executeClientCommandWithEnv(args, null, closure)
    }

    static ProcessResult executeClientCommandWithEnvSuccessfully(List<String> args, Map<String, String> envMap, Closure closure = null) {
        def result = executeClientCommandWithEnv(args, envMap, closure)
        if (result.process.exitValue() != 0) {
            fail "ERROR: exitValue:${result.process.exitValue()}, in:[${result.out}], err:[${result.err}]"
        }
        return result
    }

    static ProcessResult executeClientCommandSuccessfully(List<String> args, Closure closure = null) {
        return executeClientCommandWithEnvSuccessfully(args, null, closure)
    }

    static void startServerIfNotRunning(int port) {
        def result = executeServerCommand(["-r", "-v", "-p", String.valueOf(port)])
        if (result.process.exitValue() != 0) {
            fail "ERROR: exitValue:${result.process.exitValue()}, in:[${result.out}], err:[${result.err}]"
        }
    }

    static void shutdownServerIfRunning(int port) {
        def result = executeServerCommand(["-k", "-p", String.valueOf(port)])
        if (result.process.exitValue() != 0) {
            fail "ERROR: exitValue:${result.process.exitValue()}, in:[${result.out}], err:[${result.err}]"
        }
    }

    static ProcessResult executeServerCommand(List<String> options) {
        def out = new ByteArrayOutputStream()
        def err = new ByteArrayOutputStream()
        def p = createProcessBuilder([serverExecutablePath, * options]).start()
        p.consumeProcessOutput(out, err)
        p.waitFor()
        return new ProcessResult(out: out.toString(), err: err.toString(), process: p)
    }

    private static createProcessBuilder(List<String> commandLine, Map<String, String> envMap = [:]) {
        ProcessBuilder processBuilder = new ProcessBuilder()
        def actualCommand = processBuilder.command()

        // This doesn't work on Cygwin/Windows. The command line is somehow split by a white space.
        //def env = processBuilder.environment()
        //envMap.each { key, value ->
        //    env.put(key.toString(), value.toString()) // without this, ArrayStoreException may occur
        //}
        // Instead, this works both on Cygwin and DOS in Windows.
        actualCommand << "env"
        envMap.each { key, value ->
            actualCommand << "${key}=${value}".toString() // without this, ArrayStoreException may occur
        }

        commandLine.each { arg ->
            actualCommand << arg.toString() // without this, ArrayStoreException may occur
        }

        return processBuilder
    }

    private static String getClientExecutablePath() {
        System.getProperty("groovyserv.executable.client")
    }

    private static String getServerExecutablePath() {
        System.getProperty("groovyserv.executable.server")
    }

    static class ProcessResult {
        Process process
        String out
        String err
    }
}