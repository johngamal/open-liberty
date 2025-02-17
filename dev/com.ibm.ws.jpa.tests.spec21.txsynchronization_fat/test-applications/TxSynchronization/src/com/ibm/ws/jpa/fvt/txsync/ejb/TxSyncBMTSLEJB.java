/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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

package com.ibm.ws.jpa.fvt.txsync.ejb;

import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.SynchronizationType;

import com.ibm.ws.jpa.fvt.txsync.ejblocal.TxSyncBMTSLLocal;
import com.ibm.ws.testtooling.vehicle.ejb.BMTEJBTestVehicle;

@Stateless(name = "TxSyncBMTSLEJB")
@Local(TxSyncBMTSLLocal.class)
@TransactionManagement(javax.ejb.TransactionManagementType.BEAN)
public class TxSyncBMTSLEJB extends BMTEJBTestVehicle {
    // Container Managed Transaction Scope
    @PersistenceContext(unitName = "TxSync",
                        type = PersistenceContextType.TRANSACTION,
                        synchronization = SynchronizationType.SYNCHRONIZED)
    private EntityManager emCMTSTxSync;

    @PersistenceContext(unitName = "TxSync",
                        type = PersistenceContextType.TRANSACTION,
                        synchronization = SynchronizationType.UNSYNCHRONIZED)
    private EntityManager emCMTSTxUnsync;
}
