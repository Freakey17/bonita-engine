package org.bonitasoft.engine.scheduler.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.bonitasoft.engine.CommonServiceTest;
import org.bonitasoft.engine.persistence.FilterOption;
import org.bonitasoft.engine.persistence.QueryOptions;
import org.bonitasoft.engine.platform.STenantNotFoundException;
import org.bonitasoft.engine.platform.model.builder.STenantBuilder;
import org.bonitasoft.engine.scheduler.JobService;
import org.bonitasoft.engine.scheduler.SchedulerService;
import org.bonitasoft.engine.scheduler.builder.SJobParameterBuilder;
import org.bonitasoft.engine.scheduler.job.ThrowsExceptionJob;
import org.bonitasoft.engine.scheduler.model.SJobDescriptor;
import org.bonitasoft.engine.scheduler.model.SJobLog;
import org.bonitasoft.engine.scheduler.model.SJobParameter;
import org.bonitasoft.engine.scheduler.trigger.OneExecutionTrigger;
import org.bonitasoft.engine.scheduler.trigger.Trigger;
import org.bonitasoft.engine.scheduler.trigger.UnixCronTrigger;
import org.bonitasoft.engine.test.util.PlatformUtil;
import org.bonitasoft.engine.test.util.TestUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JobTest extends CommonServiceTest {

    public final String DEFAULT_TENANT_STATUS = "DEACTIVATED";

    private static final SchedulerService schedulerService;

    private static final JobService JOB_SERVICE;

    private static STenantBuilder tenantBuilder;

    private long tenant1;

    private final VariableStorage storage = VariableStorage.getInstance();

    static {
        schedulerService = getServicesBuilder().buildSchedulerService();
        tenantBuilder = getServicesBuilder().buildTenantBuilder();
        JOB_SERVICE = getServicesBuilder().getInstanceOf(JobService.class);
    }

    protected void changeToDefaultTenant() throws STenantNotFoundException, Exception {
        final long defaultTenant = getPlatformService().getTenantByName("default").getId();
        TestUtil.createSessionOn(getSessionAccessor(), getSessionService(), defaultTenant);
    }

    @Before
    public void setUp() throws Exception {
        tenant1 = PlatformUtil.createTenant(getTransactionService(), getPlatformService(), tenantBuilder, "tenant1", PlatformUtil.DEFAULT_CREATED_BY,
                PlatformUtil.DEFAULT_TENANT_STATUS);
        TestUtil.startScheduler(schedulerService);
        getTransactionService().begin();
        changeToDefaultTenant();
        final QueryOptions options = new QueryOptions(0, 1, null, Collections.<FilterOption> emptyList(), null);
        final List<SJobDescriptor> jobDescriptors = JOB_SERVICE.searchJobDescriptors(options);
        assertTrue(jobDescriptors.isEmpty());
        final List<SJobParameter> jobParameters = JOB_SERVICE.searchJobParameters(options);
        assertTrue(jobParameters.isEmpty());
        final List<SJobLog> jobLogs = JOB_SERVICE.searchJobLogs(options);
        assertTrue(jobLogs.isEmpty());
        getTransactionService().complete();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        getTransactionService().begin();
        schedulerService.deleteJobs();
        getTransactionService().complete();
        TestUtil.stopScheduler(schedulerService, getTransactionService());
        storage.clear();
        PlatformUtil.deleteTenant(getTransactionService(), getPlatformService(), tenant1);
    }

    @Test
    public void retriesAFailedJob2() throws Exception {
        final Date now = new Date();
        getTransactionService().begin();
        final Trigger trigger = new OneExecutionTrigger("logevents", now, 10);
        final SJobDescriptor jobDescriptor = schedulerService.getJobDescriptorBuilder()
                .createNewInstance(ThrowsExceptionJob.class.getName(), "ThowExceptionJob").done();

        final SJobParameterBuilder parameterBuilder = schedulerService.getJobParameterBuilder();
        final SJobParameter parameter = parameterBuilder.createNewInstance("throwException", Boolean.TRUE).done();
        final List<SJobParameter> parameters = new ArrayList<SJobParameter>(2);
        parameters.add(parameter);
        schedulerService.schedule(jobDescriptor, parameters, trigger);
        getTransactionService().complete();

        Thread.sleep(500);
        getTransactionService().begin();
        List<FilterOption> filters = new ArrayList<FilterOption>(2);
        filters.add(new FilterOption(SJobDescriptor.class, "jobClassName", ThrowsExceptionJob.class.getName()));
        final QueryOptions queryOptions = new QueryOptions(0, 1, null, filters, null);
        final List<SJobDescriptor> jobDescriptors = JOB_SERVICE.searchJobDescriptors(queryOptions);
        assertEquals(1, jobDescriptors.size());

        filters = new ArrayList<FilterOption>(2);
        filters.add(new FilterOption(SJobLog.class, "jobDescriptorId", jobDescriptors.get(0).getId()));
        final QueryOptions options = new QueryOptions(0, 1, null, filters, null);
        List<SJobLog> jobLogs = JOB_SERVICE.searchJobLogs(options);
        assertEquals(1, jobLogs.size());
        getTransactionService().complete();

        getTransactionService().begin();
        parameters.clear();
        parameters.add(parameterBuilder.createNewInstance("throwException", Boolean.FALSE).done());
        schedulerService.schedule(jobDescriptors.get(0).getId(), parameters);
        getTransactionService().complete();
        Thread.sleep(1000);

        getTransactionService().begin();
        jobLogs = JOB_SERVICE.searchJobLogs(options);
        assertEquals(0, jobLogs.size());
        getTransactionService().complete();
    }

    @Test
    public void retriesAFailedJob() throws Exception {
        final Date now = new Date();
        getTransactionService().begin();
        final Trigger trigger = new OneExecutionTrigger("logevents", now, 10);
        final SJobDescriptor jobDescriptor = schedulerService.getJobDescriptorBuilder()
                .createNewInstance(ThrowsExceptionJob.class.getName(), "ThowExceptionJob2").done();

        final SJobParameterBuilder parameterBuilder = schedulerService.getJobParameterBuilder();
        final SJobParameter parameter = parameterBuilder.createNewInstance("throwException", Boolean.TRUE).done();
        final List<SJobParameter> parameters = new ArrayList<SJobParameter>(2);
        parameters.add(parameter);
        schedulerService.schedule(jobDescriptor, parameters, trigger);
        getTransactionService().complete();

        Thread.sleep(500);
        getTransactionService().begin();
        List<FilterOption> filters = new ArrayList<FilterOption>(2);
        filters.add(new FilterOption(SJobDescriptor.class, "jobClassName", ThrowsExceptionJob.class.getName()));
        final QueryOptions queryOptions = new QueryOptions(0, 1, null, filters, null);
        final List<SJobDescriptor> jobDescriptors = JOB_SERVICE.searchJobDescriptors(queryOptions);
        assertEquals(1, jobDescriptors.size());

        filters = new ArrayList<FilterOption>(2);
        filters.add(new FilterOption(SJobLog.class, "jobDescriptorId", jobDescriptors.get(0).getId()));
        final QueryOptions options = new QueryOptions(0, 1, null, filters, null);
        List<SJobLog> jobLogs = JOB_SERVICE.searchJobLogs(options);
        assertEquals(1, jobLogs.size());
        getTransactionService().complete();

        getTransactionService().begin();
        parameters.clear();
        parameters.add(parameterBuilder.createNewInstance("throwException", Boolean.FALSE).done());
        JOB_SERVICE.setJobParameters(getDefaultTenantId(), jobDescriptors.get(0).getId(), parameters);
        schedulerService.schedule(jobDescriptors.get(0).getId());
        getTransactionService().complete();
        Thread.sleep(500);

        getTransactionService().begin();
        jobLogs = JOB_SERVICE.searchJobLogs(options);
        assertEquals(0, jobLogs.size());
        getTransactionService().complete();
    }

    @Test
    public void retriesSeveralTimesAFailedJob() throws Exception {
        final Date now = new Date();
        getTransactionService().begin();
        final Trigger trigger = new OneExecutionTrigger("logevents", now, 10);
        final SJobDescriptor jobDescriptor = schedulerService.getJobDescriptorBuilder()
                .createNewInstance(ThrowsExceptionJob.class.getName(), "ThowExceptionJob").done();

        final SJobParameterBuilder parameterBuilder = schedulerService.getJobParameterBuilder();
        final SJobParameter parameter = parameterBuilder.createNewInstance("throwException", Boolean.TRUE).done();
        final List<SJobParameter> parameters = new ArrayList<SJobParameter>(2);
        parameters.add(parameter);
        schedulerService.schedule(jobDescriptor, parameters, trigger);
        getTransactionService().complete();

        Thread.sleep(500);
        getTransactionService().begin();
        List<FilterOption> filters = new ArrayList<FilterOption>(2);
        filters.add(new FilterOption(SJobDescriptor.class, "jobClassName", ThrowsExceptionJob.class.getName()));
        final QueryOptions queryOptions = new QueryOptions(0, 1, null, filters, null);
        final List<SJobDescriptor> jobDescriptors = JOB_SERVICE.searchJobDescriptors(queryOptions);
        assertEquals(1, jobDescriptors.size());

        filters = new ArrayList<FilterOption>(2);
        filters.add(new FilterOption(SJobLog.class, "jobDescriptorId", jobDescriptors.get(0).getId()));
        final QueryOptions options = new QueryOptions(0, 1, null, filters, null);
        List<SJobLog> jobLogs = JOB_SERVICE.searchJobLogs(options);
        assertEquals(1, jobLogs.size());
        assertEquals(Long.valueOf(0), jobLogs.get(0).getRetryNumber());
        getTransactionService().complete();

        getTransactionService().begin();
        schedulerService.schedule(jobDescriptors.get(0).getId());
        getTransactionService().complete();

        getTransactionService().begin();
        schedulerService.schedule(jobDescriptors.get(0).getId());
        getTransactionService().complete();
        Thread.sleep(500);
        getTransactionService().begin();
        filters = new ArrayList<FilterOption>(2);
        filters.add(new FilterOption(SJobLog.class, "jobDescriptorId", jobDescriptors.get(0).getId()));
        jobLogs = JOB_SERVICE.searchJobLogs(options);
        assertEquals(1, jobLogs.size());
        assertEquals(Long.valueOf(2), jobLogs.get(0).getRetryNumber());
        getTransactionService().complete();
    }

    @Test
    public void retriesAFailedCronJob() throws Exception {
        final Date now = new Date();
        getTransactionService().begin();
        final Trigger trigger = new UnixCronTrigger("events", now, 10, "0/1 * * * * ?");
        final SJobDescriptor jobDescriptor = schedulerService.getJobDescriptorBuilder()
                .createNewInstance(ThrowsExceptionJob.class.getName(), "retriesAFailedCronJob").done();

        final SJobParameterBuilder parameterBuilder = schedulerService.getJobParameterBuilder();
        final SJobParameter parameter = parameterBuilder.createNewInstance("throwException", Boolean.TRUE).done();
        final List<SJobParameter> parameters = new ArrayList<SJobParameter>(2);
        parameters.add(parameter);
        schedulerService.schedule(jobDescriptor, parameters, trigger);
        getTransactionService().complete();

        Thread.sleep(1000);
        getTransactionService().begin();

        List<FilterOption> filters = new ArrayList<FilterOption>(2);
        filters.add(new FilterOption(SJobDescriptor.class, "jobClassName", ThrowsExceptionJob.class.getName()));
        final QueryOptions queryOptions = new QueryOptions(0, 2, null, filters, null);
        final List<SJobDescriptor> jobDescriptors = JOB_SERVICE.searchJobDescriptors(queryOptions);
        assertEquals(1, jobDescriptors.size());
        filters = new ArrayList<FilterOption>(2);
        filters.add(new FilterOption(SJobLog.class, "jobDescriptorId", jobDescriptors.get(0).getId()));
        final QueryOptions options = new QueryOptions(0, 1, null, filters, null);
        List<SJobLog> jobLogs = JOB_SERVICE.searchJobLogs(options);
        assertEquals(1, jobLogs.size());
        getTransactionService().complete();

        getTransactionService().begin();
        parameters.clear();
        parameters.add(parameterBuilder.createNewInstance("throwException", Boolean.FALSE).done());

        schedulerService.schedule(jobDescriptors.get(0).getId(), parameters);
        getTransactionService().complete();
        Thread.sleep(500);

        getTransactionService().begin();
        jobLogs = JOB_SERVICE.searchJobLogs(options);
        assertEquals(0, jobLogs.size());
        getTransactionService().complete();

        getTransactionService().begin();
        parameters.clear();
        parameters.add(parameterBuilder.createNewInstance("throwException2", Boolean.TRUE).done());
        JOB_SERVICE.setJobParameters(getDefaultTenantId(), jobDescriptors.get(0).getId(), parameters);
        getTransactionService().complete();
        Thread.sleep(1000);

        getTransactionService().begin();
        jobLogs = JOB_SERVICE.searchJobLogs(options);
        assertEquals(1, jobLogs.size());
        getTransactionService().complete();
    }

}
