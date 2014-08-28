package yourgroupid;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.TransactionalMap;
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
import com.hazelcast.transaction.TransactionOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;

public class MapTransactionTest {

    private final static ILogger log = Logger.getLogger(MapTransactionTest.class);

    //props
    public int threadCount = 25;
    public int keyCount = 1000;
    public int logFrequency = 10000;
    public int performanceUpdateFrequency = 10000;
    public String basename = "txmap";

    private IMap<Integer, Long> map;
    private final AtomicLong operations = new AtomicLong();
    private IMap<String, Map<Integer, Long>> resultsPerWorker;
    private HazelcastInstance targetInstance;
    private String mapName;
    private TestContext testContext;

    @Setup
    public void setup(TestContext testContext) throws Exception {
        this.testContext = testContext;
        targetInstance = testContext.getTargetInstance();
        mapName = basename + "-" + testContext.getTestId();
        map = targetInstance.getMap(mapName);
        resultsPerWorker = targetInstance.getMap(basename + "ResultMap" + testContext.getTestId());
    }

    @Teardown
    public void teardown() throws Exception {
        map.destroy();
        resultsPerWorker.destroy();
    }

    @Warmup(global = true)
    public void warmup() throws Exception {
        Thread.sleep(5000);
        for (int k = 0; k < keyCount; k++) {
            map.put(k, 0l);
        }
    }

    @Run
    public void run() {
        ThreadSpawner spawner = new ThreadSpawner(testContext.getTestId());
        for (int k = 0; k < threadCount; k++) {
            spawner.spawn(new Worker());
        }
        spawner.awaitCompletion();
    }

    @Verify(global = true)
    public void verify() throws Exception {
        long[] amount = new long[keyCount];

        for (Map<Integer, Long> map : resultsPerWorker.values()) {
            for (Map.Entry<Integer, Long> entry : map.entrySet()) {
                amount[entry.getKey()] += entry.getValue();
            }
        }

        int failures = 0;
        for (int k = 0; k < keyCount; k++) {
            long expected = amount[k];
            long found = map.get(k);
            if (expected != found) {
                failures++;
            }
        }

        assertEquals("There should not be any data races", 0, failures);
    }

    @Performance
    public long getOperationCount() {
        return operations.get();
    }

    private class Worker implements Runnable {
        private final Random random = new Random();
        private final Map<Integer, Long> result = new HashMap<Integer, Long>();

        @Override
        public void run() {
            for (int k = 0; k < keyCount; k++) {
                result.put(k, 0L);
            }

            long iteration = 0;
            while (!testContext.isStopped()) {
                final Integer key = random.nextInt(keyCount);
                final long increment = random.nextInt(100);

                final TransactionOptions options = new TransactionOptions().setTimeout(5, TimeUnit.MINUTES);
                final TransactionContext txContext = targetInstance.newTransactionContext(options);
                try {
                    txContext.beginTransaction();
                    TransactionalMap<Integer, Long> map = txContext.getMap(mapName);
                    Long current = map.getForUpdate(key);
                    Long update = current + increment;
                    map.put(key, update);
                    txContext.commitTransaction();
                    increment(key, increment);
                } catch (Exception e) {
                    System.err.println("------------------------------------------" +
                            "\n ------ " + e.getMessage() + " ------------" +
                            "\n ------------------------------------------");
                    try {
                        txContext.rollbackTransaction();
                    } catch (Exception e1) {
                        System.err.println("*******************************************" +
                                "\n ****** " + e1.getMessage() + " ************" +
                                "\n *******************************************");
                    }
                }

                if (iteration % logFrequency == 0) {
                    log.info(Thread.currentThread().getName() + " At iteration: " + iteration);
                }

                if (iteration % performanceUpdateFrequency == 0) {
                    operations.addAndGet(performanceUpdateFrequency);
                }

                iteration++;
            }

            resultsPerWorker.put(UUID.randomUUID().toString(), result);
        }

        private void increment(int key, long increment) {
            result.put(key, result.get(key) + increment);
        }
    }

    public static void main(String[] args) throws Throwable {
        MapTransactionTest test = new MapTransactionTest();
        new TestRunner(test).run();
    }
}

