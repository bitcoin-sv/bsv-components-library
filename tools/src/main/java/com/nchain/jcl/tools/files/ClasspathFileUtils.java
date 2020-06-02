package com.nchain.jcl.tools.files;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2019-09-12 16:22
 *
 * Implementation where the location of the folders are based on the project Classpath.
 */
public class ClasspathFileUtils extends FileUtilsBase implements FileUtils {
    private ClassLoader refClass;

    /** Constructor */
    public ClasspathFileUtils(ClassLoader refClass) {
        this.refClass = refClass;
    }

    @Override
    public Path getDataFolder() {
        URL folderURL = refClass.getResource(DATA_FOLDER);
        if (folderURL == null) return null;
        //return Paths.get(folderURL.getPath()); // It does NOT work in Windows, since it adds a first slash
        return new File(folderURL.getPath()).toPath();
    }
    @Override
    public Path getConfigFolder() {
        URL folderURL = refClass.getResource(CONFIG_FOLDER);
        if (folderURL == null) return null;
        //return Paths.get(folderURL.getPath()); // It does NOT work in Windows, since it adds a first slash
        return new File(folderURL.getPath()).toPath();
    }
}
