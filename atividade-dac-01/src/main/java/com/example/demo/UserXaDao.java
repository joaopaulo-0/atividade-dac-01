package com.example.demo;

import org.h2.jdbcx.JdbcDataSource;
import org.springframework.stereotype.Repository;

import javax.sql.XAConnection;
import javax.transaction.xa.XAException;
import java.sql.Connection;
import java.sql.PreparedStatement;

@Repository
public class UserXaDao {

    private final XATransactionCoordinator txCoordinator;
    private final UserMongoDao mongoDao;

    public UserXaDao(
            XATransactionCoordinator txCoordinator,
            UserMongoDao mongoDao
    ) {

        this.txCoordinator = txCoordinator;
        this.mongoDao = mongoDao;
    }

    public void save(UserEntity user) {

        XAConnection h2XaConn = null;

        try {

            // ─────────────────────────────
            // BEGIN
            // ─────────────────────────────

            txCoordinator.begin();

            // ─────────────────────────────
            // H2 XA RESOURCE
            // ─────────────────────────────

            JdbcDataSource ds = new JdbcDataSource();

            ds.setURL("jdbc:h2:mem:testdb");
            ds.setUser("sa");
            ds.setPassword("password");

            h2XaConn = ds.getXAConnection();

            txCoordinator.enlist(h2XaConn);

            Connection conn = h2XaConn.getConnection();

            PreparedStatement ps =
                    conn.prepareStatement(
                            "INSERT INTO user_entity(name) VALUES(?)"
                    );

            ps.setString(1, user.getName());

            ps.executeUpdate();

            System.out.println("[APP] Usuário salvo no H2");

            // ─────────────────────────────
            // SIMULA FALHA
            // ─────────────────────────────

            if(user.getName().equals("erro")) {

                throw new RuntimeException(
                        "Falha simulada Mongo"
                );
            }

            // ─────────────────────────────
            // MONGO
            // ─────────────────────────────

            mongoDao.save(user);

            System.out.println("[APP] Usuário salvo no Mongo");

            // ─────────────────────────────
            // END
            // ─────────────────────────────

            txCoordinator.delistAll();

            // ─────────────────────────────
            // PREPARE
            // ─────────────────────────────

            boolean ok = txCoordinator.prepare();

            // ─────────────────────────────
            // COMMIT / ROLLBACK
            // ─────────────────────────────

            if(ok) {

                txCoordinator.commit();

            } else {

                txCoordinator.rollback();
            }

        } catch (Exception ex) {

            ex.printStackTrace();

            try {

                txCoordinator.rollback();

            } catch (Exception e) {

                e.printStackTrace();
            }

            // SAGA COMPENSATÓRIA
            try {

                mongoDao.delete(user.getId());

            } catch(Exception ignored){}

            throw new RuntimeException(ex);
        }
    }
}