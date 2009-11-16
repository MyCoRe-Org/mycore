package org.mycore.datamodel.classifications;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.mycore.common.MCRSession;

public class ClassificationUserTableFactory {

    public static class ClassificationUserTable {
        private ConcurrentMap<String, String> ClassUserTable = new ConcurrentHashMap<String, String>();

        public String addClassUser(String rootID, String sessionID) {
            return getClassUserTable().put(rootID, sessionID);
        }

        private ConcurrentMap<String, String> getClassUserTable() {
            return ClassUserTable;
        }

        /**
         * @param classid
         * @return
         *      the session ID string, null if not exist in the table
         */
        public String getSession(String classid) {
            return getClassUserTable().get(classid);
        }

        public void clearUserClassTable(MCRSession session) {
            for (String rootID : getClassUserTable().keySet()) {
                getClassUserTable().remove(rootID, session.getID());
            }
        }

        public void removeSession(String rootID) {
            getClassUserTable().remove(rootID);
        }
    }

    private static ClassificationUserTable classUserTable = new ClassificationUserTable();

    public static ClassificationUserTable getInstance() {
        return classUserTable;
    }

}
