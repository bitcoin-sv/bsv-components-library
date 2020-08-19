package com.nchain.jcl.tools.files;


import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.net.URL;
import java.nio.file.*;

import static com.google.common.base.Preconditions.checkState;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2019-10-29 13:36
 *
 * A Builder for Fileutils instances.
 *
 */
@Slf4j
public class FileUtilsBuilder {

    // Default name for the Root Folder.
    private final static String ROOT_FOLDER = "jcl";

    // The root folder assigned to the FileUtils instance.
    private String rootFolder = ROOT_FOLDER;

    // Define the Type of Location used for the Root folder
    enum RootFolderLocation {
        TEMPORARY_FOLDER,
        CLASSPATH_FOLDER;
    }
    private RootFolderLocation folderLocationType = RootFolderLocation.TEMPORARY_FOLDER;

    // Only applicable for TEMPORARY_FOLDER type. If TRUE, it searches for a RootFolder in the Classpath,
    // and if found, it copies all its content into the Temporary Folder. This is useful in development
    // environments: Yu can plae files in your /resources/[ROOT] folder, and these files will be copies into
    // your Temporary Folder before running
    private boolean copyFromClasspath = false;


    // Copy Folders content, replacing files in destinationif already exists
    private static void copy(Path fromFolder, Path toFolder) throws IOException {
        if (!Files.exists(toFolder)) Files.createDirectories(toFolder);
        Files.list(fromFolder).forEach(p -> {
            try {
                Path path2 = Path.of(toFolder.toString(), p.getFileName().toString());
                if (Files.isRegularFile(p))
                        Files.copy(p, path2, StandardCopyOption.REPLACE_EXISTING);
                else    copy(p, path2);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /** It will use a OS Temporary folder */
    public FileUtilsBuilder useTempFolder() {
        this.folderLocationType = RootFolderLocation.TEMPORARY_FOLDER;
        return this;
    }

    /** It will use a OS Temporary folder, pointing a the folder given */
    public FileUtilsBuilder useTempFolder(String rootFolder) {
        this.rootFolder = rootFolder;
        return useTempFolder();
    }

    /** It will use a folder located in the App classpath */
    public FileUtilsBuilder useClassPath() {
        this.folderLocationType = RootFolderLocation.CLASSPATH_FOLDER;
        return this;
    }

    /** It will use a folder localted in the Classpath, pointing at the folder given */
    public FileUtilsBuilder useClassPath(String rootFolder) {
        this.rootFolder = rootFolder;
        return useClassPath();
    }

    /**
     * Only applicable when using "TempFolder". Before running, it will search for the RootFolder within the Classpath,
     * and all its content will be copied into the root folder in the temporary folder.
     * @return
     */
    public FileUtilsBuilder copyFromClasspath() {
        checkState(folderLocationType == RootFolderLocation.TEMPORARY_FOLDER,
                "Copying from Classpath is only available when you use Temp folder");
        this.copyFromClasspath = true;
        return this;
    }

    /**
     * It creates a new instance of FileUtils.
     */
    public FileUtils build() throws IOException {

        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();

            // If we have specified CLASSPATH Folder, we make sure it exists...
            if (loader.getResource(this.rootFolder + "/") == null)
                throw new Exception("If you specify CLASSPATH in FileUtils, a '/" + rootFolder + "' folder must exist within the classpath");

            Path rootFolder = (folderLocationType == RootFolderLocation.TEMPORARY_FOLDER)
                    ? Path.of(System.getProperty("java.io.tmpdir"), this.rootFolder)
                    : Path.of(loader.getResource(this.rootFolder + "/").getPath());

            // If we have specified Temporary folder, we make sure that folder exists
            if (folderLocationType == RootFolderLocation.TEMPORARY_FOLDER) Files.createDirectories(rootFolder);

            // If we have specified to copy resources from the classpath, we do it as lonng as the folder exists in the
            // classpath:
            if (copyFromClasspath) {
                URL classpathFolder = loader.getResource(rootFolder + "/");
                if (classpathFolder != null) {
                    Path fromPath = Path.of(classpathFolder.toURI());
                    if (fromPath != null) copy(fromPath, rootFolder);
                }
            }
            return new FileUtilsImpl(rootFolder);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
