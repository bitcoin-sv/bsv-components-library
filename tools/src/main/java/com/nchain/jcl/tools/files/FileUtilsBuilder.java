package com.nchain.jcl.tools.files;


import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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

    // Copy Folders content, replacing files in destination if already exists
    private static int copy(Path fromFolder, Path toFolder) throws IOException {
        AtomicInteger result = new AtomicInteger(0);
        if (!Files.exists(toFolder)) Files.createDirectories(toFolder);
        Files.list(fromFolder).forEach(p -> {
            try {
                Path path2 = Path.of(toFolder.toString(), p.getFileName().toString());
                if (Files.isRegularFile(p)) {
                    Files.copy(p, path2, StandardCopyOption.REPLACE_EXISTING);
                    result.incrementAndGet();
                } else  result.addAndGet(copy(p, path2));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return result.get();
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
     * If the Path starts with a leading slash ("/"), it will fail in Windows, so we remove it.
     * If we are NOT in Windows, we leave it as it is...
     * A convenience method. Not a very elegant way to solve it, but it works...
     */
    private Path adjustPathLeadingSlash(Path path) {
        String OS = System.getProperty("os.name").toLowerCase();
        if (OS.contains("win") && path.toString().startsWith("/"))
            return Path.of(path.toString().substring(1));
        else return path;
    }

    // Conveniece method to indicate whether the classpath folder is inside a JAR file
    private boolean isInsideAJar(ClassLoader classLoader, String folder) throws Exception {
        URI uri = classLoader.getResource(folder).toURI();
        return uri.getScheme().equals("jar");
    }
    // Returns the Path of a classpath folder (it might be a regular folder or a folder inside a JAR File)
    private Path getPathFromClasspathFolder(ClassLoader classLoader, String folder) throws Exception {
        Path result = null;
        URI uri = classLoader.getResource(folder).toURI();
        if (uri.getScheme().equals("jar")) {
            // The folder is inside a JAR File. In this case, in order to get a PAth reference to it, we
            // need to work with FileSystem class...
            log.debug(" >> getting the file system of a JAR file: " + uri.toString());

            // We break down the path into different parts, separated by "!":
            // - the first part s the schema and jar file location.
            // - the rest are the path within the JAR File. For some reason, there might be another "!"
            //   (jar separator) in them, so we need to remove them as well.

            // in the end we'll end u with just 2 entries: one for the JAr file location, and another for
            // the folder within the JAR
            String[] pathItems = uri.toString().split("!");
            String jarFileLocation = pathItems[0];
            String jarFolderLocation = pathItems[1].substring(0, pathItems[1].length()) + pathItems[2];

            log.debug(" >> JAR file FileSystem: " + jarFileLocation);
            FileSystem fs;
            try {
                log.debug(" >> getting FileSystem...");
                fs = FileSystems.getFileSystem(URI.create(jarFileLocation));
            } catch (FileSystemNotFoundException fnf) {
                log.debug(" >> fileSystem not found. Initializing it...");
                Map<String, String> env = new HashMap<>();
                fs = FileSystems.newFileSystem(URI.create(jarFileLocation), env);
            }
            Path path = fs.getPath(jarFolderLocation);
            return path;
        } else result = Paths.get(uri);
        return result;
    }

    /**
     * It creates a new instance of FileUtils.
     */
    public FileUtils build(ClassLoader classLoader) throws IOException {

        try {
            // We perform some Verifications first:

            checkState((!copyFromClasspath || classLoader != null),
                    "If you specify 'copyFromClassPath', then you must use a ClassLoader in the 'build' method");

            checkState((!copyFromClasspath || classLoader.getResource(this.rootFolder + "/") != null),
                    "If you specify 'copyFromClassPath', then a '/" + rootFolder + "' folder must exist within the classpath");

            checkState((folderLocationType != RootFolderLocation.CLASSPATH_FOLDER || classLoader != null),
                    "If you specify 'userClassPath', then you must use a ClassLoader in the 'build' method");

            checkState((folderLocationType != RootFolderLocation.CLASSPATH_FOLDER || classLoader.getResource(this.rootFolder + "/") != null),
                    "If you specify 'userClassPath', then a '/" + this.rootFolder + "' folder must exist within the classpath");

            // We build the work folder we are using. The temporary folder is straightforward.
            // But the classpath one is tricky: if we are running the application within a JAR, then some "File"
            // operations are not available, so here we are gonna use Apache Commons IO:

            Path rootFolderPath = null;
            if (folderLocationType == RootFolderLocation.TEMPORARY_FOLDER) {
                rootFolderPath = Path.of(System.getProperty("java.io.tmpdir"), this.rootFolder);
                rootFolderPath = adjustPathLeadingSlash(rootFolderPath);
                log.debug("work dir [temporary folder]: " + rootFolderPath);
            } else {
                boolean insideJar = isInsideAJar(classLoader, rootFolder);
                rootFolderPath = getPathFromClasspathFolder(classLoader, rootFolder);
                rootFolderPath = adjustPathLeadingSlash(rootFolderPath);
                if (insideJar)
                    log.debug("work dir [classpath JAR folder]: " + rootFolderPath);
                else
                    log.debug("work dir [classpath folder]: " + rootFolderPath);
            }

            // If we have specified Temporary folder, we make sure that folder exists
            if (folderLocationType == RootFolderLocation.TEMPORARY_FOLDER) Files.createDirectories(rootFolderPath);

            if (copyFromClasspath) {
                Path classpathFolderPath = getPathFromClasspathFolder(classLoader, rootFolder);

                if (classpathFolderPath != null) {
                    log.debug(" > Copying resources from classpath folder into work folder...");
                    log.debug(" > Classpath folder: " + classpathFolderPath);
                    int numResources = copy(classpathFolderPath, rootFolderPath);
                    log.debug(" > " + numResources + " files copied.");
                }
            }
            return new FileUtilsImpl(rootFolderPath);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * It creates a new instance of FileUtils.
     */
    public FileUtils build() throws IOException {
        return build(null);
    }
}
