# JCL-Store-LevelDB

*JCL-Store-LevelDB* provides an implementation for the *JCL-Store*, using *LevelDB* as the technology Stack.

> **Level DB** is a No-sQL DataBase that ptovides *Key-Value* storage. It runs as a embedded database, so its light-weight and has a very high performance.
> More info:
> 
> * [LevelDB - wikipedia](https://en.wikipedia.org/wiki/LevelDB)
> * [LevelDB - Official Site](https://github.com/google/leveldb)



*JCL-Store-LevelDB* provides implementation for the interfaces ``BlockStore``and ``BlockChainStore``. The use of those interfaces is not discussed here, since there is already a separate documentation for that. Instead, here we'll discuss how to create an **instance** of those interfaces and the pecualirites that apply to this implementation.

## Basic Setup:

The use of *JCL-Store-LevelDB* and any other *JCL* Module always follows the same approach: we define a *Configuration* for the Module we are using, and then we create an *instance* of that module applying that Configuration.

And in some cases there is no just one *Configuration*. there might be several *Configurations* that apply to different contexts. The most basic Configuration that can be set in *JCL* is a ``Runtime Configuration``:

```
RuntimeConfig runtimeConfig = new RuntimeConfigDefault();
```

The ``RuntimeConfig`` contains values about the *SW* and *HW* environment where the Application is going to be running. At the time of writting this documentation, the implementation provided by the ``RuntimeConfigDefault``class is enough for all cases, so in most of the examples below in this document, this configuration can be ommitted and will be automatically picked up by the system.

## BlockStore Setup:

The process is very straightforward: we create a ``BlockStoreLevelDBConfig``and we use it to get an instance:

```
RuntimeConfig runtimeConfig = new RuntimeConfigDefault();
BlockStoreLevelDBConfig dbConfig = BlockStoreLevelDBConfig.builder()
                    .config(runtimeConfig)
                    .build()
BlockStore db = BlockStoreLevelDB.builder()
                    .config(dbConfig)
                    .build()
```

After the instance is created, you can already start using the ``BlockStore`` module:
> * [JCL-Store: Using the BlockStore component](../../store/doc/README.md#BlockStore-interface)

The *Block* and *Transaction* Events are *DISABLED* by Default, to enabled them:

```
RuntimeConfig runtimeConfig = new RuntimeConfigDefault();
BlockStoreLevelDBConfig dbConfig = BlockStoreLevelDBConfig.builder()
                    .config(runtimeConfig)
                    .build()
BlockStore db = BlockStoreLevelDB.builder()
                    .config(dbConfig)
                    .triggerBlockEvents(true)
                    .triggerTxEvents(true)
                    .build()
```

> **IMPORTANT: Performance Tips**
> 
> Enabling the Events might affect the performance. Enabling the *Block* Events is not a problem, but enabling *Tx* Events might be. So it's a good practice to enable the *Tx* Events *ONLY* during tests and development, but 
> *DISABLE* them in production mode.
> 
> The way the Events are triggered is also a bit different depending on the method called. As a general rule, one methjod will trigger one Event. So if you store one block using the ''saveBlock'' method, one ¡¡BlocksSAvedEvent'' will be triggered, containig the *Hash* of that Block. If you are saving several Blocks, the ``saveBlocks``(plural) will trigger *ONE* single Event containing all the Hashes. the same applies for *Transactions*.
> 
> **As a general Rule, ALWAYS use the Plural version of the ``saveXX``methods, so one Event per method invocation is triggered, instead of one Event per each object being saved.**


## BlockChainStore Setup:

The process is very straightforward: we create a ``BlockChainStoreLevelDBConfig``and we use it to get an instance. BUt in this case, since the ``BlockChainStore``keeps information about the *Chain*, it's important to configure the component properly so we add the right *Blocks* in it, and NOT *Blocks* form other *Chains*. 

For this reason, one of the parameters needed for the *Copnfiguration* is the **genesis block** of the *Chain* we are going to work with. This *genesis* block will ONLY be used when the *DB* is *empty*, which will only happen during the first execution.

> The *BlockChainStore* component is Chain-aware, so we need to specifiy *what* Chain we are going to store (*BSV-Main*, *BSV-Stn*, *BTC-Main*, etc). The way to specify the *Chain* is by providing the **genesis** Block of that Chain. 

The *genesis* Block of each *Chain* can be obtained from the *Protocol Configuration* of that *Chain*, which is accesible from the *JCL-Net* Module:


```
RuntimeConfig runtimeConfig = new RuntimeConfigDefault();
BlockStoreLevelDBConfig dbConfig = BlockChainStoreLevelDBConfig()
                    .config(runtimeConfig)
                    .build()
BlockStore db = BlockStoreLevelDB.builder()
                    .config(dbConfig)
                    .genesisBlock(new ProtocolBSVMainConfig().getGenesisBlock())
                    .build()
```

After the instance is created, you can already start using the ``BlockStore``  module:
> * [JCL-Store: Using the BlockChainStore component](../../store/doc/README.md#BlockChainStore-interface)

In the ``BlockChainStore``component, the *Streaming* of the *Fork* and *PRUNE* Events is *ALWAYS* enabled.

### Automatic Prunning:

The `BlockChainStore``component can be configured to perform an *Automatic Prunning*. If enabled, a *Chain* will be pruned if the difference in *height* with the longest ^Chain* ios bigger than a *Threshold* defined during the Set up:

The following configuration enables the *Automatic Prunning* and sets up the Frequency of that Prunning and the difference in *Heighht* needed for a *Chain* top be pruned.


```
RuntimeConfig runtimeConfig = new RuntimeConfigDefault();
BlockStoreLevelDBConfig dbConfig = BlockChainStoreLevelDBConfig()
                    .config(runtimeConfig)
                    .build()
BlockStore db = BlockStoreLevelDB.builder()
                    .config(dbConfig)
                    .genesisBlock(new ProtocolBSVMainConfig().getGenesisBlock())
                    .enableAutomaticPrunning(true)
                    .prunningFrequency(Duration.ofSeconds(60)) 
                    .prunningHeightDifference(2)
                    .build()
```

Using the Configuration above, the *BlockChainStore* component will perform the verification every 60 seconds, and only those *Chains* that are 2 *Blocks* *shorter* that the longrst *Chain* will be pruned.

> **Performance Tips**
> 
> It's ok to use a short frequency for testing purposes (like in the e4xample above), but in a real scenario, a frequency of several hours is just fine.