package io.bitcoinsv.bsvcl.tools.bigObjects.stores;

import io.bitcoinsv.bsvcl.tools.bytes.ByteArrayReader;
import io.bitcoinsv.bsvcl.tools.bytes.ByteArrayWriter;
import io.bitcoinsv.bsvcl.tools.config.RuntimeConfig;
import net.openhft.chronicle.map.ChronicleMap;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An implementation of ObjectStore, using CronicleMap as the underlying Database.
 *
 * Some characteristics of the DB/ChornicleMap:
 * - Only 1 File is used, so you need to resereve the space for it in advance by using the constructor
 *   (this store is meant to be used for "small" objects and which number is not growing very much over time.
 * - Each object is stored in a simple ENTRY, where the Key is the "objectId" and the VALUE is the object itself
 *   after being Serialized using a proper Serializer.
 *
 * @param <T> Class of the object to store.
 */
public class ObjectStoreCMap<T> implements ObjectStore<T> {

    // Object Serializer used to write/read objects to the CMap
    private ObjectSerializer<T> objSerializer;

    // Configuration Variables:
    private String storeId;
    private int avgKeySizeInBytes;
    private int avgValueSizeInBytes;
    private long numEntries;
    private Path rootFolder;

    // Store:
    private ChronicleMap<String, byte[]> cmap;

    /**
     * Constructor. It initializes the Chronicle Map. The file will be saved in the [JCL-dir]/store/[storeId] folder.
     * @param runtimeConfig         JCL Runtime Configuration
     * @param storeId               Used to create a Folder where the CMap will be saved
     * @param objSerializer         Serializer for each entry
     * @param avgKeySizeInBytes     Avg Key size
     * @param avgValueSizeInBytes   Avg Vaue Size
     * @param numEntries            Maximum number of Entries in the cmap file
     */
    public ObjectStoreCMap(RuntimeConfig runtimeConfig,
                           String storeId,
                           ObjectSerializer<T> objSerializer,
                           int avgKeySizeInBytes,
                           int avgValueSizeInBytes,
                           long numEntries) {
        this.storeId = storeId;
        this.avgKeySizeInBytes = avgKeySizeInBytes;
        this.avgValueSizeInBytes = avgValueSizeInBytes;
        this.numEntries = numEntries;
        this.objSerializer = objSerializer;
        this.rootFolder = Paths.get(runtimeConfig.getFileUtils().getRootPath().toString(), "store", storeId);
    }

    private byte[] serialize(T obj) {
        ByteArrayWriter writer = new ByteArrayWriter();
        objSerializer.serialize(obj, writer);
        return writer.reader().getFullContentAndClose();
    }

    private T deserialize(byte[] bytes) {
        ByteArrayReader reader = new ByteArrayReader(bytes);
        return objSerializer.deserialize(reader);
    }

    @Override
    public void start() {
        // ChronicleMaps initialization:
        try {
            // Reference Chronicle Map initialization
            Files.createDirectories(this.rootFolder);
            File refMapFle = Paths.get(rootFolder.toString(),  objSerializer.getClass().getSimpleName() + "_objs.dat").toFile();

            cmap = ChronicleMap
                    .of(String.class, byte[].class)
                    .name(storeId + objSerializer.getClass().getSimpleName() + "_objs")
                    .entries(numEntries)
                    .averageKeySize(avgKeySizeInBytes)
                    .averageValueSize(avgValueSizeInBytes)
                    .createOrRecoverPersistedTo(refMapFle);

        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    @Override
    public void stop() { cmap.close();}

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
    public void save(String objectId, T object) {
        cmap.put(objectId, serialize(object));
    }
    @Override
    public void remove(String objectId) {
        cmap.remove(objectId);
    }

    @Override
    public T get(String objectId) {
        byte[] bytes = cmap.get(objectId);
        return (bytes != null) ? deserialize(bytes) : null;
    }

    @Override
    public void clear() {
        cmap.clear();
    }
}