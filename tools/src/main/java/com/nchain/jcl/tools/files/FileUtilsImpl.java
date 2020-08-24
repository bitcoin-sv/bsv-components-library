package com.nchain.jcl.tools.files;

import lombok.Getter;
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
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-25 10:46
 *
 * Basic implementation for FileUtils
 */
@Slf4j
public class FileUtilsImpl implements FileUtils {

    @Getter private Path rootPath;

    /** Constructor */
    public FileUtilsImpl(Path rootPath) throws IOException{
        this.rootPath = rootPath;
        if (!Files.exists(rootPath)) Files.createDirectory(rootPath);
    }

    @Override
    public <T extends CSVSerializable> List<T> readCV(Path pathFile, Supplier<T> instanceFactory) {
        List<T> result = new ArrayList<>();
        try {
            List<String> fileContent = new ArrayList<>();
            if (Files.exists(pathFile)) fileContent = Files.lines(pathFile).collect(Collectors.toList());
            int countLines = 0;
            for (String line : fileContent)
                try {
                    countLines++;
                    T instance = instanceFactory.get();
                    instance.fromCSVLine(line);
                    result.add(instance);
                } catch (Exception e) {
                    log.error("Error Parsing a SV Line. " + countLines + " lines parsed before this error", e.getMessage());
                }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    @Override
    public <T extends CSVSerializable> int writeCSV(Path pathFile, Collection<T> items) {
        try {
            if (items == null) return 0;
            AtomicInteger result = new AtomicInteger();
            Path folderPath = pathFile.getParent();
            if (!Files.exists(folderPath)) Files.createDirectories(folderPath);
            try (BufferedWriter writer = Files.newBufferedWriter(pathFile, StandardCharsets.UTF_8)) {
                items.forEach(item -> {
                    try {
                        String line = item.toCSVLine() + "\n";
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

}
