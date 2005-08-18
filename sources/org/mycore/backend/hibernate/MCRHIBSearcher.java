package org.mycore.backend.hibernate;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.backend.query.MCRQuerySearcherInterface;
import org.mycore.common.MCRConfiguration;

public class MCRHIBSearcher implements MCRQuerySearcherInterface{

    /** The logger */
    public static Logger LOGGER = Logger.getLogger(MCRHIBSearcher.class.getName());
    
    private static String SQLQueryTable; 
    private static MCRConfiguration config;
    
    public MCRHIBSearcher(){
        config = MCRConfiguration.instance();
        SQLQueryTable = config.getString("MCR.QueryTableName", "MCRQuery");
    }
    
    public void runQuery(int no) {
        Session session = MCRHIBConnection.instance().getSession();
        Transaction tx = session.beginTransaction();
        List l = new LinkedList();       

        if (no==0){
            l = session.createQuery("from " + SQLQueryTable + " where title like '%Ein%' and author like '%Jens Kupferschmidt%'").list();
        }else if(no==1){
            l = session.createQuery("from " + SQLQueryTable + " where author like '%Jens Kupferschmidt%'").list();
        }else if(no==2){
            l = session.createQuery("from " + SQLQueryTable + " where title like '%Ein%'").list();
        }else if(no==3){
            l = session.createQuery("from " + SQLQueryTable + " where title like '%Ein%' and author like '%Heiko Helmbrecht%'").list();
        }
        System.out.println("Ergebnis: "+ l.size());

        for(int i=0; i<l.size(); i++){
            MCRHIBQuery res = new MCRHIBQuery(l.get(i));
            System.out.println("ID: " + res.getValue("getmcrid") + "  author: " + res.getValue("getauthor"));
        }
        tx.commit();
        session.close();
        
    }
    
}
