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
import com.hazelcast.stabilizer.tests.annotations.Warmup;
import com.hazelcast.stabilizer.tests.utils.ThreadSpawner;
import com.hazelcast.transaction.TransactionContext;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;

public class ExampleTest {

    private final static ILogger log = Logger.getLogger(ExampleTest.class);

    //props
    public int threadCount = 8;
    public int logFrequency = 10000;
    public int performanceUpdateFrequency = 10000;
    public static int ITEM_COUNT = 1000000;
    public static String IN_QUEUE_NAME = "inQueue";
    public static String OUT_QUEUE_NAME = "outQueue";

    private IQueue inQueue;
    private IQueue outQueue;

    private AtomicLong operations = new AtomicLong();
    private TestContext testContext;
    private AtomicLong counter = new AtomicLong();

    @Warmup(global = true)
    public void warmUp() {
        if (inQueue.size() == ITEM_COUNT) {
            return;
        }
        for (int i = 0; i < ITEM_COUNT; i++) {
            inQueue.offer("item-" + i);
            if (i % logFrequency == 0) {
                log.info("Inserting item: " + i);
            }
        }
    }

    @Setup
    public void setup(TestContext testContext) throws Exception {
        this.testContext = testContext;
        final HazelcastInstance instance = testContext.getTargetInstance();
        inQueue = instance.getQueue(IN_QUEUE_NAME);
        outQueue = instance.getQueue(OUT_QUEUE_NAME);
    }

    @Run
    public void run() {
        ThreadSpawner spawner = new ThreadSpawner(testContext.getTestId());
        for (int k = 0; k < threadCount; k++) {
            spawner.spawn(new Worker());
        }
        spawner.awaitCompletion();
    }

    @Verify
    public void verify() {
        final int inQueueSize = inQueue.size();
        final int outQueueSize = outQueue.size();
        final long iterations = counter.get();
        assertEquals(ITEM_COUNT - iterations, inQueueSize);
        assertEquals(iterations, outQueueSize);
    }

    @Teardown
    public void teardown() throws Exception {
        inQueue.destroy();
        outQueue.destroy();
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
                final TransactionContext context = instance.newTransactionContext();
                context.beginTransaction();
                final TransactionalQueue<String> inQueue = context.getQueue(IN_QUEUE_NAME);
                final TransactionalQueue<String> outQueue = context.getQueue(OUT_QUEUE_NAME);
                final String item = inQueue.poll();
                if (item == null) {
                    break;
                }
                outQueue.offer(item);
                context.commitTransaction();

                if (iteration % logFrequency == 0) {
                    log.info(Thread.currentThread().getName() + " At iteration: " + iteration);
                }

                if (iteration % performanceUpdateFrequency == 0) {
                    operations.addAndGet(performanceUpdateFrequency);
                }
                iteration++;
            }
            counter.addAndGet(iteration);
        }
    }

    public static void main(String[] args) throws Throwable {
        ExampleTest test = new ExampleTest();
        new TestRunner(test).run();
    }
}
