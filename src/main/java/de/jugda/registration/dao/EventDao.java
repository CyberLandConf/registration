package de.jugda.registration.dao;

import de.jugda.registration.domain.Event;
import de.jugda.registration.model.EventDto;
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

     public Event getEventById(String uid){
         return em.find(Event.class, uid);
     }
}
