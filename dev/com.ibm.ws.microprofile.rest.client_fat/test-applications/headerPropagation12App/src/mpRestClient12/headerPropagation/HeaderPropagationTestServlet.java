/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package mpRestClient12.headerPropagation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.junit.Test;

import componenttest.annotation.SkipForRepeat;
import componenttest.app.FATServlet;
import componenttest.rules.repeater.MicroProfileActions;

@SuppressWarnings("serial")
@ApplicationScoped
@WebServlet(urlPatterns = "/HeaderPropagationTestServlet")
public class HeaderPropagationTestServlet extends FATServlet {
    Logger LOG = Logger.getLogger(HeaderPropagationTestServlet.class.getName());

    private String httpUrl;
    private String httpsUrl;

    private static String getSysProp(String key) {
        return AccessController.doPrivileged((PrivilegedAction<String>) () -> System.getProperty(key));
    }

    @Override
    public void init() throws ServletException {
        httpUrl = "http://localhost:" + getSysProp("bvt.prop.HTTP_default") + 
                        "/headerPropagation12App/resource";
        LOG.info("httpUrl = " + httpUrl);
        httpsUrl = "https://localhost:" + getSysProp("bvt.prop.HTTP_default.secure") + 
                        "/headerPropagation12App/resource";
        LOG.info("httpsUrl = " + httpsUrl);

    }

    @Test
    public void testPropagateCustomHeader(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        String allHeaders = ClientBuilder.newClient()
                        .target(httpUrl)
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .accept(MediaType.TEXT_PLAIN_TYPE)
                        .header("MyCustomHeader", "expectedValue")
                        .get(String.class);
        LOG.log(Level.INFO, "allHeaders {0}", allHeaders);
        assertTrue("Custom header \"MyCustomHeader\" not propagated to MP Rest Client",
                   allHeaders.contains("MyCustomHeader=expectedValue"));
    }

    @Test
    public void testPropagateAuthorizationHeader(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        String authHeaderValue = createBasicAuthHeaderValue("user1", "pass1");
        String allHeaders = ClientBuilder.newClient()
                        .property("com.ibm.ws.jaxrs.client.ssl.config", "defaultSSLConfig")
                        .target(httpsUrl)
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .accept(MediaType.TEXT_PLAIN_TYPE)
                        .header(HttpHeaders.AUTHORIZATION, authHeaderValue)
                        .get(String.class);
        assertTrue("Authorization header not propagated to MP Rest Client",
                   allHeaders.contains("Authorization=" + authHeaderValue));
        assertTrue("Authorization header was propagated, but user is not in role",
                   allHeaders.contains("role=role1"));
    }

    @Test
    public void testPropagateCustomHeaderViaAnnotations(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        String allHeaders = ClientBuilder.newClient()
                        .target(httpUrl)
                        .path("/clientHeaderParam")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .accept(MediaType.TEXT_PLAIN_TYPE)
                        .get(String.class);
        LOG.log(Level.INFO, "allHeaders {0}", allHeaders);
        assertTrue("Interface level @ClientHeaderParam not sent",
                   allHeaders.contains("InterfaceHeader=abc"));
        assertTrue("Method level @ClientHeaderParam not sent",
                   allHeaders.contains("MethodHeader=def"));
    }

    @Test
    @SkipForRepeat({MicroProfileActions.MP50_ID, MicroProfileActions.MP60_ID}) // @Context injection not supported in ClientHeaderFactory instances in RESTEasy
    public void testSendCustomHeaderViaFactory(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        String allHeaders = ClientBuilder.newClient()
                        .target(httpUrl)
                        .path("/clientHeadersFactory")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .accept(MediaType.TEXT_PLAIN_TYPE)
                        .get(String.class);
        LOG.log(Level.INFO, "allHeaders {0}", allHeaders);
        assertTrue("Header from CustomClientHeadersFactory not sent",
                   allHeaders.contains("HEADER_FROM_CUSTOM_CLIENTHEADERSFACTORY=123"));
        assertTrue("@Context injection failed in custom ClientHeadersFactory",
                   allHeaders.contains("INJECTED_URI_INFO="));
        assertFalse("@Inject injection occurred in non-CDI managed ClientHeadersFactory",
                   allHeaders.contains("INJECTED_FOO=bar"));
    }

    @Test
    @SkipForRepeat({MicroProfileActions.MP50_ID, MicroProfileActions.MP60_ID}) // @Context injection not supported in ClientHeaderFactory instances in RESTEasy
    public void testSendCustomHeaderViaCDIEnabledFactory(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        String allHeaders = ClientBuilder.newClient()
                        .target(httpUrl)
                        .path("/cdiClientHeadersFactory")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .accept(MediaType.TEXT_PLAIN_TYPE)
                        .get(String.class);
        LOG.log(Level.INFO, "allHeaders {0}", allHeaders);
        assertTrue("Header from CustomClientHeadersFactory not sent",
                   allHeaders.contains("HEADER_FROM_CUSTOM_CLIENTHEADERSFACTORY=456"));
        assertTrue("@Context injection failed in custom ClientHeadersFactory",
                   allHeaders.contains("INJECTED_URI_INFO="));
        assertTrue("@Inject injection failed in CDI-managed custom ClientHeadersFactory",
                   allHeaders.contains("INJECTED_FOO=bar"));
    }

    private String createBasicAuthHeaderValue(String username, String password) throws UnsupportedEncodingException {
        return "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
    }
}