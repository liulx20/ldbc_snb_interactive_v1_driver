package org.ldbcouncil.snb.driver.runtime.executor;

import org.ldbcouncil.snb.driver.ChildOperationGenerator;
import org.ldbcouncil.snb.driver.Db;
import org.ldbcouncil.snb.driver.Operation;
import org.ldbcouncil.snb.driver.OperationHandlerRunnableContext;
import org.ldbcouncil.snb.driver.WorkloadStreams;
import org.ldbcouncil.snb.driver.runtime.ConcurrentErrorReporter;
import org.ldbcouncil.snb.driver.runtime.coordination.CompletionTimeReader;
import org.ldbcouncil.snb.driver.runtime.coordination.CompletionTimeWriter;
import org.ldbcouncil.snb.driver.runtime.metrics.MetricsService;
import org.ldbcouncil.snb.driver.runtime.scheduling.Spinner;
import org.ldbcouncil.snb.driver.temporal.TimeSource;

import java.util.concurrent.atomic.AtomicLong;

import static java.lang.String.format;

public class SameThreadOperationExecutor implements OperationExecutor
{
    private final AtomicLong uncompletedHandlers = new AtomicLong( 0 );
    private final OperationHandlerRunnableContextRetriever operationHandlerRunnableContextRetriever;
    private final ChildOperationGenerator childOperationGenerator;
    private final ChildOperationExecutor childOperationExecutor;

    public SameThreadOperationExecutor( Db db,
            WorkloadStreams.WorkloadStreamDefinition streamDefinition,
            CompletionTimeWriter completionTimeWriter,
            CompletionTimeReader completionTimeReader,
            Spinner spinner,
            TimeSource timeSource,
            ConcurrentErrorReporter errorReporter,
            MetricsService metricsService,
            ChildOperationGenerator childOperationGenerator )
    {
        this.childOperationExecutor = new ChildOperationExecutor();
        this.childOperationGenerator = childOperationGenerator;
        this.operationHandlerRunnableContextRetriever = new OperationHandlerRunnableContextRetriever(
                streamDefinition,
                db,
                completionTimeWriter,
                completionTimeReader,
                spinner,
                timeSource,
                errorReporter,
                metricsService );
    }

    // IU
    @Override
    public final void execute( Operation operation ) throws OperationExecutorException
    {
        System.out.println(uncompletedHandlers.getClass().getSimpleName());
        uncompletedHandlers.incrementAndGet();
        OperationHandlerRunnableContext operationHandlerRunnableContext = null;
        try
        {
            operationHandlerRunnableContext =
                    operationHandlerRunnableContextRetriever.getInitializedHandlerFor( operation );
            operationHandlerRunnableContext.run();
            //childOperationGenerator always == null
            childOperationExecutor.execute(
                    childOperationGenerator,
                    operationHandlerRunnableContext.operation(),
                    operationHandlerRunnableContext.resultReporter().result(),
                    operationHandlerRunnableContext.resultReporter().actualStartTimeAsMilli(),
                    operationHandlerRunnableContext.resultReporter().runDurationAsNano(),
                    operationHandlerRunnableContextRetriever
            );
            
        }
        catch ( Throwable e )
        {
            throw new OperationExecutorException(
                    format( "Error retrieving or executing handler\n" +
                            "Operation: %s\n" +
                            "Handler Context:%s",
                            operation,
                            operationHandlerRunnableContext ),
                    e
            );
        }
        finally
        {
            uncompletedHandlers.decrementAndGet();
            operationHandlerRunnableContext.cleanup();
        }
    }

    @Override
    synchronized public final void shutdown( long waitAsMilli ) throws OperationExecutorException
    {
    }

    @Override
    public long uncompletedOperationHandlerCount()
    {
        return uncompletedHandlers.get();
    }
}
