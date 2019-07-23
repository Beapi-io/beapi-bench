![alt text](https://github.com/orubel/logos/blob/master/beapi_logo_large.png)

# beapi-bench

Command line Benchmarking tool for apis. Allows users to set concurrency/requests and runs set number of tests to BOMBARD endpoint to see how it will respond. This is helpfull for numerous reasons:

- creating more stable/scalable configuration for application server(ie garbage collection, threading, synchronization of threads, etc)
- find bottlenecks and issues at points of scale
- find ideal scale point for load balancing.

Requirements: that Groovy, ApacheBench (ab) and GnuPlot already be installed (install from APT/YUM first).

Download & Install script locally and make script executable then run like so:

./BeapiBench.groovy --concurrency=200 --requests=3000 --token=<JWT_TOKEN> --method=GET --endpoint=http://localhost:8080/v1.3.0/person/show/225

or use shorthand like so...

./BeapiBench.groovy -c 200 -n 3000 -t <JWT_TOKEN> -m GET --endpoint=http://localhost:8080/v1.3.0/person/show/225

Upon completing all tests, the script will create a graph of the data like so:

![alt text](https://github.com/orubel/logos/blob/master/beapibench.png)

NOTE: There is a PAUSE between each test of (time taken for previous test*2). This is to similate a natural traffic increase over time as traffic does not instantly start at 1,000,000/rps and continue that way. As each test gets faster with the number of threads/workers spawned, the time between tests decreases thus simulating an increased spike in traffic as well.

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

