package org.acme;

import java.time.Duration;
import java.time.Instant;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.RestStreamElementType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Multi;

@Path("time")
@Singleton
public class TimeBroadcaster {

	private static final Logger LOG = LoggerFactory.getLogger(TimeBroadcaster.class);

	private final Multi<String> ticker;

	public TimeBroadcaster() {
		LOG.info("#init {}", this);

		ticker = Multi.createFrom().ticks().every(Duration.ofSeconds(1)).map(t -> t + " : " + Instant.now().toString())
				.broadcast().toAllSubscribers();
	}

	@GET
	@Path("ticker")
	@Produces(MediaType.SERVER_SENT_EVENTS)
	@RestStreamElementType(MediaType.TEXT_PLAIN)
	public Multi<String> ticker() {
		LOG.info("#ticker");
		return ticker;
	}

}
