package com.nchain.jcl.base.serialization;

import com.nchain.jcl.base.domain.api.BitcoinObject;
import com.nchain.jcl.base.domain.api.base.*;
import com.nchain.jcl.base.domain.bean.base.*;
import com.nchain.jcl.base.tools.bytes.HEX;

import java.util.HashMap;
import java.util.Map;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This class stores references to al the Serialziers implemented for all the Bitcoin Objects. Instead of locating and
 * using an specific serilziaer, this class can be used to serilize any Object.
 */
public class BitcoinSerializerFactory {
    // References to all Serializers. Each Serialier is referenes by the class of the Object it serializes, so
    // we stores them in a Map. For each one we store 2 keys: the Interface class and the implementation class

    private static Map<Class, BitcoinSerializer> serializers = new HashMap<>();
    static {
        serializers.put(TxInput.class, TxInputSerializer.getInstance());
        serializers.put(TxInputBean.class, TxInputSerializer.getInstance());

        serializers.put(TxOutPoint.class, TxOutPointSerializer.getInstance());
        serializers.put(TxOutPointBean.class, TxOutPointSerializer.getInstance());

        serializers.put(TxOutput.class, TxOutputSerializer.getInstance());
        serializers.put(TxOutputBean.class, TxOutputSerializer.getInstance());

        serializers.put(BlockHeader.class, BlockHeaderSerializer.getInstance());
        serializers.put(BlockHeaderBean.class, BlockHeaderSerializer.getInstance());

        serializers.put(Tx.class, TxSerializer.getInstance());
        serializers.put(TxBean.class, TxSerializer.getInstance());
    }

    /** Locate the Serializer for this Class */
    public static BitcoinSerializer getSerializer(Class objectClass) {
        return serializers.get(objectClass);
    }
    /** Inidcates if there is a Serialzer registered fr this Class */
    public static boolean hasFor(Class objectClass) {
        return serializers.containsKey(objectClass);
    }

    /** Serialzes the object provided. */
    public static byte[] serialize(BitcoinObject object) {
        BitcoinSerializer serialier = getSerializer(object.getClass());
        if (serialier == null) throw new RuntimeException("No Serializer for " + object.getClass().getSimpleName());
        return serialier.serialize(object);
    }

    /** Deserializes the ByteArray provided into a Bitcoin Object */
    public static BitcoinObject deserialize(Class objectClass, byte[] bytes) {
        BitcoinSerializer serialier = getSerializer(objectClass);
        if (serialier == null) throw new RuntimeException("No Serializer for " + objectClass.getSimpleName());
        return serialier.deserialize(bytes);
    }
    /** Deserializes the ByteArray (in Hexadecimal format) into a Bitcoin Object */
    public static BitcoinObject deserialize (Class objectClass, String hex) {
        return deserialize(objectClass, HEX.decode(hex));
    }

}
