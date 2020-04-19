package us.tfg.p2pmessenger.view;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Scanner;

import javax.json.Json;
import javax.json.JsonObject;

import rice.p2p.commonapi.Id;
import us.tfg.p2pmessenger.controller.ControladorApp;
import us.tfg.p2pmessenger.controller.ControladorConsolaImpl;
import us.tfg.p2pmessenger.controller.GestorFicherosConsola;
import us.tfg.p2pmessenger.model.Contacto;
import us.tfg.p2pmessenger.model.Conversacion;
import us.tfg.p2pmessenger.model.Grupo;
import us.tfg.p2pmessenger.model.Mensaje;

import netscape.javascript.JSObject;

public class VistaConsolaPublic implements Vista
{
    private ControladorApp app;

    public static final Scanner scanner = new Scanner(System.in);

    private boolean esperar;

    private boolean apagar;

    private ArrayList<Conversacion> conversaciones;
    private Conversacion conversacionSeleccionada;

    private int puerto;
    private String ip;

    private boolean notificacionesPendiente;
    
    //Setters & Getters para acceder desde instancias
    public ControladorApp getApp(){
        return this.app;
    }
    public void setApp(ControladorApp paramApp){
        this.app = paramApp;
    }
    public boolean getEsperar(){
        return this.esperar;
    }
    public void setEsperar(boolean paramEsperar){
        this.esperar = paramEsperar;
    }
    public boolean getApagar(){
        return this.apagar;
    }
    public void setApagar(boolean paramApagar){
        this.apagar = paramApagar;
    }

    public ArrayList<Conversacion> getConversaciones(){
        return this.conversaciones;
    }
    public void setConversaciones(ArrayList<Conversacion> paramConversaciones){
        this.conversaciones = paramConversaciones;
    }

    public int getPuerto(){
        return this.puerto;
    }
    public void setPuerto(int paramPuerto){
        this.puerto = paramPuerto;
    }
    
    public String getIp(){
        return this.ip;
    }
    public void setIp(String paramIp){
        this.ip = paramIp;
    }

    public Conversacion getConversacionSeleccionada(){
        return this.conversacionSeleccionada;
    }

    public void setConversacionSeleccionada(Conversacion conversacion){
        this.conversacionSeleccionada = conversacion;
    }

    public boolean getNotificacionesPendientes(){
        return this.notificacionesPendiente;
    }

    public void setNotificacionesPendientes(boolean pendiente){
        this.notificacionesPendiente = pendiente;
    }

    
    //Constructor
    public VistaConsolaPublic(String ip, int puerto)
    {
        this.puerto=puerto;
        this.ip=ip;
        this.notificacionesPendiente = false;
        app = new ControladorConsolaImpl(ip, puerto,this);
    }  
    
    public void listarGrupos()
    {
        ArrayList<Grupo> grupos = app.obtenerGrupos();
        if (grupos != null)
        {
            int i = 0;
            for (Iterator<Grupo> iterator = grupos.iterator(); iterator.hasNext(); i++)
            {
                Grupo grupo = iterator.next();
                System.out.println(i + ") " + grupo);
            }
        } else
        {
            System.out.println("No se pudieron leer los grupos.");
        }
    }

    public boolean compruebaNombre(String usu)
    {
        return this.app.buscaUsuario(usu).estaVacio();
    }

    public void nuevoContacto(String nombreUsu, String alias)
    {
        app.nuevoContacto(nombreUsu,alias);
    }

    public void imprimeListaContactos()
    {
        ArrayList<Contacto> contactos=app.obtenerContactos();
        if(contactos!=null)
        {
            int i=0;
            for (Contacto contacto : contactos)
            {
                System.out.println(i+") "+contacto.toString());
                i++;
            }
        }
        else
            System.out.println("Error al obtener los contactos guardados");
    }

    public void imprimeConversacionesAbiertas()
    {
        this.conversaciones=app.obtenerConversacionesAbiertas();
        if(this.conversaciones!=null)
        {
            int i=0;
            for(Conversacion conv:conversaciones)
            {
                System.out.println(i+++") "+conv.getAlias()+"  oculto[ id="+conv.getId()
                        +" | fecha="+conv.getFecha()+" | msj="+conv.getMensaje()+"]");
            }
        }
    }

    public void abandonaGrupo(Id nombre)
    {
        try
        {
            this.app.abandonaGrupo(nombre.toStringFull());
        }catch (Exception e)
        {
            System.out.println("Error al abandonar el grupo: "+e);
            e.printStackTrace();
        }
    }

    public void modoSesionIniciada()
    {
        try
        {
            boolean continuar = true;
            while (continuar)
            {
                System.out.println("");
                System.out.println("Sesion iniciada con " + app.getUsuario().getNombre() +
                        " con id = " + app.getUsuario().getId().toStringFull());
                System.out.println("0) Crear grupo");
                System.out.println("1) Conectarse a grupo");
                System.out.println("2) Listar grupos");
                System.out.println("3) Actualizar los grupos y empezar a escuchar");
                System.out.println("4) Buscar grupo");
                System.out.println("5) Ping");
                System.out.println("6) Abrir conversacion");
                System.out.println("7) Salir de grupo ");
                System.out.println("8) Ver lista de contacto");
                System.out.println("9) Aniadir contacto");
                System.out.println("10) Comprobar si un nombre de usuario esta en uso");
                System.out.println("11) Obtener claves guardadas en el llavero");
                System.out.println("12) Cambiar politica de pings");
                System.out.println("13) Obtener todos los bloques de mensajes importantes de un usuario");
                System.out.println("14) Obtener todos los id de bloques en cache");
                System.out.println("15) Obtener todos los mensajes importantes para mi");
                System.out.println("16) Unirse a grupo con codigo");

                System.out.println("88) Cerrar sesion");
                System.out.println("99) Salir de la aplicacion");

                int opcion =0;
                if(scanner.hasNextInt())
                    opcion= scanner.nextInt();
                else
                    opcion=-1;
                scanner.nextLine();
                System.out.println("");

                String name = "";
                switch (opcion)
                {
                    case 0:
                        System.out.println("Introduce un nombre para el grupo:");
                        if(scanner.hasNext())
                        {
                            name = scanner.next();
                            app.crearGrupo(name);
                        }
                        break;
                    case 1:
                        System.out.println("Para conectarse a un grupo necesitas el ID:");
                        String id="";
                        if(scanner.hasNext())
                        {
                            id = scanner.next();
                            System.out.println("y la clave privada:");
                            String privatekey = "";
                            if (scanner.hasNextLine())
                            {
                                privatekey = scanner.nextLine();
                                conectaAGrupo(id, privatekey);
                            }
                        }
                        break;

                    case 2:
                        listarGrupos();
                        break;
                    case 3:
                        System.out.println("Actualizando grupos...");
                        app.subscribirYActualizarGrupos();
                        app.getEnv().getTimeSource().sleep(3000);
                        System.out.println("volviendo al menu");
                        break;
                    case 4:
                        System.out.println("Introduzca id del gurpo a buscar: ");
                        String busqueda="";
                        if(scanner.hasNext())
                        {
                            busqueda = scanner.next();
                            app.buscaGrupo(busqueda);
                            app.getEnv().getTimeSource().sleep(4000);
                        }
                        break;
                    case 5:
                        System.out.print("Introduzca destinatario del ping: ");
                        String ping="";
                        if(scanner.hasNext())
                        {
                            ping = scanner.next();
                            System.out.print("Introduzca tiempo entre mensajes: ");
                            int tiempoEntreMensajes = 1;
                            if (scanner.hasNextInt())
                            {
                                tiempoEntreMensajes = scanner.nextInt();

                                System.out.print("Introduzca repeticiones: ");
                                if (scanner.hasNextInt())
                                {
                                    int repeticiones = scanner.nextInt();
                                    for (int i = 0; i < repeticiones; i++)
                                    {
                                        app.ping(ping, 1000);
                                        app.getEnv().getTimeSource().sleep(tiempoEntreMensajes*1000);
                                    }
                                    app.getEnv().getTimeSource().sleep(2000);
                                }
                            }
                        }

                        break;
                    case 6:
                        modoMensajes();
                        break;
                    case 7:
                        listarGrupos();
                        System.out.print("Elija el grupo del que quiere salir: ");
                        if(scanner.hasNextInt())
                        {
                            opcion = scanner.nextInt();
                            scanner.next();
                            ArrayList<Grupo> grupos = app.obtenerGrupos();
                            Grupo eliminar = null;
                            int i = 0;
                            for (Grupo g : grupos)
                            {
                                if (i == opcion)
                                    eliminar = g;
                                i++;
                            }
                            System.out.println("Saliendo del grupo " + eliminar.getId());
                            abandonaGrupo(eliminar.getId());
                        }
                        else
                            opcion=-1;
                        break;
                    case 8:
                        System.out.println("Los contactos guardados son:");
                        imprimeListaContactos();
                        break;
                    case 9:
                        System.out.println("Introduzca el nombre del contacto");
                        String nombreUsu="";
                        if (scanner.hasNext())
                        {
                            nombreUsu = scanner.next();
                            System.out.println("Introduzca el alias del contacto");
                            String alias = "";
                            if (scanner.hasNext())
                            {
                                alias = scanner.next();
                                nuevoContacto(nombreUsu, alias);
                            }
                        }
                        break;
                    case 10:
                        System.out.print("Introduzca nombre para buscar: ");
                        String usu="";
                        if(scanner.hasNext())
                        {
                            usu = scanner.next();
                            if (compruebaNombre(usu))
                            {
                                System.out.println("El nombre " + usu + " esta disponible");
                            } else
                            {
                                System.out.println(
                                        "El nombre " + usu + " existe. ¿Quiere añadirlo como contacto? [true/false]");
                                boolean add=false;
                                if(scanner.hasNextBoolean())
                                {
                                    add = scanner.nextBoolean();
                                    scanner.nextLine();
                                    String alias = "";
                                    System.out.println("Introduzca el alias del contacto");
                                    if (scanner.hasNext())
                                    {
                                        if (add)
                                        {
                                            alias = scanner.nextLine();
                                            nuevoContacto(usu, alias);
                                        }
                                    }
                                }
                            }
                        }
                        break;
                    case 11:
                        try
                        {
                            ArrayList<String> claves = app.obtenerClavesGuardadas();
                            int i = 0;
                            for (String clave : claves)
                            {
                                System.out.println(i + ") " + clave);
                                i++;
                            }
                        } catch (Exception e)
                        {
                            System.out.println("Error al obtener todas las claves: ");
                            e.printStackTrace();
                        }
                        break;
                    case 12:
                        System.out.print("introduzca la politica (true/false): ");
                        if(scanner.hasNextBoolean())
                            app.setPingPolicy(scanner.nextBoolean());
                        break;
                    case 13:
                        System.out.println("introduzca el id: ");
                        if(scanner.hasNext())
                        {
                            id = scanner.next();
                            app.mostrarMensajesImportantes(id);
                        }
                        break;
                    case 14:
                        app.imprimeIdEnCache();
                        break;
                    case 15:
                        app.obtieneMensajesImportantes();
                        break;

                    case 16:
                        System.out.println("Introduce el id y el codigo -> ID-CODIGO");
                        id = "";
                        if(scanner.hasNext())
                        {
                            id = scanner.next();

                            if (!"".equals(id))
                            {
                                String valores[] = id.split("-");
                                if (valores.length == 2)
                                    app.enviarPeticionUnirAGrupo(valores[0], valores[1]);
                                else
                                    System.out.println("Error al enviar la peticion. Argumentos mal formados");
                            }
                        } else
                        {
                            System.out.println("Erro al leer");
                        }
                        break;
                    case 88:
                        System.out.println("Cerrando sesion");
                        app.guardarLlavero();
                        app.cerrarSesion();
                        continuar = false;
                        break;

                    case 99:
                        System.out.println("Saliendo de la aplicacion");
                        app.guardarLlavero();
                        continuar = false;
                        this.apagar = true;
                        break;
                    default:
                        System.out.println("Numero de opcion no valido");
                        break;
                }

            }
        }catch (Exception e)
        {
            scanner.nextLine();
            e.printStackTrace();
        }
    }

    public void modoInicioSesion()
    {
        boolean continuar = true;
        while (continuar)
        {
            System.out.println("");
            System.out.println("1) iniciar sesion");
            System.out.println("2) registrar");
            System.out.println("99) Salir");
            int opcion;
            if(scanner.hasNextInt())
            {
                opcion = scanner.nextInt();
                scanner.nextLine();
                switch (opcion)
                {

                    case 1:
                        try
                        {
                            System.out.print("Nombre de usuario: ");
                            String nombre;
                            if(scanner.hasNext())
                            {
                                nombre = scanner.next();
                                System.out.print("Contraseña: ");
                                // char[] contr = System.console().readPassword("Contraseña: ");
                                String contr;

                                if(scanner.hasNext())
                                {
                                    contr = scanner.next();
                                    JsonObject contenidoJson = Json.createObjectBuilder()
                                                                   .add("usuario", nombre)
                                                                   .add("contrasena", contr)
                                                                   .build();

                                    GestorFicherosConsola gfich = new GestorFicherosConsola();
                                    gfich.escribirAFichero(ControladorConsolaImpl.NOMBRE_FICHERO_USUARIO,
                                            contenidoJson.toString().getBytes(), false);

                                    app.onStart();
                                    if (app.getError() != 0)
                                    {
                                        procesaError();
                                    }
                                    continuar = false;
                                }
                            }
                        } catch (Exception e)
                        {
                            System.out.println(e);
                            e.printStackTrace();
                        }
                        break;
                    case 2:
                        continuar = false;
                        app.setModo(ControladorApp.MODO_REGISTRO);
                        break;

                    case 99:
                        app.onStop();
                        continuar = false;
                        this.apagar = true;
                        break;
                    default:
                        System.out.println("Opcion no valida");
                }
            }
            else
            {
                scanner.nextLine();
                opcion=-1;
            }
        }
    }

    public void modoNuevaDireccion()
    {
        boolean continuar=true;
        while(continuar)
        {
            System.out.println("");
            System.out.println("1) Proporcionar IP");
            System.out.println("2) Inicializar BBDD");
            System.out.println("3) Borrar BBDD");
            System.out.println("99) Salir");
            int opcion;
            if(scanner.hasNextInt())
            {
                opcion = scanner.nextInt();
                scanner.nextLine();
                switch (opcion)
                {

                    case 1:
                        System.out.print("Introduzca IP: ");
                        String ip;
                        if(scanner.hasNext())
                        {
                            ip = scanner.next();
                            System.out.print("Introduzca puerto: ");
                            int port;
                            if(scanner.hasNextInt())
                            {
                                port = scanner.nextInt();
                                scanner.nextLine();
                                try
                                {
                                    app.nuevaDireccionArranque(ip, port);
                                    app.onStart();
                                    if (app.getError() != 0)
                                    {
                                        procesaError();
                                    }
                                    continuar = false;
                                } catch (Exception e)
                                {
                                    e.printStackTrace();
                                }
                            }
                        }
                        break;
                    case 2:
                        app.inicializaBBDD();
                        break;
                    case 3:
                        app.borraBBDD();
                        break;

                    case 99:
                        continuar = false;
                        apagar = true;
                        break;
                    default:
                        System.out.println("Opcion no valida");

                }
            }
        }
    }

    public void modoRegistro()
    {
        boolean disponible = false;
        try
        {
            boolean ok = false;
            while (!ok)
            {
                System.out.print("Insertar nombre: ");

                String nombre;
                if(scanner.hasNext())
                {
                    nombre = scanner.next();
                    disponible = compruebaNombre(nombre);
                    if (disponible)
                    {
                        System.out.println("El usuario '" + nombre + "' esta disponible");
                        System.out.print("Introduzca contraseña para el usuario " + nombre + ": ");
                        // char[] contr = System.console().readPassword(
                        //      "Introduzca contraseña para el usuario "+usuario+": ");
                        String contr;
                        if(scanner.hasNext())
                        {
                            contr = scanner.next();
                            try
                            {
                                app.registrarUsuario(nombre, contr);
                                ok = true;

                            } catch (Exception e)
                            {
                                ok = false;
                                System.out.println("Error al crear un usuario nuevo");
                                e.printStackTrace();
                            }
                        }
                    } else
                    {
                        System.out.println("Nombre de usuario " + nombre + " en uso");
                    }
                }
            }
            app.onStart();
            if(app.getError()!=0)
            {
                procesaError();
            }
        } catch (Exception e)
        {
            System.out.println(e);
            e.printStackTrace();
        }
    }

    public void modoMensajes()
    {
        boolean seguir = true;
        String msg = "";
        Conversacion actual = null;
        String nombreConversacion = "";

        int tipoMsj=0;


        int opcion;

        while (seguir)
        {
            if (actual != null)
            {
                nombreConversacion = actual.getAlias();
            }
            System.out.println("");
            System.out.println("0) Listar conversaciones abiertas");
            System.out.println("1) Seleccionar conversacion a la que dirigirse");
            System.out.println("2) Abrir conversacion "+nombreConversacion);
            System.out.println("3) Escribir mensaje");
            System.out.println("4) Enviar mensaje a la conversacion " + nombreConversacion);
            System.out.println("5) Cerrar la conversacion: " + nombreConversacion);
            System.out.println("6) Nueva conversacion");
            System.out.println("7) Obtiene codigo invitación");
            System.out.println("99) Salir del modo mensajes");


            if(scanner.hasNextInt())
            {
                opcion = scanner.nextInt();
                scanner.nextLine();


                switch (opcion)
                {
                    case 0:
                        //                    grupos=this.p2pmessenger.obtenerBBDD().getGrupos();
                        imprimeConversacionesAbiertas();
                        break;
                    case 1:
                        System.out.print("Seleccione conversacion: ");
                        if(scanner.hasNextInt())
                        {
                            opcion = scanner.nextInt();
                            actual = this.conversaciones.get(opcion);

                            System.out.println("Seleccionado " + actual.getAlias() + " con id " + actual.getId());

                            scanner.nextLine();
                        }
                        else
                            opcion=-1;
                        break;
                    case 2:
                        ArrayList<Mensaje> mensajes = app.obtieneMensajes(
                                actual.getId().toStringFull(), 0, 10, actual.getTipo());
                        if (mensajes == null)
                            procesaError();
                        else
                        {
                            for (Mensaje m : mensajes)
                            {
                                System.out.println(m);
                            }
                        }
                        app.muestraMensajes(actual);
                        break;
                    case 3:
                        System.out.println("Tipos disponibles: 1=importante\n\t2=normal");
                        System.out.println("tipo:");
                        if(scanner.hasNextInt())
                        {
                            tipoMsj = scanner.nextInt();
                            switch (tipoMsj)
                            {
                                case 1:
                                    if(actual.getTipo()==Conversacion.TIPO_INDIVIDUAL)
                                        tipoMsj = Mensaje.INDIVIDUAL_IMPORTANTE;
                                    else
                                        tipoMsj = Mensaje.GRUPO_IMPORTANTE;
                                    break;
                                case 2:
                                    if(actual.getTipo()==Conversacion.TIPO_INDIVIDUAL)
                                        tipoMsj = Mensaje.INDIVIDUAL_NORMAL;
                                    else
                                        tipoMsj = Mensaje.GRUPO_NORMAL;
                                    break;
                            }
                            scanner.nextLine();
                            System.out.println("Mensaje:");
                            if(scanner.hasNextLine())
                                msg = scanner.nextLine();
                        }
                        break;

                    case 4:
                        System.out.println("Enviando...");
                        app.enviaMensaje(tipoMsj, msg, actual.getId(), actual.getTipo() != Conversacion.TIPO_GRUPO);
                        //                    p2pmessenger.enviaMensaje(msg, grupo.getId());
                        break;
                    case 5:
                        actual = null;
                        app.noMuestresMensajes();
                        break;
                    case 6:
                        System.out.println("Para una nueva conversacion seleccione grupo(1) o usuario(2):");
                        int tipoConversacion;
                        if(scanner.hasNextInt())
                        {
                            tipoConversacion = scanner.nextInt();
                            scanner.nextLine();

                            if (tipoConversacion == 1)
                                listarGrupos();
                            else if (tipoConversacion == 2)
                                imprimeListaContactos();

                            System.out.print("introduzca interlocutor: ");
                            int interlocutor;

                            if(scanner.hasNextInt())
                            {
                                interlocutor = scanner.nextInt();

                                if (tipoConversacion == 1)
                                {
                                    ArrayList<Grupo> grupos = app.obtenerGrupos();
                                    if (interlocutor < grupos.size())
                                    {
                                        Grupo g = grupos.get(interlocutor);
                                        actual = app.getConversacion(g.getId());
                                        if (actual == null)
                                        {
                                            System.out.println("Error al obtener las conversaciones");
                                        }
                                    }
                                } else if (tipoConversacion == 2)
                                {
                                    ArrayList<Contacto> contactos = app.obtenerContactos();
                                    if (interlocutor < contactos.size())
                                    {
                                        Contacto c = contactos.get(interlocutor);
                                        actual = new Conversacion(c.getId(), new Date(),
                                                c.getAlias(), Conversacion.TIPO_INDIVIDUAL);
                                        if (!app.iniciarConversacion(c.getId().toStringFull()))
                                        {
                                            System.out.println("Error al iniciar una nueva conversacion");
                                        }
                                    }

                                }
                            }
                        }
                        break;
                    case 7:
                        String algo="";
                        System.out.print("El codigo para unirse es : ");
                        algo = app.obrenerCodigoInvitacion(actual.getId().toStringFull());
                        System.out.println(algo);
                        break;
                    case 99:
                        System.out.println("Saliendo del modo mensajes");
                        seguir = false;
                        break;
                    default:
                        System.out.println("Opcion " + opcion + " no valida");
                        break;

                }
            }
            else
            {
                scanner.nextLine();
                opcion = 3423;
            }
        }
    }

    public void conectaAGrupo(String id,String privateKeyString)
    {
        if (this.app.getModo()== ControladorApp.MODO_SESION_INICIADA)
        {
            this.app.conectaAGrupo(id,privateKeyString);
        }
    }

    @Override
    public void muestraMensaje(Mensaje mensaje,String alias)
    {
        System.out.println(mensaje);
    }

    @Override
    public void notificacion(String origen)
    {
        System.out.println("Notificacion: Nuevo mensaje de "+origen);
        setNotificacionesPendientes(true);           
    }

    @Override
    public void excepcion(Exception e)
    {
        e.printStackTrace();
    }

    public void procesaError()
    {
        switch (app.getError())
        {
            case ERROR_INICIAR_LLAVERO:
                System.out.println("Usuario o contraseña no validos");
                break;
            case ERROR_BASE_DE_DATOS:
            case ERROR_FALTA_DIRECCION_ARRANQUE:
            case ERROR_LEER_USU_PASS:
            case ERROR_CARGAR_USUARIO:
            case ERROR_CREAR_NODO:
            case ERROR_CREAR_ALMACENAMIENTO:
            case ERROR_BOOT_NODO:
                System.out.println("Ha ocurrido un error:");
        }
    }

    public void errorEnviando(int error,String mensaje)
    {
        System.out.println("Enviando: error "+error+" -> "+mensaje);
    }

    @Override
    public void setSeguir(boolean seguir)
    {

    }

    @Override
    public void notificarPing(String respuesta)
    {
        System.out.println(respuesta);
    }

    public void appOnCreateEntorno(){
        this.app.onCreateEntorno();
    }
    public void appOnStart(){
        this.app.onStart();
    }
    public int appGetError(){
        return this.app.getError();
        
    }
    public void appOnStop(){
        this.app.onStop();
    }
    public void appOnDestroy(){
        this.app.onDestroy();
    }
    public int appGetModo()
    {
        return this.app.getModo();
    }
    public boolean appNuevaDireccionArranque(String ip, int port){
        return this.app.nuevaDireccionArranque(ip,port);
    }
    public void appSetModo(int mode){
        System.out.println("APPSETMODO" + Integer.toString(mode));
        this.app.setModo(mode);
    }

    public boolean appRegistrarUsuario(String usuario, String passwd){
        boolean error = false;
        try{
            this.app.registrarUsuario(usuario, passwd);
        }catch(Exception e){
            error = true;
        }
        return error;
        
    }

    public void appGuardarLlavero(){
        this.app.guardarLlavero();
    }

    public void appCerrarSesion(){
        this.app.cerrarSesion();
    }    

    public ArrayList<Contacto> appObtenerContactos(){
        return this.app.obtenerContactos();
    }
    
    public void appNuevoContacto(String usuario, String alias){
        this.app.nuevoContacto(usuario, alias);
    }

    public void appEliminaContacto(String id){
        this.app.eliminaContacto(id);
    }

    public ArrayList<Conversacion> appObtenerConversacionesAbiertas(){
        setConversaciones(this.app.obtenerConversacionesAbiertas());
        return getConversaciones();
    }

    public ArrayList<Mensaje> appObtieneMensajes(String idStringFull, int primerMensaje, int ultimoMensaje, int tipo){
        return this.app.obtieneMensajes(idStringFull, primerMensaje, ultimoMensaje, tipo);
        
    }

    public boolean appIniciarConversacion(String idFull) {
        return app.iniciarConversacion(idFull);
    }

    public void appEliminarConversacion(String id){
        System.out.println("Llamando a this.app.eliminaconversacion");
        this.app.eliminaConversacion(id);    
    }

    public void appEnviarMensaje(String mensaje){
        int tipoMensaje;
        if(getConversacionSeleccionada().getTipo() != Conversacion.TIPO_GRUPO){
            tipoMensaje = Mensaje.INDIVIDUAL_NORMAL;
        }else{
            tipoMensaje = Mensaje.GRUPO_NORMAL;
        }
        app.enviaMensaje(tipoMensaje, mensaje, getConversacionSeleccionada().getId(), getConversacionSeleccionada().getTipo() != Conversacion.TIPO_GRUPO);
    }
    
    public boolean checkNotificacionesPendientes(){
        boolean hayNotificaciones = getNotificacionesPendientes();
        if(hayNotificaciones){
            setNotificacionesPendientes(false);
        }
        return hayNotificaciones;
    }
    

}
