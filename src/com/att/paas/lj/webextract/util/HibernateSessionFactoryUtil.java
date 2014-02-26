package com.att.paas.lj.webextract.util;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;

/**
 * Ensure a singleton instance of hibernate session factory
 * 
 * @author Amarpreet geadhoke
 *
 */
@SuppressWarnings("deprecation")
public class HibernateSessionFactoryUtil {

/** The single instance of hibernate SessionFactory */
private static org.hibernate.SessionFactory sessionFactory;

/**
 * disable contructor to guaranty a single instance
 */
private HibernateSessionFactoryUtil() {
	// prevents instantiation
}

static {
	// Annotation and XML
	sessionFactory = new AnnotationConfiguration().configure().buildSessionFactory();
	// XML only
	// sessionFactory = new Configuration().configure().buildSessionFactory();
}

/**
 * Gets the singleton
 *
 * @return singleton
 */
public static SessionFactory getInstance() {
	return sessionFactory;
}

/**
 * Opens a session and will not bind it to a session context
 * @return the session
 */
public Session openSession() {
	return sessionFactory.openSession();
}

/**
* Returns a session from the session context. If there is no session in the context it opens a session,
* stores it in the context and returns it.
 * This factory is intended to be used with a hibernate.cfg.xml
 * including the following property <property
 * name="current_session_context_class">thread</property> This would return
 * the current open session or if this does not exist, will create a new
 * session
 * 
 * @return the session
 */
public Session getCurrentSession() {
	return sessionFactory.getCurrentSession();
}

/**
 * closes the session factory
 */
public static void close() {
	if (sessionFactory != null)
		sessionFactory.close();
	sessionFactory = null;

}
}
