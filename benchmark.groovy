#!/usr/bin/env groovy

import groovy.json.JsonSlurper
import java.text.DecimalFormat;

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
class ApiBench {
    Integer testSize = 75
    Integer testTime = 60
    String login = 'admin'
    String password = 'Al1c3Inj@1L'
    String loginUri = '/api/login'
    String apiToken
    String testDomain = 'http://localhost:8080'
    //List concurrency = [50, 50, 75, 75, 100, 100, 125, 125, 150, 150, 200, 200, 250, 250, 300, 300, 350, 350, 400, 400]
    //List requests = [1000, 1500, 2000, 2500, 3000, 4000, 5000, 7500, 8000, 10500, 13000, 1700, 21000, 27500, 34000, 44500, 55000, 72000, 89000, 116500]
    String help = """

"""
    static void main(String[] args) {
        switch(arg){
            case '--help':
            case '-h':

            case '--file':
            case '-f':

            case '--concurrency':
            case '-c':

            case '--requests':
            case '-n':

            case '--endpoint':
            case '-e':

        }

        // if no file/endpoint, throw error explaining usage

        opt; do
        case $opt in
                a)
        echo "-a was triggered, Parameter: $OPTARG" >&2
        ;;
        \?)
        echo "Invalid option: -$OPTARG" >&2
        exit 1
        ;;
        :)
        echo "Option -$OPTARG requires an argument." >&2
        exit 1
        ;;
        esac

        String filename = args[0]
        ApiBench bench = new ApiBench()
        bench.initToken()
        if(bench.getData()){
            int i = 0
            List data = []
            while(i<bench.testSize){
                //String c = bench.concurrency.get(it)
                //String n = bench.requests[it]
                String method = "GET"
                String controller = 'person'
                String action = 'show'
                String url = "${bench.testDomain}/v1.3.0/${controller}/${action}?id=225"


                // call method; move this to method
                List returnData = bench.callApi(url, bench.apiToken, method)
                if(data.size()>0){
                    DecimalFormat df = new DecimalFormat("0.00")
                    int size = data.size()-1
                    Float floatTemp1 =  Float.parseFloat(returnData[1])
                    Float floatTemp2 = Float.parseFloat(data[size][0])
                    String floatTemp3 = Float.sum(floatTemp1,floatTemp2) as String
                    //Float result = df.format(floatTemp3)
                    List temp = [floatTemp3,returnData[2]]
                    data.add(temp)
                }else{
                    List temp = [returnData[1],returnData[2]]
                    data.add(temp)
                }

                float waitTime = Float.parseFloat(returnData[1])
                waitTime = (waitTime*1000)
                sleep(waitTime as Integer)

                i++
            }

            // output to file
            // TODO: check if file exists and ask if they want to overwrite if it does
            def apiBenchData = new File(filename)
            apiBenchData.append('# X   Y\n')
            data.each() {
                apiBenchData.append '   '
                apiBenchData.append it.join('   ')
                apiBenchData.append '\n'
            }
            //println(apiBenchData.text)
        }

    }

    protected List callApi(String url, String token, String method){
        String bench = "ab -c 100 -n 5000 -H 'Content-Type: application/json' -H'Authorization: Bearer ${token}' -m ${method} ${url}"
        def proc = ['bash', '-c', bench].execute()
        proc.waitFor()
        DecimalFormat df = new DecimalFormat("0.00")
        def outputStream = new StringBuffer()
        def error = new StringWriter()
        proc.waitForProcessOutput(outputStream, error)
        String output = outputStream.toString()
        List<String> returnData = []
        if (output) {
            List lines = output.readLines()
            lines.each(){ it2 ->
                if (it2 =~ /Document Length/) {
                    List temp = it2.split(':')
                    String temp2 = temp[1].trim()
                    List temp3 = temp2.split(' ')
                    Float temp4 = Float.parseFloat(temp3[0])
                    returnData.add(df.format(temp4))
                    print("${df.format(temp4)}/")
                }
                if (it2 =~ /Time taken for tests/) {
                    List temp = it2.split(':')
                    String temp2 = temp[1].trim()
                    List temp3 = temp2.split(' ')
                    Float temp4 = Float.parseFloat(temp3[0])
                    returnData.add(df.format(temp4))
                    print("${df.format(temp4)}/")
                }
                if (it2 =~ /Requests per second/) {
                    List temp = it2.split(':')
                    String temp2 = temp[1].trim()
                    List temp3 = temp2.split(' ')
                    Float temp4 = Float.parseFloat(temp3[0])
                    returnData.add(df.format(temp4))
                    println("${df.format(temp4)}")
                }
            }
        } else {
            println("[ERROR: apiBench]:  Error message follows : " + error)
        }
        return returnData
    }

    protected void initToken(){
        def info
        String url = "curl -v  -H 'Content-Type: application/json' -H 'Origin: http://localhost' -H 'Access-Control-Request-Headers: Origin,X-Requested-With' -d '{\"username\":\"${login}\",\"password\":\"${password}\"}' ${this.testDomain}${loginUri}"
        def proc = ['bash', '-c', url].execute()
        proc.waitFor()
        def outputStream = new StringBuffer()
        def error = new StringWriter()
        proc.waitForProcessOutput(outputStream, error)
        String output = outputStream.toString()

        if (output) {
            info = new JsonSlurper().parseText(output)
        } else {
            println("[ERROR: setToken]: Check that credentials are correct and that OAuth server is UP. Error message follows : " + error)
        }

        this.apiToken = info.access_token
    }

    boolean getData() {
        String METHOD = "GET"
        String controller = 'person'
        String action = 'show'
        LinkedHashMap info = [:]

        def proc = ["curl", "-H", "Content-Type: application/json", "-H", "Authorization: Bearer ${this.apiToken}", "--request", "${METHOD}", "${this.testDomain}/v1.3.0/${controller}/${action}?id=225"].execute()
        proc.waitFor()
        def outputStream = new StringBuffer()
        def error = new StringWriter()
        proc.waitForProcessOutput(outputStream, error)
        String output = outputStream.toString()

        def slurper = new JsonSlurper()
        slurper.parseText(output).each() { k, v ->
            info[k] = v
        }

        if(info != [:]) {
            return true
        }else{
            return false
        }
    }


}