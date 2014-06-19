package com.ldbc.driver.workloads.simple;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.ldbc.driver.*;
import com.ldbc.driver.control.ConsoleAndFileDriverConfiguration;
import com.ldbc.driver.control.DriverConfigurationException;
import com.ldbc.driver.control.LocalControlService;
import com.ldbc.driver.generator.GeneratorFactory;
import com.ldbc.driver.temporal.Duration;
import com.ldbc.driver.temporal.SystemTimeSource;
import com.ldbc.driver.temporal.TimeSource;
import com.ldbc.driver.util.RandomDataGeneratorFactory;
import com.ldbc.driver.workloads.simple.db.BasicDb;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class SimpleWorkloadTests {
    TimeSource TIME_SOURCE = new SystemTimeSource();

    @Test
    public void shouldBeRepeatableWhenSameWorkloadIsUsedTwiceWithIdenticalGeneratorFactories() throws ClientException, DriverConfigurationException, WorkloadException {
        Map<String, String> paramsMap = null;
        String dbClassName = null;
        String workloadClassName = null;
        long operationCount = 100;
        int threadCount = 1;
        boolean showStatus = false;
        TimeUnit timeUnit = TimeUnit.MILLISECONDS;
        String resultFilePath = null;
        Double timeCompressionRatio = 1.0;
        Duration gctDeltaDuration = Duration.fromMinutes(10);
        List<String> peerIds = Lists.newArrayList();
        Duration toleratedExecutionDelay = Duration.fromSeconds(1);
        boolean validateDatabase = false;
        boolean validateWorkload = false;
        boolean calculateWorkloadStatistics = false;
        ConsoleAndFileDriverConfiguration params =
                new ConsoleAndFileDriverConfiguration(
                        paramsMap,
                        dbClassName,
                        workloadClassName,
                        operationCount,
                        threadCount,
                        showStatus,
                        timeUnit,
                        resultFilePath,
                        timeCompressionRatio,
                        gctDeltaDuration,
                        peerIds,
                        toleratedExecutionDelay,
                        validateDatabase,
                        validateWorkload,
                        calculateWorkloadStatistics);

        Workload workload = new SimpleWorkload();
        workload.init(params);

        Function<Operation<?>, Class> classFun = new Function<Operation<?>, Class>() {
            @Override
            public Class apply(Operation<?> operation) {
                return operation.getClass();
            }
        };

        List<Class> operationsA = ImmutableList.copyOf(
                Iterators.transform(
                        workload.operations(new GeneratorFactory(new RandomDataGeneratorFactory(42L))),
                        classFun
                ));

        List<Class> operationsB = ImmutableList.copyOf(
                Iterators.transform(
                        workload.operations(new GeneratorFactory(new RandomDataGeneratorFactory(42L))),
                        classFun
                ));

        assertThat(operationsA.size(), is(operationsB.size()));

        Iterator<Class> operationsAIt = operationsA.iterator();
        Iterator<Class> operationsBIt = operationsB.iterator();

        while (operationsAIt.hasNext()) {
            Class a = operationsAIt.next();
            Class b = operationsBIt.next();
            assertThat(a, equalTo(b));
        }
        workload.cleanup();
    }

    @Test
    public void shouldBeRepeatableWhenTwoIdenticalWorkloadsAreUsedWithIdenticalGeneratorFactories() throws ClientException, DriverConfigurationException, WorkloadException {
        Map<String, String> paramsMap = null;
        String dbClassName = null;
        String workloadClassName = null;
        long operationCount = 100;
        int threadCount = 1;
        boolean showStatus = false;
        TimeUnit timeUnit = TimeUnit.MILLISECONDS;
        String resultFilePath = null;
        Double timeCompressionRatio = 1.0;
        Duration gctDeltaDuration = Duration.fromMinutes(10);
        List<String> peerIds = Lists.newArrayList();
        Duration toleratedExecutionDelay = Duration.fromSeconds(1);
        boolean validateDatabase = false;
        boolean validateWorkload = false;
        boolean calculateWorkloadStatistics = false;
        ConsoleAndFileDriverConfiguration params =
                new ConsoleAndFileDriverConfiguration(
                        paramsMap,
                        dbClassName,
                        workloadClassName,
                        operationCount,
                        threadCount,
                        showStatus,
                        timeUnit,
                        resultFilePath,
                        timeCompressionRatio,
                        gctDeltaDuration,
                        peerIds,
                        toleratedExecutionDelay,
                        validateDatabase,
                        validateWorkload,
                        calculateWorkloadStatistics);

        Workload workloadA = new SimpleWorkload();
        workloadA.init(params);

        Workload workloadB = new SimpleWorkload();
        workloadB.init(params);

        List<Class> operationsA = ImmutableList.copyOf(
                Iterators.transform(
                        workloadA.operations(new GeneratorFactory(new RandomDataGeneratorFactory(42L))),
                        new Function<Operation<?>, Class>() {
                            @Override
                            public Class apply(Operation<?> operation) {
                                return operation.getClass();
                            }
                        }));

        List<Class> operationsB = ImmutableList.copyOf(
                Iterators.transform(
                        workloadB.operations(new GeneratorFactory(new RandomDataGeneratorFactory(42L))),
                        new Function<Operation<?>, Class>() {
                            @Override
                            public Class apply(Operation<?> operation) {
                                return operation.getClass();
                            }
                        }));

        assertThat(operationsA.size(), is(operationsB.size()));

        Iterator<Class> operationsAIt = operationsA.iterator();
        Iterator<Class> operationsBIt = operationsB.iterator();

        while (operationsAIt.hasNext()) {
            Class a = operationsAIt.next();
            Class b = operationsBIt.next();
            assertThat(a, equalTo(b));
        }
    }

    @Test
    public void shouldLoadFromConfigFile() throws DriverConfigurationException, ClientException {
        String simpleTestPropertiesPath =
                new File("ldbc_driver/workloads/simple/simpleworkload.properties").getAbsolutePath();
        String ldbcDriverTestPropertiesPath =
                new File("ldbc_driver/src/main/resources/ldbc_driver_default.properties").getAbsolutePath();

        String resultFilePath = "test_write_to_csv_results.json";
        FileUtils.deleteQuietly(new File(resultFilePath));

        assertThat(new File(resultFilePath).exists(), is(false));

        assertThat(new File(simpleTestPropertiesPath).exists(), is(true));
        assertThat(new File(ldbcDriverTestPropertiesPath).exists(), is(true));

        ConsoleAndFileDriverConfiguration configuration = ConsoleAndFileDriverConfiguration.fromArgs(new String[]{
                "-" + ConsoleAndFileDriverConfiguration.RESULT_FILE_PATH_ARG, resultFilePath,
                "-" + ConsoleAndFileDriverConfiguration.DB_ARG, BasicDb.class.getName(),
                "-P", simpleTestPropertiesPath,
                "-P", ldbcDriverTestPropertiesPath});


        assertThat(new File(resultFilePath).exists(), is(false));


        // When
        Client client = new Client(new LocalControlService(TIME_SOURCE.now().plus(Duration.fromMilli(500)), configuration), TIME_SOURCE);
        client.start();

        // Then
        assertThat(new File(resultFilePath).exists(), is(true));
        FileUtils.deleteQuietly(new File(resultFilePath));
        assertThat(new File(resultFilePath).exists(), is(false));
    }
}