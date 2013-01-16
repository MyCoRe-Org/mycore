package org.mycore.solr.logging;

import org.apache.log4j.Level;

/**
 * Collection of solr log levels.
 * 
 * @author shermann
 *
 */
public class MCRSolrLogLevels {

    private static final int SOLR_INFO_LEVEL_INT = Level.INFO_INT + 1;

    private static final int SOLR_ERROR_LEVEL_INT = Level.ERROR_INT + 1;

    private static final String SOLR_INFO_MSG = "SOLR";

    private static final String SOLR_ERROR_MSG = "SOLR-ERROR";

    /**
     * Info log level for solr.
     */
    public static final Level SOLR_INFO = new Info(SOLR_INFO_LEVEL_INT, SOLR_INFO_MSG, 7);

    /**
     * Error log level for solr.
     */
    public static final Level SOLR_ERROR = new Error(SOLR_ERROR_LEVEL_INT, SOLR_ERROR_MSG, 7);

    /**
     * Info log level.
     *
     */
    private static class Info extends Level {
        private static final long serialVersionUID = 1L;

        /**
         * @param level
         * @param levelStr
         * @param syslogEquivalent
         */
        protected Info(int level, String levelStr, int syslogEquivalent) {
            super(level, levelStr, syslogEquivalent);
        }

        /**
         * Checks whether <code>inLevel</code> is "SOLR_INFO" level. If yes then returns {@link MCRSolrLogLevels#SOLR_INFO},
         * else calls {@link MCRSolrLogLevels#toLevel(String, Level)} passing it {@link Level#DEBUG} as the defaultLevel
         *
         * @see Level#toLevel(java.lang.String)
         * @see Level#toLevel(java.lang.String, org.apache.log4j.Level)
         *
         */
        public static Level toLevel(String inLevel) {
            if (inLevel != null && inLevel.toUpperCase().equals(SOLR_INFO_MSG)) {
                return SOLR_INFO;
            }
            return (Level) toLevel(inLevel, Level.DEBUG);
        }

        /**
         * Checks whether <code>val</code> is {@link MCRSolrLogLevels#SOLR_INFO_LEVEL_INT}. If yes then returns {@link MCRSolrLogLevels#SOLR_INFO},
         * else calls {@link MCRSolrLogLevels#toLevel(int, Level)} passing it {@link Level#DEBUG} as the defaultLevel
         *
         * @see Level#toLevel(int)
         * @see Level#toLevel(int, org.apache.log4j.Level)
         *
         */
        public static Level toLevel(int val) {
            if (val == SOLR_INFO_LEVEL_INT) {
                return SOLR_INFO;
            }
            return (Level) toLevel(val, Level.DEBUG);
        }

        /**
         * Checks whether <code>val</code> is {@link MCRSolrLogLevels#SOLR_INFO_LEVEL_INT}. If yes then returns {@link MCRSolrLogLevels#SOLR_INFO},
         * else calls {@link Level#toLevel(int, org.apache.log4j.Level)}
         *
         * @see Level#toLevel(int, org.apache.log4j.Level)
         */
        public static Level toLevel(int val, Level defaultLevel) {
            if (val == SOLR_INFO_LEVEL_INT) {
                return SOLR_INFO;
            }
            return Level.toLevel(val, defaultLevel);
        }

        /**
         * Checks whether <code>sArg</code> is "SOLR_INFO" level. If yes then returns {@link MCRSolrLogLevels#SOLR_INFO},
         * else calls {@link Level#toLevel(java.lang.String, org.apache.log4j.Level)}
         *
         * @see Level#toLevel(java.lang.String, org.apache.log4j.Level)
         */

        public static Level toLevel(String level, Level defaultLevel) {
            if (level != null && level.toUpperCase().equals(SOLR_INFO_MSG)) {
                return SOLR_INFO;
            }
            return Level.toLevel(level, defaultLevel);
        }
    }

    /**
     * Error log level.
     */
    private static class Error extends Level {
        private static final long serialVersionUID = 1L;

        /**
         * @param level
         * @param levelStr
         * @param syslogEquivalent
         */
        protected Error(int level, String levelStr, int syslogEquivalent) {
            super(level, levelStr, syslogEquivalent);
        }

        /**
         * Checks whether <code>inLevel</code> is "SOLR_INFO" level. If yes then returns {@link MCRSolrLogLevels#SOLR_INFO},
         * else calls {@link MCRSolrLogLevels#toLevel(String, Level)} passing it {@link Level#ERROR} as the defaultLevel
         *
         * @see Level#toLevel(java.lang.String)
         * @see Level#toLevel(java.lang.String, org.apache.log4j.Level)
         *
         */
        public static Level toLevel(String inLevel) {
            if (inLevel != null && inLevel.toUpperCase().equals(SOLR_ERROR_MSG)) {
                return SOLR_ERROR;
            }
            return (Level) toLevel(inLevel, Level.ERROR);
        }

        /**
         * Checks whether <code>val</code> is {@link MCRSolrLogLevels#SOLR_INFO_LEVEL_INT}. If yes then returns {@link MCRSolrLogLevels#SOLR_ERROR},
         * else calls {@link MCRSolrLogLevels#toLevel(int, Level)} passing it {@link Level#ERROR} as the defaultLevel
         *
         * @see Level#toLevel(int)
         * @see Level#toLevel(int, org.apache.log4j.Level)
         *
         */
        public static Level toLevel(int val) {
            if (val == SOLR_ERROR_LEVEL_INT) {
                return SOLR_ERROR;
            }
            return (Level) toLevel(val, Level.ERROR);
        }

        /**
         * Checks whether <code>val</code> is {@link MCRSolrLogLevels#SOLR_INFO_LEVEL_INT}. If yes then returns {@link MCRSolrLogLevels#SOLR_ERROR},
         * else calls {@link Level#toLevel(int, org.apache.log4j.Level)}
         *
         * @see Level#toLevel(int, org.apache.log4j.Level)
         */
        public static Level toLevel(int val, Level defaultLevel) {
            if (val == SOLR_ERROR_LEVEL_INT) {
                return SOLR_ERROR;
            }
            return Level.toLevel(val, defaultLevel);
        }

        /**
         * Checks whether <code>sArg</code> is "SOLR_INFO" level. If yes then returns {@link MCRSolrLogLevels#SOLR_ERROR},
         * else calls {@link Level#toLevel(java.lang.String, org.apache.log4j.Level)}
         *
         * @see Level#toLevel(java.lang.String, org.apache.log4j.Level)
         */

        public static Level toLevel(String level, Level defaultLevel) {
            if (level != null && level.toUpperCase().equals(SOLR_ERROR_MSG)) {
                return SOLR_ERROR;
            }
            return Level.toLevel(level, defaultLevel);
        }
    }
}