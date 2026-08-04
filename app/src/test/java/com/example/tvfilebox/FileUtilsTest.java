package com.example.tvfilebox;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FileUtilsTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void newestUploadIsListedFirst() throws Exception {
        File directory = temporaryFolder.newFolder("uploads");
        File older = new File(directory, "older.apk");
        File newest = new File(directory, "newest.apk");
        assertTrue(older.createNewFile());
        assertTrue(newest.createNewFile());
        assertTrue(older.setLastModified(1000L));
        assertTrue(newest.setLastModified(2000L));

        List<File> files = FileUtils.listUploadsForDirectory(directory);

        assertEquals(2, files.size());
        assertEquals("newest.apk", files.get(0).getName());
        assertEquals("older.apk", files.get(1).getName());
    }
}
