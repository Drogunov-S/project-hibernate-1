package com.game.repository;

import com.game.entity.Player;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.Root;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.query.NativeQuery;
import org.springframework.stereotype.Repository;

import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

@Repository(value = "db")
public class PlayerRepositoryDB implements IPlayerRepository {
    private final SessionFactory sessionFactory;

    private PlayerRepositoryDB() {
        Properties properties = new Properties();
//        properties.put(Environment.DRIVER, "com.mysql.cj.jdbc.Driver");
        properties.put(Environment.DRIVER, "com.p6spy.engine.spy.P6SpyDriver");
        properties.put(Environment.DIALECT, "org.hibernate.dialect.MySQL8Dialect");
//        properties.put(Environment.URL, "jdbc:mysql://localhost:3306/rpg");
        properties.put(Environment.URL, "jdbc:p6spy:mysql://localhost:3306/rpg");
        properties.put(Environment.USER, "root");
        properties.put(Environment.PASS, "root");
        properties.put(Environment.HBM2DDL_AUTO, "update");
//        properties.put(Environment.SHOW_SQL, "true");

        sessionFactory = new Configuration()
                .addProperties(properties)
                .addAnnotatedClass(Player.class)
                .buildSessionFactory();
    }

    @Override
    public List<Player> getAll(int pageNumber, int pageSize) {
        String query = "SELECT * FROM player";
        int firstLine = pageNumber * pageSize;
        int lastLine = pageNumber * pageSize + pageSize;

        try (Session session = sessionFactory.openSession()) {
            NativeQuery<Player> nativeQuery = session
                    .createNativeQuery(query, Player.class)
                    .setFirstResult(firstLine)
                    .setMaxResults(lastLine);
            List<Player> list = nativeQuery
                    .list();
            return list;
        }
    }


    @Override
    public int getAllCount() {
        try (Session session = sessionFactory.openSession()) {
            return Math.toIntExact(session
                    .createNamedQuery("getAllCountPlayers", Long.class)
                    .getSingleResult());
        }
    }

    @Override
    public Player save(Player player) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();
            Long id = (Long) session.save(player);
            //TODO Мне не нравится способ возврата объекта после сохранения.
            Player result = session.get(Player.class, id);
            transaction.commit();
            return result;
        }
    }

    @Override
    public Player update(Player player) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();
            session.saveOrUpdate(player);
            //TODO Мне не нравится способ возврата объекта после сохранения.
            Player result = session.get(Player.class, player.getId());
            transaction.commit();
            return result;
        }
    }

    @Override
    public Optional<Player> findById(long id) {
        try (Session session = sessionFactory.openSession()) {
            return Optional.ofNullable(session.get(Player.class, id));
        }
    }

    @Override
    public void delete(Player player) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();

            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaDelete<Player> query = builder.createCriteriaDelete(Player.class);
            Root<Player> root = query.from(Player.class);

            query.where(builder.lessThanOrEqualTo(root.get("id"), player.getId()));

            session.createQuery(query).executeUpdate();
            transaction.commit();
        }
    }

    @PreDestroy
    public void beforeStop() {
        sessionFactory.close();
    }
}