@@ -28,8 +28,6 @@ import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.http.Browser;
@ -47,7 +45,6 @@ import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
@ -59,7 +56,6 @@ public class FileBoomMe extends PluginForHost {
    public FileBoomMe(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://fboom.me/premium.html");
        setConfigElements();
    }

    @Override
@ -71,80 +67,7 @@ public class FileBoomMe extends PluginForHost {
        link.setUrlDownload(link.getDownloadURL().replace("fileboom.me/", "fboom.me/"));
    }

    private static AtomicInteger maxPrem        = new AtomicInteger(1);
    /* User settings */
    private static final String  USE_API        = "USE_API";
    private final static String  SSL_CONNECTION = "SSL_CONNECTION";

    /* api stuff */
    private PluginForHost        k2sPlugin      = null;

    private void pluginLoaded() throws PluginException {
        if (k2sPlugin == null) {
            k2sPlugin = JDUtilities.getPluginForHost("keep2share.cc");
            if (k2sPlugin == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
    }

    /* dl stuff */
    private boolean isFree;
    private boolean resumes;
    private String  directlinkproperty;
    private int     chunks;

    private void setConstants(final Account account) {
        if (account != null) {
            if (account.getBooleanProperty("free", false)) {
                // free account
                chunks = 1;
                resumes = true;
                isFree = true;
                directlinkproperty = "freelink2";
            } else {
                // premium account
                chunks = 0;
                resumes = true;
                isFree = false;
                directlinkproperty = "premlink";
            }
            logger.finer("setConstants = " + account.getUser() + " @ Account Download :: isFree = " + isFree + ", upperChunks = " + chunks + ", Resumes = " + resumes);
        } else {
            // free non account
            chunks = 1;
            resumes = true;
            isFree = true;
            directlinkproperty = "freelink1";
            logger.finer("setConstants = Guest Download :: isFree = " + isFree + ", upperChunks = " + chunks + ", Resumes = " + resumes);
        }
    }

    private boolean apiEnabled() {
        return false;
        // this.getPluginConfig().getBooleanProperty(USE_API, false);
    }

    private void setConfigElements() {
        // this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), USE_API,
        // JDL.L("plugins.hoster.Keep2ShareCc.useAPI",
        // "Use API (recommended!)\r\nIMPORTANT: Free accounts will still be accepted in API mode but they can not be used!")).setDefaultValue(true));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), SSL_CONNECTION, JDL.L("plugins.hoster.Keep2ShareCc.preferSSL", "Use Secure Communication over SSL (HTTPS://)")).setDefaultValue(false));
    }

    public boolean checkLinks(final DownloadLink[] urls) {
        try {
            if (this.apiEnabled()) {
                pluginLoaded();
                final jd.plugins.hoster.Keep2ShareCc.kfpAPI api = ((jd.plugins.hoster.Keep2ShareCc) k2sPlugin).kfpAPI("fileboom.me");
                return api.checkLinks(urls);
            } else {
                return false;
            }
        } catch (final Exception e) {
            return false;
        }
    }
    private static AtomicInteger maxPrem = new AtomicInteger(1);

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
@ -165,27 +88,16 @@ public class FileBoomMe extends PluginForHost {

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        setConstants(null);
        checkShowFreeDialog();
        if (this.apiEnabled()) {
            pluginLoaded();
            final jd.plugins.hoster.Keep2ShareCc.kfpAPI api = ((jd.plugins.hoster.Keep2ShareCc) k2sPlugin).kfpAPI("fileboom.me");
            api.setChunks(chunks);
            api.setResumes(resumes);
            api.setDirectlinkproperty(directlinkproperty);
            api.setDl(dl);
            api.handleDownload(downloadLink, null);
        } else {
            requestFileInformation(downloadLink);
            doFree(downloadLink, null);
        }
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    private final String freeAccConLimit = "Free account does not allow to download more than one file at the same time";
    private final String reCaptcha       = "api\\.recaptcha\\.net|google\\.com/recaptcha/api/";
    private final String formCaptcha     = "/file/captcha\\.html\\?v=[a-z0-9]+";

    public void doFree(final DownloadLink downloadLink, final Account account) throws Exception, PluginException {
    public void doFree(final DownloadLink downloadLink) throws Exception, PluginException {
        checkShowFreeDialog();
        String dllink = checkDirectLink(downloadLink, "directlink");
        dllink = getDllink();
        if (dllink == null) {
@ -277,7 +189,7 @@ public class FileBoomMe extends PluginForHost {
                }
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumes, chunks);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getResponseCode() == 401) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 401", 30 * 60 * 1000l);
        }
@ -382,103 +294,69 @@ public class FileBoomMe extends PluginForHost {
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        if (account.getUser() == null || !account.getUser().contains("@")) {
        /* reset maxPrem workaround on every fetchaccount info */
        maxPrem.set(1);
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            ai.setStatus("Please use E-Mail as login/name!");
            return ai;
            throw e;
        }
        if (this.apiEnabled()) {
            pluginLoaded();
            final jd.plugins.hoster.Keep2ShareCc.kfpAPI api = ((jd.plugins.hoster.Keep2ShareCc) k2sPlugin).kfpAPI("fileboom.me");
            ai = api.fetchAccountInfo(account);
        } else {
            /* reset maxPrem workaround on every fetchaccount info */
        br.getPage("http://fboom.me/site/profile.html");
        ai.setUnlimitedTraffic();
        final String expire = br.getRegex("Premium expires:[\t\n\r ]+<b>([^<>\"]*?)</b>").getMatch(0);
        if (expire == null) {
            try {
                login(account, true);
            } catch (PluginException e) {
                account.setValid(false);
                throw e;
                maxPrem.set(1);
                /* free accounts can still have captcha */
                account.setMaxSimultanDownloads(maxPrem.get());
                account.setConcurrentUsePossible(false);
            } catch (final Throwable e) {
                /* not available in old Stable 0.9.581 */
            }
            br.getPage("http://fboom.me/site/profile.html");
            ai.setUnlimitedTraffic();
            final String expire = br.getRegex("Premium expires:[\t\n\r ]+<b>([^<>\"]*?)</b>").getMatch(0);
            if (expire == null) {
                ai.setStatus("Free Account");
                account.setProperty("nopremium", true);
            } else {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy.MM.dd", Locale.ENGLISH));
                ai.setStatus("Premium Account");
                account.setProperty("nopremium", false);
            }
            final String trafficleft = br.getRegex("Available traffic \\(today\\):[\t\n\r ]+<b><a href=\"/user/statistic\\.html\">([^<>\"]*?)</a>").getMatch(0);
            if (trafficleft != null) {
                ai.setTrafficLeft(SizeFormatter.getSize(trafficleft));
            ai.setStatus("Registered (free) user");
            account.setProperty("nopremium", true);
        } else {
            try {
                maxPrem.set(20);
                account.setMaxSimultanDownloads(maxPrem.get());
                account.setConcurrentUsePossible(true);
            } catch (final Throwable e) {
                /* not available in old Stable 0.9.581 */
            }
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy.MM.dd", Locale.ENGLISH));
            ai.setStatus("Premium user");
            account.setProperty("nopremium", false);
        }
        // api can't set these!
        if (account.getBooleanProperty("free", false)) {
            setFreeAccount(account);
        } else {
            setPremiumAccount(account);
        final String trafficleft = br.getRegex("Available traffic \\(today\\):[\t\n\r ]+<b><a href=\"/user/statistic\\.html\">([^<>\"]*?)</a>").getMatch(0);
        if (trafficleft != null) {
            ai.setTrafficLeft(SizeFormatter.getSize(trafficleft));
        }
        account.setValid(true);
        return ai;
    }

    private void setFreeAccount(Account account) {
        try {
            maxPrem.set(1);
            /* free accounts can still have captcha */
            account.setMaxSimultanDownloads(maxPrem.get());
            account.setConcurrentUsePossible(false);
        } catch (final Throwable e) {
            /* not available in old Stable 0.9.581 */
        }
    }

    private void setPremiumAccount(Account account) {
        try {
            maxPrem.set(20);
            account.setMaxSimultanDownloads(maxPrem.get());
            account.setConcurrentUsePossible(true);
        } catch (final Throwable e) {
            /* not available in old Stable 0.9.581 */
        }
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        setConstants(account);
        if (this.apiEnabled()) {
            pluginLoaded();
            final jd.plugins.hoster.Keep2ShareCc.kfpAPI api = ((jd.plugins.hoster.Keep2ShareCc) k2sPlugin).kfpAPI("fileboom.me");
            api.setChunks(chunks);
            api.setResumes(resumes);
            api.setDirectlinkproperty(directlinkproperty);
            api.setDl(dl);
            api.handleDownload(link, account);
        requestFileInformation(link);
        login(account, false);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        if (account.getBooleanProperty("nopremium", false)) {
            doFree(link);
        } else {
            requestFileInformation(link);
            login(account, false);
            br.setFollowRedirects(false);
            br.getPage(link.getDownloadURL());
            if (account.getBooleanProperty("nopremium", false)) {
                checkShowFreeDialog();
                doFree(link, account);
            } else {
                String dllink = br.getRedirectLocation();
                /* Maybe user has direct downloads disabled */
                if (dllink == null) {
                    dllink = getDllink();
                }
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, Encoding.htmlDecode(dllink), resumes, chunks);
                if (dl.getConnection().getContentType().contains("html")) {
                    logger.warning("The final dllink seems not to be a file!");
                    br.followConnection();
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                dl.startDownload();
            String dllink = br.getRedirectLocation();
            /* Maybe user has direct downloads disabled */
            if (dllink == null) {
                dllink = getDllink();
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, Encoding.htmlDecode(dllink), true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }
