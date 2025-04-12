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

/*
First one must authenticate:
 ./BeapiBench_new.groovy
    -c=./cookies.txt
    -H="Content-Type: application/json"
    --method=POST
    -a='{"username":"admin","password":"@6m!nP@s5"}'
    --endpoint=http://localhost:8080/authenticate

Then (once we have cookie and token) we make our api call with the token
(note: cookie is gotten from cookie file)
./BeapiBench_new.groovy
    -T='JSESSIONID':'A301DC7D052980C009BEFF349DF786D0'
    --token=eyJvcmlnaW4iOiIxMjcuMC4wLjEiLCJicm93c2VyIjoiVW5rbm93biIsIm9zIjoiVW5rbm93biIsImFsZyI6IkhTNTEyIn0.eyJzdWIiOiJhZG1pbiIsImV4cCI6MTczMzMzMjExMSwiaWF0IjoxNzMzMjQ1NzExfQ.y8PiB-1zOLxNVIDEcJDqRC3IU-Dv2pDGj6AH1lqk2tdtF6R2yfNB15kM4EzA4ZRzqx80UHhayo0fGxdsZ517HQ
    --concurrency=1000
    --requests=3000
    --testnum=50
    --method=GET
    --endpoint=http://localhost:8080/v1.0/user/show?id%3Dtest

 */

@Grab(group='commons-cli', module='commons-cli', version='1.4')
@Grab(group='org.apache.httpcomponents', module='httpclient', version='4.5.14')
@Grab(group='commons-io', module='commons-io', version='2.8.0')

import groovy.cli.commons.*;
import groovy.cli.picocli.*
import groovy.json.JsonSlurper
import java.text.DecimalFormat

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets

import java.text.DecimalFormat




import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse.PushPromiseHandler;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher


import org.apache.http.protocol.BasicHttpContext
import org.apache.http.protocol.HttpContext
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.impl.client.DefaultHttpClient
//import org.apache.http.client.HttpClient

import org.apache.commons.io.IOUtils

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

//import org.apache.http.HttpResponse
import org.apache.http.HttpEntity
import org.apache.http.message.BasicHeader
import org.apache.http.entity.StringEntity
import org.apache.http.entity.ByteArrayEntity
import org.apache.http.entity.ContentType

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;


import java.nio.charset.StandardCharsets

import org.apache.http.cookie.Cookie;

import org.apache.http.protocol.BasicHttpContext
import org.apache.http.protocol.HttpContext

import org.apache.http.client.methods.*
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.CookieStore
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.client.entity.UrlEncodedFormEntity;

import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.BasicCookieStore
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.Set;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.Set;

class BeapiBench {

    private List methods = ['GET', 'PUT', 'POST', 'DELETE']
    protected String method
    private List graphTypes = ['TIME', 'TOTALTIME', 'IO', 'SUCCESS_FAIL', 'ALL']
    private LinkedHashMap graphDesc = [
            'TIME'        : """The <b>Time Chart</b> is a measure of 3 separate variables: the Requests Per Second or 'RPS' (Y-coord), Number of seconds each test took (X-coord) and Number of milliseconds each request took (Point Label). This is meant to show ramp up from start highest output as well as what occurs with processor when you stay at high output for too long and where the processor/application is most 'comfortable'.""",
            'TOTALTIME'   : "",
            'IO'          : "",
            'SUCCESS_FAIL': ""
    ]
    protected String graphType = 'ALL'
    protected String endpoint

    private static final int NUM_THREADS = 50; // Number of concurrent threads
    private static final int NUM_REQUESTS = 1000; // Number of total requests
    protected Integer concurrency = 50
    protected Integer requests = 1000
    protected String token = "eyJvcmlnaW4iOiIxMjcuMC4wLjEiLCJicm93c2VyIjoiVW5rbm93biIsIm9zIjoiVW5rbm93biIsImFsZyI6IkhTNTEyIn0.eyJzdWIiOiJhZG1pbiIsImV4cCI6MTczMDM5MzM3NCwiaWF0IjoxNzMwMzA2OTc0fQ.MVBaKNhAVmN6BIJS0cG9prdgt2HjatbEoLDKUHmCLzqggV6pxeyHL55euPC7a94Qqax0DUDFyeHKakqX1KcgZw";
    protected List headers
    protected String contentType = 'application/json'
    protected String path = '/tmp/'
    protected String filename = 'beapiBench.txt'
    protected String tmpPath = path + filename
    boolean noHardcore = true
    Float totalTime
    Integer testSize = 50
    String postData
    String bytes


    static void main(String[] args) {
        CommandLineInterface cli = CommandLineInterface.INSTANCE
        cli.parse(args)
    }

    /*
    static void main(String[] args) {
        //CommandLineInterface cli = CommandLineInterface.INSTANCE
        //cli.parse(args)
        String targetUrl = "http://localhost:8080/v1.0/user/show?id%3Dtest"; // Target URL

        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        for (int i = 0; i < NUM_REQUESTS; i++) {
            executor.submit(() -> {
                try {
                    sendRequest(targetUrl,"eyJvcmlnaW4iOiIxMjcuMC4wLjEiLCJicm93c2VyIjoiVW5rbm93biIsIm9zIjoiVW5rbm93biIsImFsZyI6IkhTNTEyIn0.eyJzdWIiOiJhZG1pbiIsImV4cCI6MTczMDQwMTA0OSwiaWF0IjoxNzMwMzE0NjQ5fQ.f6HJcSjCWKzFDnF2DmHnnjemCEBxWCBcRxY7tP3mykyCtZ-9y1hQraC4C7_NE8m2tiRIYzbUI3gQPaizs6eYlw")
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        executor.shutdown();
    }
     */


    enum CommandLineInterface{
        INSTANCE

        //HttpClient httpClient = new DefaultHttpClient();

        private List methods = ['GET', 'PUT', 'POST', 'DELETE']
        protected String method
        private List graphTypes = ['TIME', 'TOTALTIME', 'IO', 'SUCCESS_FAIL', 'ALL']
        protected String graphType = 'ALL'
        protected String endpoint
        protected Integer concurrency = 50
        protected Integer requests = 1000
        protected String resource
        protected Integer transferred
        protected String token
        protected List headers = []
        protected String contentType = 'application/json'
        protected Map<String, String> cookie = [:]
        protected BasicClientCookie cookieStorage
        protected String path = '/tmp/'
        protected String filename = 'beapiBench.txt'
        protected String tmpPath = path + filename
        boolean noHardcore = true
        Float totalTime
        Integer testSize = 50
        String postData
        LinkedHashMap authData
        String protocol = 'http2'


        CliBuilder cliBuilder

        CommandLineInterface() {
            cliBuilder = new CliBuilder(
                    usage: 'BeapiBench [<options>] -m=method --endpoint=url',
                    header: 'OPTIONS:',
                    footer: "BeapiBench is a tool for benchmarking and graphing api's. It requires that both ApacheBench (ab) and gnuplot be preinstalled and available to run. Please make sure these are available and installed via your repository. If you have any questions, please visit us a http://beapi.io. Thanks again."
            )

            cliBuilder.width = 80
            cliBuilder.with {
                // HELP OPT
                h(longOpt: 'help', 'Print this help text and exit (usage: -h, --help)')

                // FORCE OPT
                f(longOpt: 'force', 'Force run without checking for dependencies')

                // REQUIRED TEST OPTS
                m(longOpt: 'method', args: 2, valueSeparator: '=', argName: 'property=value', 'request method for endpoint (GET/PUT/POST/DELETE)')
                _(longOpt: 'endpoint', args: 2, valueSeparator: '=', argName: 'property=value', 'url for making the api call (usage: --endpoint=http://localhost:8080)')
                _(longOpt: 'testnum', args: 2, valueSeparator: '=', argName: 'property=value', 'number of tests to run; defaults to 50 (usage: --testNum=100)')
                _(longOpt: 'hardcore', 'No pause between tests')
                g(longOpt: 'graphtype', args: 2, valueSeparator: '=', argName: 'property=value', 'type of graph to create: [TIME, TOTALTIME, IO, SUCCESS_FAIL, ALL]; defaults to TESTTIME (usage: -g TOTALTIME)')

                // OPTIONAL TEST OPTS
                d(longOpt: 'data', args: 2, valueSeparator: ' ', argName: 'property=value', 'value for concurrent users per test run (usage: -c 50, --concurrency=50)')
                c(longOpt: 'cookie', args: 2, valueSeparator: '=', argName: 'property=value', 'value for concurrent users per test run (usage: -c 50, --concurrency=50)')
                C(longOpt: 'concurrency', args: 2, valueSeparator: '=', argName: 'property=value', 'value for concurrent users per test run (usage: -c 50, --concurrency=50)')
                T(longOpt: 'cookie',args: 2, argName: 'property=value', 'key/val pair for passing cookie value (ie -C=\'JSESSIONID\':\'DDADD5351AD3DCAE8906F3C2FDFB8A93\')')
                n(longOpt: 'requests', args: 2, valueSeparator: '=', argName: 'property=value', 'requests to make per test run (usage: -n 1000, --requests=1000)')
                t(longOpt: 'token', args: 2, valueSeparator: '=', argName: 'property:value', 'JWT bearer token (usage: -t wer4t56g356g356h35h, --token=wer4t56g356g356h35h)')
                H(longOpt: 'header', args: 2, valueSeparator: '=', argName: 'property=value', 'optional header to pass (usage: -H <header>, --header=<header>)')
                j(longOpt: 'contenttype', args: 2, valueSeparator: '=', argName: 'property=value', "content-type header; defaults to 'application/json' (usage: -c application/xml, --contenttype=application-xml)")
                p(longOpt: 'postData', args: 2, valueSeparator: '=', argName: 'property=value', 'txt file supplying POST data (usage: -p post.txt )')

                // new_functionality
                a(longOpt: 'auth', args: 2, valueSeparator: '=', argName: 'property=value', 'token authentication handshake(usage: -i={"username":"admin","password":"@6m!nP@s5"} )')

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

                // NO ARGS
                if (!args) {
                    throw new Exception('Could not parse command line options.\n')
                }

                // HELP
                if (options.h) {
                    cliBuilder.usage()
                    System.exit 0
                }

                if (options.m || options.method){
                    if(options.endpoint) {
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
                    }else{
                        throw new Exception('Endpoint(--endpoint) is REQUIRED FLAGS for BeapiBench to work. Please try again.\n')
                    }
                }else{
                    throw new Exception('Method(--method) is REQUIRED FLAGS for BeapiBench to work. Please try again.\n')
                }

                // --AUTH
                if (options.a){
                    if(options.m && options.endpoint) {
                        this.authData = new groovy.json.JsonSlurper().parseText(options.a.trim())
                        try {
                            URL url = new URL(options.endpoint)
                        } catch (Exception e) {
                            throw new Exception('Endpoint is not a valid URL. Please try again', e)
                        }
                        this.endpoint = options.endpoint
                    }else{
                        throw new Exception('Authentication requires an endpoint(--endpoint) and method(-m).\n')
                    }
                }

                // --COOKIE
                if (options.c){
                        def temp = options.c
                        this.authData = new groovy.json.JsonSlurper().parseText(options.a.trim())
                        try {
                            URL url = new URL(options.endpoint)
                        } catch (Exception e) {
                            throw new Exception('Endpoint is not a valid URL. Please try again', e)
                        }
                        this.endpoint = options.endpoint

                }

                // --CONCURRENCY
                if (options.C) {
                    try {
                        this.concurrency = options.C as Integer
                        if (!(this.concurrency > 0)) {
                            throw new Exception('Concurrency must be a positive number greater than 0. Please try again.')
                        }
                    } catch (Exception e) {
                        throw new Exception('Concurrency (--concurrency ,-c) expects an Unsigned Integer greater than 0. Please try again.', e)
                    }
                }

                if (options.T) {
                    try {
                        List temp = options.T.split(':')
                        this.cookie[temp[0]] = temp[1]
                    } catch (Exception e) {
                        println(e)
                        throw new Exception('Cookie requires a key/value pair', e)
                    }
                }


                if (options.n) {
                    try {
                        this.requests = options.n as Integer
                        if (!(this.requests > 0)) {
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
                        if (!(temp > 0)) {
                            throw new Exception('Testnum (--testnum)must be a positive number greater than 0. Please try again.')
                        }
                        this.testSize = temp
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
            List rps = []
            List rng = []
            Float avgNum = 0.0
            Float maxNum = 0.0

            // PROCESS
            Properties props = readProperties()
            if(options.a){

                // require content-type
                // require endpoint
                //require method
                // no concurrency; if has concurrency then  'fail'
                // no requestNum; if has requestNum then  'fail'

                //
                if(props.hasProperty("token")){
                    println("### resetting current token : "+props.token)
                }

                if(this.authData){
                    println("### auth data : "+this.authData)
                }
                HttpResponse response = sendAuthRequest(this.authData, this.contentType, 'POST' , this.endpoint, this.headers)

                int statusCode = response.statusCode()
                println("Status Code: " + statusCode);

                //String responseBody = IOUtils.toString(response.stream(), StandardCharsets.UTF_8);
                String responseBody = response.body()
                println("Response: " + responseBody);

            }else {
                while (i < this.testSize) {
                    // call method; move this to method
                    print("[TEST ${i + 1} of ${this.testSize}] : ")

                    List returnData = callApi(this.postData, this.concurrency, this.requests, this.contentType, this.token, this.method, this.endpoint, this.headers, this.cookie)
                    rps.add(Float.parseFloat(returnData[2]))
                    rng.add(Float.parseFloat(returnData[1]))

                    if (!this.resource) {
                        this.resource = returnData[0] + " bytes"
                    }
                    if (!this.transferred) {
                        this.transferred = ((Integer) Float.parseFloat(returnData[5])) / this.requests
                    }

                    if (!returnData.isEmpty()) {
                        DecimalFormat df = new DecimalFormat("0.00")
                        if (data.size() > 0) {
                            int size = data.size() - 1
                            // TODO: will fail here if tests fail; need to create a way to continue; test returnData
                            //try {
                            Float floatTemp1 = Float.parseFloat(returnData[1])
                            Float floatTemp2 = Float.parseFloat(data[size][1])
                            Float floatTemp3 = Float.sum(floatTemp1, floatTemp2)
                            List temp = [returnData[1], df.format(floatTemp3), returnData[2], df.format(Float.parseFloat(returnData[3])), df.format(Float.parseFloat(returnData[4])), df.format(Float.parseFloat(returnData[5])), df.format(Float.parseFloat(returnData[6])), df.format(Float.parseFloat(returnData[7])), df.format(Float.parseFloat(returnData[8])), returnData[9], returnData[10], returnData[11], returnData[12]]
                            data.add(temp)

                            this.totalTime = floatTemp3
                            //}catch(Exception e){
                            //    println("${returnData} :" +e)
                            //}
                        } else {
                            // time/totaltime/rps
                            List temp = [returnData[1], returnData[1], returnData[2], returnData[3], returnData[4], returnData[5], returnData[6], returnData[7], returnData[8], returnData[9], returnData[10], returnData[11], returnData[12]]
                            data.add(temp)
                        }
                    } else {
                        println(" TEST FAILED. Set concurrency/requests lower or change server config.")
                    }

                    if (this.noHardcore) {
                        float waitTime = Float.parseFloat(returnData[1])
                        waitTime = ((waitTime * 1000) * 1.8)
                        sleep(waitTime as Integer)
                    }

                    i++
                }

                List range = [rng.min(), rng.max()]
                avgNum = rps.sum() / rps.size()

                maxNum = rps.max()
                Float avgMaxMedian = avgNum + maxNum / 2

                // CREATE DATA FILE
                File apiBenchData = new File("${this.tmpPath}")
                if (apiBenchData.exists() && apiBenchData.canRead()) {
                    apiBenchData.delete()
                }
                apiBenchData.append('# doc   sum   time   rps   success   fail   data   html   tpr   transferrate   connect   processing   waiting   ttime\n')

                i = 1
                data.each() {
                    apiBenchData.append "${i}   "
                    apiBenchData.append it.join('   ')
                    apiBenchData.append '\n'
                    i++
                }

                // REMOVE PREVIOUS GRAPHS
                1..4.each() {
                    def file = new File("beapi_chart${it}.png")
                    if (file.exists() && file.canRead()) {
                        file.delete()
                    }
                }
            }



            // CREATE GRAPH
            /*
        String title = "${this.concurrency} c / ${this.requests} n / ${this.testSize} tests}"
        if(this.graphType!='ALL') {
            //println("[tmp gnuplot file] >> "+this.tmpPath)
            createChart(this.graphType,"${title}", avgNum, avgMaxMedian, range)
        }else{
            //println("[tmp gnuplot file] >> "+this.tmpPath)
            this.graphTypes.each(){
                if(it!='ALL'){
                    createChart(it,title, avgNum, maxNum, range)
                }
            }
        }
        createFile()
         */
        }

        // TODO
        protected testConnection() {

        }

        protected Properties readProperties(){
            Properties properties = new Properties();

            this.getClass().getResource( './beapi_bench.properties' ).withInputStream {
                properties.load(it)
            }
            // println(properties.token)

            return properties

        }

        protected List callApi(String postData, Integer concurrency, Integer requests, String contentType, String token, String method, String endpoint, List headers, Map cookie) {
            // start time


            String bench = "ab -c ${concurrency} -n ${requests}"

            if (cookie) {
                bench += " -C ${cookie.key}=${cookie.value}"
            }

            if (postData) {
                bench += " -p ${this.postData}"
            }
            if (contentType) {
                bench += " -H 'Content-Type: ${contentType}'"
            }
            if (token) {
                bench += " -H 'Authorization: Bearer ${token}'"
            }
            if (headers) {
                headers.each() {
                    bench += " -H '${it}'"
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
            List num = []
            String finalOutput = ""


            if (output) {
                List lines = output.readLines()
                lines.each() { it2 ->
                    if (it2.trim()) {
                        finalOutput += it2 + " "
                        switch (it2) {
                            case ~/Document Length:        ([0-9]+) bytes/:
                                //println "Document Length: ${Matcher.lastMatcher[0][1]}"
                                returnData[0] = df.format(Float.parseFloat(Matcher.lastMatcher[0][1]))
                                break
                            case ~/Time taken for tests:   ([0-9]*\.[0-9]*) seconds/:
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
                                //avg_num[4] = (returnData.each() as Integer).sum() / 9
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

        public BodyPublisher ofFormData(Map<Object, Object> data) {
            var builder = new StringBuilder();
            for (Map.Entry<Object, Object> entry : data.entrySet()) {
                if (builder.length() > 0) {
                    builder.append("&");
                }
                builder.append(URLEncoder.encode(entry.getKey().toString(), StandardCharsets.UTF_8));
                builder.append("=");
                builder.append(URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8));
            }
            return BodyPublishers.ofString(builder.toString());
        }

        protected HttpResponse sendAuthRequest(LinkedHashMap authData, String contentType, String method, String endpoint, List headers) {
            // todo : autopopulate from 'beapi_bench.properties' (see file)
            HttpContext localContext = new BasicHttpContext();
            println(authData)
            println(authData.getClass())

            Map<Object, Object> data = new HashMap<>();
            data.put("username", authData['username']);
            data.put("password", authData['password']);


            switch(method){
                case 'POST':
                    HttpClient client = HttpClient.newHttpClient();
                    HttpRequest request = HttpRequest.newBuilder(new URI(endpoint))version(HttpClient.Version.HTTP_2).POST(ofFormData(data)).headers("Content-Type", "application/json").build();
                    HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
                    println(response.body())

                    //assertThat(response.version(), equalTo(HttpClient.Version.HTTP_1_1));

                /*
                    final List<Cookie> cookies = client.getCookieStore().getCookies();
                    cookies.each(){ it ->
                        if(it.getName()=='JSESSIONID'){
                            this.cookieStorage = it
                        }
                    }

                 */

                    return response
                    break;
                default:
                    //throw error
                    return null
                    break;
            }

        }
    }




}
