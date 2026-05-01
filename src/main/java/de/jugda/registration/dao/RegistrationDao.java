package de.jugda.registration.dao;

import de.jugda.registration.domain.Registration;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;

import java.util.List;

/**
 * @author Niko Köbler, http://www.n-k.de, @dasniko
 */
@ApplicationScoped
public class RegistrationDao {

    @Inject
    private EntityManager em;

    @Transactional
    public void save(Registration registration) {
        em.persist(registration);
    }

    public Registration findByEventIdAndEmail(Registration registration) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<Registration> cq = criteriaBuilder.createQuery(Registration.class);
        Root<Registration> root = cq.from(Registration.class);

        Predicate where = criteriaBuilder.and(criteriaBuilder.equal(root.get("eventId"), registration.getEventId()), criteriaBuilder.equal(root.get("email"), registration.getEmail()));
        cq.select(root).where(where);

        return em.createQuery(cq).getSingleResultOrNull();
    }

    public List<Registration> findAll() {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<Registration> cq = criteriaBuilder.createQuery(Registration.class);
        cq.select(cq.from(Registration.class));

        return em.createQuery(cq).getResultList();
    }

    public List<Registration> findByEventId(String eventId) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<Registration> cq = criteriaBuilder.createQuery(Registration.class);
        Root<Registration> root = cq.from(Registration.class);

        Predicate where = criteriaBuilder.equal(root.get("eventId"), eventId);
        cq.select(root).where(where);

        return em.createQuery(cq).getResultList();
    }

    public List<Registration> findWaitlistByEventId(String eventId) {
        return findByEventId(eventId).stream().filter(Registration::isWaitlist).toList();
    }

    public int getCount(String eventId) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = criteriaBuilder.createQuery(Long.class);
        Root<Registration> root = cq.from(Registration.class);

        Predicate where = criteriaBuilder.and(criteriaBuilder.equal(root.get("eventId"), eventId), criteriaBuilder.equal(root.get("remote"), false));
        cq.select(criteriaBuilder.count(root)).where(where);

        return em.createQuery(cq).getSingleResult().intValue();
    }

    @Transactional
    public Registration delete(String id) {
        Registration registration = em.find(Registration.class, id);
        em.remove(registration);

        return registration;
    }

}
