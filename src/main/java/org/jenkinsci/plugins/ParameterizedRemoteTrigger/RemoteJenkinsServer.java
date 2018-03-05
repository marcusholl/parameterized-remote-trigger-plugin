package org.jenkinsci.plugins.ParameterizedRemoteTrigger;

import static org.apache.commons.lang.StringUtils.trimToEmpty;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.jenkinsci.plugins.ParameterizedRemoteTrigger.auth2.Auth2;
import org.jenkinsci.plugins.ParameterizedRemoteTrigger.auth2.Auth2.Auth2Descriptor;
import org.jenkinsci.plugins.ParameterizedRemoteTrigger.auth2.NoneAuth;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;

/**
 * Holds everything regarding the remote server we wish to connect to, including validations and what not.
 * 
 * @author Maurice W.
 * 
 */
public class RemoteJenkinsServer extends AbstractDescribableImpl<RemoteJenkinsServer> implements Cloneable {

    /**
     * We need to keep this for compatibility - old config deserialization!
     * @deprecated since 2.3.0-SNAPSHOT - use {@link Auth2} instead.
     */
    @CheckForNull
    private List<Auth> auth;

    @CheckForNull
    private String     displayName;
    private boolean    hasBuildTokenRootSupport;
    @CheckForNull
    private Auth2      auth2;
    @CheckForNull
    private String     address;

    @DataBoundConstructor
    public RemoteJenkinsServer() {
        auth = null;
        displayName = null;
        hasBuildTokenRootSupport = false;
        auth2 = new NoneAuth();
        address = null;
    }

    @DataBoundSetter
    public void setDisplayName(String displayName)
    {
        this.displayName = trimToEmpty(displayName);
    }

    @DataBoundSetter
    public void setHasBuildTokenRootSupport(boolean hasBuildTokenRootSupport)
    {
        this.hasBuildTokenRootSupport = hasBuildTokenRootSupport;
    }

    @DataBoundSetter
    public void setAuth2(Auth2 auth2)
    {
        this.auth2 = (auth2 != null) ? auth2 : new NoneAuth();
    }

    @DataBoundSetter
    public void setAddress(String address)
    {
        this.address = address;
    }

    // Getters

    @CheckForNull
    public String getDisplayName() {
        String displayName = null;

        if (this.displayName == null || this.displayName.trim().equals("")) {
            if (address != null) displayName = address;
            else displayName = null;
        } else {
            displayName = this.displayName;
        }
        return displayName;
    }

    public boolean getHasBuildTokenRootSupport() {
        return hasBuildTokenRootSupport;
    }

    @CheckForNull
    public Auth2 getAuth2() {
        migrateAuthToAuth2();
        return auth2;
    }

    /**
     * Migrates old <code>Auth</code> to <code>Auth2</code> if necessary. 
     * @deprecated since 2.3.0-SNAPSHOT - get rid once all users migrated
     */
    private void migrateAuthToAuth2() {
        if(auth2 == null) {
            if(auth == null || auth.size() <= 0) {
                auth2 = new NoneAuth(); 
            } else {
                auth2 = Auth.authToAuth2(auth);
            }
        }
        auth = null;
    }

    @CheckForNull
    public String getAddress() {
        return address;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    /**
     * @return the remote server address
     * @throws RuntimeException
     *             if the address of the remote server was not set
     */
    @Nonnull
    public String getRemoteAddress() {
        if (address == null) {
            throw new RuntimeException("The remote address can not be empty.");
        } else {
            try {
                new URL(address);
            } catch (MalformedURLException e) {
                throw new RuntimeException("Malformed address (" + address + "). Remember to indicate the protocol, i.e. http, https, etc.");
            }
        }
        return address;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<RemoteJenkinsServer> {

        public String getDisplayName() {
            return "";
        }

        /**
         * Validates the given address to see that it's well-formed, and is reachable.
         * 
         * @param address
         *            Remote address to be validated
         * @return FormValidation object
         */
        public FormValidation doCheckAddress(@QueryParameter String address) {

            URL host = null;

            // no empty addresses allowed
            if (address == null || address.trim().equals("")) {
                return FormValidation.warning("The remote address can not be empty, or it must be overridden on the job configuration.");
            }

            // check if we have a valid, well-formed URL
            try {
                host = new URL(address);
                host.toURI();
            } catch (Exception e) {
                return FormValidation.error("Malformed address (" + address + "). Remember to indicate the protocol, i.e. http, https, etc.");
            }

            // check that the host is reachable
            try {
                HttpURLConnection connection = (HttpURLConnection) host.openConnection();
                connection.setConnectTimeout(5000);
                connection.connect();
            } catch (Exception e) {
                return FormValidation.warning("Address looks good, but a connection could not be stablished.");
            }

            return FormValidation.ok();
        }

        public static List<Auth2Descriptor> getAuth2Descriptors() {
            return Auth2.all();
        }

        public static Auth2Descriptor getDefaultAuth2Descriptor() {
            return NoneAuth.DESCRIPTOR;
        }
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        RemoteJenkinsServer clone = new RemoteJenkinsServer();
        clone.setAddress(address);
        clone.setAuth2(auth2);
        clone.setDisplayName(displayName);
        clone.setHasBuildTokenRootSupport(hasBuildTokenRootSupport);
        return clone;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((address == null) ? 0 : address.hashCode());
        result = prime * result + ((auth2 == null) ? 0 : auth2.hashCode());
        result = prime * result + ((displayName == null) ? 0 : displayName.hashCode());
        result = prime * result + (hasBuildTokenRootSupport ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (! (obj instanceof RemoteJenkinsServer))
            return false;
        RemoteJenkinsServer other = (RemoteJenkinsServer) obj;
        if (address == null) {
            if (other.address != null)
                return false;
        } else if (!address.equals(other.address))
            return false;
        if (auth2 == null) {
            if (other.auth2 != null)
                return false;
        } else if (!auth2.equals(other.auth2))
            return false;
        if (displayName == null) {
            if (other.displayName != null)
                return false;
        } else if (!displayName.equals(other.displayName))
            return false;
        if (hasBuildTokenRootSupport != other.hasBuildTokenRootSupport)
            return false;
        return true;
    }
}
