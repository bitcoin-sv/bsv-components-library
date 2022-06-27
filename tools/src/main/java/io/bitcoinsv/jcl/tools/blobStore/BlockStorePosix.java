package io.bitcoinsv.jcl.tools.blobStore;

import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.HeaderReadOnly;
import io.bitcoinsv.bitcoinjsv.bitcoin.bean.base.HeaderBean;
import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;
import io.bitcoinsv.bitcoinjsv.core.Utils;
import io.bitcoinsv.bitcoinjsv.core.VarInt;
import io.bitcoinsv.jcl.tools.bytes.InputStreamReader;
import io.bitcoinsv.jcl.tools.serialization.BitcoinSerializerUtils;
import io.bitcoinsv.jcl.tools.serialization.TransactionSerializerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shaded.org.apache.commons.io.FileUtils;
import shaded.org.apache.maven.wagon.ResourceDoesNotExistException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 *
 * @author m.fletcher@nchain.com
 * @date 16/03/2022
 */
public class BlockStorePosix {

    private static final Logger log = LoggerFactory.getLogger(BlockStorePosix.class);

    private static final String BLOCK_FILE_TYPE_BLOCK_DATA = "block";
    private static final String BLOCK_FILE_TYPE_TX_LIST = "txs";
    private static final String BLOCK_FILE_TYPE_TMP = "tmp";

    private BlockStorePosixConfig config;
    public BlockStorePosix(BlockStorePosixConfig config) {
        this.config = config;

    }

    /**
     * Saves the list of given block hashes. Once a block has been committed, no more data related to that block will be able to be saved.
     * @param headerReadOnly
     * @param blockHashes
     * @throws IllegalAccessException
     */
    public void saveBlockHashes(HeaderReadOnly headerReadOnly, List<Sha256Hash> blockHashes) throws IllegalAccessException {
        if (containsBlock(headerReadOnly.getHash())) {
            throw new IllegalAccessException("cannot write to a committed block");
        }

        File blockHashFile = getFileByFileType(headerReadOnly.getHash(), BLOCK_FILE_TYPE_TX_LIST);

        FileOutputStream bos = null;
        try {
            boolean exists = blockHashFile.exists();

            if(!exists){
                createTempFile(headerReadOnly.getHash());
            }

            bos = FileUtils.openOutputStream(blockHashFile, exists);
            bos.getChannel().lock();

            for(Sha256Hash hash: blockHashes){
                bos.write(hash.getBytes());
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bos != null)
                    bos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * Saves the block {@literal &} txs. If the block already exists, the txs will be appended. Method will lock on each file csaved. Note: commit must be called
     * once completed to remove the temp file created and flag that a file has been saved correctly
     *
     * @param headerReadOnly the header for the block
     * @param numberOfTxs the total txs within the block
     * @param txsBytes       raw bytes of all the txs being saved, they will be appended if the block already exists
     * @throws IllegalAccessException exception is thrown if an attempt is made to write to a committed block
     */
    public void saveBlock(HeaderReadOnly headerReadOnly, long numberOfTxs, byte[] txsBytes) throws IllegalAccessException {
        File blockFile = getFileByFileType(headerReadOnly.getHash(), BLOCK_FILE_TYPE_BLOCK_DATA);

        if (containsBlock(headerReadOnly.getHash())) {
            throw new IllegalAccessException("cannot write to a committed block");
        }

        FileOutputStream bos = null;
        try {
            boolean exists = blockFile.exists();
            bos = FileUtils.openOutputStream(blockFile, exists);
            bos.getChannel().lock();

            if (!exists) {
                createTempFile(headerReadOnly.getHash());

                bos.write(headerReadOnly.serialize());
                bos.write(new VarInt(numberOfTxs).encode());
            }

            bos.write(txsBytes);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(bos != null)
                    bos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Commits the block directory by removing the temp file which indicates the directory has finished being written too
     *
     * @param blockHash
     */
    public boolean commitBlock(Sha256Hash blockHash) {
        File blockFileTemp = getFileByFileType(blockHash, BLOCK_FILE_TYPE_TMP);

        return blockFileTemp.delete();
    }

    /**
     * Reads the block header from file
     *
     * @param blockHash
     * @return the header of the block
     */
    public HeaderReadOnly readBlockHeader(Sha256Hash blockHash) throws ResourceDoesNotExistException {
        File file = getFileByFileType(blockHash, BLOCK_FILE_TYPE_BLOCK_DATA);

        if(!containsBlock(blockHash)){
            throw new ResourceDoesNotExistException("block has either not been committed or does not exist");
        }

        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            //read the first 80 bytes from the file, which is the header
            return new HeaderBean(fileInputStream.readNBytes(HeaderReadOnly.FIXED_MESSAGE_SIZE));
        } catch (FileNotFoundException ex) {
            //should be caught by containsBlock
            log.warn("Attempted lookup on none existent block: " + blockHash);
        } catch (IOException e) {
            log.error("Error reading file:" + e);
        }

        return null;
    }

    /**
     * Reads the number of txs field from within the block
     *
     * @param blockHash
     * @return the header of the block
     */
    public Long readNumberOfTxs(Sha256Hash blockHash) throws ResourceDoesNotExistException {
        File file = getFileByFileType(blockHash, BLOCK_FILE_TYPE_BLOCK_DATA);

        if(!containsBlock(blockHash)){
            throw new ResourceDoesNotExistException("block has either not been committed or does not exist");
        }

        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file))) {
            //read and ignore the header
            reader.read(HeaderReadOnly.FIXED_MESSAGE_SIZE);
            //next bytes are number of txs
            return BitcoinSerializerUtils.deserializeVarInt(reader);
        } catch (FileNotFoundException ex) {
            //should be caught by containsBlock
            log.warn("Attempted lookup on none existent block: " + blockHash);
        } catch (IOException e) {
            log.error("Error reading file:" + e);
        }

        return null;
    }


    /**
     * Returns the full block as a stream
     * @param blockHash
     * @return
     */
    public Stream<byte[]> readBlock(Sha256Hash blockHash) throws ResourceDoesNotExistException {
        return streamBlock(blockHash, false);
    }

    /**
     * Returns the block txs as a stream
     * @param blockHash
     * @return
     */
    public Stream<byte[]> readBlockTxs(Sha256Hash blockHash) throws ResourceDoesNotExistException {
        return streamBlock(blockHash, true);
    }

    /**
     * Returns a stream containing a list of tx hashes. The stream must be closed if not fully iterated.
     *
     * @param blockHash
     * @return
     */
    private Stream<Sha256Hash> readBlockTxHashes(Sha256Hash blockHash) throws ResourceDoesNotExistException {
        File file = getFileByFileType(blockHash, BLOCK_FILE_TYPE_TX_LIST);

        if(!containsBlock(blockHash)){
            throw new ResourceDoesNotExistException("block has either not been committed or does not exist");
        }

        try {
            InputStreamReader reader = new InputStreamReader(new FileInputStream(file));

            Iterator<Sha256Hash> fisIterator = new Iterator<>() {
                @Override
                public boolean hasNext() {
                    try {
                        if (reader.available() > 0)
                            return true;
                        else {
                            reader.closeAndClear();
                            return false;
                        }
                    } catch (IOException e) {
                        log.warn("Unable to read file");
                    }

                    return false;
                }

                @Override
                public Sha256Hash next() {
                    return Sha256Hash.wrap(reader.read(Sha256Hash.LENGTH));
                }
            };


            Stream<Sha256Hash> stream = StreamSupport.stream(Spliterators.spliteratorUnknownSize(fisIterator, Spliterator.ORDERED), false).onClose(() -> {
                reader.closeAndClear();
            });

            return stream;

        } catch (IOException ex) {
            //should be caught by containsBlock
            log.warn("Attempted lookup on none existent block: " + blockHash);
        }

        return Stream.empty();
    }

    /**
     * Returns a stream containing the txs bytes. The stream must be closed if not fully iterated.
     *
     * @param blockHash
     * @return
     */
    private Stream<byte[]> streamBlock(Sha256Hash blockHash, boolean txsOnly) throws ResourceDoesNotExistException {
        File file = getFileByFileType(blockHash, BLOCK_FILE_TYPE_BLOCK_DATA);

        if(!containsBlock(blockHash)){
            throw new ResourceDoesNotExistException("block has either not been committed or does not exist");
        }

        try {
            InputStreamReader reader = new InputStreamReader(new FileInputStream(file));

            //ignore the header and numberOfTxs field
            if(txsOnly) {
                reader.read(HeaderReadOnly.FIXED_MESSAGE_SIZE);
                BitcoinSerializerUtils.deserializeVarInt(reader);
            }

            Iterator<byte[]> fisIterator = new Iterator<>() {
                @Override
                public boolean hasNext() {
                    try {
                        if (reader.available() > 0)
                            return true;
                        else {
                            reader.closeAndClear();
                            return false;
                        }
                    } catch (IOException e) {
                        log.warn("Unable to read file");
                    }

                    return false;
                }

                @Override
                public byte[] next() {
                    return reader.read(config.getBatchSize());
                }
            };


            Stream<byte[]> stream = StreamSupport.stream(Spliterators.spliteratorUnknownSize(fisIterator, Spliterator.ORDERED), false).onClose(() -> {
                    reader.closeAndClear();
            });

            return stream;

        } catch (IOException ex) {
            //should be caught by containsBlock
            log.warn("Attempted lookup on none existent block: " + blockHash);
        }

        return Stream.empty();
    }

    public Stream<byte[]> readPartiallySerializedBlockTxs(Sha256Hash blockHash) throws ResourceDoesNotExistException {
        File file = getFileByFileType(blockHash, BLOCK_FILE_TYPE_BLOCK_DATA);

        if(!containsBlock(blockHash)){
            throw new ResourceDoesNotExistException("block has either not been committed or does not exist");
        }

        try {
            InputStreamReader reader = new InputStreamReader(new FileInputStream(file));

            //ignore the first 80 bytes
            reader.read(80);

            // ignore next var int
            BitcoinSerializerUtils.deserializeVarInt(reader);

            Iterator<byte[]> transactionIterator = new Iterator<>() {
                @Override
                public boolean hasNext() {
                    try {
                        return reader.available() > 0;
                    } catch (IOException e) {
                        log.warn("Unable to read file");
                    }

                    reader.closeAndClear();

                    return false;
                }

                @Override
                public byte[] next() {
                    try {
                        if (reader.available() == 0) {
                            reader.closeAndClear();
                            throw new NoSuchElementException("No more bytes in block!");
                        }

                        return TransactionSerializerUtils.deserializeNextTx(reader);
                    } catch (IOException e) {
                        log.warn("Unable to read file");
                        throw new NoSuchElementException("Couldn't read bytes from block!");
                    }
                }
            };

            return StreamSupport.stream(Spliterators.spliteratorUnknownSize(transactionIterator, Spliterator.ORDERED), false);
        } catch (FileNotFoundException ex) {
            log.warn("Attempted lookup on none existent block: {}", blockHash);
        }

        return Stream.empty();
    }

    /**
     * Removes the block given the hash
     *
     * @param blockHash
     */
    public void removeBlock(Sha256Hash blockHash) {
        File blockFile = getFileByFileType(blockHash, BLOCK_FILE_TYPE_BLOCK_DATA);
        File blockFileTemp = getFileByFileType(blockHash, BLOCK_FILE_TYPE_TMP);

        blockFile.delete();
        blockFileTemp.delete();
    }

    /**
     * clears all data within the database fs
     */
    public void clear() {
        try {
            FileUtils.deleteDirectory(getRootDir().toFile());
        } catch (IOException ex) {
            log.warn("Unable to clear database");
        }
    }

    /**
     * Clears all blocks that have not been committed
     */
    public void clearUncommittedBlocks(){
        try {
            Files.walk(getFileTypeRootDir(BLOCK_FILE_TYPE_TMP)).filter(Files::isRegularFile)
                    .forEach(f -> {
                        Sha256Hash blockHashFromFileName = Sha256Hash.wrap(f.getFileName().toString());
                        removeBlock(blockHashFromFileName);
                    });
        } catch (IOException ex) {
            log.warn("Unable to clear uncommitted blocks from database");
        }
    }

    /**
     * @param blockHash
     * @return block size in bytes
     */
    public long getBlockSize(Sha256Hash blockHash) throws ResourceDoesNotExistException {
        try {
            if(!containsBlock(blockHash)){
                throw new ResourceDoesNotExistException("block has not been committed");
            }

            return Files.size(getFileByFileType(blockHash, BLOCK_FILE_TYPE_BLOCK_DATA).toPath());
        } catch (IOException ex) {
            return 0;
        }
    }

    /**
     * @param blockHash
     * @return true if the block exists and has been committed
     */
    public boolean containsBlock(Sha256Hash blockHash) {
        File blockFile = getFileByFileType(blockHash, BLOCK_FILE_TYPE_BLOCK_DATA);
        File blockFileTemp = getFileByFileType(blockHash, BLOCK_FILE_TYPE_TMP);

        return !blockFileTemp.exists() && blockFile.exists();
    }


    /**
     * Returns the file within the block path subdir
     * @param hash the object hash representing the file name
     * @param fileType the type of file we want to open
     * @return
     */
    private File getFileByFileType(Sha256Hash hash, String fileType){
        return getFileTypeRootDir(fileType).resolve(calculateFanoutPath(hash)).resolve(hash.toString()).toFile();
    }

    /**
     * Returns the root path of the file type
     * @param fileType
     * @return
     */
    private Path getFileTypeRootDir(String fileType) {
        return getRootDir().resolve(fileType);
    }

    /**
     * @return the base path where blocks should be stored
     */
    private Path getRootDir() {
        return Paths.get(config.getWorkingFolder().toString());
    }

    /**
     * We want to fan out directories on the first 2 bytes, so each folder has a maximum of 256 subfolders. Else we'll end upp with huge directories.
     *
     * @param blockHash
     * @return
     */
    private Path calculateFanoutPath(Sha256Hash blockHash) {
        byte[] reversedHashBytes = blockHash.getReversedBytes();

        byte[] firstByte = {reversedHashBytes[0]};
        byte[] secondByte = {reversedHashBytes[1]};

        return Paths.get(Utils.HEX.encode(firstByte), Utils.HEX.encode(secondByte));
    }

    /**
     * Creates a temp file to keep track of which blocks have been committed
     */
    private void createTempFile(Sha256Hash blockHash) throws IOException {
        File tempFile = getFileByFileType(blockHash, BLOCK_FILE_TYPE_TMP);
        tempFile.mkdirs();
        tempFile.createNewFile();
    }
}
