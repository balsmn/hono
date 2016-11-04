/**
 * Copyright (c) 2016 Bosch Software Innovations GmbH.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Bosch Software Innovations GmbH - initial creation
 */
package org.eclipse.hono.telemetry.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.qpid.proton.message.Message;
import org.eclipse.hono.server.DownstreamAdapter;
import org.eclipse.hono.server.UpstreamReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonDelivery;
import io.vertx.proton.ProtonHelper;
import io.vertx.proton.ProtonSender;

/**
 * A telemetry adapter that simply logs and discards all messages.
 * <p>
 * It can be configured to pause a sender periodically for a certain amount of time in order
 * to see if the sender implements flow control correctly.
 */
@Service
@Profile("standalone")
public final class MessageDiscardingTelemetryDownstreamAdapter implements DownstreamAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(MessageDiscardingTelemetryDownstreamAdapter.class);
    private static final int DEFAULT_CREDIT = 10;
    private final Vertx vertx;
    private final int pauseThreshold;
    private final long pausePeriod;
    private Map<String, LinkStatus> statusMap = new HashMap<>();
    private Consumer<Message> messageConsumer;

    public MessageDiscardingTelemetryDownstreamAdapter(final Vertx vertx) {
        this(vertx, null);
    }

    /**
     * 
     * @param consumer a consumer that is invoked for every message received.
     */
    public MessageDiscardingTelemetryDownstreamAdapter(final Vertx vertx, final Consumer<Message> consumer) {
        this(vertx, 0, 0, consumer);
    }

    /**
     * @param pauseThreshold the number of messages after which the sender is paused. If set to 0 (zero) the sender will
     *                       never be paused.
     * @param pausePeriod the number of milliseconds after which the sender is resumed.
     */
    public MessageDiscardingTelemetryDownstreamAdapter(final Vertx vertx, final int pauseThreshold, final long pausePeriod, final Consumer<Message> consumer) {
        this.vertx = vertx;
        this.pauseThreshold = pauseThreshold;
        this.pausePeriod = pausePeriod;
        this.messageConsumer = consumer;
    }

    @Override
    public void start(Future<Void> startFuture) {
        startFuture.complete();
    }

    @Override
    public void stop(Future<Void> stopFuture) {
        stopFuture.complete();
    }

    @Override
    public void getDownstreamSender(UpstreamReceiver client, Handler<AsyncResult<ProtonSender>> resultHandler) {
        client.replenish(DEFAULT_CREDIT);
        resultHandler.handle(Future.succeededFuture());
    }

    @Override
    public void onClientDetach(final UpstreamReceiver client) {
    }

    /**
     * Sets the consumer for telemetry messages received from upstream.
     * 
     * @param consumer a consumer that is invoked for every message received.
     */
    public void setMessageConsumer(final Consumer<Message> consumer) {
        this.messageConsumer = consumer;
    }

    @Override
    public void onClientDisconnect(final ProtonConnection con) {
    }

    @Override
    public void processMessage(final UpstreamReceiver client, final ProtonDelivery delivery, final Message data) {

        LinkStatus status = statusMap.get(client.getLinkId());
        if (status == null) {
            LOG.debug("creating new link status object [{}]", client.getLinkId());
            status = new LinkStatus(client);
            statusMap.put(client.getLinkId(), status);
        }
        LOG.debug("processing telemetry data [id: {}, to: {}, content-type: {}]", data.getMessageId(), data.getAddress(),
                data.getContentType());
        if (messageConsumer != null) {
            messageConsumer.accept(data);
        }
        ProtonHelper.accepted(delivery, true);
        status.onMsgReceived();
    }

    private class LinkStatus {
        private long msgCount;
        private UpstreamReceiver client;
        private boolean suspended;

        public LinkStatus(final UpstreamReceiver client) {
            this.client = client;
        }

        public void onMsgReceived() {
            msgCount++;
            if (pauseThreshold > 0) {
                // we need to pause every n messages
                if (msgCount % pauseThreshold == 0) {
                    pause();
                }
            } else if (msgCount % DEFAULT_CREDIT == 0) {
                // we need to replenish client every DEFAULT_CREDIT messages
                client.replenish(DEFAULT_CREDIT);
            }
        }

        public void pause() {
            LOG.debug("pausing link [{}]", client.getLinkId());
            this.suspended = true;
            vertx.setTimer(pausePeriod, fired -> {
                vertx.runOnContext(run -> resume());
            });
        }

        private void resume() {
            if (suspended) {
                LOG.debug("resuming link [{}]", client.getLinkId());
                int credit = DEFAULT_CREDIT;
                if (pauseThreshold > 0) {
                    credit = pauseThreshold;
                }
                client.replenish(credit);
                this.suspended = false;
            }
        }
    }
}