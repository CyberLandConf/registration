package de.jugda.registration.dao;

import de.jugda.registration.domain.Event;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class EventDao {

     @Inject
     private EntityManager em;

     @Transactional
     public void createEvent(Event event){
         em.persist(event);
     }

     public List<Event> getAllEvents(){
         CriteriaBuilder cb = em.getCriteriaBuilder();
         CriteriaQuery<Event> cq = cb.createQuery(Event.class);
         Root<Event> root = cq.from(Event.class);

         cq.select(root);

         return em.createQuery(cq).getResultList();
     }

     public Event getEventByEventId(String eventId){
         CriteriaBuilder cb = em.getCriteriaBuilder();
         CriteriaQuery<Event> cq = cb.createQuery(Event.class);
         Root<Event> root = cq.from(Event.class);

         cq.select(root);
         cq.where(cb.equal(root.get("eventId"), eventId));

         List<Event> resultList = em.createQuery(cq).getResultList();
         if(resultList.isEmpty()){
             return null;
         }
         return resultList.get(0);
     }
}
