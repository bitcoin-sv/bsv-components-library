package com.nchain.jcl.base.serialization;

import com.nchain.jcl.base.core.Address;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A Serializer implementation for an *Address*. Same as other Serializers in JCL, it's a state-less Singleton
 * instance.
 */
public class AddressSerializer {

    private static AddressSerializer instance;

    private AddressSerializer() {}

    public AddressSerializer getInstance() {
        if (instance == null) {
            synchronized (instance) {
                instance = new AddressSerializer();
            }
        }
        return instance;
    }

    public void serialize(Address obj, ObjectOutputStream out) {
        try { out.defaultWriteObject(); } catch (IOException ioe) {throw new RuntimeException(ioe);}
    }

    public Address deserialize(ObjectInputStream ois) {
        throw new RuntimeException("Not Implemented!");
    }
}
