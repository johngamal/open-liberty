/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.mail.fat;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysRunAndPassTest.class,
                StreamProviderTest.class
})
public class FATSuite {

    private static LibertyServer mailSesionServer = LibertyServerFactory.getLibertyServer("mailSessionTestServer");

    @BeforeClass
    public static void setupApp() throws Exception {
        ShrinkHelper.defaultApp(mailSesionServer, "TestingApp", "TestingApp.*");

    }
}
