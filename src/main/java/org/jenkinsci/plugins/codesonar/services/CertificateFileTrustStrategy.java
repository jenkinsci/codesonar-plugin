package org.jenkinsci.plugins.codesonar.services;

import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;

import org.apache.http.conn.ssl.TrustStrategy;

/**
* A trust strategy that accepts certificates that match the local copy of the endpoint's certificate.
*/
public class CertificateFileTrustStrategy implements TrustStrategy {    
    private Collection<? extends Certificate> certsLocalCopy;
    
    CertificateFileTrustStrategy(Collection<? extends Certificate> certsLocalCopy) {
        this.certsLocalCopy = certsLocalCopy;
    }
    
    @Override
    public boolean isTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
        if(chain.length > 0) {
            boolean certsMatch = certsLocalCopy.contains(chain[chain.length - 1]);
            return certsMatch;
        }
        return false;
    }

}
