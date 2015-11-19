/*******************************************************************************
 * Copyright (C) 2015 BonitaSoft S.A.
 * BonitaSoft is a trademark of BonitaSoft SA.
 * This software file is BONITASOFT CONFIDENTIAL. Not For Distribution.
 * For commercial licensing information, contact:
 * BonitaSoft, 32 rue Gustave Eiffel – 38000 Grenoble
 * or BonitaSoft US, 51 Federal Street, Suite 305, San Francisco, CA 94107
 *******************************************************************************/
package com.bonitasoft.engine.monitoring.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import org.bonitasoft.engine.log.technical.TechnicalLoggerService;
import org.bonitasoft.engine.scheduler.SchedulerService;
import org.bonitasoft.engine.transaction.TransactionService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.bonitasoft.engine.monitoring.mbean.SJvmMXBean;

@RunWith(MockitoJUnitRunner.class)
public class PlatformMonitoringServiceImplTest {

    @Mock
    private SJvmMXBean jvmMBean;

    @Mock
    private SchedulerService schedulerService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private TechnicalLoggerService loggerService;

    @Mock
    private SJobHandlerImpl jobHandler;

    private PlatformMonitoringServiceImpl platformMonitoringService;

    @Before
    public void setUp() throws Exception {
        platformMonitoringService = new PlatformMonitoringServiceImpl(true, jvmMBean, transactionService, schedulerService, jobHandler, loggerService);
    }

    @Test
    public void getNumberOfActiveTransactions_return_nb_of_active_transactions_from_transaction_service() {
        //given
        doReturn(11L).when(transactionService).getNumberOfActiveTransactions();

        //when
        final long activeTransactions = platformMonitoringService.getNumberOfActiveTransactions();

        //then
        assertThat(activeTransactions).isEqualTo(11);
    }

    @Test
    public void isOptionalMonitoringInformationAvailableShouldBeTrueForSunJVM() throws Exception {
        // given:
        when(jvmMBean.getJvmVendor()).thenReturn("JVM Sun something");

        // when:
        final boolean optional = platformMonitoringService.isOptionalMonitoringInformationAvailable();

        // then:
        assertThat(optional).isTrue();
    }

    @Test
    public void isOptionalMonitoringInformationAvailableShouldBeTrueForOracleJVM() throws Exception {
        // given:
        when(jvmMBean.getJvmVendor()).thenReturn("JVM Oracle something");

        // when:
        final boolean optional = platformMonitoringService.isOptionalMonitoringInformationAvailable();

        // then:
        assertThat(optional).isTrue();
    }


    @Test
    public void getNumberOfExecutingJobs() {
        when(jobHandler.getExecutingJobs()).thenReturn(64);

        final long numberOfActiveTransactions = platformMonitoringService.getNumberOfExecutingJobs();

        assertThat(numberOfActiveTransactions).isEqualTo(64L);
    }

}