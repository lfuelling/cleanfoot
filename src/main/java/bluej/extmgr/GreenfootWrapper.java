package bluej.extmgr;

import greenfoot.extension.GreenfootExtension;

import java.io.File;

public class GreenfootWrapper extends ExtensionWrapper {

    public GreenfootWrapper(ExtensionPrefManager prefManager) {
        super(prefManager, null);
    }

    @Override
    protected Class<?> getExtensionClass(File jarFileName) {
        return GreenfootExtension.class; // lel
    }
}
