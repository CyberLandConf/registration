package de.jugda.registration.dao;

import de.jugda.registration.Config;
import de.jugda.registration.domain.Registration;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.Select;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Niko Köbler, http://www.n-k.de, @dasniko
 */
@ApplicationScoped
public class RegistrationDao {

    @Inject
    private EntityManager em;

    @Inject
    DynamoDbClient dynamoDB;
    @Inject
    Config config;

    private static final String attributesToGet = "id, eventId, #name, email, pub, remote, waitlist, privacy, created, #ttl";
    private static final Map<String, String> expressionAttributeNames = Map.of("#name", "name", "#ttl", "ttl");

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

    public Registration delete(String id) {
        Map<String, AttributeValue> key = Map.of("id", toAttribute(id));

        Registration registration = Registration.from(dynamoDB.getItem(builder -> builder
            .tableName(config.dynamodb().table())
            .key(key)
            .projectionExpression(attributesToGet)
            .expressionAttributeNames(expressionAttributeNames)
        ).item());
        //noinspection ResultOfMethodCallIgnored
        registration.getId(); // materialize object

        dynamoDB.deleteItem(builder -> builder.tableName(config.dynamodb().table()).key(key));

        return registration;
    }

    private QueryRequest.Builder baseQueryRequestBuilder(QueryRequest.Builder builder) {
        return builder
            .tableName(config.dynamodb().table())
            .indexName(config.dynamodb().index())
            .projectionExpression(attributesToGet)
            .expressionAttributeNames(expressionAttributeNames)
            ;
    }

    private QueryRequest.Builder byEventIdQueryBuilder(QueryRequest.Builder builder, String eventId) {
        return baseQueryRequestBuilder(builder)
            .keyConditionExpression("eventId = :v_eventId")
            .expressionAttributeValues(getBaseAttributeValues(eventId))
            ;
    }

    private AttributeValue toAttribute(String value) {
        return AttributeValue.builder().s(value).build();
    }

    private Map<String, AttributeValue> getBaseAttributeValues(String eventId) {
        return Map.of(":v_eventId", toAttribute(eventId));
    }
}
