package org.mycore.webcli.cli;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class MCRJarTools {
    public static ArrayList<String> getClassesFromJar(String path) throws IOException{
        return getClassesFromJar(new File(path));
    }
    
    public static ArrayList<String> getClassesFromJar(File file) throws IOException{
        return getClassesFromJar(new JarFile(file));
    }
     
    public static ArrayList<String> getClassesFromJar(JarFile jar){
        ArrayList<String> classList = new ArrayList<String>();
        Enumeration<JarEntry> entries = jar.entries();
        
        while (entries.hasMoreElements()) {
            JarEntry jarEntry = (JarEntry) entries.nextElement();
            
            String name = jarEntry.getName();
            
            if(name.endsWith(".class")){
                String className = name.replace("/", ".").replace(".class", "");
                classList.add(className);
            }
        }
        
        return classList;
    }
    
    public static File[] listJarFiles(String path){
        return listJarFiles(new File(path));
    }
    
    public static File[] listJarFiles(File file){
        class JarFileFilter implements FilenameFilter{
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar");
            }
        }
        
        FilenameFilter jarFilter = new JarFileFilter();
        return file.listFiles(jarFilter);
    }
}
