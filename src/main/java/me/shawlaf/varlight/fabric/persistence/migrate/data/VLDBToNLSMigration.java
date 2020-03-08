package me.shawlaf.varlight.fabric.persistence.migrate.data;

import me.shawlaf.varlight.persistence.BasicCustomLightSource;
import me.shawlaf.varlight.persistence.migrate.Migration;
import me.shawlaf.varlight.persistence.nls.NLSFile;
import me.shawlaf.varlight.persistence.vldb.VLDBInputStream;
import me.shawlaf.varlight.persistence.vldb.VLDBUtil;
import me.shawlaf.varlight.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class VLDBToNLSMigration implements Migration<File> {

    @Override
    public boolean migrate(File file) throws IOException {
        boolean isVLDBOld = file.getName().toLowerCase().endsWith(".vldb");
        boolean isVLDBNew = file.getName().toLowerCase().endsWith(".vldb2");

        boolean isVLDB = isVLDBNew || isVLDBOld;

        if (!isVLDB) {
            return false;
        }

        try (InputStream is = FileUtil.openStreamInflate(file)) {
            VLDBInputStream in = new VLDBInputStream(is);

            if (isVLDBNew) {
                if (!in.readVLDBMagic()) {
                    throw new RuntimeException("Malformed VLDB File " + file.getAbsolutePath());
                }
            }

            int regionX = in.readInt32();
            int regionZ = in.readInt32();

            NLSFile nlsFile = NLSFile.newFile(new File(file.getParentFile().getAbsoluteFile(), String.format(NLSFile.FILE_NAME_FORMAT, regionX, regionZ)), regionX, regionZ);

            int amountChunks = in.readInt16();

            in.skip(amountChunks * VLDBUtil.SIZEOF_OFFSET_TABLE_ENTRY);

            for (int i = 0; i < amountChunks; ++i) {
                for (BasicCustomLightSource bcls : in.readChunk(regionX, regionZ, BasicCustomLightSource[]::new, BasicCustomLightSource::new).item2) {
                    nlsFile.setCustomLuminance(bcls.getPosition(), bcls.getCustomLuminance());
                }
            }

            nlsFile.saveAndUnload();
            in.close();
        }

        if (!file.delete()) {
            throw new IOException("Failed to delete File " + file.getAbsolutePath());
        }

        return true;
    }
}
