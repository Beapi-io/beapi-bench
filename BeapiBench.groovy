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
    private List graphTypes = ['TIME','TOTALTIME','ALL']
    protected String graphType = 'TIME'
    protected String endpoint
    protected Integer concurrency = 50
    protected Integer requests = 1000
    protected String token
    protected List headers
    protected String contentType = 'application/json'

    protected String path = '/tmp/'
    protected String filename = 'beapiBench.txt'
    protected String tmpPath = path+filename

    Float totalTime
    Integer testSize = 50
    Integer testTime = 60


    CliBuilder cliBuilder

    CommandLineInterface(){
        cliBuilder = new CliBuilder(
            usage: 'BeapiBench.groovy [<options>] -m=method --endpoint=url',
            header: 'OPTIONS:',
            footer: "BeapiBench is a tool for benchmarking and graphing api's. It requires that both ApacheBench (ab) and gnuplot be preinstalled and available to run. Please make sure these are available and installed via your repository. If you have any questions, please visit us a http://beapi.io. Thanks again."
        )

        cliBuilder.width=80
        cliBuilder.with {
            // HELP OPT
            h(longOpt: 'help', 'Print this help text and exit (usage: -h, --help)')

            // FORCE OPT
            f(longOpt: 'force', 'Force run without checking for dependencies')

            // REQUIRED TEST OPTS
            m(longOpt:'method',args:2, valueSeparator:'=',argName:'property=value', 'request method for endpoint (GET/PUT/POST/DELETE)')
            _(longOpt:'endpoint',args:2, valueSeparator:'=',argName:'property=value', 'url for making the api call (usage: --endpoint=http://localhost:8080)')
            _(longOpt:'testnum',args:2, valueSeparator:'=',argName:'property=value', 'number of tests to run; defaults to 50 (usage: --testNum=100)')
            g(longOpt:'graphtype',args:2, valueSeparator:'=',argName:'property=value', 'type of graph to create: TIME / TOTALTIME / ALL; defaults to TESTTIME (usage: -g TOTALTIME)')

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

            if (!options.f) {
                /*
                // Apache-Utils
                String abChk = "dpkg -s ab &> /dev/null | echo \$?"
                def proc2 = ['bash', '-c', abChk].execute()
                proc2.waitFor()
                def outputStream2 = new StringBuffer()
                def error2 = new StringWriter()
                proc2.waitForProcessOutput(outputStream2, error2)
                String output2 = outputStream2.toString()
                switch (output2) {
                    case '0':
                        break;
                    case '1':
                    default:
                        throw new Exception('Apache-Utils not installed. Please install via your local repositorys or use -f (--force) to force run.')
                }

                String gnuChk = "dpkg -s gnuplot &> /dev/null | echo \$?"
                def proc3 = ['bash', '-c', gnuChk].execute()
                proc3.waitFor()
                def outputStream3 = new StringBuffer()
                def error3 = new StringWriter()
                proc3.waitForProcessOutput(outputStream3, error3)
                String output3 = outputStream3.toString()
                switch (output3) {
                    case '0':
                        break;
                    case '1':
                    default:
                        throw new Exception('Gnuplot not installed. Please install via your local repositorys or use --f (--force) to force run.')
                }
                */
            }

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

            if (options.testnum) {
                try {
                    Integer temp = options.testnum as Integer
                    if (!(temp>0)) {
                        throw new Exception('Testnum (--testnum)must be a positive number greater than 0. Please try again.')
                    }
                    this.testSize=temp
                } catch (Exception e) {
                    throw new Exception('Testnum (--testnum) expects an Unsigned Integer greater than 0. Please try again.', e)
                }
            }

            if (options.g) {
                if (!this.graphTypes.contains(options.g)) {
                    throw new Exception("GraphType (--graphtype, -g) must match one of existing GraphTypes: [${this.graphTypes}]. Please try again.")
                }
                this.graphType = options.g
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

                // TODO: will fail here if tests fail; need to create a way to continue
                Float floatTemp1 = Float.parseFloat(returnData[1])
                Float floatTemp2 = Float.parseFloat(data[size][1])
                String floatTemp3 = Float.sum(floatTemp1, floatTemp2) as String
                List temp = [returnData[1], floatTemp3, returnData[2]]
                data.add(temp)

                this.totalTime = Float.parseFloat(floatTemp3)
            } else {
                // time/totaltime/rps
                List temp = [returnData[1], returnData[1], returnData[2]]
                data.add(temp)
            }

            float waitTime = Float.parseFloat(returnData[1])
            waitTime = ((waitTime * 1000)*2)+250
            sleep(waitTime as Integer)

            i++
        }


        // CREATE DATA FILE
        File apiBenchData = new File("${this.tmpPath}")
        if (apiBenchData.exists() && apiBenchData.canRead()) { apiBenchData.delete() }
        apiBenchData.append('# X   Y   Z\n')
        data.each() {
            apiBenchData.append '   '
            apiBenchData.append it.join('   ')
            apiBenchData.append '\n'
        }

        // CREATE GRAPH
        if(this.graphType!='ALL') {
            String title = "[TIME] ${this.concurrency} c / ${this.requests} n / ${this.testSize} tests}"
            createChart(this.graphType,"${title}")
        }else{
            String title = "[TOTALTIME] ${this.concurrency} c / ${this.requests} n / ${this.testSize} tests}"
            createChart('TOTALTIME',"${title}")

            String title2 = "[TIME] ${this.concurrency} c / ${this.requests} n / ${this.testSize} tests}"
            createChart('TIME',"${title2}")
        }
    }



    // TODO
    protected testConnection(){

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


    protected void createChart(String graphType, String title){
        try{
            println("[tmp gnuplot file] >> "+this.tmpPath)
            String key = "set key left bottom"
            String gridY = "set grid ytics lc rgb \\\"#bbbbbb\\\" lw 1 lt 0"
            String gridX
            String range
            switch(graphType){
                case 'TIME':
                    gridX = "set xrange [*:] reverse;set grid xtics lc rgb \\\"#bbbbbb\\\" lw 1 lt 0"
                    range = "1:3"
                    break
                case 'TOTALTIME':
                    gridX = "set grid xtics lc rgb \\\"#bbbbbb\\\" lw 1 lt 0"
                    range = "2:3"
                    break
            }

            String plot = "plot '${this.tmpPath}' using ${range} with linespoint pt 7 title \\\"${title}\\\""
            String bench = "gnuplot -p -e \"${gridX};${gridY};${key};${plot};\""
            println(bench)
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













