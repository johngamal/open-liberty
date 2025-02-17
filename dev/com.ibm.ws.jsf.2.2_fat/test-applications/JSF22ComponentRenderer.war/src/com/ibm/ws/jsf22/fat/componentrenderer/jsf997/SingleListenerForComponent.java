/*
 * Copyright (c) 2015 IBM Corporation and others.
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
package com.ibm.ws.jsf22.fat.componentrenderer.jsf997;

import java.util.Map;

import javax.faces.component.FacesComponent;
import javax.faces.component.html.HtmlInputText;
import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.event.ListenerFor;
import javax.faces.event.PostAddToViewEvent;

@FacesComponent(value = "SingleListenerForComponent")
@ListenerFor(systemEventClass = PostAddToViewEvent.class)
public class SingleListenerForComponent extends HtmlInputText {

    /**
     * This code is testing a change to the JSF infrastructure that validates that listeners of
     * ComponentSystemEvents implements ComponentSystemEventListener. If this code executes
     * correctly then the validation via ComponentSystemEvent.isAppropriateListener() returned true.
     * The code here is testing a single listener.
     */
    @Override
    public void processEvent(ComponentSystemEvent event) throws AbortProcessingException {
        Map<String, Object> requestMap = FacesContext.getCurrentInstance().getExternalContext().getRequestMap();
        if (event instanceof PostAddToViewEvent) {
            requestMap.put("PostAddToViewEvent", "PostAddToViewEvent");
        } else {
            super.processEvent(event);
        }
    }
}
