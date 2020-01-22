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
/*
 * Created on May 4, 2005
 */
package us.tfg.p2pmessenger.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import rice.environment.logging.Logger;
import rice.p2p.commonapi.Application;
import rice.p2p.commonapi.Endpoint;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.Node;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.commonapi.RouteMessage;
import rice.p2p.scribe.Scribe;
import rice.p2p.scribe.ScribeContent;
import rice.p2p.scribe.ScribeImpl;
import rice.p2p.scribe.ScribeMultiClient;
import rice.p2p.scribe.Topic;
import rice.p2p.util.rawserialization.SimpleOutputBuffer;
import us.tfg.p2pmessenger.model.Grupo;
import us.tfg.p2pmessenger.model.Mensaje;
import us.tfg.p2pmessenger.model.MensajeCifrado;

/**
 * Clase que implementa la interfaz aplicacion para recibir
 * mensajes regularmente. Tambien implementa la interfaz
 * ScribeMultiClient para poder comunicarse con varios usuarios
 * a la vez.
 *
 * @author Jeff Hoye
 */
public class AppScribe implements ScribeMultiClient, Application
{

    public static final int ERROR_LECTURA_CLAVE_SIMETRICA = 1;
    public static final int ERROR_CIFRADO_MENSAJE = 2;

    /**
     * el manejador del arbol de difusion
     */
    private Scribe myScribe;

    /**
     * Id al que se subscribe el cliente
     */
    private ArrayList<Topic> topics;

    /**
     * El Endpoint representa el nodo en el que se apoya el modulo
     * de difusion.  Haciendo las llamadas a Endpoint, se asegura
     * que el mensaje llegara sin importar a quien este destinado
     */
    private Endpoint endpoint;

    /**
     * Mostrar los logs generados
     */
    private Logger logger;

    /**
     * Interfaz que regula la comunicacion del modulo con el resto
     * del proyecto.
     */
    private Mensajero mensajero;

    /**
     * Una entrada por cada nodo que se esta solicitando eco
     */
    private Map<Id, ObservadorPing> sesiones;


    /**
     * Constructor de la clase
     *
     * @param node the PastryNode
     */
    public AppScribe(Node node, Mensajero mensajero)
    {
        // construye el punto final de los mensajes pastry
        this.endpoint = node.buildEndpoint(this,  "us.tfg.p2pmessenger.controller.scribe");

        // controlador
        this.mensajero = mensajero;

        // gestor de logs
        logger = node.getEnvironment().getLogManager()
                     .getLogger(this.getClass(), "");

        // indicacion de clase iniciada
        if (logger.level <= Logger.INFO) logger.log(getClass()+".<init>("+node+", "+mensajero+")");

        // construccion del arbol de difusion de contenido
        myScribe = new ScribeImpl(node,  "us.tfg.p2pmessenger.scribe");

        // ahora se empiezan a recibir mensajes
        endpoint.register();

        // temas
        this.topics = new ArrayList<>();

        // sesiones de eco
        this.sesiones = new HashMap<>();

    }

    /**
     * subscripcion a un grupo (cada grupo de difusion esta reflejado por un topic)
     */
    public void subscribe(Id topic)
    {
        if (logger.level <= Logger.INFO) logger.log(getClass()+".subscribe("+topic.toStringFull()+")");
        if (logger.level <= Logger.INFO) logger.log("Subscribiendome al grupo: " + topic);
        Topic t = new Topic(topic);
        int posicion = this.topics.indexOf(t);
        if (posicion == -1)
        {
            this.topics.add(t);
            myScribe.subscribe(t, this);
        }
    }

    /**
     * subscripcion a un grupo (cada grupo de difusion esta reflejado por un topic)
     * @param grupo
     */
    public void subscribe(Grupo grupo)
    {
        if (logger.level <= Logger.INFO) logger.log(getClass()+".subscribe("+grupo+")");
        Topic t = new Topic(grupo.getId());
        if (logger.level <= Logger.INFO) logger.log("Subscribiendome al grupo: " + grupo.getId());
        int posicion = this.topics.indexOf(t);
        if (posicion == -1)
        {
            this.topics.add(t);
            myScribe.subscribe(t, this);
        }
    }

    /**
     * Eliminar la subscripcion a un grupo
     */
    public void unsubscribe(Id topic)
    {
        if (logger.level <= Logger.INFO) logger.log(getClass()+".unsubscribe("+topic.toStringFull()+")");
        Topic t = new Topic(topic);
        int posicion = this.topics.indexOf(t);
        if (posicion != -1)
        {
            myScribe.unsubscribe(t, this);
            this.topics.remove(posicion);
        }
    }

    /**
     * Funcion llamada cuando recibimos un mensaje individual
     */
    public void deliver(Id id, Message mensaje)
    {

        if (logger.level <= Logger.INFO) logger.log(getClass()+".deliver("+id.toStringFull()+", "+mensaje+")");
        // Se imprime una linea de log por mensaje recibido (todo cambiar el log a FINE tras la validacion


        if (mensaje instanceof MensajeCifrado)
        {
            MensajeCifrado mcifrado = (MensajeCifrado) mensaje;

            // si no soy el destinatario del mensaje, devolver un mensaje con "host unreachable"
            // esto podra configurarse para que se haga descarte silencioso
            if (!id.equals(endpoint.getId()))
            {
                //todo disminuir la importancia de este log cuando se haya verificado su buen funcionamiento
                if (logger.level <= Logger.INFO) logger.log("Recibido un mensaje que no es para mi," +
                        " respondiendo con 'host unreachable'");
                nodoSolicitadoDesconectado(id, mcifrado);
            } // en las sucesivas comprobaciones se asume que soy el destinatario
            else if (mcifrado.getClase() == Mensaje.ECHO_REQUEST)
            {
                // para comprobar que estoy vivo, como ping
                //todo disminuir la importancia de este log cuando se haya verificado su buen funcionamiento
                if (logger.level <= Logger.INFO) logger.log("Recibido un mensaje para comprobar si estoy vivo");
                echoRequest(mcifrado);
            } else if (mcifrado.getClase() == Mensaje.ECHO_REPLY)
            {
                // procesar la respuesta recibida a un ping realizado.
                // se comprueba que el mensaje que envia es por el que
                // estoy preguntando
                //todo disminuir la importancia de este log cuando se haya verificado su buen funcionamiento
                if (logger.level <= Logger.INFO) logger.log("Recibida respuesta de comprobacion de actividad,");
                echoReply(mcifrado);
            } else
            {

                mensajero.procesa(id, mcifrado, true);

            }
        } else
        {
            if (logger.level <= Logger.INFO) logger.log("Mensaje de tipo desconocido " + mensaje);
        }
    }

    /**
     * Funcion llamda cuando recibimos un mensaje de grupo
     *
     * @param topic el grupo del que proviene el mensaje
     * @param content el contenido del mensaje
     */
    public void deliver(Topic topic, ScribeContent content)
    {
        if (logger.level <= Logger.INFO) logger.log(getClass()+".deliver("+topic+", "+content+")");
        //System.out.println("MyScribeClient.deliver("+topic+","+content+")");
        if (logger.level <= Logger.INFO) logger.log("AppScribe.deliver(Topic = "
                + topic.getId() + ",scribeContent)");
        if (content instanceof MensajeCifrado)
        {
            MensajeCifrado mcifrado = (MensajeCifrado) content;

            if(!mcifrado.getOrigen().equals(endpoint.getId())) {
                if (logger.level <= Logger.INFO) logger.log("Recibido mensaje de grupo que no he " +
                        "mandado yo");
                mensajero.procesa(topic.getId(), mcifrado, true);
            } else {
                if (logger.level <= Logger.INFO) logger.log("Recibido mensaje de un grupo al" +
                        " que estoy subscrito, pero yo he enviado el mensaje. Asi que se descarta");
            }
        }
    }

    // metos de la interfaz Scribe
    /**
     * Called when we receive an anycast.  If we return
     * false, it will be delivered elsewhere.  Returning true
     * stops the message here.
     */
    public boolean anycast(Topic topic, ScribeContent content)
    {
        boolean returnValue = myScribe.getEnvironment().getRandomSource().nextInt(3) == 0;
        if (logger.level <= Logger.CONFIG) logger
                .log("MyScribeClient.anycast(" + topic + "," + content + "):" + returnValue);
        return returnValue;
    }

    public void childAdded(Topic topic, NodeHandle child)
    {
        if (logger.level <= Logger.CONFIG) logger
                .log("MyScribeClient.childAdded(" + topic + "," + child + ")");
    }

    public void childRemoved(Topic topic, NodeHandle child)
    {
        if (logger.level <= Logger.CONFIG) logger
                .log("MyScribeClient.childRemoved(" + topic + "," + child + ")");
    }

    public void subscribeFailed(Topic topic)
    {
        if (logger.level <= Logger.CONFIG) logger
                .log("MyScribeClient.childFailed(" + topic + ")");
    }

    @Override
    public void subscribeFailed(Collection<Topic> topics)
    {
        if (logger.level <= Logger.CONFIG) logger
                .log("unsuccessfully suscribed to: ");
        for (Iterator<Topic> iterator = topics.iterator(); iterator.hasNext(); )
        {
            Topic topic = iterator.next();
            if (logger.level <= Logger.CONFIG) logger.log(topic.toString());
        }
    }

    @Override
    public void subscribeSuccess(Collection<Topic> topics)
    {
        if (logger.level <= Logger.CONFIG) logger
                .log("successfully suscribed to: ");
        for (Iterator<Topic> iterator = topics.iterator(); iterator.hasNext(); )
        {
            Topic topic = iterator.next();
            if (logger.level <= Logger.CONFIG) logger.log(topic.toString());
        }
    }

    public boolean forward(RouteMessage message)
    {
        return true;
    }


    public void update(NodeHandle handle, boolean joined)
    {

    }

    /**
     * Llamado por el controlador, determina por que medio debe enviarse el mensaje:
     * o bien de manera individual o por el canal de grupo.
     * @param mensaje
     * @param idDestino
     * @param individual
     */
    public void enviaMensaje(MensajeCifrado mensaje, Id idDestino, boolean individual)
    {
        if (logger.level <= Logger.INFO) logger.log(
                getClass()+".enviaMensaje("+mensaje+", "+idDestino.toStringFull()+
                ", individual = "+individual+")");
        if (individual)
        {
            endpoint.route(idDestino, mensaje, null);
        } else
        {
            Topic t = new Topic(idDestino);
            myScribe.publish(t, mensaje);
        }
    }

    /**
     * Metodo para informar al nodo que envia un mensaje que el destino que pretende
     * alcanzar no esta en la red.
     *
     * @param id       nodo destinatario del mensaje original
     * @param mcifrado mensaje original
     */
    private void nodoSolicitadoDesconectado(Id id, MensajeCifrado mcifrado)
    {
        if (logger.level <= Logger.INFO) logger.log(
                getClass()+".nodoSolicitadoDesconocido("+id.toStringFull()+", "+
                mcifrado+")");
        try
        {
            SimpleOutputBuffer out = new SimpleOutputBuffer();
            out.writeUTF("Host_unreachable");
            out.writeUTF("Requested node");
            out.writeShort(id.getType());
            id.serialize(out);
            out.writeUTF("Receiver node");
            out.writeShort(endpoint.getId().getType());
            endpoint.getId().serialize(out);
            out.writeUTF("Message");

            out.writeShort(mcifrado.getType());
            mcifrado.serialize(out);

            byte[] raw = out.getBytes();
            String contenidoRespuesta = us.tfg.p2pmessenger.util.Base64.getEncoder().encodeToString(raw);
            MensajeCifrado respuesta = new MensajeCifrado(endpoint.getId(), contenidoRespuesta,
                    Message.LOW_PRIORITY, Mensaje.HOST_UNREACHABLE);

            endpoint.route(mcifrado.getOrigen(), respuesta, null);


        } catch (Exception e)
        {
            if (logger.level <= Logger.INFO)
                logger.logException("Error al enviar host unreachable", e);
        }

    }

    /**
     * Envia peticion de eco.
     * @param mensaje contenido de la peticion
     */
    private void echoRequest(MensajeCifrado mensaje)
    {
        if (logger.level <= Logger.INFO) logger.log(
                getClass()+".echoRequest("+mensaje+")");
        if (mensajero.responderEcho())
        {

            if (logger.level <= Logger.INFO) logger.log("Echo reply para " + mensaje.getOrigen());
            String carga = mensaje.getContenido();
            MensajeCifrado response = new MensajeCifrado(
                    endpoint.getId(), carga, Message.LOW_PRIORITY, Mensaje.ECHO_REPLY);
            endpoint.route(mensaje.getOrigen(), response, null);
        } else if (logger.level <= Logger.INFO) logger.log("Respuesta de echo suprimida");
    }


    /**
     * Respuesta eco
     * @param mensaje contenido de la repsuesta, debe ser el mismo que el proporcionado
     *                en la peticion
     */
    private void echoReply(MensajeCifrado mensaje)
    {
        if (logger.level <= Logger.INFO) logger.log(
                getClass()+".echoReply("+mensaje+")");
        ObservadorPing sesion = sesiones.get(mensaje.getOrigen());
        if (sesion != null)
            sesiones.remove(mensaje.getOrigen()).notificarPing(
                    mensaje.getClase(), mensaje.getOrigen(), mensaje.getContenido());

    }

    /**
     * Funcion que inicia el proceso de eco - respuesta
     * @param objetivo destinatario del ping (id de usuario, no de grupo)
     * @param carga contenido del mensaje (generado aleatoriamente)
     * @param observador a quien avisar cuando se obtenga una respuesta
     */
    public void ping(Id objetivo, String carga, ObservadorPing observador)
    {
        if (logger.level <= Logger.INFO) logger.log(
                getClass()+".ping("+objetivo.toStringFull()+", "+carga+", "+
                observador+")");
        ObservadorPing sesion = sesiones.get(objetivo);
        if (sesion == null)
        {
            sesiones.put(objetivo, observador);
        }

        MensajeCifrado mensaje = new MensajeCifrado(endpoint.getId(),
                carga, Mensaje.LOW_PRIORITY, Mensaje.ECHO_REQUEST);
        mensajero.procesa(objetivo, mensaje, false);
        endpoint.route(objetivo, mensaje, null);
    }

    /**
     * Para dejar de recibir las notificaciones de respuestas de eco
     * @param objetivo
     */
    public void cancelarPing(Id objetivo)
    {
        if (logger.level <= Logger.INFO) logger.log(
                getClass()+".cancelarPing("+objetivo.toStringFull()+")");
        ObservadorPing observadorPing = sesiones.remove(objetivo);
    }

    public void cancelarClave(Id interlocutor)
    {
        if (logger.level <= Logger.INFO) logger.log(
                getClass()+".cancelarClave("+interlocutor.toStringFull()+")");
    }


}
