/*
 * Copyright (c) 2015, 2023 IBM Corporation and others.
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
package com.ibm.ws.jsf22.fat.miscbean.jar.faces40;

import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.faces.component.html.HtmlCommandButton;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

@Named("miscellaneousViewScopeBean")
@ViewScoped
public class MiscellaneousViewScopeBean implements Serializable {
//    private static final Logger LOGGER = Logger.getLogger(MiscellaneousViewScopeBean.class.getName());
    private static final long serialVersionUID = -3118004082493064756L;

    private HtmlCommandButton button;
    private Integer counter = 0;

    public void setCounter(Integer results) {
        this.counter = results;
    }

    public String getCounter() {
        return ("PostConstruct counter = " + counter);
    }

    public HtmlCommandButton getButton() {
        return button;
    }

    public void setButton(HtmlCommandButton button) {
        this.button = button;
    }

    @PostConstruct
    public void init() {
        counter++;
    }

}
