package io.bitcoinsv.bsvcl.common.bigObjects.stores;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Longs;

import io.bitcoinsv.bitcoinjsv.core.Utils;
import io.bitcoinsv.bsvcl.common.bigObjects.BigCollectionChunk;
import io.bitcoinsv.bsvcl.common.bigObjects.BigCollectionChunkImpl;
import io.bitcoinsv.bsvcl.common.bytes.ByteArrayReader;
import io.bitcoinsv.bsvcl.common.bytes.ByteArrayWriter;
import io.bitcoinsv.bsvcl.common.config.RuntimeConfig;
import net.openhft.chronicle.map.ChronicleMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shaded.org.apache.commons.lang3.SerializationUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Implementation of BigCollectionStore, using ChronicleMap as the underlying DB.
 * Some characeristics of the ChornicleMap/DB:
 *
 * It uses a Pool of ChronicleMaps, each one saved to a File on disk. When we save a Chunk of a Collection, it
 * looks for a CMap with free storage, and if there is none then a new CMap is created and added to the Pool. If
 * some CMaps become empty after removing some collections, Cmaps are removed and files removed from disk too.
 *
 * @param <I> Type of each Item in the Collection
 */
public class BigCollectionChunksStoreCMap<I> implements BigCollectionChunksStore<I> {

    private static Logger log = LoggerFactory.getLogger(BigCollectionChunksStoreCMap.class);
    /*
        We use 2 Types of Chronicle Maps:

        A Global REFERENCE MAP that contains FOR EACH Collection:
            - Total Size (num items)
            - Total Size (bytes)
            - List of Chunk Ordinals received for that collection

        A POOL of CONTENT ChronicleMaps, each one contains information about CHUNKS. for each Chunk, it contains:
            - Number of Items within the chunk
            - The Items themselves serialized

        Example:
        In a common scenario where we use this class to save Blocks and Txs, a CHUNK contain a LISt of TXs. So if
        we save a Block #000AAAA and 2 CHUNKS with 2 Txs each (250 bytes per TX), this will be the content of the Chronicle Maps:

        Reference Map:
            - #000AAA_chunks        = 0, 1
            - #000AAA_size          = 4
            - #000AAA_sizeInBytes   =  1000

        Content Maps:
        If all the Chunks can fit into a single Map:
            - #000AAA_chunk0_numItems = 2
            - #000AAA_chunk0_item0 = [serialized Tx]
            - #000AAA_chunk0_item1 = [serialized Tx]
            - #000AAA_chunk1_item0 = [serialized Tx]
            - #000AAA_chunk1_item1 = [serialized Tx]
     */

    // File naming:
    private static final String REF_MAP_FILE_NAME = "ref.dat";
    private static final String CONTENT_MAPS_FILE_PREFFIX = "content_";

    // Global Reference Map Configuration:
    private static final long   REFMAP_AVG_VALUE_SIZE   = 1_000; // 1 KB

    /**
     * An iterator that loops over the Chunks of one Collection. It assumes that all the chunks are saved and there are
     * no gaps. If there is a Gap between Chunk0 and Chunk2, then the Iterator will stop after Chunk0.
     */
    class ChunkIterator implements Iterator<BigCollectionChunk<I>> {
        private String collectionId;
        private int currentChunk = 0;

        public ChunkIterator(String collectionId) {
            this.collectionId = collectionId;
        }

        @Override
        public boolean hasNext() {
            return BigCollectionChunksStoreCMap.this.chunkAlreadySaved(collectionId, currentChunk);
        }

        @Override
        public BigCollectionChunk<I> next() {
            List<I> items = BigCollectionChunksStoreCMap.this.getItemsFromChunk(collectionId, currentChunk);
            BigCollectionChunk<I> result = new BigCollectionChunkImpl<>(items, currentChunk);
            currentChunk++;
            return result;
        }
    }


    private final Path rootFolder;
    private final String storeId;
    private final ObjectSerializer<I> itemSerializer;
    private final long avgCollectionIdSize;
    private final long maxCollections;
    private final long avgItemSize;
    private final long maxItemsEachFile;

    // If this property is true, then the CronicleMaps used internally will NOT be removed when they become empty,
    // instead they will remain available so they can be used for future inserts.
    // The default is true since the most efficient way is to remove CMaps (and their backing files) when they are
    // empty and create ones when needed. but if those files are big, removing them from disk might led to errors if
    // it takes the OS too long to flush the to disk.

    private boolean reuseCMaps = false;

    private ChronicleMap<String, byte[]> refMap;
    private List<ChronicleMap<String, byte[]>> contentMaps = new ArrayList<>();

    /**
     * Constructor
     * @param runtimeConfig             JCL Runtime Configuration
     * @param storeId                   Store ID (a folder will be created)
     * @param itemSerializer            Serializer of the Items
     * @param avgCollectionIdSize       Avg Size of the CollectionId
     * @param maxCollections            Maximum Number of Collections to Store
     * @param avgItemSize               Avg size of each Item
     * @param maxItemsEachFile          Maximum Number of Items in each CMAP file.
     */
    public BigCollectionChunksStoreCMap(RuntimeConfig runtimeConfig,
                                        String storeId,
                                        ObjectSerializer<I> itemSerializer,
                                        long avgCollectionIdSize,
                                        long maxCollections,
                                        long avgItemSize,
                                        long maxItemsEachFile) {
        this.rootFolder = Paths.get(runtimeConfig.getFileUtils().getRootPath().toString(), "store", storeId);
        this.storeId = storeId;
        this.itemSerializer = itemSerializer;
        this.avgCollectionIdSize = avgCollectionIdSize;
        this.maxCollections = maxCollections;
        this.avgItemSize = avgItemSize;
        this.maxItemsEachFile = maxItemsEachFile;
    }

    public void setToReuseCMaps() {
        this.reuseCMaps = true;
    }

    private String keyForChunkNumItems(String collectionId, int chunkOrdinal) {
        return collectionId + "_chunk" + chunkOrdinal + "_numItems";
    }

    private String keyForChunkItem(String collectionId, int chunkOrdinal, int itemIndex, int itemPartial) {
        return collectionId + "_chunk" + chunkOrdinal + "_item" + itemIndex + "_" + itemPartial;
    }

    private String keyForCollectionSizeInBytes(String collectionId) {
        return collectionId + "_sizeInBytes";
    }
    
    private String keyForTotalStorageSizeInBytes() {
        return "totalStorageSizeInBytes";
    }
    
    private String keyForCollectionChunks(String collectionId) {
        return collectionId + "_chunks";
    }

    private String keyForCollectionSize(String collectionId) {
        return collectionId + "_size";
    }

    private String keyForCollectionCompleted(String collectionId) {
        return collectionId + "_completed";
    }

    private ChronicleMap<String, byte[]> addNewContentMap(int cmapIndex) {
        try {
            String fileName = CONTENT_MAPS_FILE_PREFFIX + cmapIndex + ".dat";
            ChronicleMap<String, byte[]> refMap = ChronicleMap
                    .of(String.class, byte[].class)
                    .name(CONTENT_MAPS_FILE_PREFFIX + cmapIndex)
                    .averageKeySize(this.avgCollectionIdSize + 10) // 10-byte buffer for suffixes...
                    .averageValueSize(this.avgItemSize)
                    .maxBloatFactor(3.0)
                    .entries(this.maxItemsEachFile)
                    .createPersistedTo(Paths.get(rootFolder.toString(), fileName).toFile());
            this.contentMaps.add(refMap);
            return refMap;
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    private ChronicleMap<String, byte[]> addNewContentMap() {
        return addNewContentMap(getNextMapIndexToUse());
    }

    private int getNextMapIndexToUse() {
        if (this.contentMaps.isEmpty()) return 0;
        int maxIndex = this.contentMaps.stream().map(m -> m.file()).mapToInt(f -> getIndexNumberFromMapFile(f)).max().getAsInt();
        return maxIndex + 1;
    }

    private int getIndexNumberFromMapFile(File file) {
        String fileName = file.toPath().getFileName().toString();
        return Integer.valueOf(fileName.substring(fileName.lastIndexOf("_") + 1, fileName.lastIndexOf(".")));
    }

    @Override
    public void start() {
        try {
            // We make sure the folder exists:
            Files.createDirectories(this.rootFolder);

            // We create/reload the Reference Map:
            this.refMap = ChronicleMap
                    .of(String.class, byte[].class)
                    .name("refMap")
                    .averageKeySize(this.avgCollectionIdSize + 10) // 10-byte buffer for suffixes...
                    .averageValueSize(REFMAP_AVG_VALUE_SIZE)
                    .entries(this.maxCollections * 3)
                    .createPersistedTo(Paths.get(rootFolder.toString(), REF_MAP_FILE_NAME).toFile());

            // We load previous Content Maps if any:
            File[] chunkItemsFiles = this.rootFolder.toFile().listFiles((dir, name) -> name.startsWith(CONTENT_MAPS_FILE_PREFFIX));
            if (chunkItemsFiles != null) {
                for (File file : chunkItemsFiles) {
                    int fileIndex = getIndexNumberFromMapFile(file);
                    addNewContentMap(fileIndex);
                }
            }

        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    @Override
    public void stop() {
        refMap.close();
        this.contentMaps.forEach(m -> m.close());
    }

    @Override
    public void destroy() {
        try {
            // We remove the whole folder:
            Files.walk(this.rootFolder)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    @Override
    public synchronized boolean save(String collectionId, BigCollectionChunk<I> chunk) {
        Preconditions.checkArgument(chunk.getChunkOrdinal() >= 0, "chunkOrdinal must be >= 0");

        // If This chunk is already saved, we just return:
        if (chunkAlreadySaved(collectionId, chunk.getChunkOrdinal())) {
            return false;
        }

        // We locate a Content Map with enough capacity for this Chunk, and we save all the ITEMS in it:
        // ----------------------------------------------------------------------------------------------

        // In order to know how many entries this Chunk is gonna take, we need to Serialize the chunk first, since
        // that number depends on the size of all the Items...

        // So we come up with a List of Items serialized. TThe local memory should be able to handle this:
        List<byte[]> serializedItems = chunk.getItems().stream().map(i -> serializeItem(i)).collect(Collectors.toList());
        int numEntries = serializedItems.stream().mapToInt(b -> Utils.readUint16(b, 0)).sum();

        ChronicleMap<String, byte[]> chunkItemsMap = getContentMapWithFreeCapacity(numEntries).orElseGet(this::addNewContentMap);

        // If we are going to exceed the limit of this CMap, we log it:
        if ((chunkItemsMap.size() + numEntries) > this.maxItemsEachFile) {
            log.warn("writing too many items in a CMAP (current: {}, after writing: {}, limit: {}, )",
                    chunkItemsMap.size(), (chunkItemsMap.size() + numEntries), this.avgItemSize);
        }

        String numItemsKey = keyForChunkNumItems(collectionId, chunk.getChunkOrdinal());
        chunkItemsMap.put(numItemsKey, SerializationUtils.serialize(chunk.getItems().size()));

        long totalChunkBytes = 0;
        for (int i= 0; i < serializedItems.size(); i++) {
            byte[] itemBytes = serializedItems.get(i);
            int numItemBytes = saveItem(collectionId, chunkItemsMap, itemBytes, chunk.getChunkOrdinal(), i);
            totalChunkBytes += numItemBytes;
        }

        // We save the Reference info of this Chunk in the REFERENCE Map:
        // ----------------------------------------------------------------------------------------------

        // We update the ordinals off all the Chunks stored for this collection:
        String chunksKey = keyForCollectionChunks(collectionId);
        List<Integer> chunkIds =  getCollectionChunkOrdinals(collectionId);
        chunkIds.add(chunk.getChunkOrdinal());
        refMap.put(chunksKey, SerializationUtils.serialize((ArrayList) chunkIds));

        // we update the total size of this collection in terms of number of Items:
        String sizeKey = keyForCollectionSize(collectionId);
        Long currentSize = refMap.containsKey(sizeKey) ? Longs.fromByteArray(refMap.get(sizeKey)) : 0;
        refMap.put(sizeKey, Longs.toByteArray(currentSize + chunk.getItems().size()));

        // we update the total size of this collection in terms of number of BYTES
        String colSizeInBytesKey = keyForCollectionSizeInBytes(collectionId);
        Long currentColSizeInBytes = sizeInBytes(collectionId) + totalChunkBytes;
        refMap.put(colSizeInBytesKey, Longs.toByteArray(currentColSizeInBytes));

        // We update the total size IN BYTES of the Store:
        updateTotalStorageSize(totalChunkBytes);

        return true;
    }

    @Override
    public void registerAsCompleted(String collectionId) {
        String key = keyForCollectionCompleted(collectionId);
        this.refMap.put(key, new byte[1]);
    }

    @Override
    public boolean isCompleted(String collectionId) {
        String key = keyForCollectionCompleted(collectionId);
        return this.refMap.get(key) != null;
    }

    private int getNumItemsInChunk(String collectionId, int chunkOrdinal, ChronicleMap<String, byte[]> contentMap) {
        String key = keyForChunkNumItems(collectionId, chunkOrdinal);
        int numItems = SerializationUtils.<Integer>deserialize(contentMap.get(key));
        return numItems;
    }

    @Override
    public synchronized void remove(String collectionId) {

        if (contains(collectionId)) {
            // We remove first the ITEMS of this Chunk from any CONTENT MAP
            List<Integer> chunkOrdinals = getCollectionChunkOrdinals(collectionId);

            // We remove References and Items for each Ordinal:
            for (Integer chunkOrdinal : chunkOrdinals) {
                ChronicleMap<String, byte[]> contentMap = getContentMapContainingChunk(collectionId, chunkOrdinal).get();

                // we get the number of Items and remove them one by one:
                int numItems = getNumItemsInChunk(collectionId, chunkOrdinal, contentMap);
                for (int i = 0; i < numItems; i++) {
                    removeItem(collectionId, contentMap, chunkOrdinal, i);
                }
                contentMap.remove(keyForChunkNumItems(collectionId, chunkOrdinal));
            }

            // We update the total size (in bytes) of the Store:
            updateTotalStorageSize(-sizeInBytes(collectionId));

            // We remove all the entries from the REFERENCE Map:
            
            String chunksKey = keyForCollectionChunks(collectionId);
            refMap.remove(chunksKey);
            String keySize = keyForCollectionSize(collectionId);
            refMap.remove(keySize);
            String keySizeInBytes = keyForCollectionSizeInBytes(collectionId);
            refMap.remove(keySizeInBytes);
            String keyCompleted = keyForCollectionCompleted(collectionId);
            refMap.remove(keyCompleted);

            // After removing this Collection, we check if we can save space on disk:
            checkAndRemoveCMapsIfEmpty();
        }
    }

    @Override
    public Iterator<BigCollectionChunk<I>> getChunks(String collectionId) {
        return new BigCollectionChunksStoreCMap.ChunkIterator(collectionId);
    }

    @Override
    public synchronized long size(String collectionId) {
        String keySize = keyForCollectionSize(collectionId);
        return Longs.fromByteArray(refMap.get(keySize));
    }

    @Override
    public synchronized long sizeInBytes(String collectionId) {
        String key = keyForCollectionSizeInBytes(collectionId);
        return refMap.containsKey(key)? Longs.fromByteArray(refMap.get(key)) : 0;
    }

    @Override
    public synchronized List<String> getCollectionsIds() {
        Iterable<String> keysIt = () -> refMap.keySet().iterator();
        List<String> result = StreamSupport.stream(keysIt.spliterator(), false)
                .filter(k -> k.endsWith("_chunks"))
                .map(k -> k.substring(0, k.indexOf("_chunks")))
                .collect(Collectors.toList());
        return result;
    }

    @Override
    public synchronized long sizeInBytes() {
        return getTotalStorageSizeInBytes();
    }

    @Override
    public synchronized void clear() {
        refMap.clear();
        this.contentMaps.forEach(m -> this.removeAndCleanCMap(m));
        this.contentMaps.clear();
    }

    @Override
    public synchronized void compact() {
        this.contentMaps.stream()
                .filter(m -> m.isEmpty())
                .forEach(m -> this.removeAndCleanCMap(m));
    }

    private void checkAndRemoveCMapsIfEmpty() {
        // We only remove a CMap file if the Map is empty and there are at least 2 Maps empty: this way we avoid the
        // situation where files might be created and removed continuously when collections and added/removed.

        List<ChronicleMap<String, byte[]>> contentMapsEmpty = this.contentMaps.stream()
                .filter(cmap -> cmap.isEmpty())
                .collect(Collectors.toList());
        if (contentMapsEmpty.size() >= 2) {
            // We left 1 Content Map without removing...
            int numMapsRemoved = 0;
            for (ChronicleMap<String, byte[]> contentMap : contentMapsEmpty) {
                // Depending on config, we remove this cmap or we leave it for future reuse:
                if (!this.reuseCMaps) {
                    this.contentMaps.remove(contentMap);
                    removeAndCleanCMap(contentMap);
                }
                numMapsRemoved++;
               if (numMapsRemoved == (contentMapsEmpty.size() - 1)) break;
            }
        }
    }

    private void removeAndCleanCMap(ChronicleMap<String, byte[]> cmap) {
       cmap.clear();
       // We remove the File:
       final int MAX_ATTEMPTS = 5;
       int numAttempt = 0;
       while (true) {
           try {
               Files.delete(cmap.file().toPath());
               return;
           } catch (IOException e) {
               log.error("Error removing CMAP fle {}, {} attempts left", cmap.file().toPath(), (MAX_ATTEMPTS - numAttempt));
               if (++numAttempt < MAX_ATTEMPTS) {
                   try { Thread.sleep(100);} catch (InterruptedException ie) {}
               } else throw new RuntimeException(e);
           }
       } // while...
    }

    @Override
    public boolean contains(String collectionId) {
        String key = keyForCollectionSizeInBytes(collectionId);
        return refMap.containsKey(key);
    }

    private void updateTotalStorageSize(long bytesToAdd) {
        String key = keyForTotalStorageSizeInBytes();
        Long currentTotalSize = getTotalStorageSizeInBytes() + bytesToAdd;
        refMap.put(key, Longs.toByteArray(currentTotalSize));
    }

    public synchronized long getTotalStorageSizeInBytes() {
        String key = keyForTotalStorageSizeInBytes();
        return refMap.containsKey(key)? Longs.fromByteArray(refMap.get(key)) : 0;
    }

    private List<Integer> getCollectionChunkOrdinals(String collectionId) {
        String key = keyForCollectionChunks(collectionId);
        ArrayList<Integer> chunkIds =  refMap.containsKey(key)
                ? SerializationUtils.<ArrayList<Integer>>deserialize(refMap.get(key))
                : new ArrayList<>();
        return chunkIds;
    }

    private byte[] serializeItem(I item) {
        ByteArrayWriter writer = new ByteArrayWriter();
        itemSerializer.serialize(item, writer);
        byte[] itemBytes = writer.reader().getFullContentAndClose();

        // We calculate the number of "partials" for this item:
        int numPartialItems = getNumPartialsForItem(itemBytes);

        // And we serialize it all :the first 2 bytes PLUS the bytes themselves:
        ByteArrayWriter finalWriter = new ByteArrayWriter();
        finalWriter.writeUint16LE(numPartialItems);
        finalWriter.write(itemBytes);
        return finalWriter.reader().getFullContentAndClose();
    }

    protected I deserializeItem(byte[] bytes) {
        ByteArrayReader reader = new ByteArrayReader(bytes);
        // We discard the first 2 bytes, since they contain the number of "partials" for this Item
        reader.readUint16();
        // and we deserialize the ITem normally:
        I result = itemSerializer.deserialize(reader);
        return result;
    }

    private int getNumPartialsForItem(byte[] itemBytes) {
        int bytesToAdd = itemBytes.length + 2;
        return (bytesToAdd > this.avgItemSize)
                ? (int) Math.ceil((double)bytesToAdd / this.avgItemSize)
                : 1;
    }

    private Optional<ChronicleMap<String, byte[]>> getContentMapContainingChunk(String collectionId, int chunkOrdinal) {
        String key =  keyForChunkNumItems(collectionId, chunkOrdinal);
        Optional<ChronicleMap<String, byte[]>> result = this.contentMaps.stream()
                .filter(cm -> cm.containsKey(key))
                .findFirst();
        return result;
    }

    private boolean chunkAlreadySaved(String collectionId, int chunkOrdinal) {
        Optional<ChronicleMap<String, byte[]>> refMap = getContentMapContainingChunk(collectionId, chunkOrdinal);
        return refMap.isPresent();
    }

    protected List<I> getItemsFromChunk(String collectionId, int chunkOrdinal) {
        List<I> result = new ArrayList<>();
        ChronicleMap<String, byte[]> contentMap = getContentMapContainingChunk(collectionId, chunkOrdinal).get();

        // The chunk is split into different Items, so first we check the number of parts and then we loop over them...
        String numItemsKey = keyForChunkNumItems(collectionId, chunkOrdinal);
        int numItems = SerializationUtils.<Integer>deserialize(contentMap.get(numItemsKey));

        for (int i = 0; i < numItems; i++) {
            I item = getItem(collectionId, contentMap, chunkOrdinal, i);
            result.add(item);
        }

        return result;
    }

    /*
        It saves an individual Item from within a Chunk for a Collection
     */
    private int saveItem(String collectionId, ChronicleMap<String, byte[]> chunkItemsMap, I item, int chunkOrdinal, int itemOrdinal) {
        byte[] itemBytes = serializeItem(item);
        return saveItem(collectionId, chunkItemsMap, itemBytes, chunkOrdinal, itemOrdinal);
    }

    /*
        It saves an individual Item from within a Chunk for a Collection
     */
    private int saveItem(String collectionId, ChronicleMap<String, byte[]> chunkItemsMap, byte[] itemBytes, int chunkOrdinal, int itemOrdinal) {
        int numPartials = Utils.readUint16(itemBytes, 0);
        if (numPartials == 1) {
            String itemKey = keyForChunkItem(collectionId, chunkOrdinal, itemOrdinal, 0);
            chunkItemsMap.put(itemKey, itemBytes);
        } else {
            int bytesLeftToSave = itemBytes.length;
            int i = 0;
            while (bytesLeftToSave > 0) {
                int numBytesToSave = (int) Math.min(bytesLeftToSave, this.avgItemSize);
                byte[] bytesToSAve = new byte[numBytesToSave];
                System.arraycopy(itemBytes, (int) (i * this.avgItemSize), bytesToSAve, 0, bytesToSAve.length);
                String itemKey = keyForChunkItem(collectionId, chunkOrdinal, itemOrdinal, i);
                chunkItemsMap.put(itemKey, bytesToSAve);
                i++;
                bytesLeftToSave -= numBytesToSave;
            } // while...
        }
        return itemBytes.length;
    }

    /*
        It returns an individual Item from within a Chunk for a Collection
     */
    private I getItem(String collectionID, ChronicleMap<String, byte[]> chunkItemsMap, int chunkOrdinal, int itemOrdinal) {
        I item = null;
        // We read the number of "Partials" of this Item, which are stored in the first entry:
        byte[] firstEntry = chunkItemsMap.get(keyForChunkItem(collectionID, chunkOrdinal, itemOrdinal, 0));
        int numPartials = Utils.readUint16(firstEntry, 0);
        if (numPartials == 1) {
            item = deserializeItem(firstEntry);
        } else {
            byte[] totalItem = new byte[(int) this.avgItemSize * numPartials];
            System.arraycopy(firstEntry, 0, totalItem, 0, firstEntry.length);
            for (int i = 1; i < numPartials; i++) {
                String itemPartialKey = keyForChunkItem(collectionID, chunkOrdinal, itemOrdinal, i);
                byte[] itemsPartialBytes = chunkItemsMap.get(itemPartialKey);
                System.arraycopy(itemsPartialBytes, 0, totalItem, (int) (this.avgItemSize * i), itemsPartialBytes.length);
            }
            item = deserializeItem(totalItem);
        }

        return item;
    }

    /*
        It removes an individual Item from within a Chunk for a Collection
     */
    private void removeItem(String collectionID, ChronicleMap<String, byte[]> chunkItemsMap, int chunkOrdinal, int itemOrdinal) {
        // We read the number of "Partials" of this Item, which are stored in the first entry:
        byte[] firstEntry = chunkItemsMap.get(keyForChunkItem(collectionID, chunkOrdinal, itemOrdinal, 0));
        int numPartials = Utils.readUint16(firstEntry, 0);
        for (int i = 0; i < numPartials; i++) {
            chunkItemsMap.remove(keyForChunkItem(collectionID, chunkOrdinal, itemOrdinal, i));
        }
    }

    private Optional<ChronicleMap<String, byte[]>> getContentMapWithFreeCapacity(int numEntriesNeeded) {
        return this.contentMaps.stream()
                .filter(m -> m.size() < (this.maxItemsEachFile - numEntriesNeeded))
                .findFirst();
    }

    // for Testing ONLY
    public void printContent() {
        StringBuffer line = new StringBuffer();

        // We print the contents of the REFERENCE MAP:
        line.append(":: Reference CMap Keys:\n");
        refMap.keySet().forEach(k -> line.append("   - " + k  + "\n"));

        // We print the contents of ALL the CONTENT MAPS:
        line.append(":: Content CMap Keys:\n");
        contentMaps.forEach(cmap -> {
            line.append(":: CMap " + cmap.name() + ", " + cmap.percentageFreeSpace() + "% free :\n");
            for (String k : cmap.keySet()) {
                line.append("   - " + k  + "\n");
            }
        });
        System.out.println(line.toString());
    }

}