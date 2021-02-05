
# JCL: Java Component Library for Bitcoin


# Introduction

*JCL* is a Java Library or Suite of libraries that provides different functionalities for use in a Blockchain projects. Some Modules will be commonly used by any project, whereas others might only be included if needed.

*JCL* is currently composed of the following modules:

> **Click on the Links below to access each module specific documentation:**

 
 * [JCL-Net](../net/doc/README.md): Provides Connections and Streaming capabilities, allowing an App to connect to the blockchain and "listen" to Events coming from it, and also "send/broadcast" messages, and other operations.

 * [JCL-Store](../store/doc/README.md): Operations to *save* or *retrieve* information from a Repository. The information to store might be different depending on the different *Store-subModule* we use, and the *Repository* might be different deopending on the *implementation* provided (there are multiple implementations for this Module)
 

All the libraries within *JCL* make up a hierarchy dependency tree, as shown in the diagram below. *JCL* has a dependency on the *BitcoinJ-base*, where the *Domain Classes* for representing basic entities (Blocks, Transactions, Inputs, outputs, etc) are defined, along with many other functionalities.

![high level architecture](jcl-highLevel.png) 




# How to import JCL

Since *JCL* is made up of different modules, "importing JCL" actually means "importing a JCL Module". The specific module to import will depends on your needs.

> **NOTE**
> The JCL Modules are stored in a *Nexus* server in *nChain* premises, so before going any further you'll need a *user* and *password* so you can access it and download the libraries from it. From this moment moving forward, the credentials will be referenced as **NEXUS_USER** and **NEXUS_PASSWORD** in this documentation.

> *JCL-Tools* is an internal Library for use by JCL-Modules, not by the user, but it might be needed to be expecifically imported in the project along the rest of modules (some gradle engines might have some trouble getting transitive dependencies from private Maven repositories, so in those cases we might need to declare this dependency explicitely).  The same can also be applied to the *bitcoinJ* dependency, which must be explicitely declared as a dependency.

## Import JCL in a *Gradle* project

Edit your *build.gradle* file and include the definition of the Repository:

```
repositories {
    ...
    maven {
        url "http://161.35.175.46:8081/repository/maven-releases/"
        credentials {
            username = "NEXUS USER"
            password = "NEXUS PASSWORD"
        }
    }
}
```
> Bear in mind that the credentials are shown here in the *build.gradle* file only for academic purposes. In a real project those should be stored in a a separate file (*gradle.properties*) and **not** shared.

then, add the dependency (replace the module with the one you actually need):

```
dependencies {
...
implementation 'com.nchain.bitcoinj:base:2.0'
implementation 'com.nchain.jcl:jcl-tools:1.0.0'
...
}

```

## Import JCL in a *Maven* project

You need to define a new Repository in your *pom.xml* file:

```
<repositories>
	...
	<repository>
            <!--
            The username and password are retrieved by looking for the Repository
            Id in the $HOME/.m2/settings.xml file.
            -->
            <!-- id Must Match the Unique Identifier in settings.xml -->
            <id>nChain-Nexus-Repository</id>
            <url>http://161.35.175.46:8081/repository/maven-releases/</url>
            <releases/>
        </repository>
	...
</repositories>
```

then, add the dependency:

```
<dependencies>
	...
	<dependency>
      <groupId>com.nchain.bitcoinj</groupId>
      <artifactId>base</artifactId>
      <version>2.0</version>
   </dependency>
	<dependency>
      <groupId>com.nchain.jcl</groupId>
      <artifactId>jcl-tools</artifactId>
      <version>1.0.0</version>
   </dependency>
	...
</dependencies>

```

And you must store the credentials in the *settings.xml* file:

```
<settings>
  <servers>
    ...
    <server>
      <id>nChain-Nexus-Repository</id>
      <username><NEXUS USER></username>
      <password><NEXUS PASSWORD></password>
    </server>
    ...
  </servers>
</settings>
```

> NOTE That the value of the **id** field must match the value of the **id** field in the *pom.xml* file.


# How to set up *Lombok*

*Lombok* is a *plugin* that includes a set of *Annotations* and a Pre-processor that parses automatically the source fles and generates new methods or extends some classes in the *generated class files*. Examples of these are the autogeneration of *getters*. *builders*, etc.

## How to enable *Lombok* in IntelliJ IDEA
The *Lombok* libraries are already included in the project, so we only need to *tell* the IDE to be aware of it, so the auto-complete features of the IDE can work properly, among others.

go to *Preferences > Build, Execution, Deployment* and check the *Enable annotation processing* check on the top right corner. Also make usre the *Obtain processors from roject classpath* is selected, right below it.

![high level architecture](lombokInIntelliJIDEA.png) 


## How to enable *Lombok* in Eclipse
The installation in *Eclipse* takes an additional step, bt it's also very straightforward. You can get the instructions [here](https://www.baeldung.com/lombok-ide#eclipse)