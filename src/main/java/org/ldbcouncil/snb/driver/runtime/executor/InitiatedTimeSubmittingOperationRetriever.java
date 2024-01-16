package org.ldbcouncil.snb.driver.runtime.executor;

import org.ldbcouncil.snb.driver.Operation;
import org.ldbcouncil.snb.driver.WorkloadStreams;
import org.ldbcouncil.snb.driver.runtime.coordination.CompletionTimeException;
import org.ldbcouncil.snb.driver.runtime.coordination.CompletionTimeWriter;

import java.util.Iterator;

// TODO test
class InitiatedTimeSubmittingOperationRetriever
{
    //只有IC IU 全都是nonDependencyOperations
    private final Iterator<Operation> nonDependencyOperations; // TimeMappingOperationGenerator
    private final Iterator<Operation> dependencyOperations;
    private final CompletionTimeWriter completionTimeWriter;
    private Operation nextNonDependencyOperation = null;
    private Operation nextDependencyOperation = null;

    InitiatedTimeSubmittingOperationRetriever( WorkloadStreams.WorkloadStreamDefinition streamDefinition,
            CompletionTimeWriter completionTimeWriter )
    {
        this.nonDependencyOperations = streamDefinition.nonDependencyOperations();
        this.dependencyOperations = streamDefinition.dependencyOperations();
        this.completionTimeWriter = completionTimeWriter;
    }

    boolean hasNextOperation()
    {
        return nonDependencyOperations.hasNext() || dependencyOperations.hasNext();
    }

    /*
    1. get next operation (both dependent & dependency)
    2. submit initiated time
    4. return operation with lowest scheduled start time
     */
    Operation nextOperation() throws OperationExecutorException, CompletionTimeException
    {
        if ( dependencyOperations.hasNext() && null == nextDependencyOperation )
        {
            nextDependencyOperation = dependencyOperations.next();
            
            // submit initiated time as soon as possible so /dependencies can advance as soon as possible
            completionTimeWriter.submitInitiatedTime( nextDependencyOperation.timeStamp() );
            if ( !dependencyOperations.hasNext() )
            {
                // after last write operation, submit highest possible IT to ensure that CT progresses
                // to time of highest CT write
                completionTimeWriter.submitInitiatedTime( Long.MAX_VALUE );
            }
        }
        if ( nonDependencyOperations.hasNext() && null == nextNonDependencyOperation )
        {
            nextNonDependencyOperation = nonDependencyOperations.next();
            //System.out.println("class name:" + nonDependencyOperations.getClass().getSimpleName());
            // no need to submit initiated time for an operation that should not write to CT
        }
        // return operation with lowest start time
        if ( null != nextDependencyOperation && null != nextNonDependencyOperation )
        {
            Operation nextOperation;
            if ( nextNonDependencyOperation.timeStamp() < nextDependencyOperation.timeStamp() )
            {
                nextOperation = nextNonDependencyOperation;
                nextNonDependencyOperation = null;
            }
            else
            {
                nextOperation = nextDependencyOperation;
            
                nextDependencyOperation = null;
            }
            return nextOperation;
        }
        else if ( null == nextDependencyOperation && null != nextNonDependencyOperation )
        {
            Operation nextOperation = nextNonDependencyOperation;
            nextNonDependencyOperation = null;
            return nextOperation;
        }
        else if ( null != nextDependencyOperation && null == nextNonDependencyOperation )
        {
            Operation nextOperation = nextDependencyOperation;
            nextDependencyOperation = null;
            return nextOperation;
        }
        else
        {
            throw new OperationExecutorException( "Unexpected error in " + getClass().getSimpleName() );
        }
    }
}
