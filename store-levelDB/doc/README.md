# JCL-Store-LevelDB

*JCL-Store-LevelDB* provides an implementation for the *JCL-Store*, using *LevelDB* as the technology Stack.

> **Level DB** is a No-sQL DataBase that ptovides *Key-Value* storage. It runs as a embedded database, so its light-weight and has a very high performance.
> More info:
> 
> * [LevelDB - wikipedia](https://en.wikipedia.org/wiki/LevelDB)
> * [LevelDB - Official Site](https://github.com/google/leveldb)



The *JCL-Store-LevelDB* modue provides implementation for the interfaces ``BlockStore``and ``BlockChainStore``. The use of those interfaces is not discussed here, since there is already a separate documentation for that. Instead, here we'll discuss how to create an **instance** of those interfaces and the pecualirites that apply to this implementation.

## Basic Setup:

The use of *JCL-Store-LevelDB* and any other *JCL* Module always follows the same approach: we define a *Configuration* for the Module we are using, and then we create an *instance* of that module applying that Configuration.

And in some cases there is no just one *Configuration*. there might be several *Configurations* that apply to different contexts. The most basic Configuration that can be set in *JCL* is a ``Runtime Configuration``:

```
RuntimeConfig runtimeConfig = new RuntimeConfigDefault();
```

The ``RuntimeConfig`` contains values about the *SW* and *HW* environment where the Application is going to be running. At the time of writting this documentation, the implementation provided by the ``RuntimeConfigDefault``class is enough for all cases, so in most of the examples below in this document, this configuration can be ommitted and will be automatically picked up by the system.

## BlockStore Setup:

The process is very straightforward: we create a ``BlockStoreLevelDBConfig``and we use it to get an instance. This Configuration makes use of the *RuntimeConfiguration* defined previously.

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

The *Block* and *Transaction* Events are *DISABLED* by Default. To enabled them:

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
> The way the Events are triggered is also a bit different depending on the method called. As a general rule, one method will trigger one Event. So if you store one block using the ``saveBlock()`` method, one ``BlocksSavedEvent`` will be triggered, containig the *Hash* of that Block. If you are saving several Blocks, the ``saveBlocks()``(plural) will trigger *ONE* single Event containing all the Hashes. The same applies for *Transactions*.
> 
> **As a general Rule, ALWAYS use the Plural version of the ``saveXX``methods, so one Event per method invocation is triggered, instead of one Event per each object being saved.**

The following example shows the difference in relation to the Events Triggered:

```
BlockStore db = ...(from previous examples)

// We assume we have a List of 1000 Transactions:
List<Tx> txs = ...(a list of 1000 Txs)

// The following code will trigger 1000 'TxsSavedEvent' Events:
txs.forEach(tx -> db.save(tx)); // WARNING: IMPACT ON PERFORMANCE

// The following code will trigger just ONE 'TxsSavedEvent' Event:
db.saveTxs(txs);				// PERFORMANCE OK
```

## BlockChainStore Setup2:

## BlockChainStore Setup:

The process is very straightforward: we create a ``BlockChainStoreLevelDBConfig``and we use it to get an instance. But in this case, since the ``BlockChainStore`` keeps information about the *Chain*, it's important to configure the component properly so we add the right *Blocks* in it, and NOT *Blocks* form other *Chains*. 

For this reason, one of the parameters needed for the Configuration is the **genesis block** of the *Chain* we are going to work with. This *genesis* block will ONLY be used during the *DB* initialization, when the *DB* is *empty*, which will only happen during the first execution.

The *genesis* Block of each *Chain* can be obtained from the *Protocol Configuration* of that *Chain*, which is accesible from the *JCL-Net* Module:


```
RuntimeConfig runtimeConfig = new RuntimeConfigDefault();
BlockStoreLevelDBConfig dbConfig = BlockChainStoreLevelDBConfig()
                    .config(runtimeConfig)
                    .build()
BlockStore db = BlockStoreLevelDB.builder()
                    .config(dbConfig)
                    .genesisBlock(ProtocolConfigBuilder
                    	.get(new MainNetParams(Net.MAINNET)
                    	.getGenesisBlock()) // from JCL-Net
                    .build()
```

After the instance is created, you can already start using the ``BlockStore``  module:
> * [JCL-Store: Using the BlockChainStore component](../../store/doc/README.md#BlockChainStore-interface)

In the ``BlockChainStore``component, the *Streaming* of the *Fork* and *PRUNE* Events are *ALWAYS* enabled.

### Automatic Fork Prunning:

The ``BlockChainStore`` component can be configured to perform an *Automatic Fork Prunning*. If enabled, a *Chain* will be pruned if the difference in *height* with the longest *Chain* is bigger than a *Threshold* defined during the Set up:

The following configuration enables the *Automatic Prunning* and sets up the Frequency of that Prunning and the difference in *Height* needed for a *Chain* to be pruned.


```
RuntimeConfig runtimeConfig = new RuntimeConfigDefault();
BlockStoreLevelDBConfig dbConfig = BlockChainStoreLevelDBConfig()
                    .config(runtimeConfig)
                    .genesisBlock(ProtocolConfigBuilder
                    	.get(new MainNetParams(Net.MAINNET)
                    	.getGenesisBlock())
                    .forkPrunningHeightDifference(2)
                    .build()
BlockStore db = BlockStoreLevelDB.builder()
                    .config(dbConfig)
                    .enableAutomaticForkPrunning(true)
                    .forkPrunningFrequency(Duration.ofSeconds(60)) 
                    .build()
```

Using the Configuration above, the *BlockChainStore* component will perform the verification every 60 seconds, and only those *Chains* that are 2 *Blocks* *shorter* that the longest *Chain* will be pruned.

> If not specified, tips are pruned when their *height* is 2 Blocks shorter than the longest chain.

> **Performance Tips**
> 
> It's ok to use a short frequency for testing purposes (like in the example above), but in a real scenario a frequency of several hours is more suitable.
> 

### Automatic Orphan Prunning:

The ``BlockChainStore`` component can be configured to perform an *Automatic Orphan Prunning*. If enabled, all the **Orphan** Blocks will be removed if they are also **older** than a specifi threashold that can be set during Configuration. 

The following configuration enables the *Automatic Orphan Prunning* and sets up the Frequency of that Prunning and the minimim *Age* that a Block must have in order to be removed.


```
RuntimeConfig runtimeConfig = new RuntimeConfigDefault();
BlockStoreLevelDBConfig dbConfig = BlockChainStoreLevelDBConfig()
                    .config(runtimeConfig)
                    .genesisBlock(ProtocolConfigBuilder
                    	.get(new MainNetParams(Net.MAINNET)
                    	.getGenesisBlock())
                    .orphanPrunningBlockAge(Duration.ofMinutes(30)
                    .build()
BlockStore db = BlockStoreLevelDB.builder()
                    .config(dbConfig)
                    .enableAutomaticOrphanPrunning(true)
                    .orphanPrunningFrequency(Duration.ofSeconds(40)) 
                    .build()
```

Using the Configuration above, the *BlockChainStore* component will perform the verification every 40 seconds, and only those **Orphan** Blocks **older** than 30 minutes fromt he current time will be removed.

> If not specified, **Orphan** Blocks are removed when they are **older** than 30 minutes ago.


### State Streaming

The ``BlockChainStore``component can stream an additional set of events, on top of the ones that can be already streamed by the ``BlockStore``component. These new Events are ``ChainPruneEvent``, ``ChainForkEvent`` and ``State`` event. The first two are always eabled by default, but the ``State`` Event is triggered on a frequency basis, and that frequency must be specified by the Configuration. In the following snippet, the ``State`` Event is triggered every 5 minutes:

```
RuntimeConfig runtimeConfig = new RuntimeConfigDefault();
BlockStoreLevelDBConfig dbConfig = BlockChainStoreLevelDBConfig()
                    .config(runtimeConfig)
                    .build()
BlockStore db = BlockStoreLevelDB.builder()
                    .config(dbConfig)
                    .genesisBlock(new ProtocolBSVMainConfig().getGenesisBlock())
                    .stateFrequency(Duration.ofMinutes(5))
                    .build()

```