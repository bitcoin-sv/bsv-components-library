package com.nchain.jcl.store.foundationDB.common

import spock.lang.Specification

/**
 * A Testing class for the Serializer of the HashesList Object, using the Bitcoin Codification
 */
class HashesListSerializerSpec extends Specification {
    /**
     * We just test that the object can be serialized and deserialized back properly
     */
    def "testing Serialization/Deserialization"() {
        given:
            String firstWord = "first"      // regular word
            String secondWord = "123_!()"   // funny characters
            String thirdWord = " 090  909 "   // with spaces and blanks
            List<String> words = Arrays.asList(firstWord, secondWord, thirdWord)
            HashesList original = HashesList.builder().hashes(words).build()
            HashesListSerializer serializer = HashesListSerializer.getInstance()
        when:
            byte[] serialized = serializer.serialize(original)
            HashesList deserialized = serializer.deserialize(serialized)
        then:
            original.equals(deserialized)
    }
}
