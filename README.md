![alt text](https://github.com/orubel/logos/blob/master/beapi_logo_large.png)

# beapi-bench

Command line Benchmarking tool for apis. Allows users to set concurrency/requests and runs set number of tests to BOMBARD endpoint to see how it will respond. For example, the following test was done LOCALLY (hence the low IO) on a 3.2GHZ 4-core processor with 2 GB of dedicated RAM:

![alt text](https://github.com/orubel/logos/blob/master/bench.png)

This benchmark is helpful for numerous reasons:
- creating more stable/scalable configuration for application server(ie garbage collection, threading, synchronization of threads, etc)
- find bottlenecks and issues at points of scale
- find ideal scale point for load balancing.

## System PreInstallation Requirements: 
- Groovy
- ApacheBench (ab)
- GnuPlot

## Ubuntu Installation:
You can install on Ubuntu simply by installing the PPA:
~~~~
sudo add-apt-repository ppa:orubel/beapi-bench
sudo apt-get update
~~~~

## Usage 
Download & Install script locally and make script executable (chmod 775 BeapiBench.groovy) then run like so:
~~~~
./BeapiBench.groovy -m GET --endpoint=http://localhost:8080/v1.3.0/person/show/225
~~~~
This will use the DEFAULT VALUES to create the test. If you want to be more precise with your testing, you can create your test like so:
~~~~
./BeapiBench.groovy --concurrency=200 --requests=3000 --token=<JWT_TOKEN> --method=GET --endpoint=http://localhost:8080/v1.3.0/person/show/225 --testnum=50
~~~~
or use shorthand like so...
~~~~
./BeapiBench.groovy -c 200 -n 3000 -t <JWT_TOKEN> -m GET --endpoint=http://localhost:8080/v1.3.0/person/show/225 --testnum=50
~~~~

## Graph Types 
- TIME (-g TIME) : This benchmark is perfect for showing 'rampup' from ZERO. If you have a server that just started up and you want to test how quickly it ramps up, this is the test you run. This shows 'requests per second / time taken for entire test'

![alt text](https://github.com/orubel/logos/blob/master/bench_time.png)

- TOTALTIME (-g TOTALTIME) : This benchmark is an all purpose test that can be run anytime and shows each test in comparison to all other tests. It shows 'requests per second / test current totaltime'

![alt text](https://github.com/orubel/logos/blob/master/bench_alltime.png)

- IO (-g IO) : This benchmark measures throughput and the time it takes for connect time, processing time and wait time (for data to be returned) in milliseconds per test

![alt text](https://github.com/orubel/logos/blob/master/IO.png)

## Is there a good concurrency to requests ratio I should use?
Think of 'concurrency' as the STRENGTH OF THE TEST and 'requests' as the LENGTH OF THE TEST. 
- 'concurrency' sets how many concurrent requests happen per second. 
- 'requests' sets how many requests must be fulfilled until the test stops. 

I usually set concurrency to 2.5 - 5% of the number of requests (depending on how hard I want to hammer the server). You don't have to be exact but be aware that higher concurrency changes how many new threads the server will try to spawn early on; threads spawned are set in your config and the higher the thread count spawned earlier, the more processing power and memory it requires... so it is a balancing act to get a good thread count for your instance and required traffic.

Also, most single servers will start failing tests over 500 concurrency so try to set your concurrency to somewhere between 50-400 for most tests and requests between 1000-80000.

## Help File Output 
~~~~
usage: BeapiBench.groovy [<options>] -m=method --endpoint=url
OPTIONS:
 -c,--concurrency <property=value>   value for concurrent users per test run
                                     (usage: -c 50, --concurrency=50)
    --endpoint <property=value>      url for making the api call (usage:
                                     --endpoint=http://localhost:8080)
 -f,--force                          Force run without checking for dependencies
 -g,--graphtype <property=value>     type of graph to create: TIME / TOTALTIME /
                                     ALL; defaults to TESTTIME (usage: -g
                                     TOTALTIME)
 -h,--help                           Print this help text and exit (usage: -h,
                                     --help)
    --hardcore                       No pause between tests
 -j,--contenttype <property=value>   content-type header; defaults to
                                     'application/json' (usage: -c
                                     application/xml,
                                     --contenttype=application-xml)
 -m,--method <property=value>        request method for endpoint
                                     (GET/PUT/POST/DELETE)
 -n,--requests <property=value>      requests to make per test run (usage: -n
                                     1000, --requests=1000)
 -p,--postData <property=value>      txt file supplying POST data (usage: -p
                                     post.txt )
 -t,--token <property=value>         JWT bearer token (usage: -t
                                     wer4t56g356g356h35h,
                                     --token=wer4t56g356g356h35h)
    --testnum <property=value>       number of tests to run; defaults to 50
                                     (usage: --testNum=100)
 -H,--header <property=value>        optional header to pass (usage: -H
                                     <header>, --header=<header>)
BeapiBench is a tool for benchmarking and graphing api's. It requires that both
ApacheBench (ab) and gnuplot be preinstalled and available to run. Please make
sure these are available and installed via your repository. If you have any
questions, please visit us a http://beapi.io. Thanks again.
~~~~

