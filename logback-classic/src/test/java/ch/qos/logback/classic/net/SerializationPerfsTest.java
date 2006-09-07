package ch.qos.logback.classic.net;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

import junit.framework.TestCase;
import ch.qos.logback.classic.net.testObjectBuilders.Builder;
import ch.qos.logback.classic.net.testObjectBuilders.LoggingEventBuilder;
import ch.qos.logback.classic.net.testObjectBuilders.MinimalSerBuilder;

public class SerializationPerfsTest extends TestCase {

	ObjectOutputStream oos;

	int loopNumber = 10000;
	int resetFrequency = 500;
	int pauseFrequency = 500;
	long pauseLengthInMillis = 20;

	/**
	 * <p>
	 * Run the test with a MockSocketServer or with a NOPOutputStream
	 * </p>
	 * <p>
	 * Run with external mock can be done using the ExternalMockSocketServer. It
	 * needs to be launched from a separate JVM. The ExternalMockSocketServer does
	 * not consume the events but passes through the available bytes that it is
	 * recieving.
	 * </p>
	 * <p>
	 * For example, with 4 test methods, you can launch the
	 * ExternalMockSocketServer this way:
	 * </p>
	 * <p>
	 * <code>java ch.qos.logback.classic.net.ExternalMockSocketServer 4</code>
	 * </p>
	 */
	boolean runWithExternalMockServer = true;

	/**
	 * Last results:
	 * Data sent mesured in bytes.
	 * Avg time mesured in nanos.
	 * 
	 * NOPOutputStream: 
	 *   |                |  Runs | Avg time | Data sent |
	 *   | MinimalObj Ext | 10000 |  6511    |           |
	 *   | MinimalObj Ser | 10000 |  7883    |           |
	 *   | LoggEvent Ext  | 10000 |  9641    |           |
	 *   | LoggEvent Ser  | 10000 | 25729    |           |
	 * 
	 * External MockServer with 45 letters-long message: 
	 * 	 |                |  Runs | Avg time | Data sent |
	 *   | MinimalObj Ext | 10000 |  70240   | 1171384   |
	 *   | MinimalObj Ser | 10000 |  62754   | 1157584   |
	 *   | LoggEvent Ext  | 10000 | 198910   | 1509984   |
	 *   | LoggEvent Ser  | 10000 | 189970   | 1715984   |
	 *	 pauseFrequency = 200 and pauseLengthInMillis = 50
	 *
	 * External MockServer with 2 letters-long message: 
	 * 	 |                |  Runs | Avg time | Data sent |
	 *   | MinimalObj Ext | 10000 |  43234   |  311384   |
	 *   | MinimalObj Ser | 10000 |  31603   |  297584   |
	 *   | LoggEvent Ext  | 10000 | 106442   |  649984   |
	 *   | LoggEvent Ser  | 10000 |  93467   |  855984   |
	 *	 pauseFrequency = 200 and pauseLengthInMillis = 50
	 *
	 * External MockServer with 45 letters-long message:
	 * This test was done by sending always the _same_ loggerEvent object.
	 * 	 |                |  Runs | Avg time | Data sent |
	 *   | MinimalObj Ext | 10000 |  28739   |  123604   |
	 *   | MinimalObj Ser | 10000 |  27431   |  129604   |
	 *   | LoggEvent Ext  | 10000 |  30112   |  125604   |
	 *   | LoggEvent Ser  | 10000 |  26059   |  153404   |
	 *	 pauseFrequency = 500 and pauseLengthInMillis = 50
	 */

	public void setUp() throws Exception {
		super.setUp();
		if (runWithExternalMockServer) {
			oos = new ObjectOutputStream(new Socket("localhost",
					ExternalMockSocketServer.PORT).getOutputStream());
		} else {
			oos = new ObjectOutputStream(new NOPOutputStream());
		}
	}

	public void tearDown() throws Exception {
		super.tearDown();
		oos.close();
		oos = null;
	}

	public void runPerfTest(Builder builder, String label) throws Exception {
		// long time1 = System.nanoTime();

		// Object builtObject = builder.build(1);
		
		// first run for just in time compiler
		int resetCounter = 0;
		int pauseCounter = 0;
		for (int i = 0; i < loopNumber; i++) {
			try {
				oos.writeObject(builder.build(i));
				oos.flush();
				if (++resetCounter >= resetFrequency) {
					oos.reset();
					resetCounter = 0;
				}
				if (++pauseCounter >= pauseFrequency) {
					Thread.sleep(pauseLengthInMillis);
					pauseCounter = 0;
				}

			} catch (IOException ex) {
				fail(ex.getMessage());
			}
		}

		// second run
		Long t1;
		Long t2;
		Long total = 0L;
		resetCounter = 0;
		pauseCounter = 0;
		// System.out.println("Beginning mesured run");
		for (int i = 0; i < loopNumber; i++) {
			try {
				t1 = System.nanoTime();
				oos.writeObject(builder.build(i));
				oos.flush();
				t2 = System.nanoTime();
				total += (t2 - t1);
				if (++resetCounter >= resetFrequency) {
					oos.reset();
					resetCounter = 0;
				}
				if (++pauseCounter >= pauseFrequency) {
					Thread.sleep(pauseLengthInMillis);
					pauseCounter = 0;
				}
			} catch (IOException ex) {
				fail(ex.getMessage());
			}
		}
		System.out.println(label + " : average time = " + total / loopNumber
				+ " after " + loopNumber + " writes.");

		// long time2 = System.nanoTime();
		// System.out.println("********* -> Time needed to run the test method: " +
		// Long.toString(time2-time1));
	}

//	public void testWithMinimalExternalization() throws Exception {
//		Builder builder = new MinimalExtBuilder();
//		runPerfTest(builder, "Minimal object externalization");
//	}

	public void testWithMinimalSerialization() throws Exception {
		Builder builder = new MinimalSerBuilder();
		runPerfTest(builder, "Minimal object serialization");
	}

//	public void testWithExternalization() throws Exception {
//		Builder builder = new LoggingEventExtBuilder();
//		runPerfTest(builder, "LoggingEvent object externalization");
//	}

	public void testWithSerialization() throws Exception {
		Builder builder = new LoggingEventBuilder();
		runPerfTest(builder, "LoggingEvent object serialization");
	}
}
