package us.tfg.p2pmessenger.controller;

import java.util.Timer;
import java.util.TimerTask;

import rice.p2p.commonapi.Id;
import us.tfg.p2pmessenger.model.Mensaje;

/**
 * Clase para tener el observador al pedir la clave de sesion a
 * otro usuario. Si el temporizador se activa, se avisa con clave
 * obtenida nula. Se avisa con clave nula tras varios reintentos.
 */
public class KeyWithTimeout
{

    /**
     * Tiempo del temporizador
     */
    private final long timeout;

    /**
     * La clave del usuario que esperamos recibir
     */
    private final Id interlocutor;

    /**
     * El objeto al que avisar cuando la clave este disponible
     */
    private ObservadorKey observador;

    /**
     * El objeto temporizador
     */
    private Timer timer;

    /**
     * El numero de reintentos
     */
    private int intento;

    /**
     * Constructor de la clase
     * @param observador a quien avisar con el resultado
     * @param timeout tiempo de espera entre reintentos
     * @param objetivo de quien se espera obtener respuesta
     */
    public KeyWithTimeout(ObservadorKey observador, long timeout,final Id objetivo)
    {

        System.out.println(getClass()+".<init>("+observador+", timeout = "+timeout
                +", "+objetivo.toStringFull()+")");
        this.intento = 0;
        this.observador = observador;
        this.timeout=timeout;
        this.interlocutor=objetivo;
        // Start the timer.

        this.timer = new Timer(true);
        timer.schedule(new TimerTask()
        {
            private KeyWithTimeout observador;

            /**
             * Accion ejecutada al consumirse el temporizador
             */
            @Override
            public void run()
            {
                System.out.println(getClass()+".run()");
                this.observador.observador.notificarClave(Mensaje.CLAVE_SESION_TIMEOUT, objetivo, null);
            }

            /**
             * Inicializacion de las variables
             * @param observador
             * @return
             */
            public TimerTask init(KeyWithTimeout observador)
            {
                System.out.println(getClass()+".init("+observador+")");
                this.observador = observador;
                return this;
            }

        }.init(this), timeout);

    }

    /**
     * Cuando se obtiene la respuesta, se cancela para evitar
     * que siga reenviando peticiones al acabarse el temporizador
     */
    public void cancel()
    {
        System.out.println(getClass()+".cancel()");
        timer.cancel();
    }

    /**
     * Numero de intentos hasta el momento
     * @return
     */
    public int getIntento()
    {
        System.out.println(getClass()+".getIntento()");
        return intento;
    }

    /**
     * Para indicar la cantidad de veces que se debera reintentar contactar
     * @param intento
     * @return
     */
    public KeyWithTimeout setIntento(int intento)
    {
        System.out.println(getClass()+".setIntento("+intento+")");
        timer.schedule(new TimerTask()
        {

            private KeyWithTimeout observador;

            @Override
            public void run()
            {
                System.out.println(getClass()+".run()");
                this.observador.observador.notificarClave(Mensaje.CLAVE_SESION_TIMEOUT, interlocutor, null);
            }

            TimerTask init(KeyWithTimeout observador)
            {
                System.out.println(getClass()+".init("+observador+")");
                this.observador = observador;
                return this;
            }

        }.init(this), timeout);

        this.intento = intento;
        return this;
    }
}