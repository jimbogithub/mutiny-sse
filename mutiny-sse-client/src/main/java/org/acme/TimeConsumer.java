package org.acme;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.Cancellable;

@QuarkusMain
public class TimeConsumer implements QuarkusApplication {

	private static final Logger LOG = LoggerFactory.getLogger(TimeConsumer.class);

	private final CountDownLatch latch = new CountDownLatch(20);

	@Inject
	@RestClient
	TimeBroadcasterClient client;

	@Override
	public int run(String... args) throws Exception {
		LOG.info("#run");
		Cancellable stream = client.ticker() //
				.onCancellation().invoke(this::onCancellation) //
				.onTermination().invoke(this::onTermination) //
				.subscribe().with(this::onItem, this::onFailure, this::onComplete);
		latch.await(30, TimeUnit.SECONDS);
		stream.cancel();
		return 0;
	}

	private void onCancellation() {
		LOG.info("#onCancellation");
	}

	private void onTermination(Throwable t, Boolean b) {
		LOG.info("#onTermination {}", b, t);
	}

	private void onItem(String item) {
		LOG.info(item);
		latch.countDown();
	}

	private void onFailure(Throwable t) {
		LOG.info("#onFailure", t);
		drainLatch();
	}

	private void onComplete() {
		LOG.info("#onComplete");
		drainLatch();
	}

	private void drainLatch() {
		while (latch.getCount() > 0) {
			latch.countDown();
		}
	}

	@RegisterRestClient(configKey = "time")
	@Path("time")
	public interface TimeBroadcasterClient {

		@GET
		@Path("ticker")
		@Produces(MediaType.SERVER_SENT_EVENTS)
		Multi<String> ticker();

	}

}
