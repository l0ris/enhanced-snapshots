package com.sungardas.enhancedsnapshots.service.upgrade;


import java.nio.file.Path;

public interface SystemUpgrade {

    void upgrade(Path tempFolderName, String initVersion);

}
