/*
 * Copyright (c) 2015, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.jsf22.fat.cdicommon.beans.injected;

import javax.enterprise.inject.Produces;

/**
 *
 */
public class FieldBean {

    protected String value;

    /**
     * @param string
     */
    public void addData(String string) {
        value = value + ":" + string;
    }

    public String getData() {
        return this.getClass() + (value == null ? ":" : ":" + value);
    }

    private final String producerText = ":ProducerInjected:";

    @Produces
    @ProducerType
    String getProducerType() {
        return producerText;
    }
}
