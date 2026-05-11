package com.example.demo;

import org.springframework.stereotype.Service;

@Service
public class DistributedTransactionService {

    private final UserXaDao userXaDao;
    private final UserMongoDao userMongoDao;
    private final XATransactionCoordinator coordinator;

    public DistributedTransactionService(
            UserXaDao userXaDao,
            UserMongoDao userMongoDao,
            XATransactionCoordinator coordinator
    ) {

        this.userXaDao = userXaDao;
        this.userMongoDao = userMongoDao;
        this.coordinator = coordinator;
    }

    public void save(UserEntity user) {

        try {

            // ─────────────────────────────
            // BEGIN GLOBAL TX
            // ─────────────────────────────

            coordinator.begin();

            // ─────────────────────────────
            // H2 XA
            // ─────────────────────────────

            userXaDao.save(user);

            System.out.println(
                    "[APP] Usuário salvo no H2 XA"
            );

            // ─────────────────────────────
            // SIMULA FALHA
            // ─────────────────────────────

            if ("erro".equalsIgnoreCase(user.getName())) {

                throw new RuntimeException(
                        "Falha simulada Mongo"
                );
            }

            // ─────────────────────────────
            // MONGO
            // ─────────────────────────────

            userMongoDao.save(user);

            System.out.println(
                    "[APP] Usuário salvo no Mongo"
            );

            // ─────────────────────────────
            // END XA
            // ─────────────────────────────

            coordinator.delistAll();

            // ─────────────────────────────
            // PREPARE
            // ─────────────────────────────

            boolean ok = coordinator.prepare();

            // ─────────────────────────────
            // DECISÃO GLOBAL
            // ─────────────────────────────

            if (ok) {

                coordinator.commit();

                System.out.println(
                        "[APP] COMMIT GLOBAL"
                );

            } else {

                coordinator.rollback();

                System.out.println(
                        "[APP] ROLLBACK GLOBAL"
                );
            }

        } catch (Exception ex) {

            System.out.println(
                    "[APP] ERRO -> rollback global"
            );

            ex.printStackTrace();

            try {

                coordinator.rollback();

            } catch (Exception ignored) {
            }

            // ─────────────────────────────
            // SAGA COMPENSATÓRIA
            // ─────────────────────────────

            try {

                if (user.getId() != null) {

                    userMongoDao.delete(user.getId());
                }

            } catch (Exception ignored) {
            }

            throw new RuntimeException(ex);
        }
    }
}