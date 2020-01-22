/*******************************************************************************

"FreePastry" Peer-to-Peer Application Development Substrate

Copyright 2002-2007, Rice University. Copyright 2006-2007, Max Planck Institute 
for Software Systems.  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

- Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.

- Redistributions in binary form must reproduce the above copyright
notice, this list of conditions and the following disclaimer in the
documentation and/or other materials provided with the distribution.

- Neither the name of Rice  University (RICE), Max Planck Institute for Software 
Systems (MPI-SWS) nor the names of its contributors may be used to endorse or 
promote products derived from this software without specific prior written 
permission.

This software is provided by RICE, MPI-SWS and the contributors on an "as is" 
basis, without any representations or warranties of any kind, express or implied 
including, but not limited to, representations or warranties of 
non-infringement, merchantability or fitness for a particular purpose. In no 
event shall RICE, MPI-SWS or contributors be liable for any direct, indirect, 
incidental, special, exemplary, or consequential damages (including, but not 
limited to, procurement of substitute goods or services; loss of use, data, or 
profits; or business interruption) however caused and on any theory of 
liability, whether in contract, strict liability, or tort (including negligence
or otherwise) arising in any way out of the use of this software, even if 
advised of the possibility of such damage.

*******************************************************************************/ 
package rice.pastry.testing;

import rice.pastry.Id;
import rice.pastry.NodeHandle;
import rice.pastry.PastryNode;
import rice.pastry.client.PastryAppl;
import rice.pastry.messaging.Message;
import rice.pastry.routing.SendOptions;

/**
 * A very simple ping object.
 * 
 * @version $Id: PingClient.java 3613 2007-02-15 14:45:14Z jstewart $
 * 
 * @author Andrew Ladd
 */

public class PingClient extends PastryAppl {
  private static class PingAddress {
    private static int myCode = 0x9219d8ff;

    public static int getCode() {
      return myCode;
    }
  }

  private static int pingAddress = PingAddress.getCode();

  public PingClient(PastryNode pn) {
    super(pn);
  }

  public int getAddress() {
    return pingAddress;
  }

  public void sendPing(Id nid) {
    // routeMessage, sans the getAddress() in the RouteMessage constructor
    routeMsg(nid, new PingMessage(pingAddress, getNodeId(), nid), 
        new SendOptions());
  }

  public void sendTrace(Id nid) {
    System.out.println("sending a trace from " + getNodeId() + " to " + nid);
    // sendEnrouteMessage
    routeMsg(nid, new PingMessage(pingAddress, getNodeId(), nid), 
        new SendOptions());
  }

  public void messageForAppl(Message msg) {
    System.out.print(msg);
    System.out.println(" received");
  }

  public boolean enrouteMessage(Message msg, Id from, NodeHandle nextHop,
      SendOptions opt) {
    System.out.print(msg);
    System.out.println(" at " + getNodeId());

    return true;
  }

  public void leafSetChange(NodeHandle nh, boolean wasAdded) {
    if (true) return;
    System.out.println("at... " + getNodeId() + "'s leaf set");
    System.out.print("node " + nh.getNodeId() + " was ");
    if (wasAdded)
      System.out.println("added");
    else
      System.out.println("removed");
  }

  public void routeSetChange(NodeHandle nh, boolean wasAdded) {
    if (true) return;
    System.out.println("at... " + getNodeId() + "'s route set");
    System.out.print("node " + nh.getNodeId() + " was ");
    if (wasAdded)
      System.out.println("added");
    else
      System.out.println("removed");
  }
}

/**
 * DO NOT declare this inside PingClient; see HelloWorldApp for details.
 */

class PingMessage extends Message {
  private Id source;

  private Id target;

  public PingMessage(int pingAddress, Id src, Id tgt) {
    super(pingAddress);
    source = src;
    target = tgt;
  }

  public String toString() {
    String s = "";
    s += "ping from " + source + " to " + target;
    return s;
  }
}

