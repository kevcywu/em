package provider.xmlwz;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import provider.MapleData;
import provider.MapleDataDirectoryEntry;
import provider.MapleDataProvider;
import provider.wz.WZDirectoryEntry;
import provider.wz.WZFile;
import provider.wz.WZFileEntry;

public class XMLWZFile implements MapleDataProvider {

    private Logger log = LoggerFactory.getLogger(MapleDataProvider.class);
    private File root;
    private WZDirectoryEntry rootForNavigation;

    public XMLWZFile(File fileIn) {
        root = fileIn;
        rootForNavigation = new WZDirectoryEntry(fileIn.getName(), 0, 0, null);
        fillMapleDataEntitys(root, rootForNavigation);
    }

    private void fillMapleDataEntitys(File lroot, WZDirectoryEntry wzdir) {
        for (File file : lroot.listFiles()) {
            String fileName = file.getName();
            if (file.isDirectory() && !fileName.endsWith(".img")) {
                WZDirectoryEntry newDir = new WZDirectoryEntry(fileName, 0, 0, wzdir);
                wzdir.addDirectory(newDir);
                fillMapleDataEntitys(file, newDir);
            } else if (fileName.endsWith(".xml")) {
                // get the real size here?
                wzdir.addFile(new WZFileEntry(fileName.substring(0, fileName.length() - 4), 0, 0, wzdir));
            }
        }
    }

    @Override
    public MapleData getData(String path) {
        File dataFile = new File(root, path + ".xml");
        File imageDataDir = new File(root, path);
        if (!dataFile.exists()) {
            log.error("Datafile " + path + " does not exist in " + root.getAbsolutePath());
            return null;
//            throw new RuntimeException("Datafile " + path + " does not exist in " + root.getAbsolutePath());
        }
        FileInputStream fis;
        try {
            fis = new FileInputStream(dataFile);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Datafile " + path + " does not exist in " + root.getAbsolutePath());
        }
        final XMLDomMapleData domMapleData;
        try {
            domMapleData = new XMLDomMapleData(fis, imageDataDir.getParentFile());
        } finally {
            try {
                fis.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return domMapleData;
    }

    @Override
    public MapleDataDirectoryEntry getRoot() {
        return rootForNavigation;
    }
}
