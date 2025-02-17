/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package com.ibm.ws.policyattachments.service3;

import javax.jws.WebService;

@WebService(serviceName = "HelloService3", wsdlLocation = "WEB-INF/wsdl/HelloService3.wsdl")
public class HelloService3 {
    public String helloWithoutPolicy() {
        return "helloWithoutPolicy invoked";
    }

    public String helloWithPolicy() {
        return "helloWithPolicy invoked";
    }

    public String helloWithOptionalPolicy() {
        return "helloWithOptionalPolicy invoked";
    }

    public String helloWithYouWant() {
        return "helloWithYouWant invoked";
    }
}
