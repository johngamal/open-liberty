/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2011
 *
 * The source code for this program is not published or other-
 * wise divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 */

package com.ibm.ws.ssl.fat.pkcs12;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.webcontainer.security.test.servlets.SSLBasicAuthClient;

import componenttest.annotation.AllowedFFDC;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.vulnerability.LeakedPasswordChecker;

public class NonDefaultPKCS12SSLTest extends CommonSSLTest {
    private static final Class<?> c = NonDefaultPKCS12SSLTest.class;
    private static boolean isOracle6 = false;

    public NonDefaultPKCS12SSLTest() {
        super(LibertyServerFactory.getLibertyServer("com.ibm.ws.ssl.fat.pkcs12.nondefaultpkcs12"));
    }

    @Rule
    public TestRule passwordChecker = new LeakedPasswordChecker(server);

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        if (!isIBMJVM() && isVersion6())
            isOracle6 = true;
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        server.removeInstalledAppForValidation("basicauth");
    }

    /**
     * Validate that with the SSL feature and a non-default SSL config specified in server.xml,
     * where the location specifies a PKCS12 keystore file, and no type is specified, the PKCS12 keystore
     * file is used.
     *
     */
    @Test
    @AllowedFFDC("java.lang.NoClassDefFoundError")
    public void testNonDefaultSSLConfigUsingPKCS12() throws Exception {

        Log.info(c, name.getMethodName(), "Entering " + name.getMethodName());
        String protocol = isOracle6 ? TLS_PROTOCOL : TLSV11_PROTOCOL;

        server.setServerConfigurationFile(NON_DEFAULT_KEYSTORE_LOCATION_USING_PKCS12_NO_TYPE_SPECIFIED);
        server.startServer(name.getMethodName() + ".log");

        // Requires info trace
        server.addInstalledAppForValidation("basicauth");

        assertNotNull("We need to wait for the SSL certificate to be generated at the default path, but we did not receive the message",
                      server.waitForStringInLog("CWPKI0803A:.*" + DEFAULT_GENERATED_KEY_PATH));

        assertNotNull("SSL TCP Channel did not start in time.",
                      server.waitForStringInLog("CWWKO0219I.*ssl"));
        assertNotNull("Need to wait for 'smarter planet' message (server is ready).",
                      server.waitForStringInLog("CWWKF0011I"));

        assertTrue("The expected key file " + DEFAULT_GENERATED_KEY_PATH + " was generated",
                   fileExists(server, DEFAULT_GENERATED_KEY_PATH));

        // Hit the servlet on the SSL port and validate it requires SSL
        SSLBasicAuthClient sslClient = null;
        String response = null;
        try {
            sslClient = createSSLClientWithTrust(DEFAULT_GENERATED_KEY_PATH, DEFAULT_GENERATED_KEY_PASSWORD, protocol);
            response = sslClient.accessUnprotectedServlet(SSLBasicAuthClient.UNPROTECTED_NO_SECURITY_CONSTRAINT);
        } catch (Exception e) {
        }
        if (response != null)
            assertTrue("Did not get the expected response",
                       sslClient.verifyUnauthenticatedResponse(response));
        else
            assertTrue("Did not get the expected response", false);

        // Check to see if the pkcs12 file has NOT been generated

        if (server != null && server.isStarted()) {
            server.stopServer("CWNEN0050W", "SRVE0272W");
        }
        Log.info(c, name.getMethodName(), "Exiting " + name.getMethodName());

    }

    private static boolean isIBMJVM() {
        String vendorName = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty("java.vendor");
            }
        });
        return (vendorName != null && vendorName.toLowerCase().contains("ibm"));
    }

    private static boolean isVersion6() {
        String version = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty("java.version");
            }
        });
        return (version != null && version.startsWith("1.6"));
    }

}
