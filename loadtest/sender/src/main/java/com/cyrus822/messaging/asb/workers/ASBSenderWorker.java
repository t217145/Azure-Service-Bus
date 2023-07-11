package com.cyrus822.messaging.asb.workers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.azure.core.amqp.exception.AmqpException;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.cyrus822.messaging.asb.domains.ASBMsg;
import com.cyrus822.messaging.asb.domains.ASBSenderWorkerObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CountDownLatch;

public class ASBSenderWorker implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ASBSenderWorker.class);

    private int id;
    private DateFormat dtmFmt;

    // App Setting
    private int msgCntPerThread;
    private boolean enableSession;
    private ServiceBusSenderClient[] clients;
    private ServiceBusMessage[] asbMsgs;
    private CountDownLatch latch;

    public ASBSenderWorker(int _id, ASBSenderWorkerObject obj) {
        this.id = _id;
        this.clients = obj.getClients();
        this.latch = obj.getLatch();
        this.msgCntPerThread = obj.getMsgCntPerThread();
        this.enableSession = obj.isEnableSession();
        this.dtmFmt = new SimpleDateFormat("yyyy-mm-dd HH:mm:ss");
        this.asbMsgs = new ServiceBusMessage[this.clients.length * this.msgCntPerThread];
    }

    public void run() {
        try {
            int ctr = 0;
            String msg = "";
            for (int clientCnt = 1; clientCnt <= clients.length; clientCnt++) {
                for (int cnt = 1; cnt <= msgCntPerThread; cnt++) {
                    // Step-3 : prepare the message
                    LOGGER.info("[Start::ASBSenderWorker::run()::Step-3a");
                    Date dtm = new Date();
                    msg = String.format("Thread::%d::MsgCnt::%d::Timestamp:%s", this.id, cnt, dtmFmt.format(dtm));
                    LOGGER.info("[Start::ASBSenderWorker::run()::Step-3b::Thread::{}]", msg);

                    // Step-4 : prepare the ServiceBusMessage
                    LOGGER.info("[Start::ASBSenderWorker::run()::Step-4]");
                    String ctxId = enableSession ? String.format("ctx-%d-%d", id, (cnt % 10)) : "";
                    ASBMsg msgContent = new ASBMsg(ctxId, cnt, dtm, msg);
                    String paymentJSON = new ObjectMapper().writeValueAsString(msgContent);
                    ServiceBusMessage asbMsg = new ServiceBusMessage(paymentJSON.getBytes(UTF_8));

                    if (!ctxId.isEmpty()) {
                        asbMsg = asbMsg.setSessionId(ctxId);
                        LOGGER.info("[Start::ASBSenderWorker::run()::Step-4b::Session Id::{}]", ctxId);
                    }

                    // Step-5 : Add message to batch out the message
                    LOGGER.info("[Start::ASBSenderWorker::run()::Step-5]");
                    this.asbMsgs[ctr] = asbMsg;
                    ctr++;
                } // loop to generate message
            }

            latch.await();

            ctr = 0;
            for (int clientCnt = 1; clientCnt <= clients.length; clientCnt++) {
                for (int cnt = 1; cnt <= msgCntPerThread; cnt++) {
                    try {
                        clients[clientCnt - 1].sendMessage(this.asbMsgs[ctr]);
                        ctr++;
                    } catch (AmqpException ae) {
                        LOGGER.error("AmqpException found", ae);
                        return;
                    } // end of try-catch
                } // inner for loop : msgCntPerThread
                clients[clientCnt - 1].close();
            } // outer for loop : clients.length
        } catch (Exception e) {
            LOGGER.error("Error in ASBSenderWorker", e);
        } // end of try-catch
    }// end of run()
}// end of class
