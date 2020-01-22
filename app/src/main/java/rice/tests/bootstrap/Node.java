package rice.tests.bootstrap;
/*
 * Created on Jun 24, 2005
 */

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Vector;

import rice.Continuation;
import rice.environment.Environment;
import rice.p2p.commonapi.Id;
import rice.p2p.past.Past;
import rice.p2p.past.PastContent;
import rice.p2p.past.PastImpl;
import rice.p2p.past.PastPolicy;
import rice.p2p.past.gc.GCPastImpl;
import rice.pastry.NodeIdFactory;
import rice.pastry.PastryNode;
import rice.pastry.PastryNodeFactory;
import rice.pastry.commonapi.PastryIdFactory;
import rice.pastry.socket.SocketPastryNodeFactory;
import rice.pastry.standard.RandomNodeIdFactory;
import rice.persistence.LRUCache;
import rice.persistence.MemoryStorage;
import rice.persistence.PersistentStorageConsole;
import rice.persistence.Storage;
import rice.persistence.StorageManagerImpl;
import rice.tutorial.scribe.MyScribeClient;

/**
 * This tutorial shows how to use Past.
 *
 * @author FPiriz
 */

public class Node implements Stopable
{

    private static Group group;

    // loop to construct the nodes/apps
    /* this will keep track of our Past applications */
    private Vector<Past> apps = new Vector<Past>();
    private Environment env;
    private Messenger app2;
    private MyScribeClient app3;
    private boolean contin;


    /**
     * Based on the rice.tutorial.scribe.ScribeTutorial
     *
     * This constructor launches numNodes PastryNodes. They will bootstrap to an
     * existing ring if one exists at the specified location, otherwise it will
     * start a new ring.
     *
     * @param bindport the local port to bind to
     * @param bootaddress the IP:port of the node to boot from
     * @param env the Environment
     */
    private Node(int bindport, InetSocketAddress bootaddress,
                 final Environment env,boolean search) throws Exception {

        // Generate the NodeIds Randomly
        NodeIdFactory nidFactory = new RandomNodeIdFactory(env);
        this.env=env;
        this.contin=true;

        // construct the PastryNodeFactory, this is how we use rice.pastry.socket
        PastryNodeFactory factory = new SocketPastryNodeFactory(nidFactory,
                bindport, env);

        // construct a node, passing the null boothandle on the first loop will
        // cause the node to start its own ring
        PastryNode node = factory.newNode();

        // used for generating PastContent object Ids.
        // this implements the "hash function" for our DHT
        PastryIdFactory idf = new PastryIdFactory(env);

        // create a different storage root for each node
        String storageDirectory = "./store/storage"+(node.getId().toStringFull());

        // create the persistent part
        Storage stor = new PersistentStorageConsole(idf, storageDirectory, 4 * 1024 * 1024, node
                .getEnvironment());
        //Storage stor = new MemoryStorage(idf);
        Past app = new GCPastImpl(node,new StorageManagerImpl(idf, stor, new LRUCache(
                new MemoryStorage(idf), 512 * 1024, node.getEnvironment())),
                5,"",new PastPolicy.DefaultPastPolicy(),10);
        //Past app = new PastImpl(node, new StorageManagerImpl(idf, stor, new LRUCache(
        //        new MemoryStorage(idf), 512 * 1024, node.getEnvironment())), 5, "");

        apps.add(app);
        app2=new Messenger(node);
        app2.setElement(this);

        app3=new MyScribeClient(node,"Domingo");
        app3.subscribe();

        node.boot(bootaddress);

        // the node may require sending several messages to fully boot into the ring
        synchronized(node) {
            while(!node.isReady() && !node.joinFailed()) {
                // delay so we don't busy-wait
                node.wait(500);

                // abort if can't join
                if (node.joinFailed()) {
                    throw new IOException("Could not join the FreePastry ring.  Reason:"
                            +node.joinFailedReason());
                }
            }
        }

        System.out.println("Finished creating new node " + node);


        int prob2=env.getRandomSource().nextInt(10);
        if(search)
        {
            env.getTimeSource().sleep(prob2*1000);
            final Past p = (Past) apps.get(0);

            String name="Domingo";

            PastryIdFactory localFactory = new PastryIdFactory(env);
            final Id lookupKey=localFactory.buildId(name);

            System.out.println("Looking up " + lookupKey + " at node " + p.getLocalNodeHandle()+
                    " with replication factor = "+p.getReplicationFactor());
            int replications=p.getReplicationFactor();

           /*
            p.lookupHandles(lookupKey,replications,new Continuation() {

                public void receiveResult(Object o) {

                    if (o instanceof PastContentHandle[]) {

                        PastContentHandle[] elements = (PastContentHandle[]) o;

                        for (int i = 0; i < elements.length; i++)
                        {

                            System.out.println("elements[" + i + "] = " + elements[i]);

                            if(elements[i]!=null){
                                p.fetch(elements[i],this);
                            }
                        }
                    }
                    else if(o instanceof Grupo)
                    {
                        if(o!=null)
                        {
                            Grupo group=(Grupo)o;
                            System.out.println("Fetched Grupo " +group);
                            System.out.println("Is mutable = "+group.isMutable());
                        }
                    }
                    else
                        System.out.println("result("+o+") -- no handles returned!");

                }

                public void receiveException(Exception e) {
                    System.out.println("exception("+e+")");
                    e.printStackTrace();
                }
            });

            p.lookup(lookupKey,new Continuation<PastContent,Exception>() {

                public void receiveResult(PastContent o) {

                    System.out.println("Received "+o);
                }
                public void receiveException(Exception e) {
                    System.out.println("exception("+e+")");
                    e.printStackTrace();
                }
            });*/
            p.lookup(lookupKey,new Continuation<PastContent, Exception>()
            {
                private int retry;
                private Continuation father;

                public void receiveResult(PastContent result)
                {
                    father=this;
                    System.out.println("\033[31mresult = "+result+".\033[0m");
                    if(result!=null)
                    {
                        group = (Group) result;
                        System.out.println("\033[31mSuccessfully looked up key " + lookupKey + ".\033[0m");
                        System.out.println("\033[31mInserting in group as new component with id = "
                                + apps.get(0).getLocalNodeHandle().getId()+"\033[0m");

                        group.insert(apps.get(0).getLocalNodeHandle().getId());
                        System.out.println("\033[31mInserting againg.\033[0m");
                        apps.get(0).insert(group, new Continuation<Boolean[], Exception>()
                        {
                            // the result is an Array of Booleans for each insert
                            public void receiveResult(Boolean[] results)
                            {
                                int numSuccessfulStores = 0;
                                for (Boolean result : results)
                                {
                                    if (result)
                                        numSuccessfulStores++;
                                }
                                System.out.println("\033[31m"+ lookupKey+"successfully stored at "
                                        +numSuccessfulStores+"locations.\033[0m");
                            }

                            public void receiveException(Exception result)
                            {
                                System.out.println("Error storing " + lookupKey);
                                //                                result.printStackTrace();
                                retry++;
                                try
                                {
                                    apps.get(0).getEnvironment().getTimeSource().sleep(1000);
                                } catch (InterruptedException e)
                                {
                                    e.printStackTrace();
                                }
                                if(retry<3)
                                {

                                    apps.get(0).lookup(lookupKey, father);
                                }
                            }
                        });
                    }
                    else
                    {
                        if(retry<3)
                        {
                            try
                            {
                                env.getTimeSource().sleep(3000);
                            } catch (InterruptedException e)
                            {
                                System.out.println("\033[31mError sleeping when retrying to lookup"+
                                        "\n With stackTrace "+result.toString()+"\033[0m");
                                e.printStackTrace();
                            }
                            retry++;
                            p.lookup(lookupKey, this);
                        }
                        else
                            System.out.println("\033[31mCan't lookup the key\033[0m");
                    }
                }


                public void receiveException(Exception result)
                {
                    System.out.println("\033[31mError looking up " + lookupKey+
                            "\n With stackTrace "+result.toString()+"\033[0m");
                    result.printStackTrace();
                }

            });
        }
        while(contin)
        {
            try
            {
                env.getTimeSource().sleep(1000);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    public void getGroup(Id lookupKey,Continuation cont)
    {
        Past p = apps.get(0);

        p.lookup(lookupKey, cont);

    }


    public void insertGroup(Group group)
    {
        PastImpl p = (PastImpl) apps.get(0);
        p.insert(group, new Continuation<Boolean[], Exception>() {
            // the result is an Array of Booleans for each insert
            public void receiveResult(Boolean[] results) {
                int numSuccessfulStores = 0;
                for (Boolean result : results)
                {
                    if (result)
                        numSuccessfulStores++;
                }
                System.out.println("Grupo successfully stored at " +
                        numSuccessfulStores + " locations.");
            }

            public void receiveException(Exception result) {
                System.out.println("Error storing the group");
                result.printStackTrace();
            }
        });
    }

    public void shutdown()
    {

        PastryIdFactory localFactory = new PastryIdFactory(apps.get(0).getEnvironment());
        getGroup(localFactory.buildId("Domingo"),new Continuation<PastContent, Exception>()
        {
            public void receiveResult(PastContent result)
            {
                group=(Group)result;
                group.remove(apps.get(0).getLocalNodeHandle().getId());
                insertGroup(group);
                contin=false;
            }

            public void receiveException(Exception result)
            {
                System.out.println("Error looking up group Domingo");
                result.printStackTrace();
            }

        });

    }

    private static void usage()
    {
        System.out.println("Usage:");
        System.out.println("java -cp pastry.jar rice.tests.bootstrap.Node"+
                " localbindport bootIP bootPort numNodes");
    }
    /**
     * Usage: java [-cp FreePastry- <version>.jar]
     * rice.tutorial.past.PastTutorial localbindport bootIP bootPort numNodes
     * example java rice.tutorial.past.PastTutorial 9001 pokey.cs.almamater.edu 9001 10
     */
    public static void main(String[] args) throws Exception {
        // Loads pastry configurations
        Environment env = new Environment();
        Node bt=null;

        // disable the UPnP setting (in case you are testing this on a NATted LAN)
        env.getParameters().setString("nat_search_policy","never");

        if(args.length<3)
            usage();
        else
        {
            try
            {
                System.out.println("Iniciando...");
                // the port to use locally
                int bindport = Integer.parseInt(args[0]);

                // build the bootaddress from the command line args
                InetAddress bootaddr = InetAddress.getByName(args[1]);
                int bootport = Integer.parseInt(args[2]);
                boolean search;
                search = args.length >= 4;

                InetSocketAddress bootaddress = new InetSocketAddress(bootaddr, bootport);

                // launch our node!
                bt = new Node(bindport, bootaddress, env,search);

            } catch (Exception e)
            {
                // remind user how to use
                System.out.println("Exception = " + e);
                throw e;
            } finally
            {
                if (bt != null)
                {
                    bt.shutdown();
                }
                env.destroy();
            }
        }
    }
}


