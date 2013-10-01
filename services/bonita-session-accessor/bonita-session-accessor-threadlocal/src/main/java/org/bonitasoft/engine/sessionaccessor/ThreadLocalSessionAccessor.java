/**
 * Copyright (C) 2011 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation
 * version 2.1 of the License.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA 02110-1301, USA.
 **/
package org.bonitasoft.engine.sessionaccessor;


/**
 * @author Yanyan Liu
 * @author Matthieu Chaffotte
 */
public class ThreadLocalSessionAccessor implements SessionAccessor {

    private final Object mutex = new Object();

    private final ThreadLocal<Long> sessionData = new ThreadLocal<Long>();
    private final ThreadLocal <Long> tenantData = new ThreadLocal<Long>();

    @Override
    public long getSessionId() throws SessionIdNotSetException {
        Long sessionId = null;
        synchronized (mutex) {
            sessionId = sessionData.get();
            if (sessionId == null) {
                throw new SessionIdNotSetException("No session set.");
            }
        }
        return sessionId;
    }

    @Override
    public void setSessionInfo(final long sessionId, final long tenantId) {
        synchronized (mutex) {
            sessionData.set(sessionId);
            tenantData.set(tenantId);
        }
    }
    
    @Override
    public void setTenantId(final long tenantId) {
    	synchronized (mutex) {
            tenantData.set(tenantId);
        }
    }

    @Override
    public void deleteSessionId() {
        synchronized (mutex) {
            sessionData.remove();
        }
    }
    
    @Override
    public void deleteTenantId() {
    	synchronized (mutex) {
            tenantData.remove();
        }   
    }

    @Override
    public long getTenantId() throws TenantIdNotSetException {
        Long tenantId = null;
        synchronized (mutex) {
        	tenantId = tenantData.get();
            if (tenantId == null) {
                throw new TenantIdNotSetException("No tenantId set.");
            }
        }
        return tenantId;
    }

}
