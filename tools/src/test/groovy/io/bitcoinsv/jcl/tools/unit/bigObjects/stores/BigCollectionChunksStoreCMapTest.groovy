package io.bitcoinsv.jcl.tools.unit.bigObjects.stores

import io.bitcoinsv.jcl.tools.bigObjects.BigCollectionChunk
import io.bitcoinsv.jcl.tools.bigObjects.BigCollectionChunkImpl
import io.bitcoinsv.jcl.tools.bigObjects.stores.BigCollectionChunksStore

import io.bitcoinsv.jcl.tools.bigObjects.stores.BigCollectionChunksStoreCMap
import io.bitcoinsv.jcl.tools.bigObjects.stores.ObjectSerializer
import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader
import io.bitcoinsv.jcl.tools.bytes.ByteArrayWriter
import io.bitcoinsv.jcl.tools.config.RuntimeConfig
import io.bitcoinsv.jcl.tools.config.provided.RuntimeConfigDefault
import spock.lang.Ignore
import spock.lang.Specification

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Collectors
import java.util.stream.IntStream

@Ignore("This test is not working yet. It needs to be fixed.")  // todo: fix this test
class BigCollectionChunksStoreCMapTest extends Specification {

    // Size of Each Test Item:
    private static final int ITEM_BYTES_SIZE  = 100_000;       // 100 KB
    // Aprox size of the Keys. We are simulating collections related to blocks, so we use a block Hash as an example
    private static final int KEY_BYTES_SIZE   = "00000000000000000c18143a90c306161909362a00f7cdd441c5630aa0498d34".getBytes(StandardCharsets.UTF_8).length
    // MAx size of the whole Storage
    private static final int STORE_MAX_ENTRIES = 500;
    // Max Size of each ChronicleMap. Once one is full, a new one is created.
    private static final int CMAP_MAX_ENTRIES  = 20;

    /**
     * Definition of the Items we'll use of this test.
     */
    class ItemTest {
        private byte[] content;
        ItemTest(byte[] content) {
            this.content = content;
        }
    }

    /**
     * Item Serializer
     */
    class ItemTestSerializer implements ObjectSerializer<ItemTest> {
        @Override
        void serialize(ItemTest item, ByteArrayWriter writer) {
            writer.write(item.content)
        }
        @Override
        ItemTest deserialize(ByteArrayReader reader) {
            return new ItemTest(reader.read(ITEM_BYTES_SIZE))
        }
    }

    private BigCollectionChunksStore<ItemTest> getStore(String storeId, RuntimeConfig runtimeConfig) {
        return new BigCollectionChunksStoreCMap<>(
                runtimeConfig,
                storeId,
                new ItemTestSerializer(),
                KEY_BYTES_SIZE,
                STORE_MAX_ENTRIES,
                ITEM_BYTES_SIZE,
                CMAP_MAX_ENTRIES);
    }
    /**
     * We test that when we use the Store, the proper folder and files are created, and then removed when the store
     * is empty
     */
    def "testing Folder %& files"() {
        given:
            String storeId = "testingFolder1"
            RuntimeConfig runtimeConfig = new RuntimeConfigDefault()
        when:
            // We instantiate the Store and check the folder and ref File are created...
            BigCollectionChunksStoreCMap<ItemTest> store = getStore(storeId, runtimeConfig);
            store.start()
            Path storeFolder = Paths.get(runtimeConfig.getFileUtils().getRootPath().toString(), "store", storeId);
            boolean storeFolderCreated = Files.exists(storeFolder);
            boolean refCmapFileCreated = Files.exists(Paths.get(storeFolder.toString(), "ref.dat"));


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
     * We check that the total size of the store is properly calculated after adding and removing collections from it.
     * We also check that the files are properly removed when its possible to save some disk space
     *
     * In this tests, the "ReusingCMaps" option is disabled (false), so CMaps are removed from memory and Disk when the
     * map is empty, and new Cmaps are crated fresh when we need more storage.
     */
    def "testing adding/Removing collections and check size & Files - ReusingMaps Disabled"() {
        given:
            String storeId = "testingCollectionsAndSizes_ReusingDisabled"
            RuntimeConfig runtimeConfig = new RuntimeConfigDefault()
        when:
            // We instantiate the Store....
            BigCollectionChunksStoreCMap<ItemTest> store = getStore(storeId, runtimeConfig);
            store.start()

            // We create and save some collections:
            int NUM_COLLECTIONS = 3 // Big enough so several "collection_data" files are generated:
            int NUM_CHUNKS_IN_EACH_COLLECTION = 2;
            int NUM_ITEMS_IN_EACH_CHUNK = 10

            // WE SAVE THE COLLECTIONS:
            // ----------------------------------------------------------------------------------------------

            for (int i = 0; i < NUM_COLLECTIONS; i++) {
                String collectionId = "collection-" + i;
                for (int b = 0; b < NUM_CHUNKS_IN_EACH_COLLECTION; b++) {
                    println("Inserting Collection " + i + ", Chunk " + b + " ...")
                    List<ItemTest> items = IntStream
                            .range(0, NUM_ITEMS_IN_EACH_CHUNK)
                            .mapToObj({v -> new ItemTest(new byte[ITEM_BYTES_SIZE])})
                            .collect(Collectors.toList())
                    BigCollectionChunk<ItemTest> chunk = new BigCollectionChunkImpl<>(items, b)
                    store.save(collectionId, chunk)
                }
            }

            // We print the content, just to check:
            println("CONTENT AFTER INSERTING " + NUM_COLLECTIONS
                    + " COLLECTIONS WITH A TOTAL OF "
                    + (NUM_COLLECTIONS * NUM_CHUNKS_IN_EACH_COLLECTION * NUM_ITEMS_IN_EACH_CHUNK)
                    + " ITEMS.")
            ((BigCollectionChunksStoreCMap<ItemTest>) store).printContent()

            // We get the totalSize of the store:
            long totalSizeAfterSaving = store.sizeInBytes()

            // We remove the last collection and get sizes again:
            String collectionToRemoveId = "collection-" + (NUM_COLLECTIONS - 1)
            long sizeCollectionToRemove = store.sizeInBytes(collectionToRemoveId)
            store.remove(collectionToRemoveId)
            long totalSizeAfterRemoving = store.sizeInBytes()

            // We get the number of "dat" files within the Folder:
            int numContentFilesBeforeCompleteRemoving = Files.list(Paths.get(runtimeConfig.getFileUtils().rootPath.toString(), "store", storeId))
                    .filter( {p -> p.getFileName().toString().startsWith("content_")})
                    .peek({p -> println("File: " + p.toString())})
                    .count()

            // WE REMOVE ALL THE COLLECTIONS:
            // ----------------------------------------------------------------------------------------------

            // now we remove all the Collections:
            IntStream.range(0, NUM_COLLECTIONS).forEach( {i -> store.remove("collection-" + i)})
            long totalSizeAfterCompleteRemoval = store.sizeInBytes();

            // We get again the number of "dat" files within the Folder:
            int numContentFilesAfterCompleteRemoving = Files.list(Paths.get(runtimeConfig.getFileUtils().rootPath.toString(), "store", storeId))
                    .filter( {p -> p.getFileName().toString().startsWith("content_")})
                    .peek({p -> println("File: " + p.toString())})
                    .count()

            // We print the content, just to check:
            println("CONTENT AFTER REMOVING ALL THE COLLECTIONS:")
            ((BigCollectionChunksStoreCMap<ItemTest>) store).printContent()

            // We Clear the Store...
            store.stop()
            store.destroy()
        then:
            totalSizeAfterSaving == (totalSizeAfterRemoving + sizeCollectionToRemove)
            totalSizeAfterCompleteRemoval == 0
            numContentFilesBeforeCompleteRemoving > numContentFilesAfterCompleteRemoving
            numContentFilesAfterCompleteRemoving == 1
    }


    /**
     * We check that the total size of the store is properly calculated after adding and removing collections from it.
     * We also check that the files remain in the fileSystem even if CMAps are removed, but they are eventually
     * removed afterwards when we call the "clean()" method.
     *
     * In this tests, the "ReusingCMaps" option is enabled (false9, so CMaps are NOT removed from memory and Disk when
     * they are empty, instead they remain for future use. The files are only removed when you call "compact()"
     */
    def "testing adding/Removing collections and check size & Files - ReusingMaps Enabled"() {
        given:
            String storeId = "testingCollectionsAndSizes_ReusingEnabled"
            RuntimeConfig runtimeConfig = new RuntimeConfigDefault()
            when:
            // We instantiate the Store....
            BigCollectionChunksStoreCMap<ItemTest> store = getStore(storeId, runtimeConfig);
            store.setToReuseCMaps();
            store.start()

            // We create and save some collections:
            int NUM_COLLECTIONS = 3 // Big enough so several "collection_data" files are generated:
            int NUM_CHUNKS_IN_EACH_COLLECTION = 2;
            int NUM_ITEMS_IN_EACH_CHUNK = 10

            // WE SAVE THE COLLECTIONS:
            // ----------------------------------------------------------------------------------------------

            for (int i = 0; i < NUM_COLLECTIONS; i++) {
                String collectionId = "collection-" + i;
                for (int b = 0; b < NUM_CHUNKS_IN_EACH_COLLECTION; b++) {
                    println("Inserting Collection " + i + ", Chunk " + b + " ...")
                    List<ItemTest> items = IntStream
                            .range(0, NUM_ITEMS_IN_EACH_CHUNK)
                            .mapToObj({v -> new ItemTest(new byte[ITEM_BYTES_SIZE])})
                            .collect(Collectors.toList())
                    BigCollectionChunk<ItemTest> chunk = new BigCollectionChunkImpl<>(items, b)
                    store.save(collectionId, chunk)
                }
            }

            // We print the content, just to check:
            println("CONTENT AFTER INSERTING " + NUM_COLLECTIONS
                    + " COLLECTIONS WITH A TOTAL OF "
                    + (NUM_COLLECTIONS * NUM_CHUNKS_IN_EACH_COLLECTION * NUM_ITEMS_IN_EACH_CHUNK)
                    + " ITEMS.")
            ((BigCollectionChunksStoreCMap<ItemTest>) store).printContent()

            // We get the totalSize of the store:
            long totalSizeAfterSaving = store.sizeInBytes()

            // We remove the last collection and get sizes again:
            String collectionToRemoveId = "collection-" + (NUM_COLLECTIONS - 1)
            long sizeCollectionToRemove = store.sizeInBytes(collectionToRemoveId)
            store.remove(collectionToRemoveId)
            long totalSizeAfterRemoving = store.sizeInBytes()

            // We get the number of "dat" files within the Folder:
            int numContentFilesBeforeCompleteRemoving = Files.list(Paths.get(runtimeConfig.getFileUtils().rootPath.toString(), "store", storeId))
                    .filter( {p -> p.getFileName().toString().startsWith("content_")})
                    .peek({p -> println("File: " + p.toString())})
                    .count()

            // WE REMOVE ALL THE COLLECTIONS:
            // ----------------------------------------------------------------------------------------------

            IntStream.range(0, NUM_COLLECTIONS).forEach( {i -> store.remove("collection-" + i)})
            long totalSizeAfterCompleteRemoval = store.sizeInBytes();

            // We print the content, just to check:
            println("CONTENT AFTER REMOVING ALL THE COLLECTIONS:")
            ((BigCollectionChunksStoreCMap<ItemTest>) store).printContent()

            // We get again the number of "dat" files within the Folder:
            int numContentFilesAfterCompleteRemoving = Files.list(Paths.get(runtimeConfig.getFileUtils().rootPath.toString(), "store", storeId))
                    .filter( {p -> p.getFileName().toString().startsWith("content_")})
                    .peek({p -> println("File: " + p.toString())})
                    .count()

            // WE COMPACT THE COLLECTIONS:
            // ----------------------------------------------------------------------------------------------

            // We print the content, just to check:
            println("CONTENT AFTER COMPACTING:")
            // Now we clear the whole store, this will remove the backing files too:
            store.compact()

            ((BigCollectionChunksStoreCMap<ItemTest>) store).printContent()

            // We get again the number of "dat" files within the Folder:
            int numContentFilesAfterCompacting = Files.list(Paths.get(runtimeConfig.getFileUtils().rootPath.toString(), "store", storeId))
                    .filter( {p -> p.getFileName().toString().startsWith("content_")})
                    .count()

            // We Clear the Store...
            store.stop()
            store.destroy()
        then:
            totalSizeAfterSaving == (totalSizeAfterRemoving + sizeCollectionToRemove)
            totalSizeAfterCompleteRemoval == 0
            numContentFilesBeforeCompleteRemoving == numContentFilesAfterCompleteRemoving
            numContentFilesAfterCompacting == 0
    }

    /**
     * We test that we save multiple collections and if we retrieve the iterators, it returns the
     * right Items from each collection
     */
    def "testing Iterator"() {
        given:
            String storeId = "testingIterator"
            RuntimeConfig runtimeConfig = new RuntimeConfigDefault()
        when:
            // We instantiate the Store...
            BigCollectionChunksStoreCMap<ItemTest> store = getStore(storeId, runtimeConfig);
            store.start()

            // We create 2 small collections:
            store.save("collection1", new BigCollectionChunkImpl<ItemTest>(Arrays.asList(
                    new ItemTest(new byte[ITEM_BYTES_SIZE]),
                    new ItemTest(new byte[ITEM_BYTES_SIZE])),
                    0))

            store.save("collection1", new BigCollectionChunkImpl<ItemTest>(Arrays.asList(
                    new ItemTest(new byte[ITEM_BYTES_SIZE])),
                    1))

            store.save("collection2", new BigCollectionChunkImpl<ItemTest>(Arrays.asList(
                    new ItemTest(new byte[ITEM_BYTES_SIZE]),
                    new ItemTest(new byte[ITEM_BYTES_SIZE]),
                    new ItemTest(new byte[ITEM_BYTES_SIZE])),
                    0))

            // Now we get iterator from each collections and we check each one contains the right data.
            Iterator<BigCollectionChunk<ItemTest>> itemsCol1 = store.getChunks("collection1")
            Iterator<BigCollectionChunk<ItemTest>> itemsCol2 = store.getChunks("collection2")

            BigCollectionChunk<ItemTest> col1Chunk1 = itemsCol1.next()
            BigCollectionChunk<ItemTest> col1Chunk2 = itemsCol1.next()
            BigCollectionChunk<ItemTest> col2Chunk1 = itemsCol2.next()

            boolean col1Chunk1OK = col1Chunk1.chunkOrdinal == 0 && col1Chunk1.items.size() == 2
            boolean col1Chunk2OK = col1Chunk2.chunkOrdinal == 1 && col1Chunk2.items.size() == 1
            boolean col2Chunk1OK = col2Chunk1.chunkOrdinal == 0 && col2Chunk1.items.size() == 3

            boolean col1ItEmptyOK = !itemsCol1.hasNext()
            boolean col2ItEmptyOK = !itemsCol2.hasNext()

            // We print the content, just to check:
            ((BigCollectionChunksStoreCMap<ItemTest>) store).printContent()

            // We Clear the Store...
            store.stop()
            store.destroy()
        then:
            col1Chunk1OK
            col1Chunk2OK
            col2Chunk1OK
            col1ItEmptyOK
            col2ItEmptyOK
    }


    /**
     * We test that, if we initialize the BigCollectionStore in a Folder where there are already some files from a
     * previous execution, those files are picked up and the whole store is populated with them properly
     */
    def "testing loading pre-existing Files in Folder"() {
        given:
            String storeId = "testingPreexistingFiles"
            RuntimeConfig runtimeConfig = new RuntimeConfigDefault()
        when:
            // We initialize a Store and save a collection. Then we initialize a second store using the same ID, so
            // it will be created in the same folder. This second store should have the same state as the first one
            // , containing the collection saved by the first one.

            // We instantiate the First Store and save something...
            BigCollectionChunksStoreCMap<ItemTest> store = getStore(storeId, runtimeConfig);
            store.start()

            store.save("collection-1", new BigCollectionChunkImpl<ItemTest>(Arrays.asList(new ItemTest(new byte[ITEM_BYTES_SIZE])), 0))
            long storeSize = store.sizeInBytes()
            store.stop()

            // Now we init the second one with the same id:
            BigCollectionChunksStoreCMap<ItemTest> store2 = getStore(storeId, runtimeConfig)
            store2.start()
            long storeSize2 = store2.sizeInBytes()
            Iterator<BigCollectionChunk<ItemTest>> it = store2.getChunks("collection-1")
            boolean collectionRecovered = it.hasNext();
            BigCollectionChunk<ItemTest> chunk = it.next()
            boolean collectionDataRecovered = (chunk.items.size() == 1) &&
                    (chunk.chunkOrdinal == 0) &&
                    (chunk.getItems().get(0).content.length == ITEM_BYTES_SIZE)


            // We clear everything:

            store.destroy()

        then:
            storeSize == storeSize2
            collectionRecovered
            collectionDataRecovered
    }
}