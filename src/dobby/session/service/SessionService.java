package dobby.session.service;

import dobby.session.DefaultSessionStore;
import dobby.session.ISessionStore;
import dobby.session.Session;
import dobby.task.SchedulerService;
import dobby.Config;
import common.logger.Logger;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * The SessionService class is used to manage sessions
 */
public class SessionService {
    private static final Logger LOGGER = new Logger(SessionService.class);
    private ISessionStore sessionStore = new DefaultSessionStore(); // initialize with default session store because the config option is read AFTER running preStart(). accessing sessions (for what ever reason) before the config is read will result in a NPE
    private final int maxSessionAge = Config.getInstance().getInt("dobby.session.maxAge", 24);

    private SessionService() {
        final int cleanupInterval = Config.getInstance().getInt("dobby.session.cleanUpInterval", 30);
        LOGGER.info("starting session cleanup scheduler with interval of " + cleanupInterval + " min...");
        SchedulerService.getInstance().addRepeating(this::cleanUpSessions, cleanupInterval, TimeUnit.MINUTES);
    }

    public static SessionService getInstance() {
        return SessionServiceHolder.INSTANCE;
    }

    public void setSessionStore(ISessionStore sessionStore) {
        this.sessionStore = sessionStore;
    }

    private long getCurrentTime() {
        return System.currentTimeMillis();
    }

    private void cleanUpSessions() {
        LOGGER.info("Cleaning up sessions");
        long currentTime = getCurrentTime();
        final Map<String, Long> sessions = sessionStore.getSessionAges();

        sessions.forEach((id, lastAccessed) -> {
            if (currentTime - lastAccessed > (long) maxSessionAge * 60 * 60 * 1000) {
                sessionStore.remove(id);
            }
        });
    }

    /**
     * Find a session by its id
     *
     * @param sessionId Session id
     * @return The session if found, otherwise empty
     */
    public Optional<Session> find(String sessionId) {
        final Optional<Session> optSession = sessionStore.find(sessionId);

        if (optSession.isEmpty()) {
            return Optional.empty();
        }

        optSession.get().setLastAccessed(getCurrentTime());
        return optSession;
    }

    /**
     * Save a session
     *
     * @param session Session to save
     */
    public void set(Session session) {
        session.setLastAccessed(getCurrentTime());
        sessionStore.update(session);
    }

    /**
     * Remove a session
     *
     * @param session Session to remove
     */
    public void remove(Session session) {
        if (session.getId() == null) {
            return;
        }
        sessionStore.remove(session.getId());
    }

    /**
     * Create a new session
     *
     * @return The new session
     */
    public Session newSession() {
        Session session = new Session();
        session.setId(generateSessionId());
        session.setLastAccessed(getCurrentTime());
        sessionStore.update(session);
        return session;
    }

    private String generateSessionId() {
        return UUID.randomUUID() + UUID.randomUUID().toString();
    }

    private static class SessionServiceHolder {
        private static final SessionService INSTANCE = new SessionService();
    }
}
