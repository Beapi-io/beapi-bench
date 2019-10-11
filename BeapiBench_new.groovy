#!/usr/bin/env groovy

/*
 * Copyright 2013-2019 Beapi.io
 *
 * Licensed under the MPL-2.0 License;
 * you may not use this file except in compliance with the License.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@Grab(group='commons-cli', module='commons-cli', version='1.4')

import groovy.json.JsonSlurper
import java.text.DecimalFormat
import java.util.regex.Matcher

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
    private List graphTypes = ['TIME','TOTALTIME','IO','SUCCESS_FAIL','ALL']
    private LinkedHashMap graphDesc = [
            'TIME':"""The <b>Time Chart</b> is a measure of 3 separate variables: the Requests Per Second or 'RPS' (Y-coord), Number of seconds each test took (X-coord) and Number of milliseconds each request took (Point Label). This is meant to show ramp up from start highest output as well as what occurs with processor when you stay at high output for too long and where the processor/application is most 'comfortable'.""",
            'TOTALTIME':"",
            'IO':"",
            'SUCCESS_FAIL':""
    ]
    protected String graphType = 'ALL'
    protected String endpoint
    protected Integer concurrency = 50
    protected Integer requests = 1000
    protected String token
    protected List headers
    protected String contentType = 'application/json'

    protected String path = '/tmp/'
    protected String filename = 'beapiBench.txt'
    protected String tmpPath = path+filename
    boolean noHardcore = true
    Float totalTime
    Integer testSize = 50
    String postData
    String bytes

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
            _(longOpt: 'hardcore', 'No pause between tests')
            g(longOpt:'graphtype',args:2, valueSeparator:'=',argName:'property=value', 'type of graph to create: [TIME, TOTALTIME, IO, SUCCESS_FAIL, ALL]; defaults to TESTTIME (usage: -g TOTALTIME)')

            // OPTIONAL TEST OPTS
            c(longOpt:'concurrency',args:2, valueSeparator:'=',argName:'property=value', 'value for concurrent users per test run (usage: -c 50, --concurrency=50)')
            n(longOpt:'requests',args:2, valueSeparator:'=',argName:'property=value', 'requests to make per test run (usage: -n 1000, --requests=1000)')
            t(longOpt:'token',args:2, valueSeparator:'=',argName:'property=value', 'JWT bearer token (usage: -t wer4t56g356g356h35h, --token=wer4t56g356g356h35h)')
            H(longOpt:'header',args:2, valueSeparator:'=',argName:'property=value', 'optional header to pass (usage: -H <header>, --header=<header>)')
            j(longOpt:'contenttype',args:2, valueSeparator:'=',argName:'property=value', "content-type header; defaults to 'application/json' (usage: -c application/xml, --contenttype=application-xml)")
            p(longOpt:'postData',args:2, valueSeparator:'=',argName:'property=value', 'txt file supplying POST data (usage: -p post.txt )')

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
            if (options.H) {
                options.H.each() {
                    this.headers.add(it.trim())

                }
            }
            if (options.p) {
                this.postData = options.p.trim()
            }
            if (options.j) {
                this.contentType = options.j.trim()
            }

            if (options.hardcore) {
                this.noHardcore = false
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
            print("[TEST ${i+1} of ${this.testSize}] : ")
            List returnData = callApi(this.postData, this.concurrency, this.requests, this.contentType, this.token, this.method, this.endpoint, this.headers)
            if(!returnData.isEmpty()) {
                DecimalFormat df = new DecimalFormat("0.00")
                if (data.size() > 0) {
                    int size = data.size() - 1
                    // TODO: will fail here if tests fail; need to create a way to continue; test returnData
                    //try {
                        Float floatTemp1 = Float.parseFloat(returnData[1])
                        Float floatTemp2 = Float.parseFloat(data[size][1])
                        Float floatTemp3 = Float.sum(floatTemp1, floatTemp2)
                        List temp = [returnData[1], df.format(floatTemp3), returnData[2], df.format(Float.parseFloat(returnData[3])), df.format(Float.parseFloat(returnData[4])), df.format(Float.parseFloat(returnData[5])), df.format(Float.parseFloat(returnData[6])), df.format(Float.parseFloat(returnData[7])), df.format(Float.parseFloat(returnData[8])),returnData[9],returnData[10],returnData[11],returnData[12]]
                        data.add(temp)

                        this.totalTime = floatTemp3
                    //}catch(Exception e){
                    //    println("${returnData} :" +e)
                    //}
                } else {
                    // time/totaltime/rps
                    List temp = [returnData[1], returnData[1], returnData[2],returnData[3],returnData[4],returnData[5],returnData[6],returnData[7],returnData[8],returnData[9],returnData[10],returnData[11],returnData[12]]
                    data.add(temp)
                }
            }else{
                println(" TEST FAILED. Set concurrency/requests lower or change server config.")
            }

            if(this.noHardcore) {
                float waitTime = Float.parseFloat(returnData[1])
                waitTime = ((waitTime * 1000) * 1.8)
                sleep(waitTime as Integer)
            }
            i++
        }

        createFile(data)
    }

    // TODO
    protected testConnection(){

    }

    protected void createFile(List data){
        // data{*}{0}
        List xrange1 = []
        List xrange2 = []
        List xrange3 = []
        List xrange4 = []

        // data*[2]
        String values1 = ""
        List values2 = []
        List values3 = []
        List values4 = []

        data.each(){
            if(it[0]!=null) {
                xrange1.add(it[0])
				xrange2.add(it[1])
            }
            if(it[2]!=null) {
                values1 += "{x:${it[0]},y:${it[2]}},"
				values2.add(it[2])
            }
        }
        def file =new File("beapi_bench.html")
        if(file.exists() && file.canRead()){
            file.text = ''
        }
        file.text = """
<html>
<head>
<title>BeAPI API Benchmarking Tool</title>
<link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css" integrity="sha384-ggOyR0iXCbMQv3Xipma34MD+dH/1fQ784/j6cY/iJTQUOhcWr7x9JvoRxT2MZw1T" crossorigin="anonymous">
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/Chart.js/2.8.0/Chart.min.css">
<script src="https://cdnjs.cloudflare.com/ajax/libs/Chart.js/2.8.0/Chart.bundle.min.js"></script>

<style>

.nav-tabs > a { 
    border: 0px solid #1A3E5E;
    background-color:#2F71AB; 
    color:#fff;
    border-bottom: none;
    margin-left: 5px;
}

.nav-tabs {
   border-bottom: 1px solid #2F71AB;
}
</style>
</head>
<body bgcolor="#ffffff">

<div class="container" style="padding-top:20px;">
  <div class="row">
    <div class="col-sm-12">
<div><img src='https://raw.githubusercontent.com/orubel/logos/master/beapi_logo_large.png' style="height:75px;"/><h1 style='float:right;font-family: Arial, Helvetica, sans-serif;color:#000000;'>API Benchmarking Tool</h1></div>
    </div>
  </div>
</div>

<div style="background-color:#e4e4e4;padding:0;border-top: 1px solid #2F71AB;border-bottom: 1px solid #2F71AB;">
<div class="container" style="padding-top:20px;padding-bottom:50px;">
  <div class="row">
    <div class="col-sm-1"><b>URL:</b></div>
    <div class="col-sm-5">${endpoint}</div>
    <div class="col-sm-2"><b>Concurrency:</b></div>
    <div class="col-sm-4">${concurrency}</div>
  </div>
  <div class="row">
    <div class="col-sm-1"><b>Method:</b></div>
    <div class="col-sm-5">${method}</div>
    <div class="col-sm-2"><b>Requests:</b></div>
    <div class="col-sm-4">${requests}</div>
  </div>
  <div class="row">
    <div class="col-sm-1"><b>Type:</b></div>
    <div class="col-sm-5">${contentType}</div>
    <div class="col-sm-2"><b># Tests:</b></div>
    <div class="col-sm-4">${testSize}</div>
  </div>
  <div class="row">
    <div class="col-sm-1"><b>Bytes:</b></div>
    <div class="col-sm-5">${bytes}</div>
    <div class="col-sm-2"><b>Hardcore:</b></div>
    <div class="col-sm-4">${!noHardcore}</div>
  </div>
</div>
</div>

<div class="container" style="position:relative;top:-62px;">
  <div class="row" style="padding-top:20px;">
    <div class="col-lg-12">
    <nav>
      <div class="nav nav-tabs" id="nav-tab" role="tablist">
      <a class="nav-item nav-link" id="nav-TIME-tab" data-toggle="tab" href="#nav-TIME" role="tab" aria-controls="nav-TIME" aria-selected="true">TIME</a>
      <a class="nav-item nav-link" id="nav-TOTALTIME-tab" data-toggle="tab" href="#nav-TOTALTIME" role="tab" aria-controls="nav-TOTALTIME">TOTALTIME</a>
      <a class="nav-item nav-link" id="nav-IO-tab" data-toggle="tab" href="#nav-IO" role="tab" aria-controls="nav-IO">I/O</a>
      <a class="nav-item nav-link" id="nav-SUCCESS_FAIL-tab" data-toggle="tab" href="#nav-SUCCESS_FAIL" role="tab" aria-controls="nav-SUCCESS_FAIL">SUCCESS/FAIL</a>
      </div>
    </nav>
    <div class="tab-content" id="nav-tabContent">
    <div class="tab-pane fade" id="nav-TIME" role="tabpanel" aria-labelledby="nav-TIME-tab" style="padding-top:20px;">${graphDesc['TIME']}<br>
        <p>
        <div class="chartjs-wrapper">
        <canvas id="chartjs-0" class="chartjs" width="undefined" height="undefined"></canvas>
        <script>
        new Chart(document.getElementById("chartjs-0"),{
            "type":"line",
            "data":{
                "labels":${xrange1},
                "datasets":[{
                    "label":"${concurrency} c / ${requests} n / ${testSize} tests",
                    "data":[${values1}],
                    "fill":false,
                    "borderColor":"rgb(75, 192, 192)",
                    "lineTension":0.1
                }]
            },
            "options":{}
        });
        </script>
        </div>
        </p>
    </div>
    
    <div class="tab-pane fade" id="nav-TOTALTIME" role="tabpanel" aria-labelledby="nav-TOTALTIME-tab" style="padding-top:20px;">${graphDesc['TOTALTIME']}<br>
        <p>
        <div class="chartjs-wrapper">
        <canvas id="chartjs-1" class="chartjs" width="undefined" height="undefined"></canvas>
        <script>
        new Chart(document.getElementById("chartjs-1"),{
            "type":"line",
            "data":{
                "labels":${xrange2},
                "datasets":[{
                    "label":"${concurrency} c / ${requests} n / ${testSize} tests",
                    "data":${values2},
                    "fill":false,
                    "borderColor":"rgb(75, 192, 192)",
                    "lineTension":0.1
                }]
            },
            "options":{}
        });
        </script>
        </div>
        </p>
    </div>
    
    <div class="tab-pane fade" id="nav-IO" role="tabpanel" aria-labelledby="nav-IO-tab" style="padding-top:20px;">${graphDesc['IO']}<br>
        <p>
        <div class="chartjs-wrapper">
        <canvas id="chartjs-2" class="chartjs" width="undefined" height="undefined"></canvas>
        <script>
        new Chart(document.getElementById("chartjs-2"),{
            "type":"line",
            "data":{
                "labels":["January","February","March","April","May","June","July"],
                "datasets":[{
                    "label":"My First Dataset",
                    "data":[65,59,80,81,56,55,40],
                    "fill":false,
                    "borderColor":"rgb(75, 192, 192)",
                    "lineTension":0.1
                }]
            },
            "options":{}
        });
        </script>
        </div>
        </p>
    </div>
    
    <div class="tab-pane fade" id="nav-SUCCESS_FAIL" role="tabpanel" aria-labelledby="nav-SUCCESS_FAIL-tab" style="padding-top:20px;">${graphDesc['SUCCESS_FAIL']}<br>
        <p>
        <div class="chartjs-wrapper">
        <canvas id="chartjs-3" class="chartjs" width="undefined" height="undefined"></canvas>
        <script>
        new Chart(document.getElementById("chartjs-3"),{
            "type":"line",
            "data":{
                "labels":["January","February","March","April","May","June","July"],
                "datasets":[{
                    "label":"My First Dataset",
                    "data":[65,59,80,81,56,55,40],
                    "fill":false,
                    "borderColor":"rgb(75, 192, 192)",
                    "lineTension":0.1
                }]
            },
            "options":{}
        });
        </script>
        </div>
        </p>
    </div>
    
    </div>
    </div>
  </div>
</div>



<script src="https://code.jquery.com/jquery-3.3.1.slim.min.js" integrity="sha384-q8i/X+965DzO0rT7abK41JStQIAqVgRVzpbzo5smXKp4YfRvH+8abtTE1Pi6jizo" crossorigin="anonymous"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.14.7/umd/popper.min.js" integrity="sha384-UO2eT0CpHqdSJQ6hJty5KVphtPhzWj9WO1clHTMGa3JDZwrnQq4sF86dIHNDz0W1" crossorigin="anonymous"></script>
<script src="https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/js/bootstrap.min.js" integrity="sha384-JjSmVgyd0p3pXB1rRibZUAYoIIy6OrQ6VrjIEaFf/nJGzIxFDsf4x0xIM+B07jRM" crossorigin="anonymous"></script>


</body>
</html>
"""
        println("### [beapi bench complete] >> See results in 'beapi_bench.html' file ###")
    }

    protected List callApi(String postData, Integer concurrency, Integer requests, String contentType, String token, String method, String endpoint, List headers) {
        String bench = "ab -c ${concurrency} -n ${requests}"
        if(postData){ bench += " -p ${this.postData}" }
        if(contentType){ bench +=  " -H 'Content-Type: ${contentType}'" }
        if(token){ bench +=  " -H 'Authorization: Bearer ${token}'" }
        if(headers){
            headers.each(){
                bench +=  " -H '${it}'"
            }
        }

        bench += " -m ${method} ${endpoint}"
        def proc = ['bash', '-c', bench].execute()
        proc.waitFor()
        DecimalFormat df = new DecimalFormat("0.00")
        def outputStream = new StringBuffer()
        def error = new StringWriter()
        proc.waitForProcessOutput(outputStream, error)
        String output = outputStream.toString()
        List<String> returnData = ['0', '0', '0', '0', '0', '0', '0', '0', '0']
        String finalOutput = ""

        if (output) {
            List lines = output.readLines()
            lines.each() { it2 ->
                if(it2.trim()) {
                    finalOutput += it2 + " "
                    switch(it2){
                        case ~/Document Length:        ([0-9]+) bytes/:
                            //println "Document Length: ${Matcher.lastMatcher[0][1]}"
                            returnData[0] = df.format(Float.parseFloat(Matcher.lastMatcher[0][1]))
                            if(bytes==null){
                                bytes = "${returnData[0]} bytes"
                            }
                            break
                        case ~/Time taken for tests:   ([0-9\.]+) seconds/:
                            //println "Time taken for tests: ${Matcher.lastMatcher[0][1]}"
                            returnData[1] = df.format(Float.parseFloat(Matcher.lastMatcher[0][1]))
                            break
                        case ~/Complete requests:      ([0-9]+)/:
                            //println "Complete requests: ${Matcher.lastMatcher[0][1]}"
                            returnData[3] = df.format(Float.parseFloat(Matcher.lastMatcher[0][1]))
                            break
                        case ~/Failed requests:        ([0-9]+)/:
                            //println "Failed requests: ${Matcher.lastMatcher[0][1]}"
                            returnData[4] = df.format(Float.parseFloat(Matcher.lastMatcher[0][1]))
                            break
                        case ~/Total transferred:      ([0-9]+) bytes/:
                            //println "Total transferred: ${Matcher.lastMatcher[0][1]}"
                            returnData[5] = df.format(Float.parseFloat(Matcher.lastMatcher[0][1]))
                            break
                        case ~/HTML transferred:       ([0-9]+) bytes/:
                            //println "HTML transferred: ${Matcher.lastMatcher[0][1]}"
                            returnData[6] = df.format(Float.parseFloat(Matcher.lastMatcher[0][1]))
                            break
                        case ~/Requests per second:    ([0-9\.]+) \[#\/sec\] \(mean\)/:
                            //println "Requests per second: ${Matcher.lastMatcher[0][1]}"
                            returnData[2] = df.format(Float.parseFloat(Matcher.lastMatcher[0][1]))
                            break
                        case ~/Time per request:       ([0-9\.]+) \[ms\] \(mean, across all concurrent requests\)/:
                            //println "Time per request: ${Matcher.lastMatcher[0][1]}"
                            returnData[7] = df.format(Float.parseFloat(Matcher.lastMatcher[0][1]))
                            break
                        case ~/Transfer rate:          ([0-9\.]+) \[Kbytes\/sec\] received/:
                            //println "Transfer rate: ${Matcher.lastMatcher[0][1]}"
                            returnData[8] = df.format(Float.parseFloat(Matcher.lastMatcher[0][1]))
                            break
                        case ~/Connect: ( *[0-9]+) ( *[0-9]+) ( *[0-9\.]+) ( *[0-9]+) ( *[0-9]+)/:
                            returnData[9] = df.format(Float.parseFloat(Matcher.lastMatcher[0][2]))
                            break
                        case ~/Processing: ( *[0-9]+) ( *[0-9]+) ( *[0-9\.]+) ( *[0-9]+) ( *[0-9]+)/:
                            returnData[10] = df.format(Float.parseFloat(Matcher.lastMatcher[0][2]))
                            break
                        case ~/Waiting: ( *[0-9]+) ( *[0-9]+) ( *[0-9\.]+) ( *[0-9]+) ( *[0-9]+)/:
                            returnData[11] = df.format(Float.parseFloat(Matcher.lastMatcher[0][2]))
                            break
                        case ~/Total: ( *[0-9]+) ( *[0-9]+) ( *[0-9\.]+) ( *[0-9]+) ( *[0-9]+)/:
                            returnData[12] = df.format(Float.parseFloat(Matcher.lastMatcher[0][2]))
                            break
                    }
                }
            }
            println("[${returnData[0]}  bytes / ${returnData[8]} kb/s] : ${returnData[2]} rps")
        } else {
            println("[ERROR: apiBench]:  Error message follows : " + error)
        }
        return returnData
    }


    protected void createChart(String graphType, String title){
        try{
            String output
            String key = "set key left top;"
            String style = "set style textbox opaque;"
            String gridY = ""
            String gridX = ""
            String range
            String pointLabel
            String setTitle = ""
            String ylabel = ""
            String xlabel = ""
            String plot

            switch(graphType){
                case 'TIME':
                case 'TOTALTIME':
                    style = "set style textbox opaque;"
                    ylabel = "set ylabel \\\"RPS For Each Test\\\" ;"
                    switch(graphType){
                        case 'TIME':
                            output = "set term png;set output 'beapi_chart1.png';"
                            xlabel = "set xlabel \\\"Seconds To Do ${this.requests} Requests\\\" ;"
                            setTitle = "set title \\\"Time For Each API Test  (Plot Points show time for each request in ms)\\\" ;"
                            gridY = "set grid ytics lc rgb \\\"#bbbbbb\\\" lw 1 lt 0;"
                            gridX = "set xrange [*:] reverse;set grid xtics lc rgb \\\"#bbbbbb\\\" lw 1 lt 0;"
                            // 3::0 sets every 3rd tic / 1:3:8 (x-range, y-range, string)
                            //pointLabel = "every 3::0 using 1:3:8 with labels center boxed notitle"
                            pointLabel = "every 3::0 using 2:4:9 with labels center boxed notitle"
                            //range = "1:3:1"
                            range = "2:4:2"
                            break
                        case 'TOTALTIME':
                            output = "set term png;set output 'beapi_chart2.png';"
                            xlabel = "set xlabel \\\"Total Seconds For Tests\\\" ;"
                            setTitle = "set title \\\"Concatenated Time Of Concurrent API Tests (Plot Points show time for each request in ms)\\\" ;"
                            gridY = "set grid ytics lc rgb \\\"#bbbbbb\\\" lw 1 lt 0;"
                            gridX = "set grid xtics lc rgb \\\"#bbbbbb\\\" lw 1 lt 0;"
                            //pointLabel = "every 5::0 using 2:3:8 with labels center boxed notitle"
                            pointLabel = "every 5::0 using 3:4:9 with labels center boxed notitle"
                            //range = "2:3:2"
                            range = "3:4:3"
                            break
                    }
                    plot = "plot '${this.tmpPath}' using ${range} with linespoint pt 7 title \\\"${title}\\\",      ''          ${pointLabel}"
                    break
                case 'IO':
                    output = "set term png;set output 'beapi_chart3.png';"
                    xlabel = "set xlabel \\\"Test # of ${this.testSize} Tests\\\" ;"
                    key = "set key right top;"
                    gridY = "set grid ytics lc rgb \\\"#bbbbbb\\\" lw 1 lt 0;"
                    style = "set style data histograms;set style histogram rowstacked gap 10; set style fill solid 0.5 border -1;"
                    ylabel = "set ylabel \\\"Total Time in Milliseconds\\\" ;"
                    gridX = "set xtics border in scale 0,0 nomirror center; set xrange [0:${this.testSize}] noreverse writeback;set x2range [ * : * ] noreverse writeback;"
                    plot = "plot '${this.tmpPath}' using 11 t \\\"connection time\\\", '' using 13 t \\\"waiting\\\", '' using 12:xtic(1) t \\\"processing\\\";"
                    break;
                case 'SUCCESS_FAIL':
                    output = "set term png;set output 'beapi_chart4.png';"
                    xlabel = "set xlabel \\\"Test # of ${this.testSize} Tests\\\" ;"
                    key = "set key right top;"
                    gridY = "set grid ytics lc rgb \\\"#bbbbbb\\\" lw 1 lt 0; set yrange [0:${this.requests + (this.requests/2)}];"
                    style = "set style data histograms;set style histogram rowstacked gap 10; set style fill solid 0.5 border -1;"
                    ylabel = "set ylabel \\\"Total Number of Requests\\\" ;"
                    gridX = "set xtics border in scale 0,0 nomirror center; set xrange [0:${this.testSize}] noreverse writeback;set x2range [ * : * ] noreverse writeback;"
                    plot = "plot '${this.tmpPath}' using 5  every ::1 t \\\"successful requests\\\", '' using 6:xtic(1) t \\\"failed requests\\\";"
                    break;

            }

            String bench = "gnuplot -p -e \"${output}${gridX}${gridY}${xlabel}${ylabel}${setTitle}${key}${style}${plot};\""
            // println(bench)
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













