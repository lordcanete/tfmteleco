package rice.tests.bootstrap;

import rice.p2p.commonapi.Application;
import rice.p2p.commonapi.Endpoint;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.Node;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.commonapi.RouteMessage;

/**
 * Created by FPiriz on 19/6/17.
 */
public class Messenger implements Application
{
    /**
     * The Endpoint represents the underlieing node.  By making calls on the
     * Endpoint, it assures that the message will be delivered to a MyApp on whichever
     * node the message is intended for.
     */
    protected Endpoint endpoint;

    /**
     * The node we were constructed on.
     */
    protected Node node;

    private Stopable sender;

    public Messenger(Node node) {
        // We are only going to use one instance of this application on each PastryNode
        this.endpoint = node.buildEndpoint(this, "messenger");

        this.node = node;

        // now we can receive messages
        this.endpoint.register();
    }

    /**
     * Getter for the node.
     */
    public Node getNode() {
        return node;
    }

    /**
     * Called to route a message to the id
     */
    public void routeMsg(Id id,String content,int type) {
        System.out.println(this+" sending to "+id);
        Envelope msg = new Envelope(endpoint.getId(), id,content,type);
        endpoint.route(id, msg, null);
    }

    /**
     * Called to directly send a message to the nh
     */
    public void routeMsgDirect(NodeHandle nh,String content,int type) {
        System.out.println(this+" sending direct to "+nh);
        Envelope msg = new Envelope(endpoint.getId(), nh.getId(),content,type);
        endpoint.route(null, msg, nh);
    }

    /**
     * Called when we receive a envelope.
     */
    public void deliver(Id id, Message envelope) {
        System.out.println(this+" received "+ envelope);
        Envelope env=(Envelope)envelope;

        if(env.getType()== Envelope.APAGAR)
        {
            sender.shutdown();
        }
//            this.node.getEnvironment().destroy();
        //coment
    }

    /**
     * Called when you hear about a new neighbor.
     * Don't worry about this method for now.
     */
    public void update(NodeHandle handle, boolean joined) {
        System.out.println("New neighbor found :" +handle);
        if(joined)
            System.out.println("Neighbor joined");
    }

    /**
     * Called a message travels along your path.
     * Don't worry about this method for now.
     */
    public boolean forward(RouteMessage message) {
        System.out.println("Forwarding msg :"+message);
        return true;
    }

    public void setElement(Stopable sender)
    {
        this.sender=sender;
    }

    public String toString() {
        return "MyApp "+endpoint.getId();
    }


}