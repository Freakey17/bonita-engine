/*******************************************************************************
 * Copyright (C) 2015 BonitaSoft S.A.
 * BonitaSoft is a trademark of BonitaSoft SA.
 * This software file is BONITASOFT CONFIDENTIAL. Not For Distribution.
 * For commercial licensing information, contact:
 * BonitaSoft, 32 rue Gustave Eiffel – 38000 Grenoble
 * or BonitaSoft US, 51 Federal Street, Suite 305, San Francisco, CA 94107
 *******************************************************************************/
package com.bonitasoft.engine.authentication.impl;

import java.io.Serializable;
import java.util.Map;

import org.bonitasoft.engine.authentication.AuthenticationConstants;
import org.bonitasoft.engine.authentication.GenericAuthenticationService;
import org.bonitasoft.engine.commons.LogUtil;
import org.bonitasoft.engine.identity.IdentityService;
import org.bonitasoft.engine.identity.SUserNotFoundException;
import org.bonitasoft.engine.log.technical.TechnicalLogSeverity;
import org.bonitasoft.engine.log.technical.TechnicalLoggerService;

/**
 * @author Abdelhilah Boudhan
 * @author Matthieu Chaffotte
 * @author Celine Souchet
 */
public class NoAuthenticationServiceImpl implements GenericAuthenticationService {

    private final IdentityService identityService;

    private final TechnicalLoggerService logger;

    public NoAuthenticationServiceImpl(final IdentityService identityService, final TechnicalLoggerService logger) {
        this.identityService = identityService;
        this.logger = logger;
    }

    @Override
    public String checkUserCredentials(final Map<String, Serializable> credentials) {
        try {
            final String userName = String.valueOf(credentials.get(AuthenticationConstants.BASIC_USERNAME));
            logBeforeMethod();
            identityService.getUserByUserName(userName);
            logAfterMethod();
            return userName;
        } catch (final SUserNotFoundException sunfe) {
            logOnExceptionMethod(sunfe);
            return null;
        }
    }

    void logOnExceptionMethod(final SUserNotFoundException sunfe) {
        if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "checkUserCredentials", sunfe));
        }
    }

    void logAfterMethod() {
        if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogAfterMethod(this.getClass(), "checkUserCredentials"));
        }
    }

    void logBeforeMethod() {
        if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogBeforeMethod(this.getClass(), "checkUserCredentials"));
        }
    }

}