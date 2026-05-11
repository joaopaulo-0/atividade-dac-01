package com.example.demo.service;

import com.example.demo.OutboxEvent;
import com.example.demo.UserEntity;
import com.example.demo.UserMongoDao;
import com.example.demo.OutboxRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OutboxProcessor {

    private final OutboxRepository outboxRepository;
    private final UserMongoDao mongoUserDao;

    public OutboxProcessor(OutboxRepository outboxRepository,
                           UserMongoDao mongoUserDao) {

        this.outboxRepository = outboxRepository;
        this.mongoUserDao = mongoUserDao;
    }

    @Scheduled(fixedDelay = 5000)
    public void process() {

        List<OutboxEvent> events =
                outboxRepository.findByProcessedFalse();

        for (OutboxEvent evt : events) {

            try {

                String[] data = evt.getPayload().split(";");

                UserEntity user = new UserEntity();

                user.setId(Long.parseLong(data[0]));
                user.setName(data[1]);
                user.setEmail(data[2]);

                mongoUserDao.save(user);

                evt.setProcessed(true);

                outboxRepository.save(evt);

                System.out.println("[OUTBOX] evento processado");

            } catch (Exception e) {

                System.out.println("[SAGA] erro no Mongo");
                System.out.println("[SAGA] compensação pode ocorrer aqui");
            }
        }
    }
}