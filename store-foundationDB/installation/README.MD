
## Installation
First, install the Client & Server libraries from there:
 
[FoundationDB Client & Server Packages](https://www.foundationdb.org/download/)    


> During the installation process, you must select both *Client* and the *Server* options.

Then, you can run the *FoundationDB* Server by running the ``docker-compose``file:`

``docker-compose -f docker-compose.yml -d up fdb``

That will start the Server. To check the server is running, you can run the *FoundationDB Client Command Line*, which 
has been installed during the first step (Installation of the client & Server libraries).

You open a Command line console and execute:

`` fdbcli``

If everything goes right, the output will look like this one:

```
Using cluster file `/usr/local/etc/foundationdb/fdb.cluster'.

The database is available.

Welcome to the fdbcli. For help, type `help'.
```

if you type ``status`` and press *ENTER* you should see a status summary similar to this:

```
Using cluster file `/usr/local/etc/foundationdb/fdb.cluster'.

Configuration:
  Redundancy mode        - single
  Storage engine         - memory-2
  Coordinators           - 1
  Usable Regions         - 1

Cluster:
  FoundationDB processes - 1
  Zones                  - 1
  Machines               - 1
  Memory availability    - 4.2 GB per process on machine with least available
  Fault Tolerance        - 0 machines
  Server time            - 11/17/20 14:06:45

Data:
  Replication health     - Healthy
  Moving data            - 0.000 GB
  Sum of key-value sizes - 0 MB
  Disk space used        - 105 MB

Operating space:
  Storage server         - 1.0 GB free on most full server
  Log server             - 45.9 GB free on most full server

Workload:
  Read rate              - 19 Hz
  Write rate             - 0 Hz
  Transactions started   - 8 Hz
  Transactions committed - 0 Hz
  Conflict rate          - 0 Hz

Backup and DR:
  Running backups        - 0
  Running DRs            - 0

Client time: 11/17/20 14:06:45
```
