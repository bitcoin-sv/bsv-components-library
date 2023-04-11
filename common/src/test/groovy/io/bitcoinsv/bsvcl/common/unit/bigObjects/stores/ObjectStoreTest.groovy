package io.bitcoinsv.bsvcl.common.unit.bigObjects.stores


import spock.lang.Ignore
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class ObjectStoreTest extends Specification {

    // MAX NUMBER OF ENTRIES:
    private static final int MAX_NUM_ENTRIES = 100;
    // AVG KEY SIZE IN BYTES:
    private static final int AVG_KEY_SIZE = 1;  // 1 Byte
    // AVG VALUE SIZE IN BYTES
    private static final int AVG_VALUE_SIZE = 200; // 200 bytes

    class TestClass {
        private int id;
        private byte[] content;

        TestClass(int id, byte[] content) {
            this.id = id;
            this.content = content;
        }

        int getId() { return this.id;}
        byte[] getContent() { return this.content;}

        @Override
        boolean equals(Object obj) {
            if (!obj instanceof TestClass) {return false;}
            TestClass other = (TestClass) obj;
            return (this.id == other.id) && (this.content.length == other.content.length);
        }
    }

    class TestSerializer implements io.bitcoinsv.bsvcl.common.bigObjects.stores.ObjectSerializer<TestClass> {
        @Override
        void serialize(TestClass object, io.bitcoinsv.bsvcl.common.bytes.ByteArrayWriter writer) {
            writer.writeUint16LE(object.id)
            writer.writeUint32LE(object.content.length)
            writer.write(object.content);
        }
        @Override
        TestClass deserialize(io.bitcoinsv.bsvcl.common.bytes.ByteArrayReader reader) {
            int id = reader.readUint16();
            int contentLength = reader.readUint32();
            byte[] content = reader.read(contentLength);
            return new TestClass(id, content);
        }
    }

    private io.bitcoinsv.bsvcl.common.bigObjects.stores.ObjectStore<TestClass> getStore(String storeId, io.bitcoinsv.bsvcl.common.config.RuntimeConfig runtimeConfig) {
        return new io.bitcoinsv.bsvcl.common.bigObjects.stores.ObjectStoreCMap<TestClass>(
                runtimeConfig,
                storeId,
                new TestSerializer(),
                AVG_KEY_SIZE,
                AVG_VALUE_SIZE,
                MAX_NUM_ENTRIES)
    }

    /**
     * We test that when we use the Store, the proper folder and files are created, and then removed when the store
     * is empty
     */
    @Ignore
    def "testing Folder %& files"() {
        given:
        String storeId = "testingObjFolder"
        io.bitcoinsv.bsvcl.common.config.RuntimeConfig runtimeConfig = new io.bitcoinsv.bsvcl.common.config.provided.RuntimeConfigDefault()
        when:
        // We instantiate the Store and check the folder and ref File are created...
        io.bitcoinsv.bsvcl.common.bigObjects.stores.ObjectStore<TestClass> store = getStore(storeId, runtimeConfig);
        store.start()
        Path storeFolder = Paths.get(runtimeConfig.getFileUtils().getRootPath().toString(), "store", storeId);
        boolean storeFolderCreated = Files.exists(storeFolder);
        boolean refCmapFileCreated = Files.exists(Paths.get(storeFolder.toString(), TestSerializer.class.getSimpleName() + "_objs.dat"));

        // We Clear the Store and check the folder is removed:
        store.stop()
        store.destroy()
        boolean storeFolderRemoved = !Files.exists(storeFolder);

        then:
        storeFolderCreated
        refCmapFileCreated
        storeFolderRemoved
    }

    /**
     * We test that we can save different Objects, read and removed them properly.
     */
    @Ignore
    def "testing Saving, Getting and Removing"() {
        given:
        String storeId = "testingObjOperations"
        io.bitcoinsv.bsvcl.common.config.RuntimeConfig runtimeConfig = new io.bitcoinsv.bsvcl.common.config.provided.RuntimeConfigDefault()
        when:
        io.bitcoinsv.bsvcl.common.bigObjects.stores.ObjectStore<TestClass> store = getStore(storeId, runtimeConfig);
        store.start()
        // We store 2 objects:
        TestClass obj1 = new TestClass(1, new byte[200])
        TestClass obj2 = new TestClass(2, new byte[210])

        store.save("object 1", obj1)
        store.save("object 2", obj2)

        // We check that we read the contents right:
        TestClass obj1Read = store.get("object 1")
        TestClass obj2Read = store.get("object 2")
        TestClass objUnknown = store.get("object made up") // should return null

        // Now we remove them oen try to read t again:
        store.remove("object 1")
        TestClass objRemovedAndRead = store.get("object 1")

        // We clean everything.
        store.stop()
        store.destroy()
        then:
        obj1.equals(obj1Read)
        obj2.equals(obj2Read)
        objUnknown == null
        objRemovedAndRead == null
    }
}