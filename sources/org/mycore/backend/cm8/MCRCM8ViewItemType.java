
package mycore.cm8;

import com.ibm.mm.sdk.common.*;
import com.ibm.mm.sdk.server.*;
import java.io.*;


public class MCRCM8ViewItemType
{
	final static String database="icmnlsdb";
	final static String userid="icmadmin";
	final static String password="Mf1sPwe.";
	final static String itemType="MCR_Demo_Doc7";
	//final static String itemType="MCR_Demo_Legal1";
	final static String prefix="d7";
	//final static String prefix="l1";
	final static String mcrid="MyCoReDemoDC_Document_1";
	//final static String mcrid="MyCoReDemoDC_LegalEntity_1";


    	public static void main(String argv[]) throws DKException, Exception
	{
		DKDatastoreICM dsICM = new DKDatastoreICM();  // Create new datastore object.
                dsICM.connect(database,userid,password,""); // Connect to the datastore.

		String query = "/"+itemType+"[@"+prefix+"ID=\""+mcrid+"\"]";
		System.out.println("| All items in "+itemType + ", server "+database);
                System.out.println("| Query >>>"+query+"<<<");
                
                // Specify Search / Query Options
                DKNVPair options[] = new DKNVPair[3];
                options[0] = new DKNVPair(DKConstant.DK_CM_PARM_MAX_RESULTS, "0"); // No Maximum (Default)                    
                options[1] = new DKNVPair(DKConstant.DK_CM_PARM_RETRIEVE,new Integer(DKConstant.DK_CM_CONTENT_YES)); 
                options[2] = new DKNVPair(DKConstant.DK_CM_PARM_END,null);        

                DKResults results = (DKResults)dsICM.evaluate(query, DKConstantICM.DK_CM_XQPE_QL_TYPE, options);              
                dkIterator iter = results.createIterator();
                System.out.println("| Number of Results:  "+results.cardinality());
                  
                while(iter.more())
		{ 
   		    System.out.println("|");
		    Object ddo=iter.next();  // Move pointer to next element and obtain that object.                          
         	    processDDO(ddo,"");
                }

		dsICM.disconnect();
	}

	private static void processDDO(Object obj, String pre) throws Exception
	{			
		System.out.println(pre+"|--+-- Class name: "+obj.getClass().getName());

		DKDDO ddo=(DKDDO) obj;
		ddo.retrieve();
		
		String itemId = ((DKPidICM)ddo.getPidObject()).getItemId();
               	System.out.println(pre+"+  +-- Item ID:  "+itemId+"  ("+ddo.getPidObject().getObjectType()+")"); 
		if (ddo instanceof DKLobICM)
		{
			DKLobICM lob=(DKLobICM) ddo;
			System.out.println(pre+"+  +-- Size: "+lob.getSize()+" Bytes, Mime-Type: "+lob.getMimeType());
			System.out.println(pre+"+  +-- URL: "+lob.getContentURL(-1,-1,1));
		}

		for (short i=1; i<=ddo.dataCount(); i++)
		{
			System.out.println(pre+"|  +-- "+ddo.getDataNameSpace(i)+":"+ddo.getDataName(i)+"="+ddo.getData(i));
			if (ddo.getDataNameSpace(i).equals("CHILD"))
			{
				DKChildCollection col= (DKChildCollection) ddo.getData(i);
				System.out.println(pre+"|  |   Cardinality: "+col.cardinality());	
				dkIterator iter=col.createIterator();
				while (iter.more())
				{
					processDDO((DKDDO)iter.next(),"|  "+pre);
				}
			}
			if (ddo.getDataName(i).equals("DKParts"))
			{
				DKSequentialCollection col= (DKSequentialCollection) ddo.getData(i);
				System.out.println(pre+"|  |   Cardinality: "+col.cardinality());	
				dkIterator iter=col.createIterator();
				while (iter.more())
				{
					processDDO((DKDDO)iter.next(),"|  "+pre);
				}
			}			
		}

	}
}
