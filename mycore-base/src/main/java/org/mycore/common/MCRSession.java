/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.common;

import static org.mycore.common.events.MCRSessionEvent.Type.ACTIVATED;
import static org.mycore.common.events.MCRSessionEvent.Type.PASSIVATED;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.events.MCRSessionEvent;
import org.mycore.common.events.MCRShutdownHandler;
import org.mycore.common.events.MCRShutdownHandler.Closeable;
import org.mycore.util.concurrent.MCRTransactionableRunnable;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * Instances of this class collect information kept during a session like the currently active user, the preferred
 * language etc.
 *
 * @author Detlev Degenhardt
 * @author Jens Kupferschmidt
 * @author Frank Lützenkirchen
 */
sealed public class MCRSession implements Cloneable permits MCRScopedSession {

    private static final URI DEFAULT_URI = URI.create("");

    /** A map storing arbitrary session data * */
    private Map<Object, Object> map = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    private Map.Entry<Object, Object>[] emptyEntryArray = new Map.Entry[0];

    private List<Map.Entry<Object, Object>> mapEntries;

    private boolean mapChanged = true;

    AtomicInteger accessCount;

    AtomicInteger concurrentAccess;

    ThreadLocal<AtomicInteger> currentThreadCount = ThreadLocal.withInitial(AtomicInteger::new);

    /** the logger */
    private static final Logger LOGGER = LogManager.getLogger();

    /** The user ID of the session */
    private MCRUserInformation userInformation;

    /** The language for this session as upper case character */
    private String language;

    private Locale locale;

    /** The unique ID of this session */
    private String sessionID;

    private String ip;

    private long loginTime;
    private long lastAccessTime;
    private long thisAccessTime;
    private long createTime;

    private final StackTraceElement[] constructingStackTrace;

    private URI firstURI;

    private String firstUserAgent;

    private URI lastURI;

    private final ThreadLocal<Throwable> lastActivatedStackTrace = new ThreadLocal<>();

    private final ThreadLocal<Queue<Runnable>> onCommitTasks = ThreadLocal.withInitial(ArrayDeque::new);

    private static final ExecutorService COMMIT_SERVICE;

    private static final MCRUserInformation GUEST_USER_INFORMATION = MCRSystemUserInformation.GUEST;

    static {
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("MCRSession-OnCommitService-#%d")
            .setThreadFactory(Thread.ofVirtual().factory())
            .build();
        COMMIT_SERVICE = Executors.newThreadPerTaskExecutor(threadFactory);
        MCRShutdownHandler.getInstance().addCloseable(new Closeable() {

            @Override
            public void prepareClose() {
                COMMIT_SERVICE.shutdown();
            }

            @Override
            public int getPriority() {
                return Integer.MIN_VALUE + 8;
            }

            @Override
            public void close() {
                if (!COMMIT_SERVICE.isTerminated()) {
                    try {
                        COMMIT_SERVICE.awaitTermination(10, TimeUnit.MINUTES);
                    } catch (InterruptedException e) {
                        LOGGER.warn("Error while waiting for shutdown.", e);
                    }
                }
            }

        });
    }

    /**
     * The constructor of a MCRSession. As default the user ID is set to the value of the property variable named
     * 'MCR.Users.Guestuser.UserName'.
     */
    MCRSession() {
        userInformation = GUEST_USER_INFORMATION;
        setCurrentLanguage(MCRConfiguration2.getString("MCR.Metadata.DefaultLang").orElse(MCRConstants.DEFAULT_LANG));
        accessCount = new AtomicInteger();
        concurrentAccess = new AtomicInteger();

        ip = "";
        sessionID = buildSessionID();
        MCRSessionMgr.addSession(this);

        LOGGER.debug("MCRSession created {}", sessionID);
        setLoginTime();
        createTime = loginTime;
        Throwable t = new Throwable();
        t.fillInStackTrace();
        constructingStackTrace = t.getStackTrace();
    }

    protected final void setLoginTime() {
        loginTime = System.currentTimeMillis();
        lastAccessTime = loginTime;
        thisAccessTime = loginTime;
    }

    /**
     * Constructs a unique session ID for this session, based on current time and IP address of host where the code
     * runs.
     */
    private static String buildSessionID() {
        return UUID.randomUUID().toString();
    }

    /**
     * Returns the unique ID of this session
     */
    public String getID() {
        return sessionID;
    }

    /**
     * Returns a list of all stored object keys within MCRSession. This method is not thread safe. I you need thread
     * safe access to all stored objects use {@link MCRSession#getMapEntries()} instead.
     *
     * @return Returns a list of all stored object keys within MCRSession as java.util.Ierator
     */
    public Iterator<Object> getObjectsKeyList() {
        return Collections.unmodifiableSet(map.keySet()).iterator();
    }

    /**
     * Returns an unmodifiable list of all entries in this MCRSession This method is thread safe.
     */
    public List<Map.Entry<Object, Object>> getMapEntries() {
        if (mapChanged) {
            mapChanged = false;
            final Set<Entry<Object, Object>> entrySet = Collections.unmodifiableMap(map).entrySet();
            final Map.Entry<Object, Object>[] entryArray = entrySet.toArray(emptyEntryArray);
            mapEntries = List.of(entryArray);
        }
        return mapEntries;
    }

    /** returns the current language */
    public final String getCurrentLanguage() {
        return language;
    }

    /** sets the current language */
    public final void setCurrentLanguage(String language) {
        Locale newLocale = Locale.forLanguageTag(language);
        this.language = language;
        this.locale = newLocale;
    }

    public Locale getLocale() {
        return locale;
    }

    /** Write data to the logger for debugging purposes */
    public final void debug() {
        LOGGER.debug("SessionID = {}", sessionID);
        LOGGER.debug("UserID    = {}", () -> getUserInformation().getUserID());
        LOGGER.debug("IP        = {}", ip);
        LOGGER.debug("language  = {}", language);
    }

    /** Stores an object under the given key within the session * */
    public Object put(Object key, Object value) {
        mapChanged = true;
        return map.put(key, value);
    }

    /**
     * Retrieves the value associated with the given key within the session, or computes and stores a new value if
     * absent.
     * <p>
     * This method ensures thread-safe lazy initialization by leveraging
     * {@link ConcurrentHashMap#computeIfAbsent(Object, Function)}.
     * <p>
     * If the key is not already present in the session, the provided mapping function is used to compute the value,
     * which is then stored in the session.
     *
     * @param key             the key whose associated value is to be retrieved or computed
     * @param mappingFunction the function to compute a value if the key is absent
     * @return the existing or newly computed value associated with the key
     * @throws NullPointerException if the key or mapping function is null
     */
    public Object computeIfAbsent(Object key, Function<Object, Object> mappingFunction) {
        if(!map.containsKey(key)) {
            mapChanged = true;
        }
        return map.computeIfAbsent(key, mappingFunction);
    }

    /** Returns the object that was stored in the session under the given key * */
    public Object get(Object key) {
        return map.get(key);
    }

    public void deleteObject(Object key) {
        mapChanged = true;
        map.remove(key);
    }

    /** Get the current ip value */
    public String getCurrentIP() {
        return ip;
    }

    /** Set the ip to the given IP */
    public final void setCurrentIP(String newip) {
        //a necessary condition for an IP address is to start with an hexadecimal value or ':'
        if (Character.digit(newip.charAt(0), 16) == -1 && newip.charAt(0) != ':') {
            LOGGER.error("Is not a valid IP address: {}", newip);
            return;
        }
        try {
            InetAddress inetAddress = InetAddress.getByName(newip);
            ip = inetAddress.getHostAddress();
        } catch (UnknownHostException e) {
            LOGGER.error("Exception while parsing new ip {} using old value.", newip, e);
        }
    }

    public final long getLoginTime() {
        return loginTime;
    }

    public void close() {
        // remove from session list
        LOGGER.debug("Remove myself from MCRSession list");
        MCRSessionMgr.removeSession(this);
        // clear bound objects
        LOGGER.debug("Clearing local map.");
        try {
            clearClosableValues(map);
        } finally {
            map.clear();
            mapEntries = null;
            sessionID = null;
        }
    }

    static void clearClosableValues(Map<Object, Object> map) {
        Queue<Exception> exceptions = new ConcurrentLinkedQueue<>();
        map.values().stream()
            .filter(java.io.Closeable.class::isInstance)
            .map(java.io.Closeable.class::cast)
            .forEach(c -> {
                try {
                    LOGGER.debug(() -> "Closing: " + c);
                    c.close();
                } catch (IOException | RuntimeException e) {
                    exceptions.add(e);
                }
            });
        if (!exceptions.isEmpty()) {
            MCRException e = new MCRException("Exceptions while closing scoped values.");
            exceptions.forEach(e::addSuppressed);
            throw e;
        }
    }

    @Override
    public String toString() {
        return "MCRSession[" + getID() + ",user:'" + getUserInformation().getUserID() + "',ip:" + getCurrentIP()
            + "]";
    }

    public long getLastAccessedTime() {
        return lastAccessTime;
    }

    public void logHttpRequest(URI uri, String userAgent) {
        lastURI = uri;
        if (firstURI == null) {
            firstURI = lastURI;
            firstUserAgent = userAgent;
        }
    }

    /**
     * Activate this session. For internal use mainly by MCRSessionMgr.
     *
     * @see MCRSessionMgr#setCurrentSession(MCRSession)
     */
    void activate() {
        lastAccessTime = thisAccessTime;
        thisAccessTime = System.currentTimeMillis();
        accessCount.incrementAndGet();
        if (currentThreadCount.get().getAndIncrement() == 0) {
            lastActivatedStackTrace.set(new RuntimeException("This is for debugging purposes only"));
            fireSessionEvent(ACTIVATED, concurrentAccess.incrementAndGet());
        } else {
            MCRException e = new MCRException(
                "Cannot activate a Session more than once per thread: " + currentThreadCount.get().get());
            LOGGER.warn("Too many activate() calls stacktrace:", e);
            LOGGER.warn("First activate() call stacktrace:", lastActivatedStackTrace::get);
        }
    }

    /**
     * Passivate this session. For internal use mainly by MCRSessionMgr.
     *
     * @see MCRSessionMgr#releaseCurrentSession()
     */
    void passivate() {
        if (currentThreadCount.get().getAndDecrement() == 1) {
            lastActivatedStackTrace.set(null);
            fireSessionEvent(PASSIVATED, concurrentAccess.decrementAndGet());
        } else {
            LOGGER.debug("deactivate currentThreadCount: {}", () -> currentThreadCount.get().get());
        }
        if (firstURI == null) {
            firstURI = DEFAULT_URI;
        }
        if (firstUserAgent == null) {
            firstUserAgent = "";
        }
        if (lastURI == null) {
            lastURI = DEFAULT_URI;
        }
        onCommitTasks.remove();
    }

    /**
     * Fire MCRSessionEvents. This is a common method that fires all types of MCRSessionEvent. Mainly for internal use
     * of MCRSession and MCRSessionMgr.
     *
     * @param type
     *            type of event
     * @param concurrentAccessors
     *            number of concurrentThreads (passivateEvent gets 0 for singleThread)
     */
    void fireSessionEvent(MCRSessionEvent.Type type, int concurrentAccessors) {
        MCRSessionEvent event = new MCRSessionEvent(this, type, concurrentAccessors);
        LOGGER.debug(event);
        MCRSessionMgr.getListeners().forEach(l -> l.sessionEvent(event));
    }

    public long getThisAccessTime() {
        return thisAccessTime;
    }

    public long getCreateTime() {
        return createTime;
    }

    public StackTraceElement[] getConstructingStackTrace() {
        return constructingStackTrace.clone();
    }

    public Optional<URI> getFirstURI() {
        return Optional.ofNullable(firstURI);
    }

    public Optional<String> getFirstUserAgent() {
        return Optional.ofNullable(firstUserAgent);
    }

    public Optional<URI> getLastURI() {
        return Optional.ofNullable(lastURI);
    }

    /**
     * @return the userInformation
     */
    public MCRUserInformation getUserInformation() {
        return userInformation;
    }

    /**
     * @param userSystemAdapter
     *            the userInformation to set
     * @throws IllegalArgumentException if transition to new user information is forbidden (privilege escalation)
     */
    public void setUserInformation(MCRUserInformation userSystemAdapter) {
        //check for MCR-1400
        if (!isTransitionAllowed(userSystemAdapter)) {
            throw new IllegalArgumentException("User transition from "
                + getUserInformation().getUserID()
                + " to " + userSystemAdapter.getUserID()
                + " is not permitted within the same session.");
        }
        this.userInformation = userSystemAdapter;
        setLoginTime();
    }

    /**
     * Add a task which will be executed after {@link MCRTransactionManager#commitTransactions()} was called.
     *
     * @param task thread witch will be executed after an commit
     */
    public void onCommit(Runnable task) {
        this.onCommitTasks.get().offer(Objects.requireNonNull(task));
    }

    protected void submitOnCommitTasks() {
        Queue<Runnable> runnables = onCommitTasks.get();
        onCommitTasks.remove();
        CompletableFuture.allOf(runnables.stream()
            .map(r -> new MCRTransactionableRunnable(r, this))
            .map(MCRSession::toCompletableFuture)
            .toArray(CompletableFuture[]::new))
            .join();
    }

    private static CompletableFuture<?> toCompletableFuture(MCRTransactionableRunnable r) {
        try {
            return CompletableFuture.runAsync(r, COMMIT_SERVICE);
        } catch (RuntimeException e) {
            LOGGER.error("Could not submit onCommit task. Running it locally.", e);
            try {
                r.run();
            } catch (RuntimeException e2) {
                LOGGER.fatal("Argh! Could not run task either. This task is lost 😰", e2);
            }
            return CompletableFuture.completedFuture(null);
        }
    }

    private boolean isTransitionAllowed(MCRUserInformation userSystemAdapter) {
        //allow if current user super user or system user or not logged in
        if (MCRSystemUserInformation.SUPER_USER.getUserID().equals(userInformation.getUserID())
            || MCRSystemUserInformation.GUEST.getUserID().equals(userInformation.getUserID())
            || MCRSystemUserInformation.SYSTEM_USER.getUserID().equals(userInformation.getUserID())) {
            return true;
        }
        //allow if new user information has default rights of guest user
        //or userID equals old userID
        return MCRSystemUserInformation.GUEST.getUserID().equals(userSystemAdapter.getUserID())
            || MCRSystemUserInformation.SYSTEM_USER.getUserID().equals(userSystemAdapter.getUserID())
            || userInformation.getUserID().equals(userSystemAdapter.getUserID());
    }

}
