package Server.Common;

import java.io.Serializable;

public class Crash implements Serializable {

}


//package Server.Common;
//
//import java.io.Serializable;
//
//public class MasterRecord implements Serializable {
//  private static final long serialVersionUID = 2671453558178166872L;
//  private ShadowPage<RMHashMap> pointer;
//  private int xid;
//  
//  public MasterRecord() {
//    pointer = null;
//    xid = -1;
//  }
//  
//  public MasterRecord(ShadowPage<RMHashMap> file, int transaction_id) {
//    pointer = file;
//    xid = transaction_id;
//  }
//  
//  public ShadowPage<RMHashMap> getPointer() {
//    return pointer;
//  }
//  
//  public int getXid() {
//    return xid;
//  }
//  
//  public void setPointer(ShadowPage<RMHashMap> file) {
//    pointer = file;
//  }
//  
//  public void setId(int transaction_id) {
//    xid = transaction_id;
//  }
//  
//}
