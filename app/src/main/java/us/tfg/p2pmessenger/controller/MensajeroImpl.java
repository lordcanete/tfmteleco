package us.tfg.p2pmessenger.controller;

import java.io.ByteArrayInputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import rice.environment.logging.Logger;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.Node;
import us.tfg.p2pmessenger.model.Mensaje;
import us.tfg.p2pmessenger.model.MensajeCifrado;

import static us.tfg.p2pmessenger.controller.Controlador.ALGORITMO_CLAVE_SIMETRICA;

/**
 * Clase mediadora entre el controlador de la aplicacion
 * y el modulo de comunicacion de pastry. Implementa
 * la interfaz {@link Mensajero} y {@link ObservadorKey}
 * para ser notificado cuando una peticion de clave se
 * resuelve.
 */
public class MensajeroImpl implements Mensajero, ObservadorKey
{
    /**
     * Nodo pastry sobre el que se asienta la aplicacion
     */
    protected Node node;

    /**
     * Para mostrar los logs
     */
    private Logger logger;

    /**
     * Modulo de mensajes de pastry
     */
    private AppScribe appScribe;

    /**
     * Controlador de la aplicacion completa
     */
    private Controlador controlador;

    /**
     * Lista de los observadores de claves.
     */
    private Map<Id, KeyWithTimeout> observadoresClave;

    /**
     * Mensajes pendiente de resolucion de clave. Al obtenerse
     * la clave pendiente de una conversacion, se le
     * envian de golpe los mensajes pendientes
     */
    private Map<Id, ArrayList<Mensaje>> colaDeSalida;

    /**
     * Mensajes pendiente de resolucion de clave. Al obtenerse
     * la clave pendiente de una conversacion, se
     * procesan de golpe los mensajes pendientes en la
     * bandeja de entrada
     */
    private Map<Id, ArrayList<Mensaje>> colaDeEntrada;

    /**
     * Constructor de la clase
     * @param node
     * @param controlador
     */
    public MensajeroImpl(Node node, Controlador controlador)
    {
        // logs
        this.logger = node.getEnvironment().getLogManager().getLogger(
                getClass(), "us.tfg.app.mensajero");

        if(logger.level<=Logger.INFO) logger.log(getClass()+".<init>("+node+", "+controlador+")");

        // nodo pastry
        this.node = node;

        // controlador de la aplicacion
        this.controlador = controlador;

        // now we can receive messages
        this.appScribe = new AppScribe(this.node, this);

        this.observadoresClave = new HashMap<>();
        this.colaDeSalida = new HashMap<>();

    }

    /**
     * Devuelve el nodo pastry
     */
    public Node getNode()
    {
        if(logger.level<=Logger.INFO) logger.log(getClass()+".getNode()");
        return node;
    }

    /**
     * Obtiene el llavero del usuario que esta utilizando
     * la aplicacion
     * @return
     */
    public Llavero getLlavero()
    {
        if(logger.level<=Logger.INFO) logger.log(getClass()+".getLlavero()");
        return this.controlador.getLlavero();
    }

    /**
     * Metodo llamado cuando el modulo de pastry recibe un mensaje.
     * Aqui se decide que hacer con el mensaje recibido. Este
     * metodo se ejecuta a la entrada y a la salida de cada mensaje,
     * en el sentido saliente no se le hace nada.
     * @param fuente id del grupo que habla, si el id coincide con el
     *               nuestro es porque se trata de un mensaje privado
     *               para nosotros
     * @param mensajeCifrado mensaje recibido
     * @param entrante si el mensaje entra o sale de la app
     */
    public void procesa(Id fuente, MensajeCifrado mensajeCifrado, boolean entrante)
    {
        if(logger.level<=Logger.INFO) logger.log(getClass()+".procesa("+fuente+", "+mensajeCifrado+", entrante = "+entrante+")");

        Mensaje mensaje = null;
        String direccion = "";
        if (entrante)
            direccion = "entrante";
        else
            direccion = "saliente";

        if (logger.level <= Logger.INFO)
            logger.log("Procesando mensaje " + direccion + " --> " + mensajeCifrado);

        if (entrante)
        {
            // de internet hacia nuestra aplicacion
            if (fuente == null)
            {
                // mensaje enviado por nosotros mismos
                if (logger.level <= Logger.INFO) logger
                        .log("Mensaje con origen local");
            } else if (fuente.equals(node.getId()))
            {
                // somos el destino del mensaje
                int clase = mensajeCifrado.getClase();
                switch (clase)
                {
                    case Mensaje.INDIVIDUAL_IMPORTANTE:
                        mensaje = desencripta(null, mensajeCifrado);
                        controlador.mensajeRecibido(mensaje, mensaje.getOrigen());
                        break;
                    case Mensaje.INDIVIDUAL_NORMAL:
                        mensaje = desencripta(null, mensajeCifrado);
                        controlador.mensajeRecibido(mensaje, mensaje.getOrigen());
                        break;
                    case Mensaje.INDIVIDUAL_ENVIANDO_FICHERO:
                        mensaje = desencripta(null, mensajeCifrado);
                        controlador.mensajeRecibido(mensaje, mensaje.getOrigen());
                        break;
                    case Mensaje.INDIVIDUAL_PETICION_INICIO_CHAT:
                        inicioChatIndividual(mensajeCifrado);
                        break;
                    case Mensaje.INDIVIDUAL_RESPUESTA_INICIO_CHAT:
                        respuestaInicioChatIndividual(mensajeCifrado);
                        break;

                    case Mensaje.INDIVIDUAL_PETICION_UNIR_GRUPO:
                        mensaje = desencripta(null, mensajeCifrado);
                        controlador.peticionUnirAGrupo(mensaje);
                        break;
                    case Mensaje.INDIVIDUAL_RESPUESTA_UNIR_GRUPO:
                        mensaje = desencripta(null, mensajeCifrado);
                        controlador.respuestaUnirAGrupo(mensaje);
                    default:
                        if (logger.level <= Logger.WARNING) logger
                                .log("Mensaje tipo " + clase + " desconocido");
                        break;
                }
            } else
            {
                // mensaje recibido de un grupo
                int clase = mensajeCifrado.getClase();

                switch (clase)
                {
                    case Mensaje.GRUPO_IMPORTANTE:
                        mensaje = desencripta(fuente, mensajeCifrado);
                        controlador.mensajeRecibido(mensaje, fuente);
                        break;
                    case Mensaje.GRUPO_NORMAL:
                        mensaje = desencripta(fuente, mensajeCifrado);
                        controlador.mensajeRecibido(mensaje, fuente);
                        break;
                    case Mensaje.GRUPO_ENVIANDO_FICHERO:
                        mensaje = desencripta(fuente, mensajeCifrado);
                        controlador.mensajeRecibido(mensaje, fuente);
                        break;
                    case Mensaje.GRUPO_PEDIR_ADDR:
                        responderIp(fuente);
                        break;
                    case Mensaje.GRUPO_RESPUESTA_IPADDR:
                        recibirIp(mensajeCifrado);
                        break;
                    case Mensaje.GRUPO_ENTRA:
                        mensaje = desencripta(fuente, mensajeCifrado);
                        controlador.mensajeRecibido(mensaje, fuente);
                        break;
                    case Mensaje.GRUPO_SALE:
                        mensaje = desencripta(fuente, mensajeCifrado);
                        controlador.mensajeRecibido(mensaje, fuente);
                        break;
                    case Mensaje.GRUPO_CAMBIO_LIDER:
                        mensaje = desencripta(fuente, mensajeCifrado);
                        controlador.mensajeRecibido(mensaje, fuente);
                        break;
                    case Mensaje.GRUPO_BLOQUE_UTILIZADO:
                        mensaje = desencripta(fuente, mensajeCifrado);
                        controlador.actualizaDireccionBloque(fuente, mensaje);
                        break;

                    default:
                        if (logger.level <= Logger.WARNING) logger
                                .log("Mensaje tipo " + clase + " desconocido");
                        break;
                }
            }
        }
    }

    /**
     * Cuando nos salimos de un grupo, le decimos a la aplicacion
     * que deje de recibir mensajes que provengan de ese id.
     * @param idGrupo
     */
    @Override
    public void abandonaGrupo(Id idGrupo)
    {
        if(logger.level<=Logger.INFO) logger.log(getClass()+".abandonarGrupo("+idGrupo+")");
        appScribe.unsubscribe(idGrupo);
    }

    /**
     * Le dice al modulo de comunicacion que empiece a escuchar
     * de un id concreto
     * @param grupo
     */
    @Override
    public void subscribe(Id grupo)
    {
        appScribe.subscribe(grupo);
    }

    /**
     * Llamado por el controlador de la aplicacion para enviar
     * un nuevo mensaje.
     * @param myMessage mensaje sin encriptar que se quiere enviar
     * @param individual si va destinado a un grupo o a un usuario individual
     */
    @Override
    public void enviaMensaje(Mensaje myMessage, boolean individual)
    {
        if(logger.level<=Logger.INFO) logger.log(getClass()+".enviaMensaje("+myMessage+", individual = "+individual+")");
        Id idDestino = myMessage.getDestino();
        controlador.guardaMensaje(myMessage, idDestino);

        MensajeCifrado sobre = null;
        System.out.println("traza1");
        if (individual)
        {
            System.out.println("traza2");
            System.out.println("traza2 detalle - idDestino: " + idDestino.toStringFull());
            // busca la clave simetrica de un usuario. para mensajes
            // que se envian, si no la encuentra le pide que genere
            // una nueva al otro extremo
            Key claveSimetrica = getLlavero().getClaveSimetrica(
                    idDestino.toStringFull() + Llavero.EXTENSION_CLAVE_SALIENTE);
            if (claveSimetrica != null)
            {
                System.out.println("traza2a");
                // clave encontrada
                try
                {
                    System.out.println("traza2b");
                    sobre = new MensajeCifrado(myMessage, claveSimetrica);
                } catch (Exception e)
                {
                    if (logger.level <= Logger.INFO) logger.logException("No se pudo enviar el mensaje", e);
                }

                if (sobre != null)
                {
                    System.out.println("traza2c");
                    // envia
                    procesa(node.getId(), sobre, false);
                    System.out.println("tipo mensaje 3: " + myMessage.getClase());
                    appScribe.enviaMensaje(sobre, idDestino, true);
                }
            } else
            {
                // no hay clave para los mensajes salientes. Le enviara un mensaje
                // especial al otro extremo para que la genere y se la mande
                // para poder iniciar la comunicacion
                KeyWithTimeout peticionClave = observadoresClave.get(idDestino);
                if (peticionClave == null)
                {
                    // todavia no se ha pedido una clave
                    
                    JsonObject contenidoJson = Json.createObjectBuilder()
                                                   .add("algoritmo", ALGORITMO_CLAVE_SIMETRICA)
                                                   .add("nueva", false)
                                                   .build();

                    sobre = new MensajeCifrado(node.getId(), contenidoJson.toString(),
                            Mensaje.MEDIUM_PRIORITY, Mensaje.INDIVIDUAL_PETICION_INICIO_CHAT);
                    appScribe.enviaMensaje(sobre, idDestino, true);
                    observadoresClave.put(idDestino, new KeyWithTimeout(this, 10000, idDestino));
                }

                // encola un nuevo mensaje en la "bandeja de salida"
                ArrayList<Mensaje> mensajes = colaDeSalida.get(idDestino);
                if (mensajes != null)
                    mensajes.add(myMessage);
                else
                {
                    mensajes = new ArrayList<>();
                    mensajes.add(myMessage);
                    colaDeSalida.put(idDestino, mensajes);
                }
            }
        } else
        {
            System.out.println("traza3");
            // enviar mensje de grupo
            Key clave = getLlavero().getClaveSimetrica(idDestino.toStringFull());
            if (clave != null)
            {
                try
                {
                    sobre = new MensajeCifrado(myMessage, clave);
                } catch (Exception e)
                {
                    if (logger.level <= Logger.INFO) logger.logException("No se pudo enviar el mensaje", e);
                    errorEnviando(AppScribe.ERROR_CIFRADO_MENSAJE,
                            "No se dispone de clave para el grupo " + idDestino.toStringFull());
                }
                if (sobre != null)
                {
                    procesa(node.getId(), sobre, false);
                    appScribe.enviaMensaje(sobre, idDestino, false);
                }
            } else
            {
                errorEnviando(AppScribe.ERROR_LECTURA_CLAVE_SIMETRICA,
                        "No se dispone de clave para el grupo " + idDestino.toStringFull());
            }

        }
    }

    /**
     * Se ha producido al enviar un mensaje
     * @param error
     * @param mensaje
     */
    @Override
    public void errorEnviando(int error, String mensaje)
    {
        if(logger.level<=Logger.INFO) logger.log(getClass()+".errorEnviando(err = "+error+", "+mensaje+")");
        this.controlador.errorEnviando(error, mensaje);
    }


    // ---------- funciones de respuesta para los mensajes ---------

    /**
     * Funcion para responder automaticamente a un mensaje que
     * pregunta cual es nuestra direccion IP (la pide para
     * almacenarla en la bbdd y poder iniciar con ellas
     * la siguiente vez)
     * @param origen
     */
    private void responderIp(Id origen)
    {
        if(logger.level<=Logger.INFO) logger.log(getClass()+".responderIp("+origen.toStringFull()+")");
        InetSocketAddress direccion = controlador.getDireccion();
        if (direccion != null)
        {
            String respuesta = direccion.getHostName() + "\n" + direccion.getPort();
            MensajeCifrado m = new MensajeCifrado(controlador.getUsuario().getId(),respuesta,
                    Message.HIGH_PRIORITY,Mensaje.GRUPO_RESPUESTA_IPADDR);
            appScribe.enviaMensaje(m,origen,false);
        }
    }

    /**
     * Procesa la respuesta del metodo {@link MensajeroImpl#responderIp(Id)}.
     * Almacena la ip recibida (si esta bien formada) en la base de datos
     * @param mensaje
     */
    private void recibirIp(final MensajeCifrado mensaje)
    {
        if(logger.level<=Logger.INFO) logger.log(getClass()+".recibirIp("+mensaje+")");
        new Thread(new Runnable() {
            @Override
            public void run() {
                String[] dir = mensaje.getContenido().split("[\\r\\n]+");
                InetAddress ip = null;
                try
                {
                    ip = InetAddress.getByName(dir[0]);
                    int puerto = Integer.parseInt(dir[1]);
                    controlador.guardarDireccion(ip, puerto);
                } catch (UnknownHostException e)
                {
                    if (logger.level <= Logger.INFO)
                        logger.logException("Error al obtener la ip a partir del nombre", e);
                }

            }
        }).start();
    }

    /**
     * Le pregunta al controlador si debe responder las peticiones de eco.
     * Por defectos, estas estan activadas
     * @return
     */
    public boolean responderEcho()
    {
        if(logger.level<=Logger.INFO) logger.log(getClass()+".responderEcho()");
        boolean ret = true;
        ret = controlador.responderEcho();
        return true;
    }

    /**
     * Envia un mensaje de peticion de eco al id proporcionado. Tambien
     * proporciona el observador al que se debe avisar cuando se reciba
     * el mensaje de respuesta
     * @param objetivo
     * @param observador
     * @param carga
     */
    public void ping(Id objetivo, ObservadorPing observador, String carga)
    {
        if(logger.level<=Logger.INFO) logger.log(getClass()+".ping("+objetivo.toStringFull()
                +", "+observador+", "+carga+")");
        appScribe.ping(objetivo, carga, observador);
    }

    /**
     * Eliminar el subscriptor del evento de respuesta de eco recibida
     * @param objetivo
     */
    public void cancelarPing(Id objetivo)
    {
        if(logger.level<=Logger.INFO) logger.log(getClass()+".cancerlarPing("+objetivo+")");
        appScribe.cancelarPing(objetivo);
    }

    /**
     * Le pide al controlador que procese una respuesta para esta peticion de
     * inicio de chat. Respondera con una clave simetrica
     * cifrada con el certificado de ese usuario que realiza la peticion
     * a traves del metodo {@link MensajeroImpl#responderSolicitudClave(Id, String)}
     * @param mcifrado mensaje de peticion
     */
    public void inicioChatIndividual(MensajeCifrado mcifrado)
    {
        if(logger.level<=Logger.INFO) logger.log(getClass()+".inicioChatIndividual("+mcifrado+")");
        controlador.inicioChatIndividual(mcifrado);
    }


    /**
     * Metodo que procesa la respuesta del mensaje enviado en
     * {@link MensajeroImpl#enviaMensaje(Mensaje, boolean)}
     * cuando no se ha encontrado una clave para cifrar la
     * comunicacion saliente con un usuario concreto
     *
     * @param evento
     * @param interlocutor
     * @param claveCifrada
     */
    @Override
    public void notificarClave(int evento, Id interlocutor, String claveCifrada)
    {
        if(logger.level<=Logger.INFO) logger.log(getClass()+".notificarClave(evento = "+evento+", "+interlocutor+", "+claveCifrada+")");
        try
        {
            // recibida respuesta, desubscribimos del publicador
            KeyWithTimeout obs = observadoresClave.remove(interlocutor);

            // se ha recibido una clave. si es null es por temporizador agotado
            if (claveCifrada != null)
            {
                SecretKey sk = ManejadorClaves.desencriptaClaveSimetrica(
                        claveCifrada, getLlavero().getClavePrivada(controlador.getUsuario().getNombre()),
                        ALGORITMO_CLAVE_SIMETRICA);

                // guarda la clave
                this.getLlavero().setClaveSimetrica(
                        interlocutor.toStringFull() + Llavero.EXTENSION_CLAVE_SALIENTE, sk);
                this.getLlavero().guardarLlavero(controlador.getUsuario().getNombre());

                // envia los mensajes pendientes
                ArrayList<Mensaje> pendientes = colaDeSalida.get(interlocutor);
                if (pendientes != null)
                {
                    for (Mensaje mensaje : pendientes)
                    {
                        MensajeCifrado mcifrado = new MensajeCifrado(mensaje, sk);
                        appScribe.enviaMensaje(mcifrado, interlocutor, true);
                    }

                    colaDeSalida.remove(interlocutor);
                }

                // cancela el observador, ya teiene la clave
                if (obs != null)
                {
                    obs.cancel();
                } else
                {
                    if (logger.level <= Logger.WARNING) logger.log(
                            "No se pudo obtener el observador asociado a esta peticion de clave.");
                }

                this.getLlavero().guardarLlavero(controlador.getUsuario().getNombre());
            } else if (obs != null)
            {
                if (logger.level <= Logger.WARNING) logger.log(
                        "Error al obtener la clave simetrica de la nueva conversacion. Reintento " + obs
                                .getIntento() + ". Timeout");

                if (obs.getIntento() < 10)
                {
                    obs.setIntento(obs.getIntento() + 1);
                    JsonObject contenidoJson = Json.createObjectBuilder()
                                                   .add("algoritmo", ALGORITMO_CLAVE_SIMETRICA)
                                                   .add("nueva", false)
                                                   .build();

                    MensajeCifrado sobre = new MensajeCifrado(node.getId(), contenidoJson.toString(),
                            Mensaje.MEDIUM_PRIORITY, Mensaje.INDIVIDUAL_PETICION_INICIO_CHAT);
                    appScribe.enviaMensaje(sobre, interlocutor, true);
                    observadoresClave.put(interlocutor, obs);
                } else
                {
                    if (logger.level <= Logger.WARNING) logger.log(
                            "Alcanzado maximo de reintentos, el otro extremo no responde.");
                }

            } else
            {
                if (logger.level <= Logger.WARNING) logger.log(
                        "Error al procesar el evento de notificacion de clave.");
            }

        } catch (Exception e)
        {
            if (logger.level <= Logger.WARNING) logger.logException(
                    "Error al procesar la clave simetrica de la nueva conversacion", e);
        }
    }


    /**
     * Se llama cuando se recibe la respuesta o no se esta interesado mas en ella
     * @param interlocutor
     */
    @Override
    public void cancelarClave(Id interlocutor)
    {
        if(logger.level<=Logger.INFO) logger.log(getClass()+".cancelarClave("+interlocutor+")");

        KeyWithTimeout observable = this.observadoresClave.remove(interlocutor);
        if (observable != null)
        {
            observable.cancel();
            appScribe.cancelarClave(interlocutor);
        }
    }

    /**
     * Metodo que responde a la peticion de clave de sesion entre usuarios.
     * @param interlocutor
     * @param claveCifrada
     */
    @Override
    public void responderSolicitudClave(Id interlocutor, String claveCifrada)
    {
        if(logger.level<=Logger.INFO) logger.log(getClass()+".responderSolicitudClave("+interlocutor+", "+claveCifrada+")");
        JsonObject contenido = Json.createObjectBuilder().add("clave_cifrada", claveCifrada).build();
        if(logger.level<=Logger.INFO) logger.log("contenido = "+contenido);

        MensajeCifrado mcifrado = new MensajeCifrado(this.node.getId(), contenido.toString(),
                Message.LOW_PRIORITY, Mensaje.INDIVIDUAL_RESPUESTA_INICIO_CHAT);
        appScribe.enviaMensaje(mcifrado, interlocutor, true);

    }

    /**
     * Procesa la respuesta a la peticion de clave realizada.
     * @param mensaje
     */
    public void respuestaInicioChatIndividual(MensajeCifrado mensaje)
    {
        if(logger.level<=Logger.INFO) logger.log(getClass()+".respuestaInicioChatIndividual("+mensaje+")");

        // deserializa la clave
        JsonReader jsonReader = Json.createReader(
                new ByteArrayInputStream(mensaje.getContenido().getBytes(StandardCharsets.UTF_8)));
        JsonObject objeto = jsonReader.readObject();

        String clave_cifrada = null;
        try
        {
            clave_cifrada = objeto.getString("clave_cifrada");
        } catch (NullPointerException ignored)
        {
            if (logger.level <= Logger.INFO)
                logger.logException("Error al recibir la clave del inicio de chat " +
                        "individual. No se encontro el campo clave cifrada\n"
                        + mensaje.getContenido(), ignored);
        }

        // si esta bien formado, notifica al observador que ya tiene la clave
        if (clave_cifrada != null)
        {
            notificarClave(0, mensaje.getOrigen(), clave_cifrada);
        }

    }

    /**
     * Recibe un mensaje cifrado y quien lo envio y lo desencripta,
     * independientemente del origen.
     * @param destino
     * @param mcifrado
     * @return
     */
    public Mensaje desencripta(Id destino, MensajeCifrado mcifrado)
    {
        if(logger.level<=Logger.INFO) logger.log(getClass()+".desencripta("+destino+", "+mcifrado+")");
        Mensaje mensaje = null;
        Key sk = null;

        // si destino es null, es porque el mensaje tiene
        // origen en un usuario y no un grupo
        if (destino != null)
        {
            sk = getLlavero().getClaveSimetrica(destino.toStringFull());
        } else
        {
            sk = getLlavero().getClaveSimetrica(mcifrado.getOrigen().toStringFull()
                    + Llavero.EXTENSION_CLAVE_ENTRANTE);
        }

        // si se tiene clave, se desencripta, en otro caso devuelve error
        if (sk != null)
        {
            try
            {
                mensaje = mcifrado.desencripta(sk);
            } catch (Exception e)
            {
                if (logger.level <= Logger.INFO)
                    logger.logException("Error al desencriptar el mensaje", e);
            }
        } else
        {
            if (logger.level <= Logger.INFO) logger.log("La clave simetrica '"
                    + mcifrado.getOrigen().toStringFull()
                    + Llavero.EXTENSION_CLAVE_ENTRANTE + "' no existe");
        }

        return mensaje;
    }


}
