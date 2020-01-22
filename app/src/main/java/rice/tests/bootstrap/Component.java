/*
 * Created on Jun 24, 2005
 */
package rice.tests.bootstrap;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.Scanner;

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
 * @author Jeff Hoye, Jim Stewart, Ansley Post
 */
public class Component implements Stopable{


    /**
     * this will keep track of our Past applications
     */
    Past app;
    Messenger app2;
    MyScribeClient app3;
    String name;
    boolean complete;
    boolean contin=true;
    Group group;
    public static final Scanner scanner=new Scanner(System.in);


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
    public Component(int bindport, InetSocketAddress bootaddress,
                     final Environment env) throws Exception {


        // Generate the NodeIds Randomly
        NodeIdFactory nidFactory = new RandomNodeIdFactory(env);

        // construct the PastryNodeFactory, this is how we use rice.pastry.socket
        PastryNodeFactory factory = new SocketPastryNodeFactory(nidFactory,
                bindport, env);

        // loop to construct the nodes/apps
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
        app = new GCPastImpl(node,new StorageManagerImpl(idf, stor, new LRUCache(
                new MemoryStorage(idf), 512 * 1024, node.getEnvironment())),
                5,"",new PastPolicy.DefaultPastPolicy(),10);
        //Past app = new PastImpl(node, new StorageManagerImpl(idf, stor, new LRUCache(
        //        new MemoryStorage(idf), 512 * 1024, node.getEnvironment())), 5, "");

        app2=new Messenger(node);
        app2.setElement(this);

        //            apps2.get(0).setElement((Stopable)this);

        app3=null;

        node.boot(bootaddress);

        // the node may require sending several messages to fully boot into the ring
        synchronized(node) {
            while(!node.isReady() && !node.joinFailed()) {
                // delay so we don't busy-wait
                node.wait(500);

                // abort if can't join
                if (node.joinFailed()) {
                    throw new IOException("Could not join the FreePastry ring.  Reason:"+node.joinFailedReason());

                }
            }
        }

        System.out.println("Finished creating new node " + node);


        // wait 5 seconds
        env.getTimeSource().sleep(5000);

        PastryIdFactory localFactory = new PastryIdFactory(env);

        complete=true;

        group=null;
        name="";

        // Menu para controlar el comportamiento del cliente
        while (contin) {
            System.out.println("");
            System.out.println("Introduzca la opcion que quiera realizar:");
            System.out.println("0) Set name of group");
            System.out.println("1) Create group "+name);
            System.out.println("2) Sign in the group "+name);
            System.out.println("3) Get users in the group "+name);
            System.out.println("4) Send message to "+name);
            System.out.println("5) Sign out the group "+name);
            System.out.println("7) Remove the group "+name);
            System.out.println("8) Suscribe scribe topic "+name);
            System.out.println("9) Schedule task");
            System.out.println("10) Unsuscribe from topic "+name);
            System.out.println("11) Cancel task");
            System.out.println("12) Figure out external IP");
            System.out.println("6) Exit");

            int opcion = scanner.nextInt();
            System.out.println("The chosen option is "+opcion);
            scanner.nextLine();
            System.out.println("");

            if(opcion!=0&&name.equals(""))
            {
                System.out.print("First you have to provide a group name");
            }
            else
            {
                switch (opcion)
                {
                    case 0:
                        System.out.print("Insert name of the group: ");
                        name = scanner.nextLine();
                        break;

                    case 1:
                        System.out.println(new Date().toString() +" - Creating group with id =" + localFactory.buildId(name) +
                                " and name " + name);
                        group = new Group(localFactory.buildId(name), name);
                        insertGroup(group);
                        break;

                    case 2:
                        System.out.println("Signing up with id =" + localFactory.buildId(name) +
                                " and name " + name);
                        complete=false;
                        getGroup(localFactory.buildId(name),new Continuation<PastContent, Exception>()
                        {
                            public void receiveResult(PastContent result)
                            {
                                group=(Group)result;
                                System.out.println("Successfully looked up key " + name);
                                group.insert(app.getLocalNodeHandle().getId());
                                insertGroup(group);
                                complete=true;
                            }

                            public void receiveException(Exception result)
                            {
                                System.out.println("Error looking up group " + name);
                                result.printStackTrace();
                                complete=true;
                            }

                        });
                        break;


                    // Envia un nuevo paquete al ap. Simula trafico normal entre el cliente
                    // e internet
                    case 3:
                        System.out.println("Listing group's components in group "+ name);
                        complete=false;
                        getGroup(localFactory.buildId(name),new Continuation<PastContent, Exception>()
                        {
                            public void receiveResult(PastContent result)
                            {
                                group=(Group)result;
                                System.out.println(group);
                                complete=true;
                            }

                            public void receiveException(Exception result)
                            {
                                System.out.println("Error looking up group " + name);
                                result.printStackTrace();
                                complete=true;
                            }

                        });
                        break;

                    // consultar el consumo de bytes hasta el momento
                    case 4:
                        msg(name);
                        break;

                    // desconecta al cliente del ap en el que se encuentra
                    case 5:
                        System.out.println("Signing out from group " +name);
                        complete=false;
                        getGroup(localFactory.buildId(name),new Continuation<PastContent, Exception>()
                        {
                            public void receiveResult(PastContent result)
                            {
                                group=(Group)result;
                                System.out.println("Successfully looked up name " + name);
                                group.remove(app.getLocalNodeHandle().getId());
                                insertGroup(group);
                                complete=true;
                            }

                            public void receiveException(Exception result)
                            {
                                System.out.println("Error looking up group " + name);
                                result.printStackTrace();
                                complete=true;
                            }

                        });
                        break;

                    // finaliza la ejecucion del cliente
                    case 6:
                        System.out.println("Saliendo...");
                        contin=false;
                        break;

                    case 7:
                        System.out.println("Removing group with id = "+group.getGroupId());
                        removeGroup(group.getGroupId());
                        break;
                    case 8:
                        System.out.println("Suscribing...");
                        if(app3==null)
                            app3=new MyScribeClient(node,name);
                        app3.subscribe();
                        break;
                    case 9:
                        System.out.println("Scheduling messages");
                        app3.startPublishTask();
                        break;
                    case 10:
                        System.out.println("Unsuscribing...");
                        app3.unsuscribe();
                        break;
                    case 11:
                        System.out.println("cancel messages");
                        app3.cancelPublishTask();
                        break;
                    case 12:

                        System.out.println("My addr = "+((SocketPastryNodeFactory)factory).getBindAddress());
                        break;
                    // si se introduce un numero de opcion erroneo
                    default:
                        System.out.println("Numero de opcion \"" + opcion + "\" no valido");
                        break;
                }
            }

            while(!complete)
                env.getTimeSource().sleep(500);

        }
        // se cierra el descriptor de entrada
        scanner.close();

    }

    @Override
    public void shutdown()
    {

        System.out.println("Shutting down");
        System.out.println("Signing out from group " +name);

        PastryIdFactory localFactory = new PastryIdFactory(app.getEnvironment());
        getGroup(localFactory.buildId(name),new Continuation<PastContent, Exception>()
        {
            public void receiveResult(PastContent result)
            {
                group=(Group)result;
                System.out.println("Successfully looked up name " + name);
                group.remove(app.getLocalNodeHandle().getId());
                insertGroup(group);
                contin=false;
            }

            public void receiveException(Exception result)
            {
                System.out.println("Error looking up group " + name);
                result.printStackTrace();
                complete=true;
            }

        });

    }

    public void insertGroup(Group group)
    {
        app.insert(group, new Continuation<Boolean[], Exception>() {
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

    public void getGroup(Id lookupKey,Continuation cont)
    {
        app.lookup(lookupKey, cont);

    }

    public void removeGroup(Id group)
    {
        PastImpl p = (PastImpl) app;
        p.remove(group, new Continuation()
        {
            @Override
            public void receiveResult(Object result)
            {
                System.out.println("Removed "+result);
            }

            @Override
            public void receiveException(Exception exception)
            {
                System.out.println("Received exception "+exception);
            }
        });
    }

    public void msg(final String name)
    {
        boolean cont=true;

        String content="";
        int type=0;
        int destination=0;

        while(cont)
        {
            PastryIdFactory localFactory = new PastryIdFactory(app.getEnvironment());

            System.out.println("");
            System.out.println("Introduzca la opcion que quiera realizar:");
            System.out.println("0) Load group "+name);
            System.out.println("1) Set content ");
            System.out.println("2) Set type ");
            System.out.println("3) Set destination");
            System.out.println("4) Send message");
            System.out.println("5) Send message to the whole group");
            System.out.println("6) Exit");

            int opcion = scanner.nextInt();
            System.out.println("The chosen option is "+opcion);
            scanner.nextLine();
            System.out.println("");

            switch (opcion)
            {
                case 0:
                    complete=false;
                    getGroup(localFactory.buildId(name),new Continuation<PastContent, Exception>()
                    {
                        public void receiveResult(PastContent result)
                        {
                            if(result!=null)
                            {
                                group = (Group) result;
                                for (int i = 0; i < group.getComponents().size(); i++)
                                {
                                    System.out.println("Component " + i + ") " + group.getComponents().get(i));
                                }
                                complete = true;
                            }
                            else
                            {
                                System.out.println("The group does not exist");
                            }
                        }

                        public void receiveException(Exception result)
                        {
                            System.out.println("Error looking up group " + name);
                            result.printStackTrace();
                            complete=true;
                        }

                    });
                    break;
                case 1:
                    System.out.println("Content: ");
                    content=scanner.nextLine();
                    System.out.println("Content = "+content);
                    break;
                case 2:
                    System.out.print("Type: ");
                    type=scanner.nextInt();
                    scanner.nextLine();
                    System.out.println("type = "+type);
                    break;
                case 3:
                    System.out.print("destination: ");
                    destination=scanner.nextInt();
                    scanner.nextLine();
                    try
                    {
                        System.out.println("destination ="+group.getComponents().get(destination));
                    } catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                    break;
                case 4:
                    System.out.println("Routing...");
                    try
                    {
                        app2.routeMsg(group.getComponents().get(destination),content,type);
                    } catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                    break;
                case 5:
                    System.out.println("Sending msg to the whole group");
                    for (int i = 0; i < group.getComponents().size(); i++)
                    {
                        Id elementAt =  group.getComponents().get(i);
                        app2.routeMsg(elementAt,content,type);
                    }
                    break;
                case 6:
                    System.out.println("Exiting mode messenger...");
                    cont=false;
                    break;
            }


            while(!complete)
            {
                try
                {
                    app.getEnvironment().getTimeSource().sleep(500);
                } catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
        }

    }



    private static void usage()
    {
        System.out.println("Usage:");
        System.out.println("java -cp pastry.jar rice.tests.bootstrap.Component"+
                " localbindport bootIP bootPort");
    }
    /**
     * Usage: java [-cp FreePastry- <version>.jar]
     * rice.tutorial.past.PastTutorial localbindport bootIP bootPort numNodes
     * example java rice.tutorial.past.PastTutorial 9001 pokey.cs.almamater.edu 9001 10
     */
    public static void main(String[] args) throws Exception {
        // Loads pastry configurations
        Environment env = new Environment();
        Component component=null;

        // disable the UPnP setting (in case you are testing this on a NATted LAN)
        env.getParameters().setString("nat_search_policy","never");



        if(args.length<3)
            usage();
        else
        {

            try
            {
                // the port to use locally
                int bindport = Integer.parseInt(args[0]);

                // build the bootaddress from the command line args
                InetAddress bootaddr = InetAddress.getByName(args[1]);
                int bootport = Integer.parseInt(args[2]);
                InetSocketAddress bootaddress = new InetSocketAddress(bootaddr, bootport);

                // launch our node!
                component = new Component(bindport, bootaddress, env);

            } catch (Exception e)
            {
                // remind user how to use
                System.out.println("Exception = " + e);
                e.printStackTrace();
            } finally
            {
                component.shutdown();
                env.getTimeSource().sleep(3000);

                env.destroy();
            }

        }
    }
}

