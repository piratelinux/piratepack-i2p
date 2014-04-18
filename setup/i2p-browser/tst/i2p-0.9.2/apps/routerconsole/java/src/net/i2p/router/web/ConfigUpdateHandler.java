package net.i2p.router.web;

import java.util.HashMap;
import java.util.Map;

import net.i2p.I2PAppContext;
import net.i2p.crypto.TrustedUpdate;
import net.i2p.data.DataHelper;
import net.i2p.util.FileUtil;
import net.i2p.util.PortMapper;

/**
 *
 */
public class ConfigUpdateHandler extends FormHandler {
    private String _newsURL;
    private long _refreshFrequency;
    private String _updateURL;
    private String _updatePolicy;
    private String _proxyHost;
    private String _proxyPort;
    private boolean _updateThroughProxy;
    private String _trustedKeys;
    private boolean _updateUnsigned;
    private String _zipURL;

    public static final String PROP_NEWS_URL = "router.newsURL";
//  public static final String DEFAULT_NEWS_URL = "http://dev.i2p.net/cgi-bin/cvsweb.cgi/i2p/news.xml?rev=HEAD";
    public static final String OLD_DEFAULT_NEWS_URL = "http://complication.i2p/news.xml";
    public static final String DEFAULT_NEWS_URL = "http://echelon.i2p/i2p/news.xml";
    public static final String PROP_REFRESH_FREQUENCY = "router.newsRefreshFrequency";
    public static final long DEFAULT_REFRESH_FREQ = 36*60*60*1000l;
    public static final String DEFAULT_REFRESH_FREQUENCY = Long.toString(DEFAULT_REFRESH_FREQ);
    public static final String PROP_UPDATE_POLICY = "router.updatePolicy";
    public static final String DEFAULT_UPDATE_POLICY = "download";
    public static final String PROP_SHOULD_PROXY = "router.updateThroughProxy";
    public static final String DEFAULT_SHOULD_PROXY = Boolean.TRUE.toString();
    public static final String PROP_PROXY_HOST = "router.updateProxyHost";
    public static final String DEFAULT_PROXY_HOST = "127.0.0.1";
    public static final String PROP_PROXY_PORT = "router.updateProxyPort";
    public static final int DEFAULT_PROXY_PORT_INT = 4444;
    public static final String DEFAULT_PROXY_PORT = "" + DEFAULT_PROXY_PORT_INT;
    /** default false */
    public static final String PROP_UPDATE_UNSIGNED = "router.updateUnsigned";
    /** default false - use for distros */
    public static final String PROP_UPDATE_DISABLED = "router.updateDisabled";
    /** no default */
    public static final String PROP_ZIP_URL = "router.updateUnsignedURL";
    
    public static final String PROP_UPDATE_URL = "router.updateURL";
    /**
     *  Changed as of release 0.8 to support both .sud and .su2
     *  Some JVMs (IcedTea) don't have pack200
     *  Update hosts must maintain both
     */
    private static final String PACK200_URLS =
    "http://echelon.i2p/i2p/i2pupdate.su2\r\n" +
    "http://inr.i2p/i2p/i2pupdate.su2\r\n" +
    "http://stats.i2p/i2p/i2pupdate.su2\r\n" +
    "http://www.i2p2.i2p/_static/i2pupdate.su2\r\n" +
    "http://update.killyourtv.i2p/i2pupdate.su2\r\n" +
    "http://update.postman.i2p/i2pupdate.su2" ;

    private static final String NO_PACK200_URLS =
    "http://echelon.i2p/i2p/i2pupdate.sud\r\n" +
    "http://inr.i2p/i2p/i2pupdate.sud\r\n" +
    "http://stats.i2p/i2p/i2pupdate.sud\r\n" +
    "http://www.i2p2.i2p/_static/i2pupdate.sud\r\n" +
    "http://update.killyourtv.i2p/i2pupdate.sud\r\n" +
    "http://update.postman.i2p/i2pupdate.sud" ;

    public static final String DEFAULT_UPDATE_URL;
    static {
        if (FileUtil.isPack200Supported())
            DEFAULT_UPDATE_URL = PACK200_URLS;
        else
            DEFAULT_UPDATE_URL = NO_PACK200_URLS;
    }

    public static final String PROP_TRUSTED_KEYS = "router.trustedUpdateKeys";
    
    /**
     *  Convenience method for updaters
     *  @return the configured value, else the registered HTTP proxy, else the default
     *  @since 0.8.13
     */
    static int proxyPort(I2PAppContext ctx) {
        return ctx.getProperty(PROP_PROXY_PORT,
                               ctx.portMapper().getPort(PortMapper.SVC_HTTP_PROXY, DEFAULT_PROXY_PORT_INT));
    }

    @Override
    protected void processForm() {
        if (_action == null)
            return;
        if (_action.equals(_("Check for updates"))) {
            NewsFetcher fetcher = NewsFetcher.getInstance(_context);
            fetcher.fetchNews();
            if (fetcher.shouldFetchUnsigned())
                fetcher.fetchUnsignedHead();
            if (fetcher.updateAvailable() || fetcher.unsignedUpdateAvailable()) {
                if ( (_updatePolicy == null) || (!_updatePolicy.equals("notify")) )
                    addFormNotice(_("Update available, attempting to download now"));
                else
                    addFormNotice(_("Update available, click button on left to download"));
                // So that update() will post a status to the summary bar before we reload
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {}
            } else
                addFormNotice(_("No update available"));
            return;
        }

        Map<String, String> changes = new HashMap();

        if ( (_newsURL != null) && (_newsURL.length() > 0) ) {
            String oldURL = ConfigUpdateHelper.getNewsURL(_context);
            if ( (oldURL == null) || (!_newsURL.equals(oldURL)) ) {
                changes.put(PROP_NEWS_URL, _newsURL);
                NewsFetcher.getInstance(_context).invalidateNews();
                addFormNotice(_("Updating news URL to {0}", _newsURL));
            }
        }
        
        if (_proxyHost != null && _proxyHost.length() > 0 && !_proxyHost.equals(_("internal"))) {
            String oldHost = _context.router().getConfigSetting(PROP_PROXY_HOST);
            if ( (oldHost == null) || (!_proxyHost.equals(oldHost)) ) {
                changes.put(PROP_PROXY_HOST, _proxyHost);
                addFormNotice(_("Updating proxy host to {0}", _proxyHost));
            }
        }
        
        if (_proxyPort != null && _proxyPort.length() > 0 && !_proxyPort.equals(_("internal"))) {
            String oldPort = _context.router().getConfigSetting(PROP_PROXY_PORT);
            if ( (oldPort == null) || (!_proxyPort.equals(oldPort)) ) {
                changes.put(PROP_PROXY_PORT, _proxyPort);
                addFormNotice(_("Updating proxy port to {0}", _proxyPort));
            }
        }
        
        changes.put(PROP_SHOULD_PROXY, "" + _updateThroughProxy);
        changes.put(PROP_UPDATE_UNSIGNED, "" + _updateUnsigned);
        
        String oldFreqStr = _context.getProperty(PROP_REFRESH_FREQUENCY, DEFAULT_REFRESH_FREQUENCY);
        long oldFreq = DEFAULT_REFRESH_FREQ;
        try { oldFreq = Long.parseLong(oldFreqStr); } catch (NumberFormatException nfe) {}
        if (_refreshFrequency != oldFreq) {
            changes.put(PROP_REFRESH_FREQUENCY, ""+_refreshFrequency);
            addFormNotice(_("Updating refresh frequency to {0}",
                            _refreshFrequency <= 0 ? _("Never") : DataHelper.formatDuration2(_refreshFrequency)));
        }

        if ( (_updatePolicy != null) && (_updatePolicy.length() > 0) ) {
            String oldPolicy = _context.router().getConfigSetting(PROP_UPDATE_POLICY);
            if ( (oldPolicy == null) || (!_updatePolicy.equals(oldPolicy)) ) {
                changes.put(PROP_UPDATE_POLICY, _updatePolicy);
                addFormNotice(_("Updating update policy to {0}", _updatePolicy));
            }
        }

        if ( (_updateURL != null) && (_updateURL.length() > 0) ) {
            _updateURL = _updateURL.replace("\r\n", ",").replace("\n", ",");
            String oldURL = _context.router().getConfigSetting(PROP_UPDATE_URL);
            if ( (oldURL == null) || (!_updateURL.equals(oldURL)) ) {
                changes.put(PROP_UPDATE_URL, _updateURL);
                addFormNotice(_("Updating update URLs."));
            }
        }

        if ( (_trustedKeys != null) && (_trustedKeys.length() > 0) ) {
            _trustedKeys = _trustedKeys.replace("\r\n", ",").replace("\n", ",");
            String oldKeys = new TrustedUpdate(_context).getTrustedKeysString();
            oldKeys = oldKeys.replace("\r\n", ",");
            if (!_trustedKeys.equals(oldKeys)) {
                // note that keys are not validated here and no console error message will be generated
                changes.put(PROP_TRUSTED_KEYS, _trustedKeys);
                addFormNotice(_("Updating trusted keys."));
            }
        }
        
        if ( (_zipURL != null) && (_zipURL.length() > 0) ) {
            String oldURL = _context.router().getConfigSetting(PROP_ZIP_URL);
            if ( (oldURL == null) || (!_zipURL.equals(oldURL)) ) {
                changes.put(PROP_ZIP_URL, _zipURL);
                addFormNotice(_("Updating unsigned update URL to {0}", _zipURL));
            }
        }
        
        _context.router().saveConfig(changes, null);
    }
    
    public void setNewsURL(String url) { _newsURL = url; }
    public void setRefreshFrequency(String freq) {
        try { _refreshFrequency = Long.parseLong(freq); } catch (NumberFormatException nfe) {}
    }
    public void setUpdateURL(String url) { _updateURL = url; }
    public void setUpdatePolicy(String policy) { _updatePolicy = policy; }
    public void setTrustedKeys(String keys) { _trustedKeys = keys; }
    public void setUpdateThroughProxy(String foo) { _updateThroughProxy = true; }
    public void setProxyHost(String host) { _proxyHost = host; }
    public void setProxyPort(String port) { _proxyPort = port; }
    public void setUpdateUnsigned(String foo) { _updateUnsigned = true; }
    public void setZipURL(String url) { _zipURL = url; }
}
