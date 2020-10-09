# JCL-Script

*JCL-Script* is the JCL Module that provides capabilities to run the *Bitcoin Script*.

**Use *JCL-Script* if you need to:**

 * Create Scripts or save/serialize them.
 * Run Scripts to check if an output in a TX can be spent


> NOTE: Some knowledge abot the *Bitcoin Script* and how it works is required in this chapter. Here are some useful links with further information:
> 
>  * [Script Wiki](https://en.bitcoin.it/wiki/Script)
>  * [Bitcoin script 101](https://bitcoindev.network/bitcoin-script-101/)
>  * [Bitcoin Script Guide](https://blockgeeks.com/guides/best-bitcoin-script-guide/)


## How to use *JCL-Script*

### A Script

A *Script* is a set of *instructions*, which all together make a *program/Script*. The BSV Script is an extension of the *Bitcoin* Script, which is a forth-like program, that is, the instructions are executed in sequence one after another, from left to right.


The *representation* of the Script might vary and take different forms. For example, the following is a Text-format representation using a human-readable format:

```
2 2 ADD 4 EQUALS
```

This script just adds 2 numbers and then expect the result to be 4.
Another (equivalent) representation of the same Script in hexadecimal format is this one:

```
0x5252935487
```

Both previous representations of the Script are equivalent, only the format is different. As it will explained later on, the *SCript* can be *loaded/parsed* in *raw* format (byte array), or "human-readable-text" format.

### Executing a Script

A Script needs to be executed in order to be useful. The execution of a Script will 
execute the Instructions (also called *opCodes*) in the same order as they appear in the script. There are different types of *opCodes* available for use:
 
 * *opCodes* that handle *data*
 * *opCodes* that verify digital signatures
 * *opCodes* that peform *binary operations*
 * etc...
 
 
The execution of the Script makes use of a structure that works like a *Stack*, which works as the "memory" of the Script Engine. The *opCodes* can *push* and *poll* data to/from this Stack.

So the 2 main elements that are needed when running a Script are:

 * The Script itself
 * A Stack (an empty one, or one already filled with data from a previous execution)

Event though you can create a *Script* and use only *data-related opCodes* to handle data and make calculations using the *Stack* (pushing and polling data to/from the Stack), the really interesting capacities of the *Script* come when you use the *opCodes* that perform advance operations like digital signature verification, etc. These other *opCodes* need not only the info contained in the *Stack*, but also other information related to the "Context" of the *Script*, for example the *Transaction* the *Script* belongs to.

So, even thought the *Script* and the *Stack* are the main element, most of the times some additional "external" infomration is also needed: tis information is usually about the "Context" of the Script: The *Transaction* that contains the *Script*, a reference to the *output* that we want to spend, etc. This "external" set of information is called *Script Context* and will be explained in the next chapters.


### Main Components to execute a Script

The *JCL-Script* Module makes use of 4 Basic Concepts:

 * A *Script*
 * A *Script Configuration*
 * A *Script Context*
 * A *Script Interpreter*


**The Script** is the Script itself, as it's been described already. It's just a series of instructions. It can be obtain from different formats, like text format, hexadecimal, or even raw data (byte array).

**The Script Configuration** Is an Object that contains the *Rules* that we want to enforce when we run the Script. Given the same Script, the result might be different depending on some variables, like:

 * The maximum size of the Script allowed
 * The maximum size of *Opcodes* allowed in the Script
 * The list of valid or invalid *OpCodes* that can be used in it
 * etc

So, for the same *Script*, the execution might vary depending on the "Rules" we apply to it. So the *Script Configuration* stores all these *Rules*.

**The Script Context** contains information about the "context" of the Script when its being executed. Some Scripts are very simple and they do not need any external information besides the Script itself, but that's a rare case. In most scenarios, the execution of the Script needs more "external" information, like for example the *Transaction* that contains this Script. That's because some of the *Instructions* that can be used in the *Script* need to access this Tx in order to verify the signature.
So, while the **Script Configuration** is usually the same, the **Script Context** is usually different each time we run the *Script*. This Context object contains not only the *Transaction*, but also other objects or information that can be useful during the execution.

**The Interpreter** is the Component that *executes* The Script. It takes all the information (the *Script*, the *Script Configuration* and the *Script Context*), and runs the Script, accesing the information contained in the *Stack* and in the *Script context* if needed. 

**How to interpret the result of the execution:**

The possible outcomes are:

 * The execution has been interrumpted (exception thrown). This is usually due to an error in some *Verifications* performed during the execution. The result is considered *FALSE/INCORRECT* in this case.
 * The execution has finished. In this case, the result depends on the value stored in the top of the *Stack*.If the value there is ZERO/NULL, then the result is *FALSE*. If the value is anything different, the result is *TRUE*.



### Configuration & Execution process

So the normal process has 3 Steps:

 1. Set up the Script Configuration and the Interpreter
 2. Create/Load the Script
 3. Create the Script Context and Run the Script


#### set up the Script Configuration & Interpreter

*JCL-Script* has alredy provides different configurations *out-of-the-box* that can be used directly:

```
ScriptConfig scriptConfig1 = new ScriptBSVGenesisConfig();
ScriptConfig scriptConfig2 = new ScriptBSVPreGenesisConfig();
ScriptConfig scriptConfig3 = new ScriptBSVPreMagneticConfig();
```
Any of these configurations can be easily changed by using the *builder* that comes with it, like in these example:

```
ScriptConfig scriptConfig1 = new ScriptBSVGenesisConfig().toBuilder()
                    .exceptionThrownOnOpReturn(true)
                    .maxStackNumElements(1_000)
                    .build();

```

You can also create the Script Configuration from scratch, using the *builder*:

```
ScriptConfig scriptConfig = ScriptConfig.builder()
                    .maxDataSizeInBytes(100_000)
                    .maxNumberSizeInBytes(32_000)
                    ...// more properties...
                    .maxOpCount(100)
                    .build();

```

The *Verification Flags* can also be used to create a Script Configuration. Each Script Configuration, like the ones already provided by *JCL-Script* (*ScriptBSVGenesisConfig*, *ScriptBSVPreGenesisConfig*, *ScriptBSVPreMagneticConfig*), already contains each a *Set* of *Verification Flags* that affect the output of the *Script*. But the other way around is also possible: Just by providing a list of *Verification Flags*, *JCL-Script* is capable of "building" an instance of *Script Configuration*:

```
Set<ScriptVerifyFlag> flags = ...
ScriptConfig scriptConfig = ScriptConfig.builder(flags).build();

```

Now that the *Script Configuration* is ready, we only need to create an instance of a *Script Interpreter* to run the Script. In most system there is only **One** Interpreter. And it's also very common that the same *Script Configuration* will be applied to all the *Scripts* we run. So we can save time by assigning the *Script Cofniguration* to the *Interpreter* at the moment of its creation, like in this example:

```
ScriptConfigurtion scriptConfig = new ScriptBSVGenesisConfig();
ScriptInterpreter scriptInterpreter = ScriptInterpreter.builder("demo")
					.config(scriptConfig)
					.build();
```

The "*demo*" string provided is just for logging purposes.

 
#### create a Script 
 
There are different ways to create a Script:

* We can create a script from scratch, and adding Instructions to it
* We can load an script from a File or other source


##### Creating a Scrit from scratch

In this example, we create from scratch the same *Script* that was used in the examples at the beginning of this tutorial (*2 2 ADD 4 EQUALS*):

```
Script script = Script.builder()
                      .smallNum(2)
                      .smallNum(2)
                      .op(ScriptOpCodes.OP_ADD)
                      .smallNum(4)
                      .op(ScriptOpCodes.OP_EQUAL)
                      .build();
```

##### Loading (parsing) a Script.

```
String scriptText = "2 2 ADD 4 EQUALS";
Scrit script = Script.builder(scriptText).build();

```


### Create the Script Context and run the Script

Now that we have the *Script Configuration*, the *Interpreter* and the *Script*, we only need to create the *Script Context* and launch the execution. If the *Script* doesn't need any "external" information, this is pretty straightforward:

```
ScriptContext scriptContext = ScriptContext.builder().build();
```

To run the Script, you only need to get an instance of a *Script Interpreter* and call the "execute" method. In the example below we are using the same interpreter instance we created a few examples ago:

```
ScriptResult result = scriptInterpreter.execute(scriptContext, script);
System.out.println("Is the result OK?" + result.isOK());
```

The *scriptInterpreter* instance has an internal reference to the *Script Configuration* that was injected upon cration, but we can also call the *execute* using another *Script Configuration*:

```
ScriptConfif otherConfig = ...
ScriptResult result = scriptInterpreter.execute(otherConfig, scriptContext, script);
System.out.println("Is the result OK?" + result.isOK());
```


### Examples:

#### Simple Example (no configuration or Context):


In this example, we load the Script from a source (a String, in this case), and:

 * *no Configuration* is provide, so the default (*Genesis Configuration*) is used
 * The *Script* does not use any external information but the data stored in the *Stack*, so no "external" information is needed, so the *ScriptContext* is no needed here.

```
	ScriptInterpreter scriptInterpreter = ScriptInterpreter.builder("demo").build();
	
	// Set up Script and Context (once per Script we need to run):
	String scriptContent = "2 2 ADD 4 EQUALS"
	Script script = Script.builder(scriptContent).build();
	ScriptContext scriptContext = ScriptContext.builder().build()
		
	// Run it and get the Result:
	ScriptResult scriptResult = Interpreter.execute(scriptContext, script);
	
```


## Checking *locking* & *Unlocking Scripts*

The "execute" method of the *Interpreter*, as it's been described, can be used to execute a single *Script*. that is useful, but in a real scenario you will most probably want to verify if an *Input* can be *spent* in a *Tx*.

For that, you need the following information:

 * The *index* of the Input within the *Tx* that you want to Spend
 * The *Locking Script* from the *Input*.
 * The *Unlocking Script* that you want to apply together with the *Locking SCript*, in order to Spend the input.
 * Info about the *Transaction* where the *Script* is in.



For this operationb, the *Interpreter* provides an alternative version of the *Execute* method, which takes all the info above and cheks that the input can actually be spent. The result will come in the form of a *ScriptResult*, same as in a single *Script* execution, or an exception might be triggered due to a Verification failiure.

Example:

```
	// Set up Config and Interpreter:
	ScriptConfig scriptConfig = new ScriptBSVGenesisiConfig();
	ScriptInterpreter scriptInterpreter = ScriptInterpreter.builder("demo")
			.config(scriptConfig)
			.build();
	
	// Set up Script and Context (once per Script we need to run):
	// Locking Script:<sig><pubKey>
	Script scriptSig = Script.builder("...").build();
	
	// Unlocking Script: <dup><hash160><pubKey><equalverify><checksig>
	Script scriptPubKey = Script.builder("...").build();
		
	// The Context contains information needed by the Scrit in order to run...
	ScriptExecContext scriptContext = ScriptExecContext.builder()
		.txContainingScript(...)
		.txInputIndex(...)
		.value(...)
		.build()
		
	// Run it and get the Result:
	ScriptResult scriptResult = Interpreter.execute(scriptConfig, scriptContext, scriptSig, scriptPubKey);
	
```



 
