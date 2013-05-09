package org.jdownloader.controlling.contextmenu;

import java.util.HashSet;

import org.appwork.storage.Storable;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.extensions.ExtensionNotLoadedException;

public class ActionData implements Storable {
    private HashSet<MenuItemProperty> properties;
    private Class<?>                  clazz;
    private static final String       PACKAGE_NAME = AbstractExtension.class.getPackage().getName();

    public Class<?> _getClazz() throws ClassNotFoundException, ExtensionNotLoadedException {

        if (clazz == null) {

            if (getClazzName().startsWith(PACKAGE_NAME)) {

                clazz = ExtensionController.getInstance().loadClass(getClazzName());

            } else {
                clazz = Class.forName(getClazzName());
            }

        } else if (getClazzName().startsWith(PACKAGE_NAME)) {
            clazz = ExtensionController.getInstance().loadClass(getClazzName());
        }
        return clazz;
    }

    private String clazzName;
    private String name;
    private String iconKey;

    public ActionData(/* Storable */) {

    }

    public ActionData(Class<?> class1, MenuItemProperty... ps) {

        this.clazz = class1;
        this.clazzName = class1.getName();
        properties = new HashSet<MenuItemProperty>();
        for (MenuItemProperty ap : ps) {
            properties.add(ap);
        }
    }

    public String getClazzName() {
        return clazzName;
    }

    public void setClazzName(String clazzName) {
        this.clazzName = clazzName;
    }

    public HashSet<MenuItemProperty> getProperties() {
        return properties;
    }

    public void setProperties(HashSet<MenuItemProperty> properties) {
        this.properties = properties;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getIconKey() {
        return iconKey;
    }

    public void setIconKey(String iconKey) {
        this.iconKey = iconKey;
    }
}