![alt text](https://github.com/orubel/logos/blob/master/beapi_logo_large.png)

# beapi-bench

Command line Benchmarking tool for apis. Allows users to set concurrency/requests and runs set number of tests to BOMBARD endpoint to see how it will respond. This is helpfull for numerous reasons:

- creating more stable/scalable configuration for application server(ie garbage collection, threading, synchronization of threads, etc)
- find bottlenecks and issues at points of scale
- find ideal scale point for load balancing.

## System PreInstallation Requirements: 
- Groovy
- ApacheBench (ab)
- GnuPlot

You can do this by running the following from your shell:
~~~~
sudo apt install groovy
sudo apt install apache-utils
sudo apt-install gnuplot
~~~~

Download & Install script locally and make script executable then run like so:
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
- TESTTIME (-g TESTTIME) : This test is perfect for showing 'rampup' from ZERO. If you have a server that just started up and you want to test how quickly it ramps up, this is the test you run. This shows 'requests per second / time taken for entire test'

![alt text](https://github.com/orubel/logos/blob/master/beapibench2.png)

- TESTOVERTIME (-g TESTOVERTIME) : This is an all purpose test that can be run anytime and shows each test in comparison to all other tests. It shows 'requests per second / test current totaltime'

![alt text](https://github.com/orubel/logos/blob/master/beapibench.png)

## Is there a good concurrency to requests ratio I should use?
This is a VERY common question and I generally use a concurrency that is 2.5 - 5% of the number of requests. You don't have to be exact but be aware that higher concurrency changes how many new threads will be spawned early on.

## Help File Output 
~~~~
usage: beapiBench [<options>] -m=method --endpoint=url
OPTIONS:
 -c,--concurrency <property=value>   value for concurrent users per test run
                                     (usage: -c 50, --concurrency=50)
    --endpoint <property=value>      url for making the api call (usage:
                                     --endpoint=http://localhost:8080)
 -h,--help                           Print this help text and exit (usage: -h,
                                     --help)
 -j,--contenttype <property=value>   content-type header; defaults to
                                     'application/json' (usage: -c
                                     application/xml,
                                     --contenttype=application-xml)
 -m,--method <property=value>        request method for endpoint
                                     (GET/PUT/POST/DELETE)
 -n,--requests <property=value>      requests to make per test run (usage: -n
                                     1000, --requests=1000)
 -p,--header <property=value>        optional header to pass (usage: -p
                                     <header>, --header=<header>)
 -t,--token <property=value>         JWT bearer token (usage: -t
                                     wer4t56g356g356h35h,
                                     --token=wer4t56g356g356h35h)
    --testnum <property=value>       number of tests to run; defaults to 50
                                     (usage: --testNum=100)
BeapiBench is a tool for benchmarking and graphing api's. It requires that both
ApacheBench (ab) and gnuplot be preinstalled and available to run. Please make
sure these are available and installed via your repository. If you have any
questions, please visit us a http://beapi.io. Thanks again.
~~~~

