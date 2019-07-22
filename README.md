![alt text](https://github.com/orubel/logos/blob/master/beapi_logo_large.png)

# beapi-bench

Command line Benchmarking tool for apis. Allows users to set concurrency/requests and runs set number of tests to BOMBARD endpoint to see how it will respond. This is helpfull for numerous reasons:

- creating more stable/scalable configuration for application server(ie garbage collection, threading, synchronization of threads, etc)
- find bottlenecks and issues at points of scale
- find ideal scale point for load balancing.

Requirements: that Groovy, ApacheBench (ab) and GnuPlot already be installed (install from APT/YUM first).

Download & Install script locally and make script executable then run like so:

./BeapiBench.groovy --concurrency=300 --requests=4000 --token=<JWT_TOKEN> --method=GET --endpoint=http://localhost:8080/v1.3.0/person/show/225

or use shorthand like so...

./BeapiBench.groovy -c 300 -n 4000 -t <JWT_TOKEN> -m GET --endpoint=http://localhost:8080/v1.3.0/person/show/225

Upon completing all tests, the script will create a graph of the data like so:

![alt text](https://github.com/orubel/logos/blob/master/beapibench.png)
