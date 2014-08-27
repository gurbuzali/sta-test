package yourgroupid;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import com.hazelcast.core.IQueue;
import com.hazelcast.core.TransactionalQueue;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;
import com.hazelcast.stabilizer.tests.TestContext;
import com.hazelcast.stabilizer.tests.TestRunner;
import com.hazelcast.stabilizer.tests.annotations.Performance;
import com.hazelcast.stabilizer.tests.annotations.Run;
import com.hazelcast.stabilizer.tests.annotations.Setup;
import com.hazelcast.stabilizer.tests.annotations.Teardown;
import com.hazelcast.stabilizer.tests.annotations.Verify;
import com.hazelcast.stabilizer.tests.utils.ThreadSpawner;
import com.hazelcast.transaction.TransactionContext;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * This stabilizer test simulates the issue #2287
 */
public class TxnQueueWithLockTest {

    private final static ILogger log = Logger.getLogger(TxnQueueWithLockTest.class);

    static final String FIRST_LOCK_NAME = "firstLock";
    static final String SECOND_LOCK_NAME = "secondLock";
    static final String QUEUE_NAME = "queue";
    static final int THREAD_COUNT = 5;
    static final int LOG_FREQUENCY = 10000;
    static final int PERFORMANCE_UPDATE_FREQUENCY = 10000;

    private AtomicLong operations = new AtomicLong();
    private AtomicLong counter = new AtomicLong();
    TestContext testContext = null;
    ILock firstLock = null;
    ILock secondLock = null;
    IQueue queue = null;

    @Setup
    public void setup(TestContext testContext) throws Exception {
        this.testContext = testContext;
        final HazelcastInstance instance = testContext.getTargetInstance();
        firstLock = instance.getLock(FIRST_LOCK_NAME);
        secondLock = instance.getLock(SECOND_LOCK_NAME);
        queue = instance.getQueue(QUEUE_NAME);
    }

    @Run
    public void run() {
        ThreadSpawner spawner = new ThreadSpawner(testContext.getTestId());
        for (int k = 0; k < THREAD_COUNT; k++) {
            spawner.spawn(new Worker());
        }
        spawner.awaitCompletion();
    }

    @Verify
    public void verify() {
        assertFalse(firstLock.isLocked());
        assertFalse(secondLock.isLocked());
        final int queueSize = queue.size();
        final long iterations = counter.get();
        assertEquals(iterations, queueSize);
    }

    @Teardown
    public void teardown() throws Exception {
        firstLock.destroy();
        secondLock.destroy();
        queue.destroy();
    }

    @Performance
    public long getOperationCount() {
        return operations.get();
    }

    private class Worker implements Runnable {
        @Override
        public void run() {
            long iteration = 0;
            final HazelcastInstance instance = testContext.getTargetInstance();
            while (!testContext.isStopped()) {
                firstLock.lock();
                TransactionContext ctx = instance.newTransactionContext();
                ctx.beginTransaction();
                try {
                    TransactionalQueue<Integer> queue = ctx.getQueue(QUEUE_NAME);
                    queue.offer(1);
                    secondLock.lock();
                    secondLock.unlock();
                    ctx.commitTransaction();
                    iteration++;
                } catch (Exception e) {
                    ctx.rollbackTransaction();
                } finally {
                    firstLock.unlock();
                }
                if (iteration % LOG_FREQUENCY == 0) {
                    log.info(Thread.currentThread().getName() + "qwe At iteration: " + iteration);
                }

                if (iteration % PERFORMANCE_UPDATE_FREQUENCY == 0) {
                    operations.addAndGet(PERFORMANCE_UPDATE_FREQUENCY);
                }
            }
            counter.addAndGet(iteration);
        }

    }

    public static void main(String[] args) throws Throwable {
        TxnQueueWithLockTest test = new TxnQueueWithLockTest();
        new TestRunner(test).run();
    }


}
