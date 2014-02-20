package com.bonitasoft.engine.business.data.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.transaction.UserTransaction;

import org.bonitasoft.engine.commons.io.IOUtil;
import org.bonitasoft.engine.dependency.DependencyService;
import org.bonitasoft.engine.dependency.model.SDependency;
import org.dbunit.DataSourceDatabaseTester;
import org.dbunit.IDatabaseTester;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.operation.DatabaseOperation;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.resource.jdbc.PoolingDataSource;

import com.bonitasoft.engine.business.data.BusinessDataNotFoundException;
import com.bonitasoft.engine.business.data.NonUniqueResultException;
import com.bonitasoft.pojo.Employee;

public class JPABusinessDataRepositoryImplIT {

    private static final String DATA_SOURCE_NAME = "java:/comp/env/jdbc/PGDS1";

    private IDatabaseTester databaseTester;

    private DependencyService dependencyService;

    private byte[] bdrArchive;

    private JPABusinessDataRepositoryImpl businessDataRepository;

    private static PoolingDataSource ds1;

    @BeforeClass
    public static void initDatasource() throws NamingException, SQLException {
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, "bitronix.tm.jndi.BitronixInitialContextFactory");
        TransactionManagerServices.getConfiguration().setJournal(null);

        ds1 = new PoolingDataSource();
        ds1.setUniqueName(DATA_SOURCE_NAME);
        ds1.setClassName("org.h2.jdbcx.JdbcDataSource");
        ds1.setMaxPoolSize(10);
        ds1.setAllowLocalTransactions(true);
        ds1.getDriverProperties().put("URL", "jdbc:h2:mem:database;LOCK_MODE=0;MVCC=TRUE;DB_CLOSE_ON_EXIT=FALSE;IGNORECASE=TRUE;AUTOCOMMIT=OFF;");
        ds1.getDriverProperties().put("user", "sa");
        ds1.getDriverProperties().put("password", "");
        ds1.init();
    }

    @AfterClass
    public static void closeDataSource() {
        ds1.close();
        TransactionManagerServices.getTransactionManager().shutdown();
    }

    public void setUpDatabase() throws Exception {
        databaseTester = new DataSourceDatabaseTester(ds1);
        final InputStream stream = JPABusinessDataRepositoryImplIT.class.getResourceAsStream("/dataset.xml");
        final FlatXmlDataSet dataSet = new FlatXmlDataSetBuilder().build(stream);
        stream.close();
        databaseTester.setDataSet(dataSet);
        databaseTester.setSetUpOperation(DatabaseOperation.CLEAN_INSERT);
        databaseTester.onSetup();
    }

    @After
    public void tearDown() throws Exception {
        if (databaseTester != null) {
            final UserTransaction ut = TransactionManagerServices.getTransactionManager();
            ut.begin();
            databaseTester.setTearDownOperation(DatabaseOperation.DELETE_ALL);
            databaseTester.onTearDown();
            ut.commit();
        }
    }

    @Before
    public void setUp() throws Exception {
        dependencyService = mock(DependencyService.class);
        bdrArchive = IOUtil.getAllContentFrom(JPABusinessDataRepositoryImplIT.class.getResourceAsStream("/bdr-jar.bak"));
        final Map<String, Object> configuration = new HashMap<String, Object>();
        configuration.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        configuration.put("hibernate.connection.datasource", DATA_SOURCE_NAME);

        businessDataRepository = spy(new JPABusinessDataRepositoryImpl(dependencyService, configuration));
        doReturn(Collections.singletonList("com.bonitasoft.pojo.Employee")).when(businessDataRepository).getClassNameList();
        doReturn(null).when(businessDataRepository).createSDependency(any(byte[].class));
        doReturn(null).when(businessDataRepository).createDependencyMapping(anyLong(), any(SDependency.class));
    }

    @Test
    public void findAnEmployeeByPrimaryKey() throws Exception {
        final UserTransaction ut = TransactionManagerServices.getTransactionManager();
        try {
            ut.begin();
            businessDataRepository.deploy(bdrArchive, 1);
            businessDataRepository.start();
            setUpDatabase();
            final Employee employee = businessDataRepository.find(Employee.class, 45l);
            assertThat(employee).isNotNull();
            assertThat(employee.getId()).isEqualTo(45l);
            assertThat(employee.getFirstName()).isEqualTo("Hannu");
            assertThat(employee.getLastName()).isEqualTo("Hakkinen");
        } finally {
            businessDataRepository.stop();
            ut.commit();
        }
    }

    @Test(expected = BusinessDataNotFoundException.class)
    public void throwExceptionWhenEmployeeNotFound() throws Exception {
        final UserTransaction ut = TransactionManagerServices.getTransactionManager();
        try {
            ut.begin();
            businessDataRepository.start();
            businessDataRepository.find(Employee.class, -145l);
        } finally {
            businessDataRepository.stop();
            ut.commit();
        }
    }

    @Test
    public void persistNewEmployee() throws Exception {
        final UserTransaction ut = TransactionManagerServices.getTransactionManager();
        final Employee employee;
        try {
            ut.begin();
            businessDataRepository.start();
            employee = businessDataRepository.merge(new Employee("Marja", "Halonen"));
        } finally {
            businessDataRepository.stop();
            ut.commit();
        }
        assertThat(employee.getId()).isNotNull();
    }

    @Test
    public void persistANullEmployee() throws Exception {
        final UserTransaction ut = TransactionManagerServices.getTransactionManager();
        try {
            ut.begin();
            businessDataRepository.start();
            businessDataRepository.merge(null);
            final Long count = businessDataRepository.find(Long.class, "SELECT COUNT(*) FROM Employee e", null);
            assertThat(count).isEqualTo(0);
        } finally {
            businessDataRepository.stop();
            ut.commit();
        }
    }

    @Test
    public void findAnEmployeeUsingParameterizedQuery() throws Exception {
        final UserTransaction ut = TransactionManagerServices.getTransactionManager();
        try {
            ut.begin();
            businessDataRepository.start();
            setUpDatabase();
            final Map<String, Object> parameters = Collections.singletonMap("firstName", (Object) "Matti");
            final Employee matti = businessDataRepository.find(Employee.class, "FROM Employee e WHERE e.firstName = :firstName", parameters);
            assertThat(matti.getFirstName()).isEqualTo("Matti");
        } finally {
            businessDataRepository.stop();
            ut.commit();
        }
    }

    @Test(expected = NonUniqueResultException.class)
    public void throwExceptionWhenFindingAnEmployeeButGettingSeveral() throws Exception {
        final UserTransaction ut = TransactionManagerServices.getTransactionManager();
        try {
            ut.begin();
            businessDataRepository.start();
            setUpDatabase();
            final Map<String, Object> parameters = Collections.singletonMap("lastName", (Object) "Hakkinen");
            businessDataRepository.find(Employee.class, "FROM Employee e WHERE e.lastName = :lastName", parameters);
        } finally {
            businessDataRepository.stop();
            ut.commit();
        }
    }

    @Test(expected = BusinessDataNotFoundException.class)
    public void throwExceptionWhenFindingAnUnknownEmployee() throws Exception {
        final UserTransaction ut = TransactionManagerServices.getTransactionManager();
        try {
            ut.begin();
            businessDataRepository.start();
            setUpDatabase();
            final Map<String, Object> parameters = Collections.singletonMap("lastName", (Object) "Makkinen");
            businessDataRepository.find(Employee.class, "FROM Employee e WHERE e.lastName = :lastName", parameters);
        } finally {
            businessDataRepository.stop();
            ut.commit();
        }
    }

    @Test(expected = IllegalStateException.class)
    public void throwExceptionWhenUsingBDRWihtoutStartingIt() throws Exception {
        final UserTransaction ut = TransactionManagerServices.getTransactionManager();
        try {
            ut.begin();
            final Map<String, Object> parameters = Collections.singletonMap("lastName", (Object) "Makkinen");
            businessDataRepository.find(Employee.class, "FROM Employee e WHERE e.lastName = :lastName", parameters);
        } finally {
            ut.commit();
        }
    }

    @Test
    public void updateAnEmployeeUsingParameterizedQuery() throws Exception {
        UserTransaction ut = TransactionManagerServices.getTransactionManager();
        try {
            ut.begin();
            businessDataRepository.start();
            setUpDatabase();
            final Map<String, Object> parameters = Collections.singletonMap("firstName", (Object) "Matti");
            final Employee matti = businessDataRepository.find(Employee.class, "FROM Employee e WHERE e.firstName = :firstName", parameters);
            matti.setLastName("Hallonen");
            businessDataRepository.merge(matti);
            matti.setLastName("Halonen");
            businessDataRepository.merge(matti);
        } finally {
            ut.commit();
        }

        ut = TransactionManagerServices.getTransactionManager();
        try {
            ut.begin();
            final Map<String, Object> parameters = Collections.singletonMap("firstName", (Object) "Matti");
            final Employee matti = businessDataRepository.find(Employee.class, "FROM Employee e WHERE e.firstName = :firstName", parameters);
            assertThat(matti.getLastName()).isEqualTo("Halonen");
        } finally {
            businessDataRepository.stop();
            ut.commit();
        }
    }

}
