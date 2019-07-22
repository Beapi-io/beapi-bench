#!/usr/bin/env groovy
@Grab(group='commons-cli', module='commons-cli', version='1.4')

import groovy.json.JsonSlurper
import java.text.DecimalFormat

// Data to retrieve:
//Document Length:        102 bytes
//Time taken for tests:   1.884 seconds
//Requests per second:    1326.87 [#/sec] (mean)

// gnuplot> plot "data.txt" using 1:2 with linespoint pt 7

/**
 *  -file=filename.txt (required)
 *  -testSize=50 (default)
 *  -testTime=60 (seconds)
 */
class BeapiBench {


    static void main(String[] args) {
        CommandLineInterface cli = CommandLineInterface.INSTANCE
        cli.parse(args)
    }
}


/**
 * CLI Interface for tool
 */
enum CommandLineInterface{
    INSTANCE

    private List methods = ['GET', 'PUT', 'POST', 'DELETE']
    protected String method
    protected String endpoint
    protected Integer concurrency = 50
    protected Integer requests = 1000
    protected String token
    protected List headers
    protected String contentType = 'application/json'

    protected String path = '/tmp/'
    protected String filename = 'beapiBench.txt'
    protected String tmpPath = path+filename

    Integer testSize = 75
    Integer testTime = 60


    CliBuilder cliBuilder

    CommandLineInterface(){
        cliBuilder = new CliBuilder(
            usage: 'beapiBench [<options>] -m=method --endpoint=url',
            header: 'OPTIONS:',
            footer: "BeapiBench is a tool for benchmarking and graphing api's. It requires that both ApacheBench (ab) and gnuplot be preinstalled and available to run. Please make sure these are available and installed via your repository. If you have any questions, please visit us a http://beapi.io. Thanks again."
        )

        cliBuilder.width=80
        cliBuilder.with {
            // HELP OPT
            h(longOpt: 'help', 'Print this help text and exit (usage: -h, --help)')

            // REQUIRED TEST OPTS
            m(longOpt:'method',args:2, valueSeparator:'=',argName:'property=value', 'request method for endpoint (GET/PUT/POST/DELETE)')
            _(longOpt:'endpoint',args:2, valueSeparator:'=',argName:'property=value', 'url for making the api call (usage: --endpoint=http://localhost:8080)')

            // OPTIONAL TEST OPTS
            c(longOpt:'concurrency',args:2, valueSeparator:'=',argName:'property=value', 'value for concurrent users per test run (usage: -c 50, --concurrency=50)')
            n(longOpt:'requests',args:2, valueSeparator:'=',argName:'property=value', 'requests to make per test run (usage: -n 1000, --requests=1000)')
            t(longOpt:'token',args:2, valueSeparator:'=',argName:'property=value', 'JWT bearer token (usage: -t wer4t56g356g356h35h, --token=wer4t56g356g356h35h)')
            p(longOpt:'header',args:2, valueSeparator:'=',argName:'property=value', 'optional header to pass (usage: -p <header>, --header=<header>)')
            j(longOpt:'contenttype',args:2, valueSeparator:'=',argName:'property=value', "content-type header; defaults to 'application/json' (usage: -c application/xml, --contenttype=application-xml)")

            // GRAPH OPTS

        }
    }

    void parse(args) {
        OptionAccessor options = cliBuilder.parse(args)
        try {
            if (!options) {
                throw new Exception('Could not parse command line options.\n')
            }
            if (options.h) {
                cliBuilder.usage()
                System.exit 0
            }

            if (options.m && options.endpoint) {
                if (!methods.contains(options.m.toUpperCase())) {
                    throw new Exception('Request method not supported. Please try again.\n')
                }
                this.method = options.m


                try {
                    URL url = new URL(options.endpoint)
                } catch (Exception e) {
                    throw new Exception('Endpoint is not a valid URL. Please try again', e)
                }
                this.endpoint = options.endpoint
            } else {
                throw new Exception('Method (--method) and Endpoint (--endpoint) are both REQUIRED for BeapiBench to work. Please try again.\n')
            }


            if (options.c) {
                try {
                    this.concurrency = options.c as Integer
                    if (!(this.concurrency>0)) {
                        throw new Exception('Concurrency must be a positive number greater than 0. Please try again.')
                    }
                } catch (Exception e) {
                    throw new Exception('Concurrency (--concurrency ,-c) expects an Unsigned Integer greater than 0. Please try again.', e)
                }
            }

            if (options.n) {
                try {
                    this.requests = options.n as Integer
                    if (!(this.requests>0)) {
                        throw new Exception('Number of requests must be a positive number greater than 0. Please try again.')
                    }
                } catch (Exception e) {
                    throw new Exception('Requests (--request, -n) expects an Unsigned Integer greater than 0. Please try again.', e)
                }
            }

            if (this.concurrency >= this.requests) {
                throw new Exception('Concurrency must be less than number of requests. Please try again')
            }

            if (options.t) {
                this.token = options.t.trim()
            }
            if (options.p) {
                options.p.each() {
                    this.headers.add(it.trim())
                }
            }
            if (options.j) {
                this.contentType = options.j.trim()
            }
        } catch (Exception e) {
            System.err << e
            System.exit 1
        }


        int i = 0
        List data = []
        while (i < this.testSize) {
            // call method; move this to method
            print("[TEST ${i+1} of ${this.testSize}] ")
            List returnData = callApi(this.concurrency, this.requests, this.contentType, this.token, this.method, this.endpoint)
            if (data.size() > 0) {
                DecimalFormat df = new DecimalFormat("0.00")
                int size = data.size() - 1

                // will fail here if tests fail; need to create a way to continue
                Float floatTemp1 = Float.parseFloat(returnData[1])
                Float floatTemp2 = Float.parseFloat(data[size][0])
                String floatTemp3 = Float.sum(floatTemp1, floatTemp2) as String
                //Float result = df.format(floatTemp3)
                List temp = [floatTemp3, returnData[2]]
                data.add(temp)
            } else {
                List temp = [returnData[1], returnData[2]]
                data.add(temp)
            }

            float waitTime = Float.parseFloat(returnData[1])
            waitTime = (waitTime * 1000)
            sleep(waitTime as Integer)

            i++
        }

        // output to file
        // TODO: check if file exists and ask if they want to overwrite if it does
        def apiBenchData = new File(this.tmpPath)
        if (apiBenchData.exists() && apiBenchData.canRead()) {
            apiBenchData.delete()
        }
        // do I have to recreate apiBenchData???

        apiBenchData.append('# X   Y\n')
        data.each() {
            apiBenchData.append '   '
            apiBenchData.append it.join('   ')
            apiBenchData.append '\n'
        }
        //println(apiBenchData.text)
        createChart()
    }

    protected List callApi(Integer concurrency, Integer requests, String contentType, String token, String method, String endpoint){
        String bench = "ab -c ${concurrency} -n ${requests} -H 'Content-Type: ${contentType}' -H'Authorization: Bearer ${token}' -m ${method} ${endpoint}"
        def proc = ['bash', '-c', bench].execute()
        proc.waitFor()
        DecimalFormat df = new DecimalFormat("0.00")
        def outputStream = new StringBuffer()
        def error = new StringWriter()
        proc.waitForProcessOutput(outputStream, error)
        String output = outputStream.toString()
        List<String> returnData = [0,0,0]
        if (output) {
            List lines = output.readLines()
            lines.each(){ it2 ->
                if (it2 =~ /Document Length/) {
                    List temp = it2.split(':')
                    String temp2 = temp[1].trim()
                    List temp3 = temp2.split(' ')
                    Float temp4 = Float.parseFloat(temp3[0])
                    returnData[0] = df.format(temp4)
                    print(" : ${df.format(temp4)} bytes/")
                }
                if (it2 =~ /Time taken for tests/) {
                    List temp = it2.split(':')
                    String temp2 = temp[1].trim()
                    List temp3 = temp2.split(' ')
                    Float temp4 = Float.parseFloat(temp3[0])
                    returnData[1] = df.format(temp4)
                    print("${df.format(temp4)} secs/")
                }
                if (it2 =~ /Requests per second/) {
                    List temp = it2.split(':')
                    String temp2 = temp[1].trim()
                    List temp3 = temp2.split(' ')
                    Float temp4 = Float.parseFloat(temp3[0])
                    returnData[2] = df.format(temp4)
                    println("${df.format(temp4)} rps")
                }
            }
        } else {
            println("[ERROR: apiBench]:  Error message follows : " + error)
        }
        return returnData
    }


    protected void createChart(){
        try{
            println(this.tmpPath)
            String bench = "gnuplot -p -e \"plot '${this.tmpPath}' using 1:2 with linespoint pt 7\""
            def proc = ['bash', '-c', bench].execute()
            proc.waitFor()
            def outputStream = new StringBuffer()
            def error = new StringWriter()
            proc.waitForProcessOutput(outputStream, error)
        } catch (Exception e) {
            throw new Exception('Error creating chart. Exception follows:', e)
        }
    }
}













