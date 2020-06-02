package com.nchain.jcl.tools.files;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2019-09-17 18:26
 *
 * Base class for the FileUtil implementations
 */
@Slf4j
public abstract class FileUtilsBase implements FileUtils {

    protected static final String DATA_FOLDER = "data";
    protected static final String CONFIG_FOLDER = "config";

    @Override
    public <T extends CSVSerializable> List<T> readCSV(Path filePath, String separator, Function<String, T> instanceFactory) {
        List<T> result = new ArrayList<>();
        try {
            List<String> fileContent = new ArrayList<>();
            if (Files.exists(filePath)) fileContent = Files.lines(filePath).collect(Collectors.toList());
            for (String line : fileContent)
                try {
                    T instance = instanceFactory.apply(line);
                    result.add(instance);
                } catch (Exception e) {}
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    @Override
    public <T extends CSVSerializable> List<T> readCSV(Path filePath, Function<String, T> instanceFactory) {
        return readCSV(filePath, CSVSerializable.SEPARATOR, instanceFactory);
    }

    @Override
    public <T extends CSVSerializable> int writeCSV(Path filePath, Stream<T> items, String separator) {
        try {
            AtomicInteger result = new AtomicInteger();
            Path folderPath = filePath.getParent();
            if (!Files.exists(folderPath)) Files.createDirectories(folderPath);
            try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
                items.forEach(item -> {
                    try {
                        String line = item.toCSVLine(separator) + "\n";
                        writer.write(line);
                        result.incrementAndGet();
                    }  catch (Exception e) {  }
                });
                writer.flush();
                return result.get();
            } catch (Exception e) {
                log.error("Error", e);
                throw new RuntimeException(e);
            }
        } catch (Exception e) {
            log.error("Error", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T extends CSVSerializable> int writeCSV(Path filePath, Collection<T> items, Predicate<T> filter) {
        Stream<T> itemsToWrite = items.stream();
        if (filter != null) itemsToWrite = itemsToWrite.filter(filter);
        int result = writeCSV(filePath, itemsToWrite, CSVSerializable.SEPARATOR);
        return result;
    }
    @Override
    public <T extends CSVSerializable> int writeCSV(Path filePath, Collection<T> items) {
        int result = writeCSV(filePath, items, null);
        return result;
    }
}
