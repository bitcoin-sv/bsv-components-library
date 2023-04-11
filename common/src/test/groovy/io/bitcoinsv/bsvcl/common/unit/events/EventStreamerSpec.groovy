package io.bitcoinsv.bsvcl.common.unit.events

import io.bitcoinsv.bsvcl.common.events.Event
import io.bitcoinsv.bsvcl.common.events.EventBus
import io.bitcoinsv.bsvcl.common.events.EventStreamer
import io.bitcoinsv.bsvcl.common.thread.ThreadUtils
import spock.lang.Ignore
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer

/**
 * Testing class for the EventStreamer
 */
@Ignore
class EventStreamerSpec extends Specification {

    class TestEvent extends Event {
        String id;
        TestEvent(String id) { this.id = id;}
    }

    /**
     * We test that se can assign multiple handlers for the same Event, and that they are all properly triggered, and
     * they are NOT blocked by each other.
     * NOTE: A more complex verification here can be developed, for now we are just checking that multiple consumers can
     * be triggered within the same EventStreamer.
     */
    def "testing multiple Consumers"() {
        given:
            final int MAX_EVENTS = 30
            // We'll define 2 Consumers. If this is TRUE, then the Consumer 1 will BLOCK, adding a long delay, so only
            // one execution of CONSUMER1 will be executed.
            final boolean CONSUMER2_BLOCK = true;

            // EventStreamer Configuration:
            EventBus eventBus = EventBus.builder().executor(ThreadUtils.getSingleThreadExecutorService("testing")).build()
            EventStreamer<TestEvent> streamer = new EventStreamer<>(eventBus, TestEvent.class, 2)

            // Handlers/Event Consumers definition:

            AtomicBoolean anyConsumer1Called = new AtomicBoolean()
            AtomicBoolean anyConsumer2Called = new AtomicBoolean()


            // First Handler: We print the Event and add some delay:
            Consumer<TestEvent> consumer1 = {e ->
                anyConsumer2Called.set(true)
                println("Consumer1 :: Received Event " + e.id)
                println("Consumer1 :: Waiting...")
                // Now we wait. If CONSUMER2_BLOCK = true, then we wait until the all Test is over.
                if (CONSUMER2_BLOCK) Thread.sleep(1000 * 100)  // a very long delay
                else Thread.sleep(1000)                 // a short delay
                println("Consumer1 :: Done.")
            }

            // First handler: We just print the Event:
            Consumer<TestEvent> consumer2 = {e ->
                anyConsumer1Called.set(true)
                println("Consumer2 :: Received Event " + e.id)
            }

            streamer.forEach({e -> consumer1.accept(e)})
            streamer.forEach({e -> consumer2.accept(e)})

        when:
            // We trigger several Event to this EventBus
            int numEventsTriggered = 0;
            while (numEventsTriggered < MAX_EVENTS) {
                TestEvent event = new TestEvent(String.valueOf(numEventsTriggered));
                println(">> Triggering Event " + event.id + "...")
                eventBus.publish(event)
                numEventsTriggered++;
                Thread.sleep(100)
            }
        then:
            // We check that both Consumers are called.
            // At this moment we do NOT perform any more complex verifications:
            anyConsumer1Called.get()
            anyConsumer2Called.get()
    }
}
