/*
 * Copyright (C) 2018 BonitaSoft S.A.
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
 */
package org.bonitasoft.engine.commons.exceptions;


/**
 * Thrown when a process fails to deploy because of V6 form presence in the bar to deploy.
 *
 * @author Danila Mazour
 * @since 7.8.0
 */
public class SV6FormsDeployException extends SObjectCreationException {

    /**
     * @param message
     */
    public SV6FormsDeployException(final String message) {
        super(message);
    }

}
