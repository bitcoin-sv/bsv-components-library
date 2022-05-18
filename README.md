
# BSV Component Library for Java (JCL)

[![Java CI with Gradle](https://github.com/Danconnolly/bsv-components-library/actions/workflows/build.yml/badge.svg)](https://github.com/Danconnolly/bsv-components-library/actions/workflows/build.yml)

# Introduction

*JCL* is a Suite of libraries that provides a variety of functionality for use in Blockchain projects. Some Modules will be commonly used by any project, whereas others might only be included if required.

*JCL* is currently composed of the following modules:

 **JCL-Net**: Provides Connections and Streaming capabilities, allowing an App to connect to the blockchain and "listen" to Events coming from it, and also "send/broadcast" messages, and other operations.

 **JCL-Store**: Operations to *save* or *retrieve* information from a Repository. The information to store might be different depending on the different *Store-subModule* used, and the *Repository* might be different depending on the *implementation* provided (there are multiple implementations for this Module).
 
All the libraries within *JCL* make up a hierarchy dependency tree. 

*JCL* has a dependency on the *BitcoinJ-base*, where the *Domain Classes* for representing basic entities (Blocks, Transactions, Inputs, outputs, etc) are defined, along with much more functionality.


# How to import JCL

Since *JCL* is made up of different modules, "importing JCL" actually means "importing a JCL Module". The specific module to import will depends on your needs.

> **NOTE**
> *JCL-Tools* is an internal Library for use by JCL-Modules, not by the user, but it might be needed to be expecifically imported in the project along the rest of modules (some gradle engines might have some trouble getting transitive dependencies from private Maven repositories, so in those cases we might need to declare this dependency explicitely). The same can also be applied to the *bitcoinJ-SV* dependency, which must be explicitely declared as a dependency.

## Import JCL in a *Gradle* project

Edit your *build.gradle* file and include the definition of the Repository:

```
repositories {
    ...
    maven {
        url "http://ip:port/repository/maven-releases/"
        credentials {
            username = "[library repo] USER"
            password = "[library repo] PASSWORD"
        }
    }
}
```
> Bear in mind that the credentials are shown here in the *build.gradle* file only for academic purposes. In a real project those should be stored in a a separate file (*gradle.properties*) and **not** shared.

then, add the dependency (replace the module with the one you actually need):

```
dependencies {
...
implementation 'io.bitcoinsv.bitcoinjsv:base:1.0.0'
implementation 'io.bitcoinsv.jcl:jcl-tools:1.0.0'
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
            <id>Your Lib-Repository</id>
            <url>http://ip:port/repository/maven-releases/</url>
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
      <groupId>io.bitcoinsv.bitcoinjsv</groupId>
      <artifactId>base</artifactId>
      <version>1.0.0</version>
   </dependency>
	<dependency>
      <groupId>io.bitcoinsv.jcl</groupId>
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
      <id>Your-library-Repository</id>
      <username><[library repo] USER></username>
      <password><[library repo] PASSWORD></password>
    </server>
    ...
  </servers>
</settings>
```

> NOTE That the value of the **id** field must match the value of the **id** field in the *pom.xml* file.
