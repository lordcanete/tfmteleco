package us.tfg.p2pmessenger.view;

import us.tfg.p2pmessenger.model.Mensaje;

/**
 * Created by FPiriz on 25/6/17.
 */
public interface Vista
{
    int ERROR_BASE_DE_DATOS = 1;
    int ERROR_FALTA_DIRECCION_ARRANQUE = 2;
    int ERROR_LEER_USU_PASS = 3;
    int ERROR_INICIAR_LLAVERO = 4;
    int ERROR_CARGAR_USUARIO = 5;
    int ERROR_CREAR_NODO = 6;
    int ERROR_CREAR_ALMACENAMIENTO = 7;
    int ERROR_BOOT_NODO = 8;
    int ERROR_INSERCION_NUEVO_GRUPO_EN_PASTRY = 9;
    int ERROR_CREACION_NUEVO_GRUPO = 10;
    int ERROR_INSERCION_NUEVO_GRUPO_EN_BBDD = 11;
    int ERROR_INSERCION_CONVERSACION_EN_BBDD = 12;
    int ERROR_CREACION_GRUPO_CIFRADO = 13;
    int ERROR_SUBSCRIPCION_GRUPO = 14;
    int ERROR_ACTUALIZACION_GRUPO = 15;


    void muestraMensaje(Mensaje mensaje, String alias);

    void errorEnviando(int error,String mensaje);

    void notificacion(String origen);

    void notificarPing(String respuesta);

    void setSeguir(boolean seguir);

    void excepcion(Exception e);
}
