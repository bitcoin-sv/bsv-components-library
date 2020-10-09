# JCL-Base

*JCL-Base* is the basic Module in *JCL*. It provides very basic functionality, and it's meant to be a *base* module that can be further extended by importing another modules, like *JCL-Net* or *JCL-Script*.

**Use *JCL-Base* if you need to:**

 * Use/manipulate information about *Transactions*, *Blocks* etc.
 * Serialize information about Transactions, Inputs/Outputs or Blocks, so it can be further stored.

 
## How to use *JCL-Base*

### *Bitcoin Objects*

The main purpose of *JCL-Base* is to define and provide operations over the so-called *bitcoin domain classes*, which is a name that groups a set of Classes defined in *JCL-Base* and used extensible accross other JCL* modules. 

The basic "domain" class is a **Bitcoin Object**. A *Bitcoin Object* is an object that:

* Stores relevant information. 
* It does NOT contain business logic: It's just a Placeholder for information.
* It's content cannot be changed (*Immutable*).
* Its Thread-Safe
* It's created by using a *helper* class called *builder* (*Builder* Design Pattern)

Any *Bitcoin Object* will share the properties above.

The following example shows how to to build a *Transaction Output* from scratch, containing *10 Satoshis* and a dummy *unlocking script (an empty byte array of length 10)*

```
TxOutput txOutput = TxOutput.builder()
                       .value(Coin.valueOf(10))   // 10 Satoshis...
                       .scriptBytes(new byte[10]) // dummy script
                       .build();

```

Most of the time, a *Bitoin Object* is composed of other *inner* *Bitcoin Objects*, in that case
the process of creation is the same. In this example, we build a *Transaction Input* from scratch, which contains an inner structure called *OutputPoint* that is created by using its respective *Builder*:

```
TxInput txInput = TxInput.builder()
                            .scriptBytes(new byte[10])
                            .sequenceNumber(1)      
                            .value(Coin.valueOf(5)). // 5 Satoshis...
                            .outpoint(TxOutPoint.builder()
                                    .hash(Sha256Wrapper.wrap(new byte[32])). // dummy Hash...
                                    .index(1)
                                    .build())
                            .build();
```

### Bitcoin Serializable Objects

Apart from all the above, a *Bitcoin Object* class might also be *Serializable*, that is, it might be possible to Serialize or Deserialize to/from a raw format (byte array). In this case, it's called a **Bitcoin Serializable Object**.


A *Bitcoin Serializable Object* is like a regular *Bitcoin Object*, but it ALSO provides a *sizeInBytes* field, which stores the size of this object once serialized. 

> NOTE: Having a field like "sizeInBytes" inside a Domain object might look strange, after all the size in Bytes is something very close to the *hardware* or *phisycal* layer of the application. But in Blockchain applications, this *Size* plays an important rol in *business decisions*, like for erxample the *fee* to apply, that depends on the Size of a Transaction.

In all the *Bitcoin Serializable Objects*, the *sizeInBytes* field is populated when the object is *deserialized*.

This is an example of how to Deserialize a *Bitcoin Serializable* objet, in this case a *Tx*, which represents a *Transaction*. The same way as when creating an oject from scratch, here we also use a *builder* to "load" and "deseriale" an object this time. The *builder()* method accepts a parameter wich is a String containing the serialized object in *hexadecimal* format:


```
String TX_HEX = "10000000193e3073ecc1d27f17e3d287cce..." // TX in HEX format
Tx tx = Tx.builder(TX_HEX).build();

System.out.println(tx.getSizeInBytes()); // We print the Size...

```

The same example, using a raw byte array:

```
byte[] TX_BYTES = ...
Tx tx = Tx.builder(TX_BYTES).build();
System.out.println(tx.getSizeInBytes()); // We print the Size...
```

The examples above use a *builder* to *Deserialize* them, which is a convenient shortcut. This *Builder* internally makes use of the *Bitcoin Serializers*, which can also be used directly.

For each *Bitcoin Serializable Object*, there is a *Bitcoin Serializer*. So for example, in the case of the *Tx* object, we also have a *TxSerializer*, whcih we can use to deserialize and serialize it:


```
// Deserializing...
String txHEX = ''; // Tx in HEX format...
Tx tx = TxSerializer.getInstance().deserialize(txHEX);

// Serializing...
ByteArrayWriter bw = new ByteArrayWriter();
txSerializer.getInstance().serialize(tx, bw)
byte[] content = bw.reader.getFullContent(); // Serialized content
```

In the previous example, we are using a *ByteArrayWriter*, which you can think of as something similar to a "OutputStream". If you want to serialize several objects together, you can define a single *ByteArrayWriter* and use it in all the Serializers.


You can use the *Bitcoin Serializers*, which requires from you to use one specific serializer depending on the object you want to serialzie, or you can use the *BitcoinSerializerFactory*. This factory is just a *wrapper* that allows you to Serialize/Deserialize any Object without having to 
instantiate the Serialzers directly. The same example from above could have been written using the *BitcoinSerializerFactory* this way:

```
// Deserializing...
String txHEX = ''; // Tx in HEX format...
Tx tx = (Tx) BitcoinSerializerFactory.deserialize(tx.class, txHEX);
// Serializing...
byte[] content = BitcoinSerializerFactory.serialize(tx);
```

*(Some times using the BitcoinSerializerFactory is more convenient)*


### Bitcoin *Hashhable* Objects

A *Bitcoin Serializable Object* might also be a **Bitcoin Hashable Object**: 

A *BitcoinHashable Object* is like any other *Bitcoin Serializable Object*, that is, it can be Serialized, **and** it also contains a special field called **hash**. This field contains a value, which is the calculated *hash* (Sha-256) of this object's content, once Serialized. All the *Bitcoin Hashable Objects* include this fields which is automatically calculated.

**Most *Domain Classes* in *JCL* are *Bitcoin Serializable Objects*, and some of them are also *Bitcoin Hashable Objects*.**

> NOTE: the *hash* field is automatically calculated by the library, so the content of this field is 
not for the user to manipulate. There is also a cache-mechanism in place, so if the "*getHash()*" is called but the *Hash* field has not value, then its value is calculated at that moment, and not changed ever after that, so it remains available for further calls. This is due to the fact that calculating the *Hash* might be a time-consuming operation when done a high number of times, so we make sure that it's only calculaled when needed. This decision also means that the *Bitcoin Hashable* Objects are not *true* immutable classes, but they are *Thread-safe*.


In this example, we are "loading" a *Transaction*(Tx), which is a *Bitcoin Hashable Object*, and we print the value of its *hash*:

```
byte[] TX_BYTES = ...
Tx tx = Tx.builder(TX_BYTES).build();
// The 'hash' field is NOT populated at this moment, you can check it by using the Debugger...
tx.getHash(); // we force the 'hash' calculation...
// The 'hash' field IS now populated
System.out.println(tx.getHash());
```
