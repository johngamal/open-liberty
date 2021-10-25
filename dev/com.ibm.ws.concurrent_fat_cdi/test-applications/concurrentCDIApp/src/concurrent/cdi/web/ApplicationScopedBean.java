/*******************************************************************************
 * Copyright (c) 2017,2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package concurrent.cdi.web;

import java.io.Serializable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.context.ApplicationScoped;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import prototype.enterprise.concurrent.Async;

@ApplicationScoped
public class ApplicationScopedBean implements Serializable {
    private static final long serialVersionUID = -2075274815197982538L;

    private char character;

    /**
     * An asynchronous method with return type of CompletableFuture.
     */
    @Async
    public CompletableFuture<String> appendThreadNameFuture(String part1) {
        return Async.Result.complete(part1 + getCharacter() + Thread.currentThread().getName());
    }

    /**
     * An asynchronous method with return type of CompletionStage.
     */
    @Async
    public CompletionStage<String> appendThreadNameStage(String part1) {
        try {
            ManagedExecutorService executor = InitialContext.doLookup("java:comp/env/concurrent/executorRef");
            return executor.completedStage(part1 + getCharacter() + Thread.currentThread().getName());
        } catch (NamingException x) {
            throw new CompletionException(x);
        }
    }

    /**
     * Asynchronous method that intentionally raises an error, for testing purposes.
     */
    @Async
    public CompletableFuture<Integer> forceError() {
        throw new Error("Intentionally raising this error.");
    }

    public char getCharacter() {
        return character;
    }

    /**
     * Looks up a resource in JNDI, asynchronously to the calling thread.
     */
    @Async
    public CompletableFuture<?> lookup(String jndiName) {
        try {
            return CompletableFuture.completedFuture(InitialContext.doLookup(jndiName));
        } catch (NamingException x) {
            throw new CompletionException(x);
        }
    }

    /**
     * This is not an asynchronous method.
     *
     * @param threadNameRef reference into which to put the name of the
     *                          thread where this method runs.
     */
    public void notAsync(AtomicReference<String> threadNameRef) {
        threadNameRef.set(Thread.currentThread().getName());
    }

    public void setCharacter(char character) {
        this.character = character;
    }
}
