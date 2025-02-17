/*******************************************************************************
 * Copyright (c) 2013, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.clients.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.http.HttpException;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.google.gson.JsonObject;
import com.ibm.ws.common.encoder.Base64Coder;
import com.ibm.ws.security.common.crypto.HashUtils;
import com.ibm.ws.security.common.structures.Cache;
import com.ibm.ws.security.openidconnect.common.Constants;
import com.ibm.ws.security.openidconnect.token.IDToken;
import com.ibm.ws.security.openidconnect.token.IDTokenValidationFailedException;
import com.ibm.ws.security.openidconnect.token.JWSHeader;
import com.ibm.ws.security.openidconnect.token.Payload;
import com.ibm.ws.security.openidconnect.token.impl.IdTokenImpl;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;
import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;
import com.ibm.ws.webcontainer.security.SSOCookieHelperImpl;
import com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.ssl.SSLSupport;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;

import io.openliberty.security.oidcclientcore.storage.OidcStorageUtils;
import test.common.SharedOutputManager;

public class OIDCClientAuthenticatorUtilTest {

    protected static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Rule
    public TestName testName = new TestName();

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    //private static final String TEST_TOKEN_STRING = "eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwOi8vaGFybW9uaWM6ODAxMSIsImlhdCI6MTM4OTk3NjkzMCwic3ViIjoidGVzdHVzZXIiLCJleHAiOjEzODk5ODA1MzAsImF1ZCI6ImNsaWVudDAxIn0.79VNXveAqipBzK5wB-cWTCJN7gXzECgj60w5K4GfZe4";
    // need one signed with new secret for jose4j
    private static final String TEST_TOKEN_STRING = "eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwOi8vaGFybW9uaWM6ODAxMSIsImlhdCI6MTM4OTk3NjkzMCwic3ViIjoidGVzdHVzZXIiLCJleHAiOjEzODk5ODA1MzAsImF1ZCI6ImNsaWVudDAxIn0.vPZ0Hg5HY7-lDhBZTOkuVBE5GrV5uSphdyi1AZgbEbI";
    private static final String TEST_ACCESS_TOKEN = "w3sYFV4Xrp4JzzfYdLKuQM0aVIVY6na8qzcGmEdg";
    private static final String TEST_COOKIE_VALUE = "rO0ABXNyABNqYXZhLnV0aWwuSGFzaHRhYmxlE7sPJSFK5LgDAAJGAApsb2FkRmFjdG9ySQAJdGhyZXNob2xkeHA/QAAAAAAACHcIAAAACwAAAAJ0AARjb2RldAAeS1FISUhFOEZVWkFZWWV2THBCWTdVbnFZVE8zbEZ3dAAFc3RhdGV0ABQzNFcybmIwTFlUREw5RnJ5dmgzWHg=";
    private static final long TEST_CLOCK_SKEW_IN_SECONDS = 300L;
    private static final String TEST_ORIGINAL_STATE = "orignalStateThatIsAtLeastAsLongAsRequired";
    private static final String ANOTHER_ORIGINAL_STATE = "34W2nb0LYTDL9Fryvh3X";
    private static final String TEST_URL = "http://harmonic.austin.ibm.com:8010/formlogin/SimpleServlet";
    private static final String TEST_REDIRECT_URL = "https://my.rp.client.com:8010/redirect/client";
    private static final String TEST_AUTHORIZATION_ENDPOINT = "https://op.ibm.com:8020/oidc/endpoint";
    private static final String TEST_TOKEN_ENDPOINT = "http://harmonic:8011/oidc/endpoint/OidcConfigSample/token";
    private static final String TEST_GRANT_TYPE = "openid profile";
    private static final String CLIENTID = "clientid";
    private static final String CLIENT01 = "client01";
    // private static final String SHARED_KEY = "secret";  // conversion from net.oauth to jose4j requires a longer key
    private static final String SHARED_KEY = "secretsecretsecretsecretsecretsecret";
    private static final String TEST_ACR_VALUES = "urn:mace:incommon:iap:silver urn:mace:incommon:iap:bronze";

    private final OidcClientConfig clientConfig = mock.mock(OidcClientConfig.class, "clientConfig");
    @SuppressWarnings("unchecked")
    private final AtomicServiceReference<SSLSupport> sslSupportRef = mock.mock(AtomicServiceReference.class, "sslSupportRef");
    private final SSLSupport sslSupport = mock.mock(SSLSupport.class, "sslSupport");
    private final IExtendedRequest req = mock.mock(IExtendedRequest.class, "req");
    private final HttpSession session = mock.mock(HttpSession.class, "session");
    private final HttpServletResponse res = mock.mock(HttpServletResponse.class, "res");
    private final ReferrerURLCookieHandler referrerURLCookieHandler = mock.mock(ReferrerURLCookieHandler.class, "referrerURLCookieHandler");
    private final Cookie cookie1 = mock.mock(Cookie.class, "cookie1");
    private final Cookie cookie2 = mock.mock(Cookie.class, "cookie2");
    private final OidcClientUtil oidcClientUtil = mock.mock(OidcClientUtil.class, "oidcClientUtil");
    private final IDToken idToken = mock.mock(IDToken.class, "idToken");
    private final Payload payload = mock.mock(Payload.class, "payload");
    private final WebAppSecurityConfig webAppSecConfig = mock.mock(WebAppSecurityConfig.class);
    private final PrintWriter pw = mock.mock(PrintWriter.class, "pw");
    private final MockOidcClientRequest oidcClientRequest = mock.mock(MockOidcClientRequest.class, "oidcClientRequest");
    private final ConvergedClientConfig convClientConfig = mock.mock(ConvergedClientConfig.class, "convClientConfig");

    private final JWSHeader jwsHeader = new JWSHeader();
    final String STATE = "state";

    private final HashMap<String, String> tokens = new HashMap<String, String>(100);
    private final OIDCClientAuthenticatorUtil oidcCAUtil = new OIDCClientAuthenticatorUtil();

    private final Cookie cookie = new Cookie(ClientConstants.WAS_OIDC_CODE, STATE);
    private final Cookie[] cookies = new Cookie[] { cookie };

    String idTokenContent = "aaaaa.bbbb.ccccc";
    String id_token = "id_token";
    String[] idTokens = new String[] { idTokenContent };
    Cookie reqParameterCookie = null;
    final String clientSecret = "secret";
    String encodedReqParams = null;

    @Before
    public void setUp() {
        WebAppSecurityCollaboratorImpl.setGlobalWebAppSecurityConfig(webAppSecConfig);

        mock.checking(new Expectations() {
            {
                allowing(webAppSecConfig).getSSORequiresSSL();
                will(returnValue(true));
                allowing(webAppSecConfig).getHttpOnlyCookies();
                will(returnValue(true));
                allowing(webAppSecConfig).getSSODomainList();
                will(returnValue(null));
                allowing(webAppSecConfig).getSSOUseDomainFromURL();
                will(returnValue(false));
                allowing(webAppSecConfig).getSameSiteCookie();
                will(returnValue("Lax"));
                allowing(webAppSecConfig).createSSOCookieHelper();
                will(returnValue(new SSOCookieHelperImpl(webAppSecConfig)));
                allowing(webAppSecConfig).createReferrerURLCookieHandler();
                will(returnValue(referrerURLCookieHandler));

                allowing(req).getAttribute("com.ibm.wsspi.security.oidc.client.request");
                will(returnValue(oidcClientRequest));

                allowing(res).addCookie(cookie2);

                allowing(oidcClientRequest).getAndSetCustomCacheKeyValue();
                will(returnValue("ThisIsCustomCacheKeyvalue"));

                allowing(sslSupportRef).getService();
                will(returnValue(sslSupport));

                allowing(referrerURLCookieHandler).createCookie(with(any(String.class)), with(any(String.class)), with(any(HttpServletRequest.class)));
                will(returnValue(cookie2));
            }
        });

        createConstructorExpectations(convClientConfig);
        tokens.clear();
    }

    @After
    public void tearDown() {
        mock.assertIsSatisfied();
        outputMgr.resetStreams();
    }

    private void createReqCookie() {
        // set up request parameter cookie
        Map<String, String[]> map = new HashMap<String, String[]>();
        map.put("access_token", new String[] { "access_token_content" });
        map.put(id_token, idTokens);
        map.put("refresh_token", new String[] { "refresh_token_content" });
        map.put("code", new String[] { "YMKexUVcHci2dhDJzNRHW2w9rhf70u" });
        map.put("state", new String[] { "001534964952438QID21LdnF" });

        JsonObject jsonObject = new JsonObject();
        Set<Map.Entry<String, String[]>> entries = map.entrySet();
        for (Map.Entry<String, String[]> entry : entries) {
            String key = entry.getKey();
            String[] strs = entry.getValue();
            if (strs != null && strs.length > 0) {
                jsonObject.addProperty(key, strs[0]);
            }
        }
        String requestParameters = jsonObject.toString();

        String localEncoded = null;
        try {
            localEncoded = Base64Coder.toString(Base64Coder.base64Encode(requestParameters.getBytes(ClientConstants.CHARSET)));
        } catch (UnsupportedEncodingException e) {
            //This should not happen, we are using UTF-8
        }

        // digest with the client_secret value
        String tmpStr = new String(localEncoded);
        tmpStr = tmpStr.concat("_").concat(clientSecret);

        encodedReqParams = new String(localEncoded).concat("_").concat(HashUtils.digest(tmpStr));
        reqParameterCookie = new Cookie(ClientConstants.WAS_OIDC_CODE, encodedReqParams);
    }

    @Test
    public void testValidateReqParameters() {

        createReqCookie();
        mock.checking(new Expectations() {
            {
                allowing(convClientConfig).getClientSecret();
                will(returnValue(clientSecret));
                allowing(convClientConfig).getClientId();
                will(returnValue(CLIENTID));
            }
        });

        Hashtable<String, String> reqParameters = new Hashtable<String, String>();
        boolean validParameter = oidcCAUtil.validateReqParameters(convClientConfig, reqParameters, encodedReqParams);
        assertTrue("the request parameter is supposed to be correct but not", validParameter);
        String newIdTokenContent = reqParameters.get(id_token);
        assertTrue("the value of id_token is not " + idTokenContent + " but " + newIdTokenContent, idTokenContent.equals(newIdTokenContent));
        String newAccessTokenContent = reqParameters.get("access_token");
        assertTrue("The access token content is not \"access_token_content\"", "access_token_content".equals(newAccessTokenContent));
        String newRefreshTokenContent = reqParameters.get("refresh_token");
        assertTrue("The access token content is not \"refresh_token_content\"", "refresh_token_content".equals(newRefreshTokenContent));
    }

    @Test
    public void testGetIssuerIdentifier() {
        try {
            final String issuer = "https://localhost:8011/oidc/endpoint/OidcConfigSample";
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getIssuerIdentifier();
                    will(returnValue(issuer));
                }
            });
            String issuerResult = OIDCClientAuthenticatorUtil.getIssuerIdentifier(convClientConfig);
            assertEquals("Issuer result is not the one expected!", issuer, issuerResult);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testGetIssuerIdentifier_issuerNotConfigured_tokenEndpointNotConfigured() {
        try {
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getIssuerIdentifier();
                    will(returnValue(null));
                    one(convClientConfig).getTokenEndpointUrl();
                    will(returnValue(null));
                }
            });
            String issuerResult = OIDCClientAuthenticatorUtil.getIssuerIdentifier(convClientConfig);
            assertNull("Issuer was expected to be null but was [" + issuerResult + "].", issuerResult);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testGetIssuerIdentifier_issuerNotConfigured_tokenEndpointEmpty() {
        try {
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getIssuerIdentifier();
                    will(returnValue(null));
                    one(convClientConfig).getTokenEndpointUrl();
                    will(returnValue(""));
                }
            });
            String issuerResult = OIDCClientAuthenticatorUtil.getIssuerIdentifier(convClientConfig);
            assertEquals("Issuer was expected to be an empty string but was not.", "", issuerResult);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testGetIssuerIdentifier_issuerNotConfigured_tokenEndpointSimpleString() {
        final String endpointValue = "some simple string that's not a URL";
        try {
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getIssuerIdentifier();
                    will(returnValue(null));
                    one(convClientConfig).getTokenEndpointUrl();
                    will(returnValue(endpointValue));
                }
            });
            String issuerResult = OIDCClientAuthenticatorUtil.getIssuerIdentifier(convClientConfig);
            assertEquals("Issuer did not match expected value.", endpointValue, issuerResult);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testGetIssuerIdentifier_issuerNotConfigured_tokenEndpointOnlySlash() {
        final String endpointValue = "/";
        try {
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getIssuerIdentifier();
                    will(returnValue(null));
                    one(convClientConfig).getTokenEndpointUrl();
                    will(returnValue(endpointValue));
                }
            });
            String issuerResult = OIDCClientAuthenticatorUtil.getIssuerIdentifier(convClientConfig);
            assertEquals("Issuer was expected to be an empty string but was not.", "", issuerResult);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testGetIssuerIdentifier_issuerNotConfigured_tokenEndpointBeginningSlash() {
        final String endpointValue = "";
        try {
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getIssuerIdentifier();
                    will(returnValue(null));
                    one(convClientConfig).getTokenEndpointUrl();
                    will(returnValue(endpointValue + "/after"));
                }
            });
            String issuerResult = OIDCClientAuthenticatorUtil.getIssuerIdentifier(convClientConfig);
            assertEquals("Issuer did not match expected value.", endpointValue, issuerResult);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testGetIssuerIdentifier_issuerNotConfigured_tokenEndpointSimpleStringWithSlashes() {
        final String endpointValue = "before/middle";
        try {
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getIssuerIdentifier();
                    will(returnValue(null));
                    one(convClientConfig).getTokenEndpointUrl();
                    will(returnValue(endpointValue + "/after"));
                }
            });
            String issuerResult = OIDCClientAuthenticatorUtil.getIssuerIdentifier(convClientConfig);
            assertEquals("Issuer did not match expected value.", endpointValue, issuerResult);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testGetIssuerIdentifier_issuerNotConfigured_tokenEndpointSimpleStringTrailingSlash() {
        final String endpointValue = "before/middle/after";
        try {
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getIssuerIdentifier();
                    will(returnValue(null));
                    one(convClientConfig).getTokenEndpointUrl();
                    will(returnValue(endpointValue + "/"));
                }
            });
            String issuerResult = OIDCClientAuthenticatorUtil.getIssuerIdentifier(convClientConfig);
            assertEquals("Issuer did not match expected value.", endpointValue, issuerResult);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testGetIssuerIdentifier_issuerNotConfigured_tokenEndpointSchemeOnly() {
        try {
            final String tokenEndpointUrl = "http://";
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getIssuerIdentifier();
                    will(returnValue(null));
                    one(convClientConfig).getTokenEndpointUrl();
                    will(returnValue(tokenEndpointUrl));
                }
            });
            String issuerResult = OIDCClientAuthenticatorUtil.getIssuerIdentifier(convClientConfig);
            assertEquals("Issuer did not match expected value.", tokenEndpointUrl, issuerResult);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testGetIssuerIdentifier_issuerNotConfigured_tokenEndpointNoContext() {
        try {
            final String tokenEndpointUrl = "http://localhost";
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getIssuerIdentifier();
                    will(returnValue(null));
                    one(convClientConfig).getTokenEndpointUrl();
                    will(returnValue(tokenEndpointUrl));
                }
            });
            String issuerResult = OIDCClientAuthenticatorUtil.getIssuerIdentifier(convClientConfig);
            assertEquals("Issuer did not match expected value.", tokenEndpointUrl, issuerResult);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    //0509 @Test
    public void testAuthenticate_redirectToServer() {

        try {
            mock.checking(new Expectations() {
                {
                    allowing(convClientConfig).getGrantType();
                    will(returnValue("authorization_code"));
                }
            });
            createCommonAuthenticateExpectations(null);
            createHandleRedirectToServerExpectations(TEST_AUTHORIZATION_ENDPOINT, null);

            mock.checking(new Expectations() {
                {
                    allowing(convClientConfig).getPrompt();
                    will(returnValue(null));
                }
            });

            ProviderAuthenticationResult result = oidcCAUtil.authenticate(req, res, convClientConfig);

            assertRedirectResult(result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    private void createGetAuthzCodeAndSateFromCookieExpectations() {
        createReqCookieExpectation(TEST_COOKIE_VALUE.getBytes());
        createReferrerUrlCookieExpectations(ClientConstants.WAS_OIDC_CODE);
    }

    private void createHandleTokensExpectations(String cookieState, String currentState, boolean isStateMismatch, final String idToken, final String accessToken) {
        mock.checking(new Expectations() {
            {
                one(clientConfig).getClientId();
                will(returnValue(CLIENT01));
            }
        });
        createVerifyResponseStateExpectations(cookieState, currentState);

        if (isStateMismatch) {
            // Runtime errors out and returns 401
            createPostParameterHelperExpectations();
            createReferrerUrlCookiesExpectations();
        } else {
            final Enumeration<String> enumParams = Collections.enumeration(Arrays.asList(Constants.ID_TOKEN, Constants.ACCESS_TOKEN, Constants.REFRESH_TOKEN));
            mock.checking(new Expectations() {
                {
                    one(req).getParameterNames();
                    will(returnValue(enumParams));
                    one(req).getParameterValues(Constants.ID_TOKEN);
                    will(returnValue(new String[] { idToken }));
                    one(req).getParameterValues(Constants.ACCESS_TOKEN);
                    will(returnValue(new String[] { accessToken }));
                    one(req).getParameterValues(Constants.REFRESH_TOKEN);
                    will(returnValue(new String[0]));
                }
            });
            createCreateResultExpectations(idToken);
        }
    }

    private void createVerifyResponseStateExpectations(final String cookieState, final String currentState) {
        final Cookie[] cookies = new Cookie[] { cookie1 };
        mock.checking(new Expectations() {
            {
                one(req).getCookies();
                will(returnValue(cookies));
                one(cookie1).getName();
                will(returnValue(OidcStorageUtils.getStateStorageKey(cookieState)));
                one(cookie1).getValue();
                will(returnValue(currentState));
            }
        });
        createReferrerUrlCookieExpectations(OidcStorageUtils.getStateStorageKey(cookieState));
    }

    private void createCreateResultExpectations(String idToken) {
        mock.checking(new Expectations() {
            {
                one(clientConfig).getClientId();
                will(returnValue(CLIENT01));
            }
        });
        if (idToken == null) {
            // Runtime errors out and returns 401
            mock.checking(new Expectations() {
                {
                    one(clientConfig).getTokenEndpointUrl();
                    will(returnValue(TEST_TOKEN_ENDPOINT));
                }
            });
        } else {
            try {
                createResultExpectations();
            } catch (Exception e) {
                e.printStackTrace();
                fail("Unexpected exception creating the result expectations: " + e);
            }
        }
        createPostParameterHelperExpectations();
        createReferrerUrlCookiesExpectations();
    }

    //0509 @Test
    public void testAuthenticate_implicit() {
        try {
            WebAppSecurityCollaboratorImpl.setGlobalWebAppSecurityConfig(webAppSecConfig);
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getGrantType();
                    will(returnValue(Constants.IMPLICIT));
                }
            });
            createGetAuthzCodeAndSateFromCookieExpectations();
            createHandleTokensExpectations(ANOTHER_ORIGINAL_STATE, ANOTHER_ORIGINAL_STATE, false, TEST_TOKEN_STRING, TEST_ACCESS_TOKEN);

            final String myStateKey = ANOTHER_ORIGINAL_STATE;
            final String originalState = ANOTHER_ORIGINAL_STATE;
            Cache requestStates = new Cache(0, 0);
            requestStates.put(myStateKey, originalState);

            ProviderAuthenticationResult result = oidcCAUtil.authenticate(req, res, convClientConfig);
            assertEquals("The authentication result status must be SUCCESS.", AuthResult.SUCCESS, result.getStatus());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    //0509 @Test
    public void testAuthenticate_implicit_stateMismatch() {
        try {
            WebAppSecurityCollaboratorImpl.setGlobalWebAppSecurityConfig(webAppSecConfig);
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getGrantType();
                    will(returnValue(Constants.IMPLICIT));
                }
            });
            createGetAuthzCodeAndSateFromCookieExpectations();
            createHandleTokensExpectations(ANOTHER_ORIGINAL_STATE, ANOTHER_ORIGINAL_STATE, true, TEST_TOKEN_STRING, TEST_ACCESS_TOKEN);

            final String myStateKey = ANOTHER_ORIGINAL_STATE;
            final String originalState = TEST_ORIGINAL_STATE;
            Cache requestStates = new Cache(0, 0);
            requestStates.put(myStateKey, originalState);

            ProviderAuthenticationResult result = oidcCAUtil.authenticate(req, res, convClientConfig);
            assertEquals("The authentication result status must be SEND_401.", AuthResult.SEND_401, result.getStatus());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testAuthenticate_missingAuthEndpoint() {
        try {
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getAuthorizationEndpointUrl();
                    will(returnValue(null));
                    one(convClientConfig).getClientId();
                    will(returnValue(CLIENTID));
                }
            });

            ProviderAuthenticationResult result = oidcCAUtil.authenticate(req, res, convClientConfig);
            assertEquals("The authentication result status must be SEND_401.", AuthResult.SEND_401, result.getStatus());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    private void createCommonAuthenticateExpectations(final byte[] oidcCodeCookie) {
        createReqCookieExpectation(oidcCodeCookie);
    }

    private void createReqCookieExpectation(final byte[] oidcCodeCookie) {
        mock.checking(new Expectations() {
            {
                //                one(req).getRequestURL();
                //                will(returnValue(new StringBuffer("http://foo.com/something")));
                one(req).getCookieValueAsBytes(ClientConstants.WAS_OIDC_CODE);
                will(returnValue(oidcCodeCookie));
            }
        });
    }

    private void createHandleRedirectToServerExpectations(String authorizationEndpoint, final String queryString) {
        createHttpsRequirementExpectationsForAuthorizationEndpoint(authorizationEndpoint);
        createClientRedirectRequirementExpectations(true);
        createSessionExpectations();
        createReferrerCookiesExpectations();
        createRedirectUrlExpectations();
        createAuthorizationEndpointUrlExpectations();
        createOptionalExpectations();
    }

    private void createSessionExpectations() {
        mock.checking(new Expectations() {
            {
                one(clientConfig).createSession();
                will(returnValue(true));
                one(req).getSession(true);
                will(returnValue(session));
            }
        });
    }

    private void createOptionalExpectations() {
        mock.checking(new Expectations() {
            {
                allowing(clientConfig).getAuthContextClassReference();
                will(returnValue(TEST_ACR_VALUES));
            }
        });
    }

    private void createHttpsRequirementExpectationsForAuthorizationEndpoint(final String authorizationEndpoint) {
        mock.checking(new Expectations() {
            {
                one(clientConfig).getAuthorizationEndpointUrl();
                will(returnValue(authorizationEndpoint));
                allowing(req).setAttribute(with(any(String.class)), with(any(OidcClientRequest.class)));
            }
        });
        createHttpsRequirementExpectations(true);
    }

    private void createReferrerCookiesExpectations() {
        mock.checking(new Expectations() {
            {
                allowing(webAppSecConfig).getSSORequiresSSL();
                will(returnValue(true));
                allowing(webAppSecConfig).getHttpOnlyCookies();
                will(returnValue(true));
            }
        });
        try {
            final long exp = 0;
            mock.checking(new Expectations() {
                {
                    one(res).setStatus(200);
                    one(res).getWriter();
                    will(returnValue(pw));
                    allowing(pw).println(with(any(String.class)));
                    allowing(res).setHeader(with(any(String.class)), with(any(String.class)));
                    allowing(res).setDateHeader("Expires", exp);
                    allowing(res).setContentType(with(any(String.class)));
                    allowing(pw).close();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createRedirectUrlExpectations() {
        mock.checking(new Expectations() {
            {
                one(clientConfig).getRedirectUrlFromServerToClient();
                will(returnValue(TEST_REDIRECT_URL));
            }
        });
        createHttpsRequirementExpectations(true);
    }

    private void createHttpsRequirementExpectations(final boolean httpsRequired) {
        mock.checking(new Expectations() {
            {
                one(clientConfig).isHttpsRequired();
                will(returnValue(httpsRequired));
            }
        });
    }

    private void createClientRedirectRequirementExpectations(final boolean clientSideRedirect) {
        mock.checking(new Expectations() {
            {
                one(clientConfig).isClientSideRedirect();
                will(returnValue(clientSideRedirect));
            }
        });
    }

    private void createAuthorizationEndpointUrlExpectations() {
        mock.checking(new Expectations() {
            {
                one(clientConfig).getClientId();
                will(returnValue(CLIENT01));
                allowing(clientConfig).getScope();
                will(returnValue(TEST_GRANT_TYPE));
                allowing(clientConfig).isNonceEnabled();
                will(returnValue(false));
                one(clientConfig).getAuthorizationEndpointUrl(); // TODO: Refactor code to remove duplicate calls
                will(returnValue(TEST_AUTHORIZATION_ENDPOINT));
                allowing(clientConfig).getUseSystemPropertiesForHttpClientConnections();
                will(returnValue(false));
            }
        });
    }

    private void assertRedirectResult(ProviderAuthenticationResult result) {
        assertEquals("The result status must be REDIRECT_TO_PROVIDER.", AuthResult.REDIRECT_TO_PROVIDER, result.getStatus());
        String redirectUrl = result.getRedirectUrl();
        assertTrue("The redirect URL must contain the authorization endpoint " + TEST_AUTHORIZATION_ENDPOINT + ".", redirectUrl.indexOf(TEST_AUTHORIZATION_ENDPOINT) >= 0);
        assertTrue("The redirect URL must contain 'response_type=code' in the query string.", redirectUrl.indexOf("response_type=code") >= 0);
        assertTrue("The redirect URL must contain 'client_id=client01' in the query string.", redirectUrl.indexOf("client_id=client01") >= 0);
    }

    private void createConstructorExpectations(final ConvergedClientConfig clientConfig) {
        WebAppSecurityCollaboratorImpl.setGlobalWebAppSecurityConfig(webAppSecConfig);
    }

    private void createResultExpectations() throws IDTokenValidationFailedException {
        // IDToken creation and properties
        mock.checking(new Expectations() {
            {
                one(clientConfig).getIssuerIdentifier();
                will(returnValue(""));
                one(clientConfig).getTokenEndpointUrl(); // TODO: Refactor code so that this is only called once.
                will(returnValue(TEST_TOKEN_ENDPOINT));
                allowing(clientConfig).getSignatureAlgorithm();
                will(returnValue("HS256"));
                one(clientConfig).getSharedKey();
                will(returnValue(SHARED_KEY));
                allowing(clientConfig).getClientId(); // TODO: Refactor code so that this is only called once.
                will(returnValue(CLIENT01));
                one(oidcClientUtil).createIDToken(with(any(String.class)),
                        with(any(Object.class)),
                        with(any(String.class)),
                        with(any(String.class)),
                        with(any(String.class)),
                        with(any(String.class)));
                will(returnValue(idToken));
                allowing(idToken).getHeader();
                will(returnValue(jwsHeader));
            }
        });
        createClientConfigExpectationsForIDToken();
        mock.checking(new Expectations() {
            {
                one(idToken).verify(with(any(Long.class)), with(any(Object.class)));
                will(returnValue(true));
                allowing(idToken).getPayload();
                will(returnValue(payload));
                one(payload).setJwtId(CLIENT01);
                will(returnValue(payload));
                one(payload).get(ClientConstants.SUB);
                will(returnValue("testuser"));
                one(idToken).addToIdTokenImpl(with(any(IdTokenImpl.class)));
                one(clientConfig).isIncludeCustomCacheKeyInSubject();
                will(returnValue(true));
                one(clientConfig).isIncludeIdTokenInSubject();
                will(returnValue(true));
            }
        });
        createClientConfigExpectationsForResult();
    }

    private void createClientConfigExpectationsForResultBeforeMapping() {
        mock.checking(new Expectations() {
            {
                allowing(clientConfig).isMapIdentityToRegistryUser();
                will(returnValue(true));
            }
        });
    }

    private void createPostParameterHelperExpectations() {
        mock.checking(new Expectations() {
            {
                allowing(req).getMethod();
                will(returnValue("POST"));
            }
        });
        createOtherParameterHelperExpectations();
    }

    private void createOtherParameterHelperExpectations() {
        mock.checking(new Expectations() {
            {
                allowing(req).getRequestURI();
                will(returnValue(TEST_URL));

                allowing(webAppSecConfig).getPostParamSaveMethod();
                will(returnValue("NoneExist"));
            }
        });
    }

    private void createReferrerUrlCookiesExpectations() {
        mock.checking(new Expectations() {
            {
                one(referrerURLCookieHandler).invalidateReferrerURLCookies(req, res,
                        OIDCClientAuthenticatorUtil.OIDC_COOKIES);
            }
        });
    }

    private void createReferrerUrlCookieExpectations(final String cookieName) {
        mock.checking(new Expectations() {
            {
                allowing(req).getRequestURL();
                will(returnValue(new StringBuffer(TEST_URL)));
                allowing(cookie2).setMaxAge(0);
                allowing(cookie2).setMaxAge(-1);
                //                one(referrerURLCookieHandler).invalidateReferrerURLCookie(req, res,
                //                        cookieName);
            }
        });
    }

    @Test
    public void testSetRedirectUrlIfNotDefined() {
        try {
            final String redirectUri = "https://mine.ibm.com:8020/oidcclient/redirect";
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).isSocial();
                    will(returnValue(false));
                    one(convClientConfig).getRedirectUrlFromServerToClient();
                    will(returnValue(redirectUri));
                    one(convClientConfig).getRedirectUrlWithJunctionPath(redirectUri);
                    will(returnValue(redirectUri));
                }
            });
            assertEquals("Redirect URL did not match expected value.", redirectUri, OIDCClientAuthenticatorUtil.setRedirectUrlIfNotDefined(req, convClientConfig));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testSetRedirectUrlIfNotDefined_withJunctionPath() {
        try {
            final String redirectUri = "https://mine.ibm.com:8020";
            final String contextPath = "/oidcclient";
            final String fullUrl = redirectUri + contextPath + "/redirect/" + CLIENT01;
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).isSocial();
                    will(returnValue(false));
                    one(convClientConfig).getRedirectUrlFromServerToClient();
                    will(returnValue(redirectUri));
                    one(convClientConfig).getRedirectUrlWithJunctionPath(redirectUri);
                    will(returnValue(fullUrl));
                }
            });
            assertNotNull("Redirect URL did not match expected value.", OIDCClientAuthenticatorUtil.setRedirectUrlIfNotDefined(req, convClientConfig));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testSetRedirectUrlIfNotDefined_socialFlow() {
        try {
            final String scheme = "https";
            final int port = 8020;
            final String serverName = "mine.ibm.com";
            final String clientId = CLIENT01;
            final String contextPath = "/oidcclient";
            final String redirectUri = scheme + "://" + serverName + ":" + port + contextPath + "/redirect/" + clientId;
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).isSocial();
                    will(returnValue(true));
                    allowing(convClientConfig).getId();
                    will(returnValue(clientId));
                    allowing(convClientConfig).getContextPath();
                    will(returnValue(contextPath));
                    one(convClientConfig).getRedirectUrlFromServerToClient();
                    will(returnValue(null));
                    one(req).getServerName();
                    will(returnValue(serverName));
                    one(req).isSecure();
                    will(returnValue(true));
                    one(req).getServerPort();
                    will(returnValue(port));
                    one(req).getScheme();
                    will(returnValue(scheme));
                    one(convClientConfig).getRedirectUrlWithJunctionPath(redirectUri);
                    will(returnValue(redirectUri));
                }
            });
            String returnedUrl = OIDCClientAuthenticatorUtil.setRedirectUrlIfNotDefined(req, convClientConfig);
            assertEquals("Redirect URL did not match expected value.", redirectUri, returnedUrl);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testVerifyResponseStateFailure() {
        try {
            final String originalState = TEST_ORIGINAL_STATE;
            final String cookieName = OidcStorageUtils.getStateStorageKey("notA" + originalState);
            final Cookie[] cookies = new Cookie[] { cookie1 };
            final ReferrerURLCookieHandler referrerURLCookieHandlerTwo = mock.mock(ReferrerURLCookieHandler.class, "referrerURLCookieHandlerTwo");
            mock.checking(new Expectations() {
                {
                    allowing(convClientConfig).getClientId();
                    will(returnValue(CLIENT01));
                    one(req).getCookies();
                    will(returnValue(cookies));
                    one(cookie1).getName();
                    will(returnValue(cookieName));
                    one(cookie1).getValue();
                    will(returnValue(originalState));
                    one(convClientConfig).getClientSecret();
                    will(returnValue("clientsecret"));
                    one(convClientConfig).getClockSkewInSeconds();
                    will(returnValue(TEST_CLOCK_SKEW_IN_SECONDS));
                    one(convClientConfig).getAuthenticationTimeLimitInSeconds();
                    will(returnValue(420L));                   
                    one(req).getRequestURL();
                    will(returnValue(new StringBuffer(TEST_URL)));
                    one(referrerURLCookieHandlerTwo).createCookie(with(any(String.class)), with(any(String.class)), with(any(HttpServletRequest.class)));
                    will(returnValue(cookie1));
                    one(cookie1).setMaxAge(0);
                    one(res).addCookie(cookie1);
                    
                }
            });
            
            OidcClientUtil.setReferrerURLCookieHandler(referrerURLCookieHandlerTwo);

            ProviderAuthenticationResult result = oidcCAUtil.verifyResponseState(req, res, "notA" + originalState, convClientConfig);
            assertNotNull("Did not get an expecyted result", result);
            assertTrue("Expect to get an " + AuthResult.SEND_401 + " but a " + result.getStatus(),
                    AuthResult.SEND_401.equals(result.getStatus()));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    private void createClientConfigExpectationsForIDToken() {
        mock.checking(new Expectations() {
            {
                one(clientConfig).getClockSkewInSeconds();
                will(returnValue(TEST_CLOCK_SKEW_IN_SECONDS));
                allowing(clientConfig).isNonceEnabled();
                will(returnValue(false));
            }
        });
    }

    private void createClientConfigExpectationsForResult() {
        mock.checking(new Expectations() {
            {
                one(clientConfig).getUserIdentifier();
                will(returnValue(ClientConstants.SUB));
            }
        });
        createClientConfigExpectationsForResultBeforeMapping();
    }

    @Test
    public void testHandleRedirectToServer_authorizationEndpointNotHttps_httpsRequired() {
        try {
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).isHttpsRequired();
                    will(returnValue(true));
                    one(convClientConfig).getAuthorizationEndpointUrl();
                    will(returnValue("some non URL"));
                    one(convClientConfig).getClientId();
                    will(returnValue(CLIENT01));
                }
            });
            ProviderAuthenticationResult result = oidcCAUtil.handleRedirectToServer(req, res, convClientConfig);
            checkForBadStatusExpectations(result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testVerifyResponseState_NullResponseState() {
        mock.checking(new Expectations() {
            {
                one(convClientConfig).getClientId();
                will(returnValue(CLIENT01));
            }
        });
        ProviderAuthenticationResult oidcResult = oidcCAUtil.verifyResponseState(req, res, null, convClientConfig);

        checkForBadStatusExpectations(oidcResult);
    }

    private void checkForBadStatusExpectations(ProviderAuthenticationResult oidcResult) {
        assertNotNull("ProviderAuthenticationResult was null but should not have been.", oidcResult);
        assertEquals("Expected to receive status:" + AuthResult.SEND_401 + " but received:" + oidcResult.getStatus() + ".", AuthResult.SEND_401, oidcResult.getStatus());

        assertEquals("Expected to receive status code:" + HttpServletResponse.SC_UNAUTHORIZED + " but received:" + oidcResult.getHttpStatusCode() + ".",
                HttpServletResponse.SC_UNAUTHORIZED, oidcResult.getHttpStatusCode());
    }

    class MockInputStream extends InputStream {
        String strOut = null;
        int iCnt = 0;

        public MockInputStream(String strOut) {
            this.strOut = strOut;
        }

        @Override
        public int read() {
            if (iCnt < strOut.length()) {
                return strOut.charAt(iCnt++);
            } else {
                return -1;
            }
        }
    }

    class mockOidcClientUtil extends OidcClientUtil {
        IOException ioe = null;
        HttpException httpe = null;

        public mockOidcClientUtil(IOException e) {
            super();
            ioe = e;
        }

        public mockOidcClientUtil(HttpException e) {
            super();
            httpe = e;
        }
    }

}
