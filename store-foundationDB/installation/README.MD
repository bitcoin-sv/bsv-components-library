
## Installation
First, install the Client & Server libraries from there:
 
[FoundationDB Client & Server Packages](https://www.foundationdb.org/download/)    


During the installation process, you can select either only Client or both Client & Server options. 

 * If you select both, the you'll have a local FDB Database that will be started automatically, so the tests will work by just using
the default configuration (where no cluster file is specified, since in that case the default cluster file will be picked up.)

 * If you only select the *Client* Libraries, then you will be able to use the command-line *fdbcli* client, but in that 
case you won't have a local *DB* running, so you will have to use another **cluster** file where you specify the DB you want to connect to.


### Running a Local FoundationDB Server

If you have selected the *Server* option during the initial installation, you must have already a *FoundationDB* running, 
since it automatically starts by default. In this case, the *Cluster* file used is one generaated by default and stored 
in a specific folder, which aries depending on the OS:

 * Linux: /etc/foundationdb/fdb.cluster
 * macOS: /usr/local/etc/foundationdb/fdb.cluster

You can also start and Stop the Server with a Command-line:
 
[https://apple.github.io/foundationdb/administration.html](Check FoundationDB Administration):



### Running a FoundationDB instance in a Docker container

The tests under the */test* directory already take care of starting and stopping the service. If you still want to 
start it yourself in order to do some quick tests or main programs, you can do it by just running a compose file.


In the **/installation** folder there are the files needed to run a **FoundationDB** DB in a **Docker** container, and 
the **cluster** file needed to connect to it.

The following command starts the Container:

``docker-compose -f docker-compose.yml -d up fdb``

That will start the DB within the container. To check that is running, you can run the *FoundationDB Client Command Line*, 
which yu must have been installed previously. In this case, you'll have to specify the *cluster* file defined in the 
*/installation* folder, which *points to* the Db running *inside* the Container:

You open a Command line console and execute:

`` fdbcli -C $CLUSTER_FILE --exec "configure new single memory" ``

That will connect to the Server and will create a Default DataBase, so we can connect to.

If everything goes right, after that you will still be connected through the *fdbcli* client which provides a 
*prompt* you can use to run some commands, for example the *status* command, which should return info about the 
Server, like this:


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
