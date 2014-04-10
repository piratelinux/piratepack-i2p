package net.i2p.router.web;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import net.i2p.I2PAppContext;

import org.mortbay.jetty.webapp.Configuration;
import org.mortbay.jetty.webapp.WebAppClassLoader;
import org.mortbay.jetty.webapp.WebAppContext;


/**
 *  Add to the webapp classpath as specified in webapps.config.
 *  This allows us to reference classes that are not in the classpath
 *  specified in wrapper.config, since old installations have
 *  individual jars and not lib/*.jar specified in wrapper.config.
 *
 *  A sample line in webapps.config is:
 *     webapps.appname.classpath=foo.jar,$I2P/lib/bar.jar
 *  Unless $I2P is specified the path will be relative to $I2P/lib for
 *  webapps in the installation and appDir/plugins/appname/lib for plugins.
 *
 *  Sadly, setting Class-Path in MANIFEST.MF doesn't work for jetty wars.
 *  We could look there ourselves, or look for another properties file in the war,
 *  but let's just do it in webapps.config.
 *
 *  No, wac.addClassPath() does not work. For more info see:
 *
 *  http://servlets.com/archive/servlet/ReadMsg?msgId=511113&listName=jetty-support
 *
 *  @since 0.7.12
 *  @author zzz
 */
public class WebAppConfiguration implements Configuration {
    private WebAppContext _wac;

    private static final String CLASSPATH = ".classpath";

    public void setWebAppContext(WebAppContext context) {
       _wac = context;
    }

    public WebAppContext getWebAppContext() {
        return _wac;
    }

    /**
     *  This was the interface in Jetty 5, now it's configureClassLoader()
     */
    private void configureClassPath() throws Exception {
        String ctxPath = _wac.getContextPath();
        //System.err.println("Configure Class Path " + ctxPath);
        if (ctxPath.equals("/"))
            return;
        String appName = ctxPath.substring(1);

        I2PAppContext i2pContext = I2PAppContext.getGlobalContext();
        File libDir = new File(i2pContext.getBaseDir(), "lib");
        // FIXME this only works if war is the same name as the plugin
        File pluginDir = new File(i2pContext.getConfigDir(),
                                        PluginUpdateHandler.PLUGIN_DIR + ctxPath);

        File dir = libDir;
        String cp;
        if (pluginDir.exists()) {
            File consoleDir = new File(pluginDir, "console");
            Properties props = RouterConsoleRunner.webAppProperties(consoleDir.getAbsolutePath());
            cp = props.getProperty(RouterConsoleRunner.PREFIX + appName + CLASSPATH);
            dir = pluginDir;
        } else {
            Properties props = RouterConsoleRunner.webAppProperties();
            cp = props.getProperty(RouterConsoleRunner.PREFIX + appName + CLASSPATH);
        }
        if (cp == null)
            return;
        StringTokenizer tok = new StringTokenizer(cp, " ,");
        StringBuilder buf = new StringBuilder();
        Set<URL> systemCP = getSystemClassPath();
        while (tok.hasMoreTokens()) {
            if (buf.length() > 0)
                buf.append(',');
            String elem = tok.nextToken().trim();
            String path;
            if (elem.startsWith("$I2P"))
                path = i2pContext.getBaseDir().getAbsolutePath() + elem.substring(4);
            else if (elem.startsWith("$PLUGIN"))
                path = dir.getAbsolutePath() + elem.substring(7);
            else
                path = dir.getAbsolutePath() + '/' + elem;
            // As of Jetty 6, we can't add dups to the class path, or
            // else it screws up statics
            // This is not a complete solution because the Windows no-wrapper classpath is set
            // by the launchi2p.jar (i2p.exe) manifest and is not detected below.
            // TODO: Add a classpath to the command line in i2pstandalone.xml?
            File jfile = new File(path);
            File jdir = jfile.getParentFile();
            if (systemCP.contains(jfile.toURI().toURL()) ||
                (jdir != null && systemCP.contains(jdir.toURI().toURL()))) {
                //System.err.println("Not adding " + path + " to classpath for " + appName + ", already in system classpath");
                continue;
            }
            System.err.println("Adding " + path + " to classpath for " + appName);
            buf.append(path);
        }
        if (buf.length() <= 0)
            return;
        ClassLoader cl = _wac.getClassLoader();
        if (cl != null && cl instanceof WebAppClassLoader) {
            WebAppClassLoader wacl = (WebAppClassLoader) cl;
            wacl.addClassPath(buf.toString());
        } else {
            // This was not working because the WebAppClassLoader already exists
            // and it calls getExtraClasspath in its constructor
            // Not sure why WACL already exists...
            _wac.setExtraClasspath(buf.toString());
        }
    }

    /** @since 0.9 */
    private static Set<URL> getSystemClassPath() {
        URLClassLoader urlClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        URL urls[] = urlClassLoader.getURLs();
        Set<URL> rv = new HashSet(32);
        for (int i = 0; i < urls.length; i++) {
            rv.add(urls[i]);
        }
        return rv;
    }

    public void configureDefaults() {}
    public void configureWebApp() {}

    /** @since Jetty 6 */
    public void deconfigureWebApp() {}

    /** @since Jetty 6 */
    public void configureClassLoader() throws Exception {
        configureClassPath();
    }
}
