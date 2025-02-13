/*
 * Copyright (C) 2004 - 2014 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.skife.jdbi.v2.sqlobject;

import net.sf.cglib.proxy.MethodProxy;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.TransactionCallback;
import org.skife.jdbi.v2.TransactionIsolationLevel;
import org.skife.jdbi.v2.TransactionStatus;
import org.skife.jdbi.v2.exceptions.TransactionException;

import java.lang.reflect.Method;

class PassThroughTransactionHandler implements Handler
{
    private final TransactionIsolationLevel isolation;

    PassThroughTransactionHandler(Method m, Transaction tx)
    {
        this.isolation = tx.value();
    }

    @Override
    public Object invoke(HandleDing ding, final Object target, final Object[] args, final MethodProxy mp)
    {
        ding.retain("pass-through-transaction");
        try {
            Handle h = ding.getHandle();

            if (h.isInTransaction()) {
                throw new TransactionException("Nested @Transaction detected - this is currently not supported.");
            }

            if (isolation == TransactionIsolationLevel.INVALID_LEVEL) {
                return h.inTransaction(new TransactionCallback<Object>()
                {
                    @Override
                    public Object inTransaction(Handle conn, TransactionStatus status) throws Exception
                    {
                        try {
                            return mp.invokeSuper(target, args);
                        }
                        catch (Throwable throwable) {
                            if (throwable instanceof Exception) {
                                throw (Exception) throwable;
                            }
                            else {
                                throw new RuntimeException(throwable);
                            }
                        }
                    }
                });
            }
            else {
                return h.inTransaction(isolation, new TransactionCallback<Object>()
                {
                    @Override
                    public Object inTransaction(Handle conn, TransactionStatus status) throws Exception
                    {
                        try {
                            return mp.invokeSuper(target, args);
                        }
                        catch (Throwable throwable) {
                            if (throwable instanceof Exception) {
                                throw (Exception) throwable;
                            }
                            else {
                                throw new RuntimeException(throwable);
                            }
                        }
                    }
                });

            }
        }

        finally {
            ding.release("pass-through-transaction");
        }
    }
}
