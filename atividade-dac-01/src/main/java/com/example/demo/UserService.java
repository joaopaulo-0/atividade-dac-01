package com.example.demo;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final OutboxRepository outboxRepository;
    private final SagaLogRepository sagaLogRepository;

    public UserService(UserRepository userRepository,
                       OutboxRepository outboxRepository,
                       SagaLogRepository sagaLogRepository) {

        this.userRepository = userRepository;
        this.outboxRepository = outboxRepository;
        this.sagaLogRepository = sagaLogRepository;
    }

    @Transactional
    public void createUser(UserEntity user) {

        SagaLog saga = new SagaLog();

        saga.setStatus("STARTED");
        saga.setStepName("SAVE_H2");

        sagaLogRepository.save(saga);

        try {

            userRepository.save(user);

            System.out.println("[H2] usuário salvo");

            OutboxEvent evt = new OutboxEvent();

            evt.setEventType("USER_CREATED");
            evt.setPayload(user.getId() + ";" + user.getName() + ";" + user.getEmail());

            outboxRepository.save(evt);

            saga.setStatus("WAITING_OUTBOX");

            sagaLogRepository.save(saga);

        } catch (Exception e) {

            saga.setStatus("FAILED");

            sagaLogRepository.save(saga);

            throw e;
        }
    }

    public List<UserEntity> listUsers() {
        return userRepository.findAll();
    }
}